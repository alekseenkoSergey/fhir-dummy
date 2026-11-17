package org.example.fhirdummy.mapper;

import org.hl7.fhir.r5.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.r5.model.ContactPoint.ContactPointUse;
import org.hl7.fhir.r5.model.DateType;
import org.hl7.fhir.r5.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r5.model.Patient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.util.Locale;

@Component
public class PatientMapper {
    public Patient toFhir(JsonNode source) {
        if (source == null || !source.isObject()) {
            throw new IllegalArgumentException("Expected a patient object");
        }
        String id = text(source, "merlinNetId");
        if (id == null) {
            throw new IllegalArgumentException("Patient identifier is required");
        }

        var patient = new Patient();
        patient.setId(id);
        patient.addIdentifier().setValue(id);

        String status = text(source, "patientStatus");
        if (status != null) {
            patient.setActive(switch (status.toLowerCase(Locale.ROOT)) {
                case "active" -> true;
                case "inactive" -> false;
                default -> throw new IllegalArgumentException("Unsupported patient status");
            });
        }

        var name = patient.getNameFirstRep().setFamily(text(source, "lastName"));
        String firstName = text(source, "firstName");
        if (firstName != null) {
            name.addGiven(firstName);
        }
        String middleName = text(source, "middleName");
        if (middleName != null) {
            name.addGiven(middleName);
        }

        String gender = text(source, "gender");
        patient.setGender(gender == null ? AdministrativeGender.UNKNOWN
                : AdministrativeGender.fromCode(gender.toLowerCase(Locale.ROOT)));

        String phone = text(source, "phone");
        if (phone != null) {
            patient.addTelecom().setSystem(ContactPointSystem.PHONE).setUse(ContactPointUse.MOBILE).setValue(phone);
        }

        String birthDate = text(source, "dateOfBirth");
        if (birthDate != null) {
            patient.setBirthDateElement(new DateType(LocalDate.parse(birthDate).toString()));
        }
        return patient;
    }

    private static String text(JsonNode source, String field) {
        JsonNode value = source.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        if (!value.isString()) {
            throw new IllegalArgumentException("Expected text for field " + field);
        }
        String text = value.asString().trim();
        return text.isEmpty() ? null : text;
    }
}
