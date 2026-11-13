package org.example.fhirdummy.configuration;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.validation.FhirValidator;

import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.NpmPackageValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.SnapshotGeneratingValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
@EnableConfigurationProperties(CardxCiedProperties.class)
public class FhirConfiguration {

    @Bean
    public FhirContext fhirContext() {
        FhirContext context = FhirContext.forR5();
        context.getRestfulClientFactory().setConnectTimeout(5_000);
        context.getRestfulClientFactory().setSocketTimeout(15_000);
        return context;
    }

    @Bean
    public IGenericClient fhirClient(FhirContext context,
            @Value("${fhir.server.base-url}") String baseUrl) {
        return context.newRestfulGenericClient(baseUrl);
    }

    @Bean
    public FhirValidator fhirValidator(
            FhirContext ctx,
            CardxCiedProperties cardxCied
    ) throws IOException {
        NpmPackageValidationSupport npm = new NpmPackageValidationSupport(ctx);
        for (String dependency : cardxCied.dependencies()) {
            npm.loadPackageFromClasspath(dependency);
        }

        npm.loadPackageFromClasspath(cardxCied.packageLocation());

        ValidationSupportChain chain = new ValidationSupportChain(
                npm,
                new DefaultProfileValidationSupport(ctx),
                new CommonCodeSystemsTerminologyService(ctx),
                new InMemoryTerminologyServerValidationSupport(ctx),
                new SnapshotGeneratingValidationSupport(ctx)
        );

        FhirValidator validator = ctx.newValidator();
        validator.registerValidatorModule(new FhirInstanceValidator(chain));
        return validator;
    }
}
