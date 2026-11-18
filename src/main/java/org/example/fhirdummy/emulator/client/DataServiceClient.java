package org.example.fhirdummy.emulator.client;

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
public class DataServiceClient {
    private final RestClient client;

    public DataServiceClient(RestClient.Builder builder, @Value("${fhir.data-service.base-url}") String baseUrl) {
        client = builder.baseUrl(baseUrl).build();
    }

    public JsonNode getPatient(String customerId, String patientId) {
        return get("/connectivity/customer/{customerId}/patients/{patientId}", "Patient", customerId, patientId);
    }

    public JsonNode getDeviceConnectivity(String customerId) {
        return get("/connectivity/customer/{customerId}/devices", "Device connectivity data", customerId);
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
