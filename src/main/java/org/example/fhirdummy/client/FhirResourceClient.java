package org.example.fhirdummy.client;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.stream.Stream;

@Deprecated
@Service
public class FhirResourceClient {
    private final IGenericClient client;

    public FhirResourceClient(IGenericClient client) {
        this.client = client;
    }

    public <T extends Resource> T getExampleResource(Class<T> resourceType) {
        return search(resourceType, 1).findFirst().orElseThrow(() -> noResourceFound(resourceType));
    }

    private <T extends Resource> Stream<T> search(Class<T> resourceType, int count) {
        Bundle bundle;
        try {
            bundle = client.search().forResource(resourceType)
                    .count(count).returnBundle(Bundle.class).execute();
        } catch (BaseServerResponseException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "FHIR server request failed", exception);
        }
        return bundle.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(resourceType::isInstance)
                .map(resourceType::cast);
    }

    private ResponseStatusException noResourceFound(Class<? extends Resource> resourceType) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND,
                "No " + resourceType.getSimpleName() + " found on the FHIR server");
    }
}
