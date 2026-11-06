package org.example.fhirdummy.configuration;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FhirConfiguration {

    @Bean
    public FhirContext fhirContext() {
        FhirContext context = FhirContext.forR4();
        context.getRestfulClientFactory().setConnectTimeout(5_000);
        context.getRestfulClientFactory().setSocketTimeout(15_000);
        return context;
    }

    @Bean
    public IGenericClient fhirClient(FhirContext context,
            @Value("${fhir.server.base-url}") String baseUrl) {
        return context.newRestfulGenericClient(baseUrl);
    }
}
