package org.example.fhirdummy.controller;

import ca.uhn.fhir.context.FhirContext;
import org.example.fhirdummy.client.FhirResourceClient;
import org.example.fhirdummy.client.DeviceClient;
import org.example.fhirdummy.dto.DeviceRequest;
import org.hl7.fhir.r4.model.Device;
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

    public DeviceController(DeviceClient client, FhirResourceClient resourceClient,
                                 FhirContext context) {
        this.client = client;
        this.resourceClient = resourceClient;
        this.context = context;
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

    private String json(Device device) {
        return context.newJsonParser().encodeResourceToString(device);
    }
}
