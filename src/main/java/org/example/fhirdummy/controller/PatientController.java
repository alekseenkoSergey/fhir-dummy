package org.example.fhirdummy.controller;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.FhirValidator;
import java.util.regex.Pattern;
import org.example.fhirdummy.service.PatientService;
import org.hl7.fhir.r5.model.OperationOutcome;
import org.hl7.fhir.r5.model.Patient;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/fhir")
public class PatientController {
    private static final String FHIR_JSON = "application/fhir+json";
    private static final MediaType FHIR_JSON_MEDIA_TYPE = MediaType.valueOf(FHIR_JSON);
    private static final Pattern COMPOSITE_ID = Pattern.compile("[^\\s:/]+:[^\\s:/]+");

    private final FhirContext context;
    private final FhirValidator validator;
    private final PatientService patientService;

    public PatientController(
            FhirContext context,
            FhirValidator validator,
            PatientService patientService
    ) {
        this.context = context;
        this.validator = validator;
        this.patientService = patientService;
    }

    @GetMapping(value = "/Patient/{id}", produces = FHIR_JSON)
    public ResponseEntity<String> getPatient(
            @PathVariable String id,
            @RequestParam(name = "_profile", defaultValue = ProfileValidation.PATIENT_PROFILE)
            String profile
    ) {
        if (!COMPOSITE_ID.matcher(id).matches()) {
            return outcome(HttpStatus.BAD_REQUEST, OperationOutcome.IssueType.INVALID,
                    "Expected a composite key in the form clinicId:patientId.");
        }
        if (!ProfileValidation.PATIENT_PROFILE.equals(profile)) {
            return outcome(HttpStatus.BAD_REQUEST, OperationOutcome.IssueType.INVALID,
                    "Expected _profile=" + ProfileValidation.PATIENT_PROFILE);
        }

        Patient patient;
        try {
            String[] ids = id.split(":", 2);
            patient = patientService.getPatient(ids[0], ids[1]);
        } catch (ResponseStatusException exception) {
            var code = exception.getStatusCode().value() == HttpStatus.NOT_FOUND.value()
                    ? OperationOutcome.IssueType.NOTFOUND
                    : OperationOutcome.IssueType.TRANSIENT;
            return outcome(exception.getStatusCode(), code, exception.getReason());
        }

        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .contentType(FHIR_JSON_MEDIA_TYPE).body(context.newJsonParser().encodeResourceToString(patient));
    }

    @PostMapping(value = "/patient/validate", consumes = {"application/json", FHIR_JSON},
            produces = "application/json")
    public ResponseEntity<ProfileValidation.Response> validate(@RequestBody String body) {
        return ProfileValidation.validate(validator, body, ProfileValidation.PATIENT_PROFILE);
    }

    private ResponseEntity<String> outcome(
            HttpStatusCode status,
            OperationOutcome.IssueType code,
            String diagnostics
    ) {
        OperationOutcome outcome = new OperationOutcome();
        outcome.addIssue().setSeverity(OperationOutcome.IssueSeverity.ERROR)
                .setCode(code).setDiagnostics(diagnostics);
        return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).contentType(FHIR_JSON_MEDIA_TYPE)
                .body(context.newJsonParser().encodeResourceToString(outcome));
    }
}
