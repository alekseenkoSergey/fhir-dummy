package org.example.fhirdummy.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "fhir.cardx-cied")
public record CardxCiedProperties(String packageLocation, List<String> dependencies) {

    public CardxCiedProperties {
        dependencies = dependencies == null ? List.of() : List.copyOf(dependencies);
    }
}
