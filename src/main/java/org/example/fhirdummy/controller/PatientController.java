package org.example.fhirdummy.controller;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.FhirValidator;
import org.example.fhirdummy.client.FhirResourceClient;
import org.example.fhirdummy.client.PatientClient;
import org.example.fhirdummy.dto.PatientRequest;
import org.hl7.fhir.r5.model.Patient;
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
    private final FhirValidator validator;
    private static final String PROFILE = ProfileValidation.IG1_PROFILE_BASE + "ig1-patient";

    public PatientController(PatientClient client, FhirResourceClient resourceClient,
                                 FhirContext context, FhirValidator validator) {
        this.client = client;
        this.resourceClient = resourceClient;
        this.context = context;
        this.validator = validator;
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

    @PostMapping(value = "/validate", consumes = {"application/json", "application/fhir+json"},
            produces = "application/json")
    public ResponseEntity<ProfileValidation.Response> validate(@RequestBody String body) {
        return ProfileValidation.validate(validator, body, PROFILE);
    }

    private String json(Patient patient) {
        return context.newJsonParser().encodeResourceToString(patient);
    }
}
