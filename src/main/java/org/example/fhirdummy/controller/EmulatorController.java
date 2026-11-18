package org.example.fhirdummy.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EmulatorController {
    private static final String PATIENT_FIXTURE_PATH = "example/fhir-data-patient.json";
    private static final String CLINIC_FIXTURE_PATH = "example/fhir-data-clinic.json";

    private final Resource patientFixture = fixture(PATIENT_FIXTURE_PATH);
    private final Resource clinicFixture = fixture(CLINIC_FIXTURE_PATH);

    private static Resource fixture(String path) {
        var sourceFile = new FileSystemResource("src/main/resources/" + path);
        return sourceFile.exists() ? sourceFile : new ClassPathResource(path);
    }

    @GetMapping(value = "/connectivity/customer/{customerId}/patients/{patientId}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public String getPatient() throws IOException {
        return patientFixture.getContentAsString(StandardCharsets.UTF_8);
    }

    @GetMapping(value = "/connectivity/customer/{customerId}/devices",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public String getDeviceConnectivity() throws IOException {
        return clinicFixture.getContentAsString(StandardCharsets.UTF_8);
    }
}
