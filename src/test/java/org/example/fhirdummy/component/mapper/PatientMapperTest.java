package org.example.fhirdummy.component.mapper;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r5.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.r5.model.ContactPoint.ContactPointUse;
import org.hl7.fhir.r5.model.Enumerations.AdministrativeGender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;

import java.time.format.DateTimeParseException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PatientMapperTest {
    private final PatientMapper mapper = new PatientMapper();
    private final JsonMapper json = new JsonMapper();

    @Test
    void mapsCompletePatientAndTrimsText() {
        var source = json.readTree("""
                {"merlinNetId":" patient ","patientStatus":" ACTIVE ",
                 "lastName":" Doe ","firstName":" Jane ","middleName":" Mary ",
                 "gender":" FEMALE ","phone":" +12345 ","dateOfBirth":" 2000-02-29 "}
                """);

        var patient = mapper.toFhir(source, "clinic");

        assertEquals("clinic-patient", patient.getIdElement().getIdPart());
        assertEquals(1, patient.getIdentifier().size());
        assertEquals("clinic-patient", patient.getIdentifierFirstRep().getValue());
        assertTrue(patient.getActive());
        assertEquals(1, patient.getName().size());
        assertEquals("Doe", patient.getNameFirstRep().getFamily());
        assertEquals(List.of("Jane", "Mary"), patient.getNameFirstRep().getGiven().stream().map(v -> v.getValue()).toList());
        assertEquals(AdministrativeGender.FEMALE, patient.getGender());
        assertEquals(1, patient.getTelecom().size());
        var phone = patient.getTelecomFirstRep();
        assertEquals(ContactPointSystem.PHONE, phone.getSystem());
        assertEquals(ContactPointUse.MOBILE, phone.getUse());
        assertEquals("+12345", phone.getValue());
        assertEquals("2000-02-29", patient.getBirthDateElement().getValueAsString());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"merlinNetId\":\"patient\"}",
            """
            {"merlinNetId":"patient","patientStatus":null,"lastName":null,"firstName":null,
             "middleName":null,"gender":null,"phone":null,"dateOfBirth":null}
            """,
            """
            {"merlinNetId":"patient","patientStatus":" ","lastName":" ","firstName":" ",
             "middleName":" ","gender":" ","phone":" ","dateOfBirth":" "}
            """})
    void handlesMissingNullAndBlankOptionalFields(String input) {
        var patient = mapper.toFhir(json.readTree(input), "clinic");

        assertFalse(patient.hasActive());
        assertFalse(patient.getNameFirstRep().hasFamily());
        assertFalse(patient.getNameFirstRep().hasGiven());
        assertEquals(AdministrativeGender.UNKNOWN, patient.getGender());
        assertFalse(patient.hasTelecom());
        assertFalse(patient.hasBirthDate());
    }

    @ParameterizedTest
    @CsvSource({"active,true", "AcTiVe,true", "inactive,false", "INACTIVE,false"})
    void mapsStatusIgnoringCase(String status, boolean active) {
        var source = json.readTree("{\"merlinNetId\":\"patient\",\"patientStatus\":\"" + status + "\"}");

        var patient = mapper.toFhir(source, "clinic");

        assertTrue(patient.hasActive());
        assertEquals(active, patient.getActive());
    }

    @ParameterizedTest
    @CsvSource({"MALE,MALE", "Female,FEMALE", "other,OTHER", "unknown,UNKNOWN"})
    void mapsAllSupportedGendersIgnoringCase(String value, AdministrativeGender gender) {
        var source = json.readTree("{\"merlinNetId\":\"patient\",\"gender\":\"" + value + "\"}");

        assertEquals(gender, mapper.toFhir(source, "clinic").getGender());
    }

    @ParameterizedTest
    @CsvSource({"firstName,Jane", "middleName,Mary"})
    void mapsGivenNamesIndependently(String field, String value) {
        var source = json.readTree("{\"merlinNetId\":\"patient\",\"" + field + "\":\"" + value + "\"}");

        var given = mapper.toFhir(source, "clinic").getNameFirstRep().getGiven();

        assertEquals(1, given.size());
        assertEquals(value, given.get(0).getValue());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"null", "[]", "true", "42", "\"patient\""})
    void rejectsNonObjectSources(String input) {
        var source = input == null ? null : json.readTree(input);

        assertEquals("Expected a patient object",
                assertThrows(IllegalArgumentException.class, () -> mapper.toFhir(source, "clinic")).getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"merlinNetId\":null}", "{\"merlinNetId\":\" \"}"})
    void requiresPatientIdentifier(String input) {
        assertEquals("Patient identifier is required",
                assertThrows(IllegalArgumentException.class, () -> mapper.toFhir(json.readTree(input), "clinic")).getMessage());
    }

    @Test
    void rejectsUnsupportedStatus() {
        var source = json.readTree("{\"merlinNetId\":\"patient\",\"patientStatus\":\"deleted\"}");

        assertEquals("Unsupported patient status",
                assertThrows(IllegalArgumentException.class, () -> mapper.toFhir(source, "clinic")).getMessage());
    }

    @Test
    void rejectsUnsupportedGender() {
        var source = json.readTree("{\"merlinNetId\":\"patient\",\"gender\":\"unsupported\"}");

        assertThrows(FHIRException.class, () -> mapper.toFhir(source, "clinic"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2001-02-29", "2020-13-01", "01/02/2000", "invalid"})
    void rejectsMalformedBirthDates(String date) {
        var source = json.readTree("{\"merlinNetId\":\"patient\",\"dateOfBirth\":\"" + date + "\"}");

        assertThrows(DateTimeParseException.class, () -> mapper.toFhir(source, "clinic"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"merlinNetId", "patientStatus", "lastName", "firstName", "middleName", "gender", "phone", "dateOfBirth"})
    void rejectsNonTextFields(String field) {
        var source = json.createObjectNode().put("merlinNetId", "patient").put(field, 42);

        assertEquals("Expected text for field " + field,
                assertThrows(IllegalArgumentException.class, () -> mapper.toFhir(source, "clinic")).getMessage());
    }
}
