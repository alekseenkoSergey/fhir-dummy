package org.example.fhirdummy.client;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FhirResourceClient {
    private final IGenericClient client;

    public FhirResourceClient(IGenericClient client) {
        this.client = client;
    }

    public <T extends Resource> T getExampleResource(Class<T> resourceType) {
        Bundle bundle;
        try {
            bundle = client.search().forResource(resourceType)
                    .count(1).returnBundle(Bundle.class).execute();
        } catch (BaseServerResponseException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "FHIR server request failed", exception);
        }
        return bundle.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(resourceType::isInstance)
                .map(resourceType::cast)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No " + resourceType.getSimpleName() + " found on the FHIR server"));
    }
}
