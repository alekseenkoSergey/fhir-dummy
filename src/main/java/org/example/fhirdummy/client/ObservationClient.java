package org.example.fhirdummy.client;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import org.example.fhirdummy.dto.ObservationRequest;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Quantity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.function.Supplier;

@Service
public class ObservationClient {
    private final IGenericClient client;

    public ObservationClient(IGenericClient client) {
        this.client = client;
    }

    public Observation create(ObservationRequest request) {
        Observation observation = apply(new Observation(), request);
        return call(() -> {
            var outcome = client.create().resource(observation).encodedJson().execute();
            return get(outcome.getId().getIdPart());
        });
    }

    public Observation get(String id) {
        validateId(id);
        return call(() -> client.read().resource(Observation.class).withId(id).execute());
    }

    public Observation update(String id, ObservationRequest request) {
        Observation observation = apply(get(id), request);
        return call(() -> {
            client.update().resource(observation).encodedJson().execute();
            return get(id);
        });
    }

    public void delete(String id) {
        validateId(id);
        call(() -> client.delete().resourceById("Observation", id).execute());
    }

    private Observation apply(Observation observation, ObservationRequest request) {
        observation.setStatus(Observation.ObservationStatus.fromCode(request.status()));
        observation.getCode().getCoding().clear();
        observation.getCode().addCoding().setSystem("http://loinc.org").setCode(request.code());
        observation.setValue(new Quantity().setValue(request.value()).setUnit(request.unit()));
        return observation;
    }

    private void validateId(String id) {
        if (!id.matches("[A-Za-z0-9\\-.]{1,64}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid FHIR resource ID");
        }
    }

    private <T> T call(Supplier<T> operation) {
        try {
            return operation.get();
        } catch (BaseServerResponseException exception) {
            int status = exception.getStatusCode();
            HttpStatus mapped = switch (status) {
                case 404, 410 -> HttpStatus.NOT_FOUND;
                case 400, 422 -> HttpStatus.BAD_REQUEST;
                case 409, 412 -> HttpStatus.CONFLICT;
                default -> HttpStatus.BAD_GATEWAY;
            };
            throw new ResponseStatusException(mapped, "FHIR Observation request failed", exception);
        }
    }
}
