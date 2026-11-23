package org.example.fhirdummy.service;

import ca.uhn.fhir.validation.FhirValidator;
import org.example.fhirdummy.client.DataServiceClient;
import org.example.fhirdummy.utility.ProfileValidation;
import org.example.fhirdummy.component.mapper.PatientMapper;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r5.model.Patient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeParseException;

@Service
public class CiedPatientService {
    private final DataServiceClient client;
    private final PatientMapper mapper;
    private final FhirValidator validator;

    public CiedPatientService(DataServiceClient client, PatientMapper mapper, FhirValidator validator) {
        this.client = client;
        this.mapper = mapper;
        this.validator = validator;
    }

    public Patient getPatient(String clinicId, String patientId) {
        var source = client.getPatient(clinicId, patientId);
        Patient patient;
        try {
            patient = mapper.toFhir(source, clinicId);
        } catch (IllegalArgumentException | DateTimeParseException | FHIRException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid patient data from the data service", exception);
        }

        patient.getMeta().addProfile(ProfileValidation.PATIENT_PROFILE);
        if (!validator.validateWithResult(patient).isSuccessful()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Mapped Patient does not conform to the CIED Patient profile");
        }
        return patient;
    }
}
