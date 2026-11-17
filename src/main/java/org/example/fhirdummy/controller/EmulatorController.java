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
    private static final String FIXTURE_PATH = "example/fhir-data-patient.json";

    private final Resource fixture;

    public EmulatorController() {
        var sourceFile = new FileSystemResource("src/main/resources/" + FIXTURE_PATH);
        fixture = sourceFile.exists() ? sourceFile : new ClassPathResource(FIXTURE_PATH);
    }

    @GetMapping(value = "/connectivity/customer/{customerId}/patients/{patientId}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public String getPatient() throws IOException {
        return fixture.getContentAsString(StandardCharsets.UTF_8);
    }
}
