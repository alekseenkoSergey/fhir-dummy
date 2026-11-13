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
    static final String CARDX_CIED_PROFILE_BASE = "http://hl7.org/fhir/uv/cardx-cied/StructureDefinition/";
    static final String CARDX_CIED_GUIDE =
            "http://hl7.org/fhir/uv/cardx-cied/ImplementationGuide/hl7.fhir.uv.cardx-cied";
    static final String PATIENT_PROFILE = CARDX_CIED_PROFILE_BASE + "cied-patient";
    static final String DEVICE_PROFILE = CARDX_CIED_PROFILE_BASE + "cied-device";
    static final String MONITOR_PROFILE = CARDX_CIED_PROFILE_BASE + "cied-monitor";
    static final String CONNECTIVITY_PROFILE = CARDX_CIED_PROFILE_BASE + "cied-connectivity-status";

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
