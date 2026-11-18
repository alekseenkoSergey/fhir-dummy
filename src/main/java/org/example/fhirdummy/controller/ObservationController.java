package org.example.fhirdummy.controller;

import ca.uhn.fhir.context.FhirContext;
import org.example.fhirdummy.service.ObservationService;
import org.hl7.fhir.r5.model.OperationOutcome;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/fhir")
public class ObservationController {
    private static final String FHIR_JSON = "application/fhir+json";
    private static final MediaType FHIR_JSON_MEDIA_TYPE = MediaType.valueOf(FHIR_JSON);

    private final FhirContext context;
    private final ObservationService observationService;

    public ObservationController(FhirContext context, ObservationService observationService) {
        this.context = context;
        this.observationService = observationService;
    }

    @GetMapping(value = "/Observation", produces = FHIR_JSON)
    public ResponseEntity<String> searchConnectivity(@RequestParam String clinicId) {
        try {
            var bundle = observationService.getConnectivityBundle(clinicId);
            return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                    .contentType(FHIR_JSON_MEDIA_TYPE).body(context.newJsonParser().encodeResourceToString(bundle));
        } catch (ResponseStatusException exception) {
            var code = exception.getStatusCode().value() == HttpStatus.NOT_FOUND.value()
                    ? OperationOutcome.IssueType.NOTFOUND
                    : OperationOutcome.IssueType.TRANSIENT;
            return outcome(exception.getStatusCode(), code, exception.getReason());
        }
    }

    private ResponseEntity<String> outcome(HttpStatusCode status, OperationOutcome.IssueType code, String diagnostics) {
        var outcome = new OperationOutcome();
        outcome.addIssue().setSeverity(OperationOutcome.IssueSeverity.ERROR).setCode(code).setDiagnostics(diagnostics);
        return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).contentType(FHIR_JSON_MEDIA_TYPE)
                .body(context.newJsonParser().encodeResourceToString(outcome));
    }
}
