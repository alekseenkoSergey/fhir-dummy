package org.example.fhirdummy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Starts the Spring Boot application that exposes the CIED FHIR proxy API.
 * Bootstrapping this class enables component scanning and automatic configuration for the controllers,
 * services, and FHIR infrastructure.
 */
@SpringBootApplication
public class FhirDummyApplication {

    public static void main(String[] args) {
        SpringApplication.run(FhirDummyApplication.class, args);
    }

}
