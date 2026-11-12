package org.example.fhirdummy.controller;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r5.model.CapabilityStatement;
import org.hl7.fhir.r5.model.CapabilityStatement.RestfulCapabilityMode;
import org.hl7.fhir.r5.model.CapabilityStatement.TypeRestfulInteraction;
import org.hl7.fhir.r5.model.DateTimeType;
import org.hl7.fhir.r5.model.Enumerations.CapabilityStatementKind;
import org.hl7.fhir.r5.model.Enumerations.FHIRVersion;
import org.hl7.fhir.r5.model.Enumerations.PublicationStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MetadataController {
    private static final String CARDX_PROFILE_BASE = "http://hl7.org/fhir/uv/cardx-cied/StructureDefinition/";

    private final String metadataJson;

    public MetadataController(FhirContext context, @Value("${fhir.public-base-url:}") String publicBaseUrl) {
        CapabilityStatement statement = new CapabilityStatement();
        statement.setName("MyCiedProxy");
        statement.setStatus(PublicationStatus.DRAFT);
        statement.setDateElement(new DateTimeType("2026-11-12"));
        statement.setKind(CapabilityStatementKind.INSTANCE);
        statement.getImplementation().setDescription("My CIED FHIR proxy");
        if (!publicBaseUrl.isBlank()) {
            statement.getImplementation().setUrl(publicBaseUrl);
        }
        statement.setFhirVersion(FHIRVersion._5_0_0);
        statement.addFormat("json");

        var server = statement.addRest()
                .setMode(RestfulCapabilityMode.SERVER)
                .setDocumentation("My Patient and CIED API.");

        var patient = server.addResource()
                .setType("Patient")
                .addSupportedProfile(CARDX_PROFILE_BASE + "cied-patient")
                .setDocumentation("Read a patient by ID.");
        patient.addInteraction().setCode(TypeRestfulInteraction.READ);
        patient.addInteraction().setCode(TypeRestfulInteraction.SEARCHTYPE);

        server.addResource()
                .setType("Device")
                .addSupportedProfile(CARDX_PROFILE_BASE + "cied-device")
                .addSupportedProfile(CARDX_PROFILE_BASE + "cied-monitor")
                .setDocumentation("Implanted devices and monitors are mapped from device data returned for a patient.");

        server.addResource()
                .setType("Observation")
                .addSupportedProfile(CARDX_PROFILE_BASE + "cied-connectivity-status")
                .setDocumentation("CIED connectivity observations are planned when patient device data provides the required connectivity details.");

        metadataJson = context.newJsonParser().encodeResourceToString(statement);
    }

    @GetMapping(value = "/fhir/metadata", produces = "application/fhir+json")
    public String metadata() {
        return metadataJson;
    }
}
