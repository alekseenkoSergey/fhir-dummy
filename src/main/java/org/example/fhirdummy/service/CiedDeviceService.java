package org.example.fhirdummy.service;

import ca.uhn.fhir.validation.FhirValidator;
import org.example.fhirdummy.client.DataServiceClient;
import org.example.fhirdummy.utility.ProfileValidation;
import org.example.fhirdummy.component.mapper.DeviceMapper;
import org.example.fhirdummy.utility.JsonText;
import org.hl7.fhir.r5.model.Device;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

import java.util.stream.StreamSupport;

/**
 * Finds a requested device in a patient's upstream record and maps it to a FHIR Device resource.
 * It validates the result against the CIED Device profile and distinguishes missing devices from invalid upstream data.
 */
@Service
public class CiedDeviceService {
    private final DataServiceClient client;
    private final DeviceMapper mapper;
    private final FhirValidator validator;

    public CiedDeviceService(DataServiceClient client, DeviceMapper mapper, FhirValidator validator) {
        this.client = client;
        this.mapper = mapper;
        this.validator = validator;
    }

    public Device getCiedDevice(String clinicId, String patientId, String deviceId) {
        var source = client.getPatient(clinicId, patientId);
        if (source == null || !source.isObject()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid patient data from the data service");
        }

        var devices = source.path("devices");
        if (devices.isMissingNode() || devices.isNull() || devices.isArray() && devices.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No device found for the patient");
        }
        if (!devices.isArray()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Expected a device list from the data service");
        }

        Device device;
        try {
            var selected = StreamSupport.stream(devices.spliterator(), false)
                    .filter(candidate -> matchesDeviceId(candidate.path("deviceId"), deviceId))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Requested device not found for the patient"));
            device = mapper.toFhir(selected, clinicId, JsonText.required(source, "merlinNetId"), deviceId);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid device data from the data service", exception);
        }

        device.getMeta().addProfile(ProfileValidation.DEVICE_PROFILE);
        if (!validator.validateWithResult(device).isSuccessful()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Mapped Device does not conform to the CIED Device profile");
        }
        return device;
    }

    private static boolean matchesDeviceId(JsonNode value, String requestedId) {
        if (value.isString()) {
            return requestedId.equals(value.asString());
        }
        return value.isIntegralNumber() && requestedId.equals(value.toString());
    }
}
