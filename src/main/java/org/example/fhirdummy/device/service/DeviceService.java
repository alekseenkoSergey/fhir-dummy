package org.example.fhirdummy.device.service;

import ca.uhn.fhir.validation.FhirValidator;
import org.example.fhirdummy.emulator.client.DataServiceClient;
import org.example.fhirdummy.utility.ProfileValidation;
import org.example.fhirdummy.device.mapper.DeviceMapper;
import org.hl7.fhir.r5.model.Device;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DeviceService {
    private final DataServiceClient client;
    private final DeviceMapper mapper;
    private final FhirValidator validator;

    public DeviceService(DataServiceClient client, DeviceMapper mapper, FhirValidator validator) {
        this.client = client;
        this.mapper = mapper;
        this.validator = validator;
    }

    public Device getCiedDevice(String clinicId, String patientId) {
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
            device = mapper.toFhir(devices.get(0));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid device data from the data service", exception);
        }

        device.getMeta().addProfile(ProfileValidation.DEVICE_PROFILE);
        if (!validator.validateWithResult(device).isSuccessful()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Mapped Device does not conform to the CIED Device profile");
        }
        return device;
    }
}
