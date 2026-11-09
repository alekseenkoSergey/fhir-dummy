package org.example.fhirdummy;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.validation.FhirValidator;
import com.sun.net.httpserver.HttpServer;
import org.example.fhirdummy.client.FhirResourceClient;
import org.example.fhirdummy.client.ObservationClient;
import org.example.fhirdummy.configuration.FhirConfiguration;
import org.example.fhirdummy.controller.FhirController;
import org.example.fhirdummy.controller.ObservationController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FhirControllerTests {
    private HttpServer server;
    private MockMvc mvc;
    private int status = 200;
    private String response;
    private final AtomicReference<String> request = new AtomicReference<>();

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            request.set(exchange.getRequestURI().toString());
            byte[] body = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/fhir+json");
            exchange.sendResponseHeaders(status, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.start();
        FhirContext context = new FhirConfiguration().fhirContext();
        context.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
        var client = context.newRestfulGenericClient("http://127.0.0.1:" + server.getAddress().getPort());
        mvc = MockMvcBuilders.standaloneSetup(
                new FhirController(new FhirResourceClient(client), context),
                new ObservationController(
                        new ObservationClient(client),
                        new FhirResourceClient(client),
                        context,
                        mock(FhirValidator.class)
                )
        ).build();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void returnsPatientAsFhirJson() throws Exception {
        response = """
                {"resourceType":"Bundle","type":"searchset","entry":[
                  {"resource":{"resourceType":"Patient","id":"example","name":[{"family":"Demo"}]}}
                ]}
                """;
        mvc.perform(get("/fhir/patient"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/fhir+json"))
                .andExpect(jsonPath("$.resourceType").value("Patient"))
                .andExpect(jsonPath("$.id").value("example"))
                .andExpect(jsonPath("$.name[0].family").value("Demo"));
        assertEquals("/Patient?_count=1", request.get());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Observation", "Condition", "Encounter", "Organization", "Practitioner"})
    void returnsRequestedResourceAsFhirJson(String resourceType) throws Exception {
        response = """
                {"resourceType":"Bundle","type":"searchset","entry":[
                  {"resource":{"resourceType":"%s","id":"example"}}
                ]}
                """.formatted(resourceType);
        mvc.perform(get("/fhir/" + resourceType.toLowerCase(java.util.Locale.ROOT)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/fhir+json"))
                .andExpect(jsonPath("$.resourceType").value(resourceType))
                .andExpect(jsonPath("$.id").value("example"));
        assertEquals("/" + resourceType + "?_count=1", request.get());
    }

    @Test
    void returnsNotFoundForEmptySearch() throws Exception {
        response = """
                {"resourceType":"Bundle","type":"searchset","entry":[]}
                """;
        mvc.perform(get("/fhir/patient")).andExpect(status().isNotFound());
    }

    @Test
    void returnsBadGatewayWhenServerFails() throws Exception {
        status = 503;
        response = """
                {"resourceType":"OperationOutcome","issue":[
                  {"severity":"error","code":"transient"}
                ]}
                """;
        mvc.perform(get("/fhir/patient")).andExpect(status().isBadGateway());
    }
}
