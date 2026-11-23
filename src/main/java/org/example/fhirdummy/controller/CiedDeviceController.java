package org.example.fhirdummy.controller;

import ca.uhn.fhir.context.FhirContext;
import java.util.regex.Pattern;
import org.example.fhirdummy.service.CiedDeviceService;
import static org.example.fhirdummy.utility.Constants.ID_DELIMITER;
import org.example.fhirdummy.utility.ProfileValidation;
import org.hl7.fhir.r5.model.Device;
import org.hl7.fhir.r5.model.OperationOutcome;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * Exposes the FHIR Device read endpoint using a composite clinic, patient, and device identifier.
 * It checks the identifier and requested profile, delegates retrieval to the device service,
 * and returns FHIR JSON with OperationOutcome resources for request or service errors.
 */
@RestController
@RequestMapping("/fhir")
public class CiedDeviceController {
    private static final String FHIR_JSON = "application/fhir+json";
    private static final MediaType FHIR_JSON_MEDIA_TYPE = MediaType.valueOf(FHIR_JSON);
    private static final Pattern COMPOSITE_ID = Pattern.compile("[A-Za-z0-9.]+"
            + Pattern.quote(ID_DELIMITER) + "[A-Za-z0-9.]+"
            + Pattern.quote(ID_DELIMITER) + "[A-Za-z0-9.]+");

    private final FhirContext context;
    private final CiedDeviceService ciedDeviceService;

    public CiedDeviceController(FhirContext context, CiedDeviceService ciedDeviceService) {
        this.context = context;
        this.ciedDeviceService = ciedDeviceService;
    }

    @GetMapping(value = "/Device/{id}", produces = FHIR_JSON)
    public ResponseEntity<String> getDevice(
            @PathVariable String id,
            @RequestParam(name = "_profile", defaultValue = ProfileValidation.DEVICE_PROFILE) String profile
    ) {
        if (!COMPOSITE_ID.matcher(id).matches()) {
            return outcome(HttpStatus.BAD_REQUEST, OperationOutcome.IssueType.INVALID,
                    "Expected a composite key in the form clinicId" + ID_DELIMITER
                            + "patientId" + ID_DELIMITER + "deviceId.");
        }
        if (!ProfileValidation.DEVICE_PROFILE.equals(profile)) {
            return outcome(HttpStatus.BAD_REQUEST, OperationOutcome.IssueType.INVALID,
                    "Expected _profile=" + ProfileValidation.DEVICE_PROFILE);
        }

        try {
            String[] ids = id.split(Pattern.quote(ID_DELIMITER), 3);
            Device device = ciedDeviceService.getCiedDevice(ids[0], ids[1], ids[2]);
            return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                    .contentType(FHIR_JSON_MEDIA_TYPE).body(context.newJsonParser().encodeResourceToString(device));
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
