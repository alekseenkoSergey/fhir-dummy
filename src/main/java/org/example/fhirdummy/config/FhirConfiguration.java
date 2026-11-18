package org.example.fhirdummy.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.validation.FhirValidator;
import java.io.IOException;
import org.hl7.fhir.common.hapi.validation.support.*;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
