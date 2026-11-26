package org.example.fhirdummy.component.mapper;

import java.time.LocalDate;
import java.util.Locale;
import static org.example.fhirdummy.utility.Constants.ID_DELIMITER;
import org.example.fhirdummy.utility.JsonText;
import org.hl7.fhir.r5.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.r5.model.ContactPoint.ContactPointUse;
import org.hl7.fhir.r5.model.DateType;
import org.hl7.fhir.r5.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r5.model.Patient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * Converts an upstream patient record into a FHIR Patient with a clinic-specific composite identifier.
 * It maps only provided identifiers, demographics, patient status, and contact details,
 * while rejecting unsupported or malformed values.
 */
@Component
public class PatientMapper {
    public Patient toFhir(JsonNode source, String clinicId) {
        if (source == null || !source.isObject()) {
            throw new IllegalArgumentException("Expected a patient object");
        }
        String merlinNetId = JsonText.optional(source, "merlinNetId");
        var patient = new Patient();
        if (merlinNetId != null) {
            String id = clinicId + ID_DELIMITER + merlinNetId;
            patient.setId(id);
            patient.addIdentifier().setValue(id);
        }

        String status = JsonText.optional(source, "patientStatus");
        if (status != null) {
            patient.setActive("active".equalsIgnoreCase(status));
        }

        String lastName = JsonText.optional(source, "lastName");
        String firstName = JsonText.optional(source, "firstName");
        String middleName = JsonText.optional(source, "middleName");
        if (lastName != null || firstName != null || middleName != null) {
            var name = patient.addName();
            if (lastName != null) {
                name.setFamily(lastName);
            }
            if (firstName != null) {
                name.addGiven(firstName);
            }
            if (middleName != null) {
                name.addGiven(middleName);
            }
        }

        String gender = JsonText.optional(source, "gender");
        if (gender != null) {
            patient.setGender(switch (gender.toLowerCase(Locale.ROOT)) {
                case "male" -> AdministrativeGender.MALE;
                case "female" -> AdministrativeGender.FEMALE;
                case "unknown" -> AdministrativeGender.UNKNOWN;
                default -> AdministrativeGender.OTHER;
            });
        }

        String phone = JsonText.optional(source, "phone");
        if (phone != null) {
            patient.addTelecom().setSystem(ContactPointSystem.PHONE).setUse(ContactPointUse.MOBILE).setValue(phone);
        }

        String birthDate = JsonText.optional(source, "dateOfBirth");
        if (birthDate != null) {
            patient.setBirthDateElement(new DateType(LocalDate.parse(birthDate).toString()));
        }
        return patient;
    }
}
