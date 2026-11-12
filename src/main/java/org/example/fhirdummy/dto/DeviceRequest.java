package org.example.fhirdummy.dto;

import org.hl7.fhir.r5.model.Device;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;

public record DeviceRequest(String status, String manufacturer, String modelNumber) {
    public DeviceRequest {
        if (status == null || manufacturer == null || manufacturer.isBlank()
                || modelNumber == null || modelNumber.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "status, manufacturer and modelNumber are required");
        }
        boolean validStatus = Arrays.stream(Device.FHIRDeviceStatus.values())
                .anyMatch(candidate -> status.equals(candidate.toCode()));
        if (!validStatus) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Device status");
        }
    }
}
