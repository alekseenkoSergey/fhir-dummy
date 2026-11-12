package org.example.fhirdummy.dto;

import java.math.BigDecimal;
import org.hl7.fhir.r5.model.Enumerations.ObservationStatus;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public record ObservationRequest(String status, String code, BigDecimal value, String unit) {
    public ObservationRequest {
        if (status == null || code == null || code.isBlank() || value == null
                || unit == null || unit.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "status, code, value and unit are required");
        }
        boolean validStatus = ObservationStatus.isValidCode(status);
        if (!validStatus) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Observation status");
        }
    }
}
