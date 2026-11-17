package org.example.fhirdummy.controller;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.FhirValidator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fhir")
public class ObservationController {
    private static final String FHIR_JSON = "application/fhir+json";

    private final FhirContext context;
    private final FhirValidator validator;

    public ObservationController(
            FhirContext context,
            FhirValidator validator
    ) {
        this.context = context;
        this.validator = validator;
    }

    @PostMapping(value = "/observation/validate", consumes = {"application/json", FHIR_JSON},
            produces = "application/json")
    public ResponseEntity<ProfileValidation.Response> validate(@RequestBody String body) {
        return ProfileValidation.validate(validator, body, ProfileValidation.CONNECTIVITY_PROFILE);
    }
}
