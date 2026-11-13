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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
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
            @Value("${fhir.cardx-cied.package}") String cardxCiedPackage
    ) throws IOException {
        NpmPackageValidationSupport cardxCied = new NpmPackageValidationSupport(ctx);
        cardxCied.loadPackageFromClasspath(cardxCiedPackage);

        ValidationSupportChain chain = new ValidationSupportChain(
                new DefaultProfileValidationSupport(ctx),
                cardxCied,
                new CommonCodeSystemsTerminologyService(ctx),
                new InMemoryTerminologyServerValidationSupport(ctx),
                new SnapshotGeneratingValidationSupport(ctx)
        );

        FhirValidator validator = ctx.newValidator();
        validator.registerValidatorModule(new FhirInstanceValidator(chain));
        return validator;
    }
}
