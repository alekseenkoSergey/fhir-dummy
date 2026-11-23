package org.example.fhirdummy.service;

import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import org.example.fhirdummy.client.DataServiceClient;
import org.example.fhirdummy.component.mapper.PatientMapper;
import org.example.fhirdummy.utility.ProfileValidation;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r5.model.Patient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

import java.time.format.DateTimeParseException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CiedPatientServiceTest {
    @Mock private DataServiceClient client;
    @Mock private PatientMapper mapper;
    @Mock private FhirValidator validator;
    @Mock private ValidationResult validation;
    @InjectMocks private CiedPatientService service;

    @Test
    void mapsPatientAndAddsProfileBeforeValidation() {
        var source = new JsonMapper().readTree("{\"merlinNetId\":\"patient\"}");
        var patient = new Patient();
        patient.getMeta().addProfile("existing-profile");
        when(client.getPatient("clinic", "patient")).thenReturn(source);
        when(mapper.toFhir(source, "clinic")).thenReturn(patient);
        when(validator.validateWithResult(patient)).thenAnswer(invocation -> {
            assertTrue(patient.getMeta().hasProfile(ProfileValidation.PATIENT_PROFILE));
            return validation;
        });
        when(validation.isSuccessful()).thenReturn(true);

        assertSame(patient, service.getPatient("clinic", "patient"));
        assertTrue(patient.getMeta().hasProfile("existing-profile"));
        verify(mapper).toFhir(source, "clinic");
        verify(validator).validateWithResult(patient);
    }

    @ParameterizedTest
    @MethodSource("mappingFailures")
    void translatesMappingFailuresToBadGateway(RuntimeException failure) {
        var source = new JsonMapper().readTree("{}");
        when(client.getPatient("clinic", "patient")).thenReturn(source);
        when(mapper.toFhir(source, "clinic")).thenThrow(failure);

        var exception = assertThrows(ResponseStatusException.class, () -> service.getPatient("clinic", "patient"));

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatusCode());
        assertEquals("Invalid patient data from the data service", exception.getReason());
        assertSame(failure, exception.getCause());
        verifyNoInteractions(validator);
    }

    static Stream<RuntimeException> mappingFailures() {
        return Stream.of(new IllegalArgumentException("Invalid field"),
                new DateTimeParseException("Invalid date", "bad-date", 0), new FHIRException("Invalid gender"));
    }

    @Test
    void rejectsPatientThatFailsProfileValidation() {
        var source = new JsonMapper().readTree("{}");
        var patient = new Patient();
        when(client.getPatient("clinic", "patient")).thenReturn(source);
        when(mapper.toFhir(source, "clinic")).thenReturn(patient);
        when(validator.validateWithResult(patient)).thenReturn(validation);
        when(validation.isSuccessful()).thenReturn(false);

        var exception = assertThrows(ResponseStatusException.class, () -> service.getPatient("clinic", "patient"));

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatusCode());
        assertEquals("Mapped Patient does not conform to the CIED Patient profile", exception.getReason());
    }

    @Test
    void propagatesClientFailureWithoutMappingOrValidation() {
        var failure = new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found");
        when(client.getPatient("clinic", "patient")).thenThrow(failure);

        assertSame(failure, assertThrows(ResponseStatusException.class, () -> service.getPatient("clinic", "patient")));
        verifyNoInteractions(mapper, validator);
    }
}
