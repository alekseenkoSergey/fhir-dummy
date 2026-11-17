package org.example.fhirdummy.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

@Component
public class PatientDataClient {
    private final RestClient client;

    public PatientDataClient(RestClient.Builder builder, @Value("${fhir.patient-data.base-url}") String baseUrl) {
        client = builder.baseUrl(baseUrl).build();
    }

    public JsonNode getPatient(String customerId, String patientId) {
        try {
            return client.get()
                    .uri("/connectivity/customer/{customerId}/patients/{patientId}", customerId, patientId)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found in the data service", exception);
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Patient data service request failed", exception);
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Patient data service request failed", exception);
        }
    }
}
