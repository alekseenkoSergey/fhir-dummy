package org.example.fhirdummy.client;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.function.Consumer;
import java.util.function.Supplier;

final class FhirCrudClient<T extends Resource> {
    private final IGenericClient client;
    private final Class<T> resourceType;

    FhirCrudClient(IGenericClient client, Class<T> resourceType) {
        this.client = client;
        this.resourceType = resourceType;
    }

    T create(T resource) {
        return call(() -> {
            var outcome = client.create().resource(resource).encodedJson().execute();
            return get(outcome.getId().getIdPart());
        });
    }

    T get(String id) {
        validateId(id);
        return call(() -> client.read().resource(resourceType).withId(id).execute());
    }

    T update(String id, Consumer<T> apply) {
        T resource = get(id);
        apply.accept(resource);
        return call(() -> {
            client.update().resource(resource).encodedJson().execute();
            return get(id);
        });
    }

    void delete(String id) {
        validateId(id);
        call(() -> client.delete().resourceById(resourceType.getSimpleName(), id).execute());
    }

    private void validateId(String id) {
        if (!id.matches("[A-Za-z0-9\\-.]{1,64}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid FHIR resource ID");
        }
    }

    private <R> R call(Supplier<R> operation) {
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
            throw new ResponseStatusException(mapped, "FHIR " + resourceType.getSimpleName() + " request failed", exception);
        }
    }
}
