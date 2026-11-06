package org.example.fhirdummy;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import com.sun.net.httpserver.HttpServer;
import org.example.fhirdummy.client.FhirResourceClient;
import org.example.fhirdummy.client.ObservationClient;
import org.example.fhirdummy.configuration.FhirConfiguration;
import org.example.fhirdummy.controller.ObservationController;
import org.hl7.fhir.r4.model.Observation;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ObservationControllerTests {
    private HttpServer server;
    private MockMvc mvc;
    private final AtomicReference<Observation> stored = new AtomicReference<>();
    private final AtomicReference<String> lastMethod = new AtomicReference<>();
    private static final String BODY = """
            {"status":"final","code":"8310-5","value":36.6,"unit":"Cel"}
            """;

    @BeforeEach
    void setUp() throws Exception {
        FhirContext context = new FhirConfiguration().fhirContext();
        context.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/Observation", exchange -> {
            String method = exchange.getRequestMethod();
            lastMethod.set(method);
            int status = 200;
            if (method.equals("POST") || method.equals("PUT")) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                Observation observation = context.newJsonParser().parseResource(Observation.class, body);
                assertEquals("http://loinc.org", observation.getCode().getCodingFirstRep().getSystem());
                assertEquals("8310-5", observation.getCode().getCodingFirstRep().getCode());
                observation.setId("Observation/example");
                stored.set(observation);
                if (method.equals("POST")) {
                    status = 201;
                    exchange.getResponseHeaders().set("Location", "http://127.0.0.1:" + server.getAddress().getPort()
                            + "/Observation/example/_history/1");
                }
            } else if (method.equals("DELETE")) {
                stored.set(null);
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            } else if (stored.get() == null) {
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
                return;
            }
            byte[] body = context.newJsonParser().encodeResourceToString(stored.get()).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/fhir+json");
            exchange.sendResponseHeaders(status, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        server.start();
        var client = context.newRestfulGenericClient("http://127.0.0.1:" + server.getAddress().getPort());
        mvc = MockMvcBuilders.standaloneSetup(new ObservationController(new ObservationClient(client),
                new FhirResourceClient(client), context)).build();
    }

    @AfterEach
    void tearDown() { server.stop(0); }

    @Test
    void completeLifecycle() throws Exception {
        mvc.perform(post("/fhir/observation").contentType("application/json").content(BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/fhir/observation/example"))
                .andExpect(content().contentTypeCompatibleWith("application/fhir+json"))
                .andExpect(jsonPath("$.id").value("example"))
                .andExpect(jsonPath("$.valueQuantity.value").value(36.6));
        mvc.perform(get("/fhir/observation/example"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("final"));
        stored.get().setLanguage("en");
        mvc.perform(put("/fhir/observation/example").contentType("application/json")
                        .content(BODY.replace("36.6", "37.2")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valueQuantity.value").value(37.2))
                .andExpect(jsonPath("$.language").value("en"));
        mvc.perform(delete("/fhir/observation/example")).andExpect(status().isNoContent());
        assertNull(stored.get());
        mvc.perform(get("/fhir/observation/example")).andExpect(status().isNotFound());
    }

    @Test
    void updateMissingResourceDoesNotCreateIt() throws Exception {
        mvc.perform(put("/fhir/observation/missing").contentType("application/json").content(BODY))
                .andExpect(status().isNotFound());
        assertEquals("GET", lastMethod.get());
        assertNull(stored.get());
    }

    @Test
    void rejectsInvalidInputBeforeCallingServer() throws Exception {
        for (String body : new String[]{"{}", BODY.replace("final", "invalid"), BODY.replace("8310-5", " ")}) {
            mvc.perform(post("/fhir/observation").contentType("application/json").content(body))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(get("/fhir/observation/bad_id")).andExpect(status().isBadRequest());
        assertNull(lastMethod.get());
    }
}
