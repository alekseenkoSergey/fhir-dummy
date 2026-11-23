package org.example.fhirdummy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Binds the CardX CIED package location and dependency package paths from application configuration.
 * It keeps the dependency list immutable and treats an omitted list as empty.
 */
@ConfigurationProperties(prefix = "fhir.cardx-cied")
public record CardxCiedProperties(String packageLocation, List<String> dependencies) {

    public CardxCiedProperties {
        dependencies = dependencies == null ? List.of() : List.copyOf(dependencies);
    }
}
