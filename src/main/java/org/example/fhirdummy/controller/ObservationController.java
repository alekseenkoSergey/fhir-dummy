package org.example.fhirdummy.controller;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.FhirValidator;
import org.example.fhirdummy.client.FhirResourceClient;
import org.hl7.fhir.r5.model.Observation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/fhir")
public class ObservationController {
    private static final String FHIR_JSON = "application/fhir+json";

    private final FhirResourceClient resourceClient;
    private final FhirContext context;
    private final FhirValidator validator;

    public ObservationController(
            FhirResourceClient resourceClient,
            FhirContext context,
            FhirValidator validator
    ) {
        this.resourceClient = resourceClient;
        this.context = context;
        this.validator = validator;
    }

    @GetMapping(value = "/observation_public_example", produces = FHIR_JSON)
    public String getExample() {
        Observation observation = resourceClient.getExampleResource(Observation.class);
        return context.newJsonParser().encodeResourceToString(observation);
    }

    @PostMapping(value = "/observation/validate", consumes = {"application/json", FHIR_JSON},
            produces = "application/json")
    public ResponseEntity<ProfileValidation.Response> validate(@RequestBody String body) {
        return ProfileValidation.validate(validator, body, ProfileValidation.CONNECTIVITY_PROFILE);
    }
}
