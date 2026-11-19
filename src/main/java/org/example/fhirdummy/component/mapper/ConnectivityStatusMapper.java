package org.example.fhirdummy.component.mapper;

import java.time.LocalDate;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import static org.example.fhirdummy.utility.Constants.ID_DELIMITER;
import org.example.fhirdummy.utility.JsonText;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r5.model.*;
import org.hl7.fhir.r5.model.Bundle.BundleType;
import org.hl7.fhir.r5.model.Bundle.SearchEntryMode;
import org.hl7.fhir.r5.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r5.model.Enumerations.ObservationStatus;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
public class ConnectivityStatusMapper {
    static final String CARDX = "http://hl7.org/fhir/uv/cardx-cied/CodeSystem/CardXCIED";
    static final String PROFILE_BASE = "http://hl7.org/fhir/uv/cardx-cied/StructureDefinition/";
    static final String DEVICE_EXTENSION = PROFILE_BASE + "cied-device-extension";
    static final String MANUFACTURER = "Abbott";

    public Bundle toFhir(JsonNode root, String clinicId) {
        Map<String, Resource> observations = new LinkedHashMap<>();
        Map<String, Resource> included = new LinkedHashMap<>();

        Organization clinic = new Organization();
        clinic.setId(clinicId);
        clinic.setName(JsonText.optional(root.path("clinic"), "name"));
        included.put("Organization/" + clinicId, clinic);

        for (JsonNode patientNode : root.path("patients")) {
            String patientId = JsonText.optional(patientNode, "merlinNetId");
            if (patientId == null) {
                throw new IllegalArgumentException("Patient identifier is required");
            }
            Patient patient = patient(patientNode, patientId);
            included.put("Patient/" + patientId, patient);

            // The monitor is identified by its transmitter serial number; without one it is not emitted.
            JsonNode transmitter = patientNode.path("transmitter");
            String monitorId = transmitter.isObject() ? JsonText.optional(transmitter, "transmitterSerialNum") : null;
            if (monitorId != null) {
                included.put("Device/" + monitorId, monitor(transmitter, monitorId));
            }

            int index = 0;
            for (JsonNode deviceNode : patientNode.path("devices")) {
                String serial = JsonText.optional(deviceNode, "deviceSerialNum");
                String deviceId = serial != null ? serial : patientId + ID_DELIMITER + index;
                index++;
                Device device = device(deviceNode, deviceId, serial, monitorId);
                included.put("Device/" + deviceId, device);

                Observation obs = observation(patientNode, deviceNode, patientId, deviceId, clinicId,
                        monitorId);
                observations.put("Observation/" + obs.getIdElement().getIdPart(), obs);
            }
        }

        Bundle bundle = new Bundle();
        bundle.setType(BundleType.SEARCHSET);
        bundle.setTimestamp(new Date());
        bundle.setTotal(observations.size());
        observations.forEach((key, resource) -> add(bundle, key, resource, SearchEntryMode.MATCH));
        included.forEach((key, resource) -> add(bundle, key, resource, SearchEntryMode.INCLUDE));
        return bundle;
    }

    private static void add(Bundle bundle, String fullUrl, Resource resource, SearchEntryMode mode) {
        Bundle.BundleEntryComponent entry = bundle.addEntry()
                .setFullUrl(fullUrl)
                .setResource(resource);

        entry.getSearch().setMode(mode);
    }

    private Patient patient(JsonNode node, String id) {
        Patient patient = new Patient();
        patient.setId(id);
        patient.getMeta().addProfile(PROFILE_BASE + "cied-patient");
        patient.addIdentifier().setValue(id);

        String first = JsonText.optional(node, "firstName");
        String middle = JsonText.optional(node, "middleName");
        String last = JsonText.optional(node, "lastName");
        if (first != null || middle != null || last != null) {
            HumanName name = patient.addName();
            name.setFamily(last);
            if (first != null) name.addGiven(first);
            if (middle != null) name.addGiven(middle);
        }
        String phone = JsonText.optional(node, "phone");
        if (phone != null)
            patient.addTelecom()
                    .setSystem(ContactPoint.ContactPointSystem.PHONE)
                    .setValue(phone);

        String email = JsonText.optional(node, "email");
        if (email != null)
            patient.addTelecom()
                    .setSystem(ContactPoint.ContactPointSystem.EMAIL)
                    .setValue(email);

        String gender = JsonText.optional(node, "gender");
        if (gender != null) {
            try {
                patient.setGender(AdministrativeGender.fromCode(gender.toLowerCase(Locale.ROOT)));
            } catch (FHIRException exception) {
                throw new IllegalArgumentException("Unsupported patient gender", exception);
            }
        }
        String dob = JsonText.optional(node, "dateOfBirth");
        if (dob != null) {
            patient.setBirthDateElement(new DateType(LocalDate.parse(dob).toString()));
        }
        JsonNode addr = node.path("address");
        if (addr.isObject()) {
            Address address = new Address();
            for (String line : new String[]{"address1", "address2", "address3"}) {
                String value = JsonText.optional(addr, line);
                if (value != null) address.addLine(value);
            }
            address.setCity(JsonText.optional(addr, "city"));
            address.setState(JsonText.optional(addr, "state"));
            address.setCountry(JsonText.optional(addr, "country"));
            address.setPostalCode(JsonText.optional(addr, "zipCode"));
            if (!address.isEmpty()) patient.addAddress(address);
        }
        return patient;
    }

    private Device device(JsonNode node, String id, String serial, String monitorId) {
        Device device = new Device();
        device.setId(id);
        device.getMeta().addProfile(PROFILE_BASE + "cied-device");
        device.setManufacturer(MANUFACTURER);
        device.setSerialNumber(serial);
        device.setModelNumber(JsonText.optional(node, "deviceModelNum"));
        String description = JsonText.optional(node, "deviceDescription");
        if (description != null) {
            device.addType().setText(description);
        }
        if (monitorId != null) {
            device.addGateway().setReference(new Reference("Device/" + monitorId));
        }
        return device;
    }

    private Device monitor(JsonNode node, String id) {
        Device device = new Device();
        device.setId(id);
        device.getMeta().addProfile(PROFILE_BASE + "cied-monitor");
        device.setManufacturer(MANUFACTURER);
        device.setSerialNumber(JsonText.optional(node, "transmitterSerialNum"));
        device.setModelNumber(JsonText.optional(node, "transmitterModelNum"));
        return device;
    }

    private Observation observation(JsonNode patientNode, JsonNode deviceNode, String patientId, String deviceId,
                                    String clinicId, String monitorId) {
        boolean forMonitor = monitorId != null;
        Observation obs = new Observation();
        obs.setId("connectivity" + ID_DELIMITER + deviceId);
        obs.getMeta().addProfile(PROFILE_BASE + "cied-connectivity-status");
        obs.addExtension().setUrl(DEVICE_EXTENSION).setValue(new Reference("Device/" + deviceId));
        obs.setStatus(ObservationStatus.FINAL);
        obs.getCode().addCoding().setSystem(CARDX).setCode("observation-cied-connectivity");
        obs.setSubject(new Reference("Patient/" + patientId));
        obs.addPerformer(new Reference("Organization/" + clinicId));
        if (forMonitor) obs.setDevice(new Reference("Device/" + monitorId));

        String lastComm = JsonText.optional(deviceNode, "lastCommunicationDate");
        String lastCheck = JsonText.optional(deviceNode, "lastDeviceCheckDate");
        String effective = lastComm != null ? lastComm : lastCheck;
        if (effective != null) {
            obs.setEffective(new DateTimeType(LocalDate.parse(effective).toString()));
        }

        String raw = JsonText.optional(patientNode, "connectivityStatus");
        if (raw != null) {
            obs.addComponent(codeComponent("connectivity-status", raw.toLowerCase(Locale.ROOT)));
        }

        String modifier = raw == null ? null : switch (raw.toUpperCase(Locale.ROOT)) {
            case "NOT_SETUP" -> "setup-not-completed";
            case "UNENROLLED" -> "unenrolled";
            case "TRANSFERRED" -> "transferred";
            default -> null;
        };
        if (modifier == null && JsonText.optional(deviceNode, "soonzeUntilDate") != null) modifier = "suspended";
        if (modifier != null) obs.addComponent(codeComponent("connectivity-modifier", modifier));

        dateComponent(obs, "last-cied-connectivity-time", lastComm);
        if (forMonitor) dateComponent(obs, "last-monitor-connectivity-time", lastComm);
        dateComponent(obs, "last-interrogation-date", lastCheck);

        String reason = JsonText.optional(patientNode, "patientStatus");
        if (reason != null) {
            obs.addComponent(stringComponent("status-reason", "Patient status: " + reason));
        }
        String snooze = JsonText.optional(deviceNode, "soonzeUntilDate");
        if (snooze != null) {
            obs.addComponent(stringComponent("status-guidance", "Alerts snoozed until " + snooze));
        }
        return obs;
    }

    private static Observation.ObservationComponentComponent codeComponent(String code, String value) {
        Observation.ObservationComponentComponent c = baseComponent(code);
        c.setValue(new CodeableConcept().addCoding(new Coding(CARDX, value, null)));
        return c;
    }

    private static Observation.ObservationComponentComponent stringComponent(String code, String value) {
        Observation.ObservationComponentComponent c = baseComponent(code);
        c.setValue(new StringType(value));
        return c;
    }

    private static void dateComponent(Observation obs, String code, String date) {
        if (date == null) return;
        obs.addComponent(baseComponent(code).setValue(new DateTimeType(LocalDate.parse(date).toString())));
    }

    private static Observation.ObservationComponentComponent baseComponent(String code) {
        Observation.ObservationComponentComponent c = new Observation.ObservationComponentComponent();
        c.getCode().addCoding().setSystem(CARDX).setCode(code);
        return c;
    }
}
