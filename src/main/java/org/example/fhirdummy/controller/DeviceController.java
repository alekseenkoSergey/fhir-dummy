package org.example.fhirdummy.controller;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.FhirValidator;
import java.util.regex.Pattern;
import org.example.fhirdummy.client.FhirResourceClient;
import org.example.fhirdummy.service.DeviceService;
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

    private final FhirResourceClient resourceClient;
    private final FhirContext context;
    private final FhirValidator validator;
    private final DeviceService deviceService;

    public DeviceController(
            FhirResourceClient resourceClient,
            FhirContext context,
            FhirValidator validator,
            DeviceService deviceService
    ) {
        this.resourceClient = resourceClient;
        this.context = context;
        this.validator = validator;
        this.deviceService = deviceService;
    }

    @GetMapping(value = "/device_public_example", produces = FHIR_JSON)
    public String getExample() {
        Device device = resourceClient.getExampleResource(Device.class);
        return context.newJsonParser().encodeResourceToString(device);
    }

    @GetMapping(value = "/Device/{id}", produces = FHIR_JSON)
    public ResponseEntity<String> getCiedDevice(
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

    @PostMapping(value = "/device/validate", consumes = {"application/json", FHIR_JSON},
            produces = "application/json")
    public ResponseEntity<ProfileValidation.Response> validate(@RequestBody String body) {
        return ProfileValidation.validate(validator, body, ProfileValidation.DEVICE_PROFILE);
    }

    @PostMapping(value = "/device/validate/monitor", consumes = {"application/json", FHIR_JSON},
            produces = "application/json")
    public ResponseEntity<ProfileValidation.Response> validateMonitor(@RequestBody String body) {
        return ProfileValidation.validate(validator, body, ProfileValidation.MONITOR_PROFILE);
    }

    private ResponseEntity<String> outcome(HttpStatusCode status, OperationOutcome.IssueType code, String diagnostics) {
        var outcome = new OperationOutcome();
        outcome.addIssue().setSeverity(OperationOutcome.IssueSeverity.ERROR).setCode(code).setDiagnostics(diagnostics);
        return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).contentType(FHIR_JSON_MEDIA_TYPE)
                .body(context.newJsonParser().encodeResourceToString(outcome));
    }
}
