package org.example.fhirdummy.controller;

import ca.uhn.fhir.context.FhirContext;
import java.util.Set;
import java.util.TreeSet;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

/**
 * Exposes the FHIR Observation search endpoint for a clinic's connectivity data.
 * It checks the requested include values, returns the search bundle as FHIR JSON,
 * and represents unsupported requests or service failures as OperationOutcome resources.
 */
@RestController
@RequestMapping("/fhir")
public class CiedObservationController {
    private static final String FHIR_JSON = "application/fhir+json";
    private static final MediaType FHIR_JSON_MEDIA_TYPE = MediaType.valueOf(FHIR_JSON);

    private final FhirContext context;
    private final ObservationService observationService;

    public CiedObservationController(FhirContext context, ObservationService observationService) {
        this.context = context;
        this.observationService = observationService;
    }

    @GetMapping(value = "/Observation", produces = FHIR_JSON)
    public ResponseEntity<String> searchConnectivity(
            @RequestParam String clinicId,
            @RequestParam(name = "_include", required = false, defaultValue = "") Set<String> include
    ) {
        if (!ObservationService.SUPPORTED_INCLUDES.containsAll(include)) {
            return outcome(HttpStatus.BAD_REQUEST, OperationOutcome.IssueType.NOTSUPPORTED,
                    "Supported _include values: " + String.join(", ", new TreeSet<>(ObservationService.SUPPORTED_INCLUDES)));
        }
        try {
            var bundle = observationService.getConnectivityBundle(clinicId, include);
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .contentType(FHIR_JSON_MEDIA_TYPE)
                    .body(context.newJsonParser().encodeResourceToString(bundle));
        } catch (ResponseStatusException exception) {
            var code = exception.getStatusCode().value() == HttpStatus.NOT_FOUND.value()
                    ? OperationOutcome.IssueType.NOTFOUND
                    : OperationOutcome.IssueType.TRANSIENT;
            return outcome(exception.getStatusCode(), code, exception.getReason());
        }
    }

    @GetMapping(value = "/Observation", params = {"clinicId", "patientId"}, produces = FHIR_JSON)
    @Operation(summary = "Search CIED connectivity status Observations for a patient of a clinic")
    public ResponseEntity<String> searchPatientConnectivity(
            @Parameter(description = "Clinic identifier", example = "clinic1")
            @RequestParam String clinicId,
            @Parameter(description = "Patient identifier", example = "858024995")
            @RequestParam String patientId,
            @Parameter(
                    description = "Resources to include in the bundle along with Observations. "
                            + "`Observation:subject` - the Patient; "
                            + "`Observation:device` - the monitor (transmitter) Device; "
                            + "`Observation:cied-device` - the CIED Device."
            )
            @RequestParam(name = "_include", required = false, defaultValue = "") Set<String> include
    ) {
        if (!ObservationService.SUPPORTED_INCLUDES.containsAll(include)) {
            return outcome(HttpStatus.BAD_REQUEST, OperationOutcome.IssueType.NOTSUPPORTED,
                    "Supported _include values: " +
                            String.join(", ", new TreeSet<>(ObservationService.SUPPORTED_INCLUDES)));
        }
        try {
            var bundle = observationService.getPatientConnectivityBundle(clinicId, patientId, include);
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .contentType(FHIR_JSON_MEDIA_TYPE)
                    .body(context.newJsonParser().encodeResourceToString(bundle));
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
