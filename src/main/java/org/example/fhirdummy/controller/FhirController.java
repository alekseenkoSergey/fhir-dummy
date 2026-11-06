package org.example.fhirdummy.controller;

import ca.uhn.fhir.context.FhirContext;
import org.example.fhirdummy.client.FhirResourceClient;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fhir")
public class FhirController {
    private final FhirResourceClient client;
    private final FhirContext context;

    public FhirController(FhirResourceClient client, FhirContext context) {
        this.client = client;
        this.context = context;
    }

    @GetMapping(value = "/patient", produces = "application/fhir+json")
    public String getPatient() {
        return context.newJsonParser().encodeResourceToString(client.getExampleResource(Patient.class));
    }

    @GetMapping(value = "/condition", produces = "application/fhir+json")
    public String getCondition() {
        return context.newJsonParser().encodeResourceToString(client.getExampleResource(Condition.class));
    }

    @GetMapping(value = "/encounter", produces = "application/fhir+json")
    public String getEncounter() {
        return context.newJsonParser().encodeResourceToString(client.getExampleResource(Encounter.class));
    }

    @GetMapping(value = "/organization", produces = "application/fhir+json")
    public String getOrganization() {
        return context.newJsonParser().encodeResourceToString(client.getExampleResource(Organization.class));
    }

    @GetMapping(value = "/practitioner", produces = "application/fhir+json")
    public String getPractitioner() {
        return context.newJsonParser().encodeResourceToString(client.getExampleResource(Practitioner.class));
    }
}
