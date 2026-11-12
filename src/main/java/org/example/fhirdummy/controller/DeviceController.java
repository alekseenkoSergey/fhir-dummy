package org.example.fhirdummy.controller;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.FhirValidator;
import org.example.fhirdummy.client.FhirResourceClient;
import org.example.fhirdummy.client.DeviceClient;
import org.example.fhirdummy.dto.DeviceRequest;
import org.hl7.fhir.r5.model.Device;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/fhir/device")
public class DeviceController {
    private final DeviceClient client;
    private final FhirResourceClient resourceClient;
    private final FhirContext context;
    private final FhirValidator validator;
    private static final String DEVICE_PROFILE = ProfileValidation.IG1_PROFILE_BASE + "ig1-cied-device";
    private static final String MONITOR_PROFILE = ProfileValidation.IG1_PROFILE_BASE + "ig1-cied-monitor";

    public DeviceController(DeviceClient client, FhirResourceClient resourceClient,
                                 FhirContext context, FhirValidator validator) {
        this.client = client;
        this.resourceClient = resourceClient;
        this.context = context;
        this.validator = validator;
    }

    @GetMapping(produces = "application/fhir+json")
    public String getExample() {
        return json(resourceClient.getExampleResource(Device.class));
    }

    @PostMapping(consumes = "application/json", produces = "application/fhir+json")
    public ResponseEntity<String> create(@RequestBody DeviceRequest request) {
        Device device = client.create(request);
        return ResponseEntity.created(URI.create("/fhir/device/" + device.getIdElement().getIdPart()))
                .body(json(device));
    }

    @GetMapping(value = "/{id}", produces = "application/fhir+json")
    public String get(@PathVariable String id) {
        return json(client.get(id));
    }

    @PutMapping(value = "/{id}", consumes = "application/json", produces = "application/fhir+json")
    public String update(@PathVariable String id, @RequestBody DeviceRequest request) {
        return json(client.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        client.delete(id);
    }

    @PostMapping(value = "/validate", consumes = {"application/json", "application/fhir+json"},
            produces = "application/json")
    public ResponseEntity<ProfileValidation.Response> validate(@RequestBody String body) {
        return ProfileValidation.validate(validator, body, DEVICE_PROFILE);
    }

    @PostMapping(value = "/validate/monitor", consumes = {"application/json", "application/fhir+json"},
            produces = "application/json")
    public ResponseEntity<ProfileValidation.Response> validateMonitor(@RequestBody String body) {
        return ProfileValidation.validate(validator, body, MONITOR_PROFILE);
    }

    private String json(Device device) {
        return context.newJsonParser().encodeResourceToString(device);
    }
}
