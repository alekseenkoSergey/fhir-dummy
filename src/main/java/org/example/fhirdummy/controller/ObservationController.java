package org.example.fhirdummy.controller;

import ca.uhn.fhir.context.FhirContext;
import org.example.fhirdummy.client.FhirResourceClient;
import org.example.fhirdummy.client.ObservationClient;
import org.example.fhirdummy.dto.ObservationRequest;
import org.hl7.fhir.r4.model.Observation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/fhir/observation")
public class ObservationController {
    private final ObservationClient client;
    private final FhirResourceClient resourceClient;
    private final FhirContext context;

    public ObservationController(ObservationClient client, FhirResourceClient resourceClient,
                                 FhirContext context) {
        this.client = client;
        this.resourceClient = resourceClient;
        this.context = context;
    }

    @GetMapping(produces = "application/fhir+json")
    public String getExample() {
        return json(resourceClient.getExampleResource(Observation.class));
    }

    @PostMapping(consumes = "application/json", produces = "application/fhir+json")
    public ResponseEntity<String> create(@RequestBody ObservationRequest request) {
        Observation observation = client.create(request);
        return ResponseEntity.created(URI.create("/fhir/observation/" + observation.getIdElement().getIdPart()))
                .body(json(observation));
    }

    @GetMapping(value = "/{id}", produces = "application/fhir+json")
    public String get(@PathVariable String id) {
        return json(client.get(id));
    }

    @PutMapping(value = "/{id}", consumes = "application/json", produces = "application/fhir+json")
    public String update(@PathVariable String id, @RequestBody ObservationRequest request) {
        return json(client.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        client.delete(id);
    }

    private String json(Observation observation) {
        return context.newJsonParser().encodeResourceToString(observation);
    }
}
