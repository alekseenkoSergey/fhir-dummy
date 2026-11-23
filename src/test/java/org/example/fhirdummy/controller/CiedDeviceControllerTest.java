package org.example.fhirdummy.controller;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.example.fhirdummy.service.CiedDeviceService;
import org.example.fhirdummy.utility.ProfileValidation;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.model.Device;
import org.hl7.fhir.r5.model.OperationOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CiedDeviceControllerTest {
    @Mock private FhirContext context;
    @Mock private IParser parser;
    @Mock private CiedDeviceService service;
    private CiedDeviceController controller;

    @BeforeEach
    void setUp() {
        controller = new CiedDeviceController(context, service);
    }

    @Test
    void returnsSerializedResourceWithFhirHeaders() {
        stubSerialization();
        var resource = new Device();
        when(service.getCiedDevice("clinic.1", "patient.2", "device.3")).thenReturn(resource);

        var response = controller.getDevice("clinic.1-patient.2-device.3", ProfileValidation.DEVICE_PROFILE);

        assertResponse(response, HttpStatus.OK);
        verify(service).getCiedDevice("clinic.1", "patient.2", "device.3");
        verify(parser).encodeResourceToString(resource);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "clinic", "clinic-patient", "clinic-patient-", "-patient-device", "clinic--device", "clinic-patient-device-extra", "clinic-patient-dev_ice", "clinic-patient-dev ice"})
    void rejectsInvalidCompositeIdWithoutCallingService(String id) {
        stubSerialization();
        var response = controller.getDevice(id, ProfileValidation.DEVICE_PROFILE);

        assertOutcome(response, HttpStatus.BAD_REQUEST, OperationOutcome.IssueType.INVALID,
                "Expected a composite key in the form clinicId-patientId-deviceId.");
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"unsupported-profile"})
    void rejectsUnexpectedProfileWithoutCallingService(String profile) {
        stubSerialization();
        var response = controller.getDevice("clinic.1-patient.2-device.3", profile);

        assertOutcome(response, HttpStatus.BAD_REQUEST, OperationOutcome.IssueType.INVALID,
                "Expected _profile=" + ProfileValidation.DEVICE_PROFILE);
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(ints = {404, 502, 503})
    void convertsServiceHttpErrorsToOperationOutcome(int status) {
        stubSerialization();
        var httpStatus = HttpStatus.valueOf(status);
        when(service.getCiedDevice("clinic.1", "patient.2", "device.3"))
                .thenThrow(new ResponseStatusException(httpStatus, "Service failure"));

        var response = controller.getDevice("clinic.1-patient.2-device.3", ProfileValidation.DEVICE_PROFILE);

        assertOutcome(response, httpStatus,
                status == 404 ? OperationOutcome.IssueType.NOTFOUND : OperationOutcome.IssueType.TRANSIENT,
                "Service failure");
    }

    @Test
    void propagatesUnexpectedServiceErrors() {
        var failure = new IllegalStateException("Unexpected failure");
        when(service.getCiedDevice("clinic.1", "patient.2", "device.3")).thenThrow(failure);

        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> controller.getDevice("clinic.1-patient.2-device.3", ProfileValidation.DEVICE_PROFILE)));
        verifyNoInteractions(parser);
    }

    private void stubSerialization() {
        when(context.newJsonParser()).thenReturn(parser);
        when(parser.encodeResourceToString(any(IBaseResource.class))).thenReturn("serialized-resource");
    }

    private void assertResponse(ResponseEntity<String> response, HttpStatus status) {
        assertEquals(status, response.getStatusCode());
        assertEquals(MediaType.valueOf("application/fhir+json"), response.getHeaders().getContentType());
        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertEquals("serialized-resource", response.getBody());
    }

    private void assertOutcome(ResponseEntity<String> response, HttpStatus status,
                               OperationOutcome.IssueType code, String diagnostics) {
        assertResponse(response, status);
        var resource = ArgumentCaptor.forClass(IBaseResource.class);
        verify(parser).encodeResourceToString(resource.capture());
        var outcome = assertInstanceOf(OperationOutcome.class, resource.getValue());
        assertEquals(1, outcome.getIssue().size());
        var issue = outcome.getIssueFirstRep();
        assertEquals(OperationOutcome.IssueSeverity.ERROR, issue.getSeverity());
        assertEquals(code, issue.getCode());
        assertEquals(diagnostics, issue.getDiagnostics());
    }
}
