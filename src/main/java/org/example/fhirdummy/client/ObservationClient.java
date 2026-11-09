package org.example.fhirdummy.client;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.example.fhirdummy.dto.ObservationRequest;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Quantity;
import org.springframework.stereotype.Service;

@Service
public class ObservationClient {
    private final FhirCrudClient<Observation> client;

    public ObservationClient(IGenericClient client) {
        this.client = new FhirCrudClient<>(client, Observation.class);
    }

    public Observation create(ObservationRequest request) {
        Observation observation = new Observation();
        apply(observation, request);
        return client.create(observation);
    }

    public Observation get(String id) {
        return client.get(id);
    }

    public Observation update(String id, ObservationRequest request) {
        return client.update(id, observation -> apply(observation, request));
    }

    public void delete(String id) {
        client.delete(id);
    }

    private void apply(Observation observation, ObservationRequest request) {
        observation.setStatus(Observation.ObservationStatus.fromCode(request.status()));
        observation.getCode().getCoding().clear();
        observation.getCode().addCoding().setSystem("http://loinc.org").setCode(request.code());
        observation.setValue(new Quantity().setValue(request.value()).setUnit(request.unit()));
    }
}
