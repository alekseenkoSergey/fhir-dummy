package org.example.fhirdummy.dto;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public record PatientRequest(String family, String given) {
    public PatientRequest {
        if (family == null || family.isBlank() || given == null || given.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "family and given are required");
        }
    }
}
