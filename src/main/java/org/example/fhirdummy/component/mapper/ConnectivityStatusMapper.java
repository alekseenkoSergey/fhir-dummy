package org.example.fhirdummy.component.mapper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.example.fhirdummy.utility.Ids;
import org.example.fhirdummy.utility.JsonText;
import org.hl7.fhir.r5.model.CodeableConcept;
import org.hl7.fhir.r5.model.Coding;
import org.hl7.fhir.r5.model.DateTimeType;
import org.hl7.fhir.r5.model.Enumerations.ObservationStatus;
import org.hl7.fhir.r5.model.Observation;
import org.hl7.fhir.r5.model.Reference;
import org.hl7.fhir.r5.model.StringType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
public class ConnectivityStatusMapper {
    static final String CARDX = "http://hl7.org/fhir/uv/cardx-cied/CodeSystem/CardXCIED";
    static final String PROFILE_BASE = "http://hl7.org/fhir/uv/cardx-cied/StructureDefinition/";
    static final String DEVICE_EXTENSION = PROFILE_BASE + "cied-device-extension";

    public static final String COUNTER = "counter";

    public List<Observation> toFhir(JsonNode root, String clinicId) {
        List<Observation> observations = new ArrayList<>();
        for (JsonNode patientNode : root.path("patients")) {
            String merlinNetId = JsonText.optional(patientNode, "merlinNetId");
            String patientId = Ids.join(clinicId, merlinNetId);

            JsonNode transmitter = patientNode.path("transmitter");
            String monitorId = transmitter.isObject()
                    ? Ids.join(clinicId, merlinNetId,
                            JsonText.optional(transmitter, "transmitterModelNum"),
                            JsonText.optional(transmitter, "transmitterSerialNum"))
                    : null;

            int observationNumber = 0;
            for (JsonNode deviceNode : patientNode.path("devices")) {
                String deviceId = Ids.join(clinicId, merlinNetId, deviceIdentifier(deviceNode));
                String counter = Ids.join(clinicId, merlinNetId, String.valueOf(++observationNumber));
                observations.add(observation(counter, patientNode, deviceNode, patientId, deviceId, clinicId,
                        monitorId));
            }
        }
        return observations;
    }

    private static String deviceIdentifier(JsonNode device) {
        String id = JsonText.identifier(device, "deviceId");
        if (id == null) {
            throw new IllegalArgumentException("Device identifier is required");
        }
        return id;
    }

    private Observation observation(String counter, JsonNode patientNode, JsonNode deviceNode, String patientId, String deviceId,
                                    String clinicId, String monitorId) {
        boolean forMonitor = monitorId != null;
        Observation obs = new Observation();
        obs.setUserData(COUNTER, counter);
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

        // The upstream connectivityStatus carries both the CardX status and modifier concepts.
        String raw = JsonText.optional(patientNode, "connectivityStatus");
        String modifier = null;
        if (raw != null) {
            switch (raw.toUpperCase(Locale.ROOT)) {
                case "NOT_SETUP" -> modifier = "setup-not-completed";
                case "UNENROLLED" -> modifier = "unenrolled";
                case "TRANSFERRED" -> modifier = "transferred";
                // CONNECTED, DISCONNECTED and anything else: passed through, the IG validates the value.
                default -> obs.addComponent(codeComponent("connectivity-status", raw.toLowerCase(Locale.ROOT)));
            }
        }
        if (modifier == null && JsonText.optional(deviceNode, "soonzeUntilDate") != null) modifier = "suspended";
        if (modifier != null) obs.addComponent(codeComponent("connectivity-modifier", modifier));

        dateComponent(obs, "last-cied-connectivity-time", lastComm);
        if (forMonitor) dateComponent(obs, "last-monitor-connectivity-time", lastCheck);

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
