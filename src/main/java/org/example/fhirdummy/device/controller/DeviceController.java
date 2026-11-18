package org.example.fhirdummy.device.controller;

import ca.uhn.fhir.context.FhirContext;

import java.util.regex.Pattern;

import org.example.fhirdummy.utility.ProfileValidation;
import org.example.fhirdummy.device.service.DeviceService;
import org.hl7.fhir.r5.model.Device;
import org.hl7.fhir.r5.model.OperationOutcome;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/fhir")
public class DeviceController {
    private static final String FHIR_JSON = "application/fhir+json";
    private static final MediaType FHIR_JSON_MEDIA_TYPE = MediaType.valueOf(FHIR_JSON);
    private static final Pattern COMPOSITE_ID = Pattern.compile("[^\\s:/]+:[^\\s:/]+");

    private final FhirContext context;
    private final DeviceService deviceService;

    public DeviceController(FhirContext context, DeviceService deviceService) {
        this.context = context;
        this.deviceService = deviceService;
    }

    @GetMapping(value = "/Device/{id}", produces = FHIR_JSON)
    public ResponseEntity<String> getDevice(
            @PathVariable String id,
            @RequestParam(name = "_profile", defaultValue = ProfileValidation.DEVICE_PROFILE) String profile
    ) {
        if (!COMPOSITE_ID.matcher(id).matches()) {
            return outcome(HttpStatus.BAD_REQUEST, OperationOutcome.IssueType.INVALID,
                    "Expected a composite key in the form clinicId:patientId.");
        }
        if (!ProfileValidation.DEVICE_PROFILE.equals(profile)) {
            return outcome(HttpStatus.BAD_REQUEST, OperationOutcome.IssueType.INVALID,
                    "Expected _profile=" + ProfileValidation.DEVICE_PROFILE);
        }

        try {
            String[] ids = id.split(":", 2);
            Device device = deviceService.getCiedDevice(ids[0], ids[1]);
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
