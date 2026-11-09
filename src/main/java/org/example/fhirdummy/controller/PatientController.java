package org.example.fhirdummy.controller;

import ca.uhn.fhir.context.FhirContext;
import org.example.fhirdummy.client.FhirResourceClient;
import org.example.fhirdummy.client.PatientClient;
import org.example.fhirdummy.dto.PatientRequest;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/fhir/patient")
public class PatientController {
    private final PatientClient client;
    private final FhirResourceClient resourceClient;
    private final FhirContext context;

    public PatientController(PatientClient client, FhirResourceClient resourceClient,
                                 FhirContext context) {
        this.client = client;
        this.resourceClient = resourceClient;
        this.context = context;
    }

    @GetMapping(produces = "application/fhir+json")
    public String getExample() {
        return json(resourceClient.getExampleResource(Patient.class));
    }

    @PostMapping(consumes = "application/json", produces = "application/fhir+json")
    public ResponseEntity<String> create(@RequestBody PatientRequest request) {
        Patient patient = client.create(request);
        return ResponseEntity.created(URI.create("/fhir/patient/" + patient.getIdElement().getIdPart()))
                .body(json(patient));
    }

    @GetMapping(value = "/{id}", produces = "application/fhir+json")
    public String get(@PathVariable String id) {
        return json(client.get(id));
    }

    @PutMapping(value = "/{id}", consumes = "application/json", produces = "application/fhir+json")
    public String update(@PathVariable String id, @RequestBody PatientRequest request) {
        return json(client.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        client.delete(id);
    }

    private String json(Patient patient) {
        return context.newJsonParser().encodeResourceToString(patient);
    }
}
