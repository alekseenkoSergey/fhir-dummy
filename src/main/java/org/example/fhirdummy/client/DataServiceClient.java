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

/**
 * Fetches patient records and clinic device connectivity data from the configured data service.
 * It returns the responses as JSON and translates upstream failures into HTTP errors that the FHIR API can report.
 */
@Component
public class DataServiceClient {
    private final RestClient client;

    public DataServiceClient(RestClient.Builder builder, @Value("${fhir.data-service.base-url}") String baseUrl) {
        client = builder.baseUrl(baseUrl).build();
    }

    public JsonNode getPatient(String clinicId, String patientId) {
        return get("/connectivity/customer/{clinicId}/patients/{patientId}", "Patient", clinicId, patientId);
    }

    public JsonNode getDeviceConnectivity(String clinicId) {
        return get("/connectivity/customer/{clinicId}/devices", "Device connectivity data", clinicId);
    }

    private JsonNode get(String uri, String subject, Object... uriVariables) {
        try {
            return client.get()
                    .uri(uri, uriVariables)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, subject + " not found in the data service", exception);
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Data service request failed", exception);
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Data service request failed", exception);
        }
    }
}
