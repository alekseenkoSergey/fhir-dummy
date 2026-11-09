package org.example.fhirdummy.controller;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.ValidationResult;
import org.example.fhirdummy.client.FhirResourceClient;
import org.example.fhirdummy.client.ObservationClient;
import org.example.fhirdummy.dto.ObservationRequest;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Observation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/fhir/observation")
public class ObservationController {
    private final ObservationClient client;
    private final FhirResourceClient resourceClient;
    private final FhirContext context;
    private final FhirValidator validator;
    private static final String HR_PROFILE =
            "http://example.org/fhir/ig1/StructureDefinition/ig1-heart-rate-observation";

    public ObservationController(ObservationClient client, FhirResourceClient resourceClient,
                                 FhirContext context, FhirValidator validator) {
        this.client = client;
        this.resourceClient = resourceClient;
        this.context = context;
        this.validator = validator;
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

    public record ValidationResponse(boolean conforms, List<String> errors, List<String> warnings) {}

    @PostMapping(value = "/validate", consumes = {"application/json", "application/fhir+json"},
            produces = "application/json")
    public ResponseEntity<ValidationResponse> validate(@RequestBody String body) {
        Observation observation;
        try {
            observation = context.newJsonParser().parseResource(Observation.class, body);
        } catch (DataFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a valid FHIR Observation JSON");
        }
        observation.getMeta().setProfile(List.of(new CanonicalType(HR_PROFILE)));

        ValidationResult result = validator.validateWithResult(observation);

        ValidationResponse response = new ValidationResponse(
                result.isSuccessful(),
                messages(result, ResultSeverityEnum.ERROR, ResultSeverityEnum.FATAL),
                messages(result, ResultSeverityEnum.WARNING)
        );

        return ResponseEntity.status(result.isSuccessful() ? 200 : 422).body(response);
    }

    private List<String> messages(ValidationResult result, ResultSeverityEnum... severities) {
        Set<ResultSeverityEnum> wanted = Set.of(severities);
        return result.getMessages().stream()
                .filter(m -> wanted.contains(m.getSeverity()))
                .map(m -> m.getLocationString() + ": " + m.getMessage())
                .toList();
    }

    private String json(Observation observation) {
        return context.newJsonParser().encodeResourceToString(observation);
    }
}
