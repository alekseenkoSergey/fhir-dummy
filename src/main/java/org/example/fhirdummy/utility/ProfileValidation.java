package org.example.fhirdummy.utility;

import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.ValidationResult;

import java.util.List;
import java.util.Set;

public final class ProfileValidation {
    public static final String CARDX_CIED_PROFILE_BASE = "http://hl7.org/fhir/uv/cardx-cied/StructureDefinition/";
    public static final String CARDX_CIED_GUIDE =
            "http://hl7.org/fhir/uv/cardx-cied/ImplementationGuide/hl7.fhir.uv.cardx-cied";
    public static final String PATIENT_PROFILE = CARDX_CIED_PROFILE_BASE + "cied-patient";
    public static final String DEVICE_PROFILE = CARDX_CIED_PROFILE_BASE + "cied-device";
    public static final String MONITOR_PROFILE = CARDX_CIED_PROFILE_BASE + "cied-monitor";
    public static final String CONNECTIVITY_PROFILE = CARDX_CIED_PROFILE_BASE + "cied-connectivity-status";

    private ProfileValidation() {}

    public static List<String> errors(ValidationResult result) {
        return messages(result, ResultSeverityEnum.ERROR, ResultSeverityEnum.FATAL);
    }

    private static List<String> messages(ValidationResult result, ResultSeverityEnum... severities) {
        Set<ResultSeverityEnum> wanted = Set.of(severities);
        return result.getMessages().stream()
                .filter(m -> wanted.contains(m.getSeverity()))
                .map(m -> m.getLocationString() + ": " + m.getMessage())
                .toList();
    }
}
