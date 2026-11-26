package org.example.fhirdummy.service;

import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import org.example.fhirdummy.client.DataServiceClient;
import org.example.fhirdummy.component.mapper.DeviceMapper;
import org.example.fhirdummy.utility.ProfileValidation;
import org.hl7.fhir.r5.model.Device;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CiedDeviceServiceTest {
    @Mock private DataServiceClient client;
    @Mock private DeviceMapper mapper;
    @Mock private FhirValidator validator;
    @Mock private ValidationResult validation;
    @InjectMocks private CiedDeviceService service;
    private final JsonMapper json = new JsonMapper();

    @ParameterizedTest
    @ValueSource(strings = {"\"42\"", "42"})
    void selectsFirstMatchingDeviceAndAddsProfileBeforeValidation(String deviceId) {
        var source = json.readTree("""
                {"merlinNetId":" canonical-patient ","devices":[
                  {}, {"deviceId":null}, {"deviceId":true}, {"deviceId":42.0},
                  {"deviceId":[]}, {"deviceId":{}}, {"deviceId":"other"},
                  {"deviceId":%s,"deviceSerialNum":"first"},
                  {"deviceId":%s,"deviceSerialNum":"second"}]}
                """.formatted(deviceId, deviceId));
        var selected = source.path("devices").get(7);
        var device = new Device();
        device.getMeta().addProfile("existing-profile");
        when(client.getPatient("clinic", "requested-patient")).thenReturn(source);
        when(mapper.toFhir(selected, "clinic", "requested-patient", "42")).thenReturn(device);
        when(validator.validateWithResult(device)).thenAnswer(invocation -> {
            assertTrue(device.getMeta().hasProfile(ProfileValidation.DEVICE_PROFILE));
            return validation;
        });
        when(validation.isSuccessful()).thenReturn(true);

        assertSame(device, service.getCiedDevice("clinic", "requested-patient", "42"));
        assertTrue(device.getMeta().hasProfile("existing-profile"));
        verify(mapper).toFhir(selected, "clinic", "requested-patient", "42");
        verify(validator).validateWithResult(device);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"null", "[]", "\"patient\"", "42", "true"})
    void rejectsInvalidPatientSource(String input) {
        stubSource(input == null ? null : json.readTree(input));

        assertFailure(HttpStatus.BAD_GATEWAY, "Invalid patient data from the data service");
        verifyNoInteractions(mapper, validator);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"devices\":null}", "{\"devices\":[]}"})
    void reportsNotFoundForMissingNullOrEmptyDevices(String input) {
        stubSource(json.readTree(input));

        assertFailure(HttpStatus.NOT_FOUND, "No device found for the patient");
        verifyNoInteractions(mapper, validator);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "\"device\"", "42", "true"})
    void rejectsDevicesThatAreNotAnArray(String devices) {
        stubSource(json.readTree("{\"devices\":" + devices + "}"));

        assertFailure(HttpStatus.BAD_GATEWAY, "Expected a device list from the data service");
        verifyNoInteractions(mapper, validator);
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"other\"", "43", "42.0", "null", "true", "[]", "{}", "\"042\""})
    void reportsNotFoundWhenNoDeviceIdMatches(String id) {
        stubSource(json.readTree("{\"devices\":[{\"deviceId\":" + id + "}]}"));

        assertFailure(HttpStatus.NOT_FOUND, "Requested device not found for the patient");
        verifyNoInteractions(mapper, validator);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"merlinNetId\":null}", "{\"merlinNetId\":\" \"}", "{\"merlinNetId\":42}"})
    void usesRequestedPatientIdDespiteMissingOrInvalidUpstreamIdentifier(String input) {
        var source = json.readTree(input);
        ((tools.jackson.databind.node.ObjectNode) source).set("devices", json.readTree("[{\"deviceId\":42}]"));
        stubSource(source);
        var selected = source.path("devices").get(0);
        var device = new Device();
        when(mapper.toFhir(selected, "clinic", "patient", "42")).thenReturn(device);
        when(validator.validateWithResult(device)).thenReturn(validation);
        when(validation.isSuccessful()).thenReturn(true);

        assertSame(device, service.getCiedDevice("clinic", "patient", "42"));
        assertTrue(device.getMeta().hasProfile(ProfileValidation.DEVICE_PROFILE));
        verify(mapper).toFhir(selected, "clinic", "patient", "42");
        verify(validator).validateWithResult(device);
    }

    @Test
    void translatesMapperFailureToBadGateway() {
        var source = validSource();
        stubSource(source);
        var failure = new IllegalArgumentException("Unsupported type");
        when(mapper.toFhir(source.path("devices").get(0), "clinic", "patient", "42")).thenThrow(failure);

        var exception = assertFailure(HttpStatus.BAD_GATEWAY, "Invalid device data from the data service");
        assertSame(failure, exception.getCause());
        verifyNoInteractions(validator);
    }

    @Test
    void rejectsDeviceThatFailsProfileValidation() {
        var source = validSource();
        var device = new Device();
        stubSource(source);
        when(mapper.toFhir(source.path("devices").get(0), "clinic", "patient", "42")).thenReturn(device);
        when(validator.validateWithResult(device)).thenReturn(validation);
        when(validation.isSuccessful()).thenReturn(false);

        assertFailure(HttpStatus.BAD_GATEWAY, "Mapped Device does not conform to the CIED Device profile");
    }

    @Test
    void propagatesClientFailureWithoutMappingOrValidation() {
        var failure = new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found");
        when(client.getPatient("clinic", "patient")).thenThrow(failure);

        assertSame(failure, assertThrows(ResponseStatusException.class,
                () -> service.getCiedDevice("clinic", "patient", "42")));
        verifyNoInteractions(mapper, validator);
    }

    private JsonNode validSource() {
        return json.readTree("{\"devices\":[{\"deviceId\":42}]}");
    }

    private void stubSource(JsonNode source) {
        when(client.getPatient("clinic", "patient")).thenReturn(source);
    }

    private ResponseStatusException assertFailure(HttpStatus status, String reason) {
        var exception = assertThrows(ResponseStatusException.class,
                () -> service.getCiedDevice("clinic", "patient", "42"));
        assertEquals(status, exception.getStatusCode());
        assertEquals(reason, exception.getReason());
        return exception;
    }
}
