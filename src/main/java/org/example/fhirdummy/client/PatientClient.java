package org.example.fhirdummy.client;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.example.fhirdummy.dto.PatientRequest;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Service;

@Service
public class PatientClient {
    private final FhirCrudClient<Patient> client;

    public PatientClient(IGenericClient client) {
        this.client = new FhirCrudClient<>(client, Patient.class);
    }

    public Patient create(PatientRequest request) {
        Patient patient = new Patient();
        apply(patient, request);
        return client.create(patient);
    }

    public Patient get(String id) {
        return client.get(id);
    }

    public Patient update(String id, PatientRequest request) {
        return client.update(id, patient -> apply(patient, request));
    }

    public void delete(String id) {
        client.delete(id);
    }

    private void apply(Patient patient, PatientRequest request) {
        var name = patient.getNameFirstRep();
        name.setFamily(request.family());
        name.getGiven().clear();
        name.addGiven(request.given());
    }
}
