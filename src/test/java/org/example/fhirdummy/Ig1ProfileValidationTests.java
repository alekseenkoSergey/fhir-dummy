package org.example.fhirdummy;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.ValidationResult;
import org.example.fhirdummy.configuration.FhirConfiguration;
import org.hl7.fhir.r5.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.r5.model.ContactPoint.ContactPointUse;
import org.hl7.fhir.r5.model.Device;
import org.hl7.fhir.r5.model.Enumerations;
import org.hl7.fhir.r5.model.Observation;
import org.hl7.fhir.r5.model.Patient;
import org.hl7.fhir.r5.model.Reference;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Ig1ProfileValidationTests {

    private static final String BASE = "http://example.org/fhir/ig1/StructureDefinition/";
    private static final String CARDX = "http://hl7.org/fhir/uv/cardx-cied/CodeSystem/CardXCIED";
    private static final String MDC = "urn:iso:std:iso:11073:10101";

    private static FhirValidator validator;

    @BeforeAll
    static void setUp() throws Exception {
        FhirContext ctx = FhirContext.forR5();
        validator = new FhirConfiguration()
                .fhirValidator(ctx, "ig1/package.tgz", "cardx-cied/package.tgz");
    }

    private static List<String> errors(ValidationResult result) {
        return result.getMessages().stream()
                .filter(m -> m.getSeverity() == ResultSeverityEnum.ERROR || m.getSeverity() == ResultSeverityEnum.FATAL)
                .map(m -> m.getLocationString() + ": " + m.getMessage())
                .toList();
    }

    private static Patient patient() {
        Patient p = new Patient();
        p.getMeta().addProfile(BASE + "ig1-patient");
        p.addIdentifier().setSystem("http://example.org/fhir/ig1/NamingSystem/merlin-net-number").setValue("123456");
        p.setActive(true);
        p.addName().setFamily("Doe").addGiven("John").addGiven("Q");
        p.setGender(Enumerations.AdministrativeGender.MALE);
        p.addTelecom().setSystem(ContactPointSystem.PHONE).setUse(ContactPointUse.MOBILE).setValue("+15551234567");
        p.setBirthDateElement(new org.hl7.fhir.r5.model.DateType("1980-05-17"));
        return p;
    }

    private static Device device(String profile, String system, String code) {
        Device d = new Device();
        d.getMeta().addProfile(BASE + profile);
        d.addIdentifier().setSystem("http://example.org/fhir/ig1/NamingSystem/patient-device-id").setValue("42");
        d.setStatus(Device.FHIRDeviceStatus.ACTIVE);
        d.setManufacturer("Abbott");
        d.setSerialNumber("SN-1");
        d.setModelNumber("M-1");
        d.addType().addCoding().setSystem(system).setCode(code);
        return d;
    }

    private static Observation observation() {
        Observation o = new Observation();
        o.getMeta().addProfile(BASE + "ig1-cied-connectivity-observation");
        o.addExtension("http://hl7.org/fhir/uv/cardx-cied/StructureDefinition/cied-device-extension",
                new Reference("Device/1"));
        o.setStatus(Enumerations.ObservationStatus.FINAL);
        o.getCode().addCoding().setSystem(CARDX).setCode("observation-cied-connectivity");
        o.setSubject(new Reference("Patient/1"));
        o.setDevice(new Reference("Device/2"));
        o.setEffective(new org.hl7.fhir.r5.model.DateTimeType(new Date()));
        Observation.ObservationComponentComponent status = o.addComponent();
        status.getCode().addCoding().setSystem(CARDX).setCode("connectivity-status");
        status.setValue(new org.hl7.fhir.r5.model.CodeableConcept().addCoding(
                new org.hl7.fhir.r5.model.Coding(CARDX, "connected", null)));
        return o;
    }

    @Test
    void conformingPatientPasses() {
        assertEquals(List.of(), errors(validator.validateWithResult(patient())));
    }

    @Test
    void patientWithoutMerlinNumberOrWithPartialBirthDateFails() {
        Patient p = patient();
        p.getIdentifier().clear();
        p.setBirthDateElement(new org.hl7.fhir.r5.model.DateType("1980-05"));
        List<String> errs = errors(validator.validateWithResult(p));
        assertTrue(errs.stream().anyMatch(e -> e.contains("identifier")), errs.toString());
        assertTrue(errs.stream().anyMatch(e -> e.contains("birthDate")), errs.toString());
    }

    @Test
    void conformingDeviceAndMonitorPass() {
        assertEquals(
                List.of(),
                errors(validator.validateWithResult(device("ig1-cied-device", MDC, "753666")))
        );
        assertEquals(
                List.of(),
                errors(validator.validateWithResult(device("ig1-cied-monitor", CARDX, "bedside-device")))
        );
    }

    @Test
    void deviceWithUnknownTypeOrMissingSerialFails() {
        Device d = device("ig1-cied-device", MDC, "999999");
        d.setSerialNumber(null);
        List<String> errs = errors(validator.validateWithResult(d));
        assertTrue(errs.stream().anyMatch(e -> e.contains("serialNumber")), errs.toString());
        assertTrue(errs.stream().anyMatch(e -> e.contains("999999")), errs.toString());
    }

    @Test
    void conformingObservationPasses() {
        assertEquals(List.of(), errors(validator.validateWithResult(observation())));
    }

    @Test
    void observationWithoutConnectivityStatusOrWithBadValueFails() {
        Observation missing = observation();
        missing.getComponent().clear();
        assertTrue(errors(validator.validateWithResult(missing)).stream()
                .anyMatch(e -> e.contains("component")));

        Observation bad = observation();
        bad.getComponentFirstRep().setValue(
                new org.hl7.fhir.r5.model.CodeableConcept().addCoding(
                    new org.hl7.fhir.r5.model.Coding(CARDX, "bogus", null)
                )
        );
        assertTrue(errors(validator.validateWithResult(bad)).stream().anyMatch(e -> e.contains("bogus")));
    }
}
