package org.example.fhirdummy;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.FhirValidator;
import org.example.fhirdummy.client.DeviceClient;
import org.example.fhirdummy.client.FhirResourceClient;
import org.example.fhirdummy.client.PatientClient;
import org.example.fhirdummy.configuration.FhirConfiguration;
import org.example.fhirdummy.controller.DeviceController;
import org.example.fhirdummy.controller.PatientController;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ValidateEndpointTests {
    private static final String CARDX = "http://hl7.org/fhir/uv/cardx-cied/CodeSystem/CardXCIED";
    private static final String PATIENT = """
            {
              "resourceType": "Patient",
              "identifier": [{"system": "http://example.org/fhir/ig1/NamingSystem/merlin-net-number", "value": "123456"}],
              "active": true,
              "name": [{"family": "Doe", "given": ["John", "Q"]}],
              "gender": "male",
              "telecom": [{"system": "phone", "use": "mobile", "value": "+15551234567"}],
              "birthDate": "1980-05-17"
            }
            """;
    private static final String DEVICE = """
            {
              "resourceType": "Device",
              "identifier": [{"system": "http://example.org/fhir/ig1/NamingSystem/patient-device-id", "value": "42"}],
              "status": "active",
              "manufacturer": "Abbott",
              "serialNumber": "SN-1",
              "modelNumber": "M-1",
              "type": [{"coding": [{"system": "urn:iso:std:iso:11073:10101", "code": "753666"}]}]
            }
            """;
    private static final String MONITOR = """
            {
              "resourceType": "Device",
              "identifier": [{"system": "http://example.org/fhir/ig1/NamingSystem/patient-device-id", "value": "43"}],
              "status": "active",
              "manufacturer": "Abbott",
              "serialNumber": "SN-2",
              "modelNumber": "M-2",
              "type": [{"coding": [{"system": "%s", "code": "bedside-device"}]}]
            }
            """.formatted(CARDX);

    private static MockMvc mvc;

    @BeforeAll
    static void setUp() throws Exception {
        FhirConfiguration config = new FhirConfiguration();
        FhirContext context = config.fhirContext();
        FhirValidator validator = config.fhirValidator(context, "ig1/package.tgz", "cardx-cied/package.tgz");
        var client = context.newRestfulGenericClient("http://127.0.0.1:1");
        var resourceClient = new FhirResourceClient(client);
        mvc = MockMvcBuilders.standaloneSetup(
                new PatientController(new PatientClient(client), resourceClient, context, validator),
                new DeviceController(new DeviceClient(client), resourceClient, context, validator)
        ).build();
    }

    private static org.springframework.test.web.servlet.ResultActions validate(String path, String body)
            throws Exception {
        return mvc.perform(post(path).contentType("application/json").content(body));
    }

    @Test
    void patientConforming() throws Exception {
        validate("/fhir/patient/validate", PATIENT)
                .andExpect(status().isOk()).andExpect(jsonPath("$.conforms").value(true))
                .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    void patientViolatingProfileIsRejected() throws Exception {
        validate("/fhir/patient/validate", PATIENT.replace("1980-05-17", "1980-05"))
                .andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.conforms").value(false))
                .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    @Test
    void patientMissingMiddleNameOrPhoneIsRejected() throws Exception {
        validate("/fhir/patient/validate", PATIENT.replace("[\"John\", \"Q\"]", "[\"John\"]"))
                .andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.errors").isNotEmpty());
        validate("/fhir/patient/validate", PATIENT.replaceAll("(?s)\"telecom\".*?\\],\\s*", ""))
                .andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.errors").isNotEmpty());
    }

    @Test
    void deviceAndMonitorConforming() throws Exception {
        validate("/fhir/device/validate", DEVICE)
                .andExpect(status().isOk()).andExpect(jsonPath("$.conforms").value(true));
        validate("/fhir/device/validate/monitor", MONITOR)
                .andExpect(status().isOk()).andExpect(jsonPath("$.conforms").value(true));
    }

    @Test
    void deviceValidatedAsMonitorIsRejected() throws Exception {
        validate("/fhir/device/validate/monitor", DEVICE)
                .andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.conforms").value(false));
    }

    @Test
    void deviceMissingSerialNumberIsRejected() throws Exception {
        validate("/fhir/device/validate", DEVICE.replace("\"serialNumber\": \"SN-1\",", ""))
                .andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.errors").isNotEmpty());
    }

    @Test
    void invalidStatusCodeIsReportedAsValidationError() throws Exception {
        validate("/fhir/device/validate", DEVICE.replace("\"active\"", "\"some status\""))
                .andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.conforms").value(false))
                .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    @Test
    void unparseableBodyIsBadRequestWithErrorBody() throws Exception {
        validate("/fhir/patient/validate", "not json")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.conforms").value(false))
                .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    @Test
    void resourceOfWrongTypeIsRejected() throws Exception {
        validate("/fhir/patient/validate", DEVICE)
                .andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.conforms").value(false));
    }
}
