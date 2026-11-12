package org.example.fhirdummy.controller;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.ValidationOptions;
import ca.uhn.fhir.validation.ValidationResult;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Set;

public final class ProfileValidation {
    static final String IG1_PROFILE_BASE = "http://example.org/fhir/ig1/StructureDefinition/";

    public record Response(boolean conforms, List<String> errors, List<String> warnings) {}

    private ProfileValidation() {}

    static ResponseEntity<Response> validate(FhirValidator validator, String body, String profileUrl) {
        ValidationResult result;
        try {
            result = validator.validateWithResult(body, new ValidationOptions().addProfile(profileUrl));
        } catch (InvalidRequestException e) {
            return ResponseEntity.badRequest()
                    .body(new Response(false, List.of(e.getMessage()), List.of()));
        }

        Response response = new Response(
                result.isSuccessful(),
                messages(result, ResultSeverityEnum.ERROR, ResultSeverityEnum.FATAL),
                messages(result, ResultSeverityEnum.WARNING)
        );

        return ResponseEntity.status(result.isSuccessful() ? 200 : 422).body(response);
    }

    private static List<String> messages(ValidationResult result, ResultSeverityEnum... severities) {
        Set<ResultSeverityEnum> wanted = Set.of(severities);
        return result.getMessages().stream()
                .filter(m -> wanted.contains(m.getSeverity()))
                .map(m -> m.getLocationString() + ": " + m.getMessage())
                .toList();
    }
}
