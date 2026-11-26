package org.example.fhirdummy.component.mapper;

import ca.uhn.fhir.context.FhirContext;
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
        assertTrue(patient.getName().isEmpty());
        assertFalse(patient.hasGender());
        assertFalse(patient.hasTelecom());
        assertFalse(patient.hasBirthDate());
    }

    @ParameterizedTest
    @CsvSource({"Active,true", "active,true", "AcTiVe,true", "inactive,false", "INACTIVE,false",
            "deleted,false", "unknown,false", "pending,false", "true,false"})
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

    @Test
    void mapsFamilyNameWithoutGivenNamesOrIdentifier() {
        var patient = mapper.toFhir(json.readTree("{\"lastName\":\" Doe \"}"), "clinic");

        assertEquals(1, patient.getName().size());
        assertEquals("Doe", patient.getNameFirstRep().getFamily());
        assertFalse(patient.getNameFirstRep().hasGiven());
        assertFalse(patient.hasId());
        assertFalse(patient.hasIdentifier());
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
    void omitsAbsentIdentifierAndStillMapsOtherFields(String input) {
        var source = (tools.jackson.databind.node.ObjectNode) json.readTree(input);
        source.put("phone", "+12345");

        var patient = mapper.toFhir(source, "clinic");

        assertFalse(patient.hasId());
        assertFalse(patient.hasIdentifier());
        assertEquals("+12345", patient.getTelecomFirstRep().getValue());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}",
            """
            {"merlinNetId":null,"patientStatus":null,"lastName":null,"firstName":null,
             "middleName":null,"gender":null,"phone":null,"dateOfBirth":null}
            """,
            """
            {"merlinNetId":" ","patientStatus":" ","lastName":" ","firstName":" ",
             "middleName":" ","gender":" ","phone":" ","dateOfBirth":" "}
            """})
    void omitsAllAbsentFieldsFromPatientAndSerializedResponse(String input) {
        var patient = mapper.toFhir(json.readTree(input), "clinic");

        assertFalse(patient.hasId());
        assertTrue(patient.getIdentifier().isEmpty());
        assertFalse(patient.hasActive());
        assertTrue(patient.getName().isEmpty());
        assertFalse(patient.hasGender());
        assertTrue(patient.getTelecom().isEmpty());
        assertFalse(patient.hasBirthDate());
        var serialized = FhirContext.forR5Cached().newJsonParser().encodeResourceToString(patient);
        assertEquals(json.readTree("{\"resourceType\":\"Patient\"}"), json.readTree(serialized));
    }

    @ParameterizedTest
    @ValueSource(strings = {"unsupported", "UNRECOGNIZED", " nonbinary ", "123"})
    void mapsUnsupportedGenderToOther(String gender) {
        var source = json.createObjectNode().put("gender", gender);

        var patient = mapper.toFhir(source, "clinic");

        assertTrue(patient.hasGender());
        assertEquals(AdministrativeGender.OTHER, patient.getGender());
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
