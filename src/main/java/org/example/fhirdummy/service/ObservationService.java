package org.example.fhirdummy.service;

import ca.uhn.fhir.validation.FhirValidator;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.example.fhirdummy.client.DataServiceClient;
import org.example.fhirdummy.component.mapper.ConnectivityStatusMapper;
import org.example.fhirdummy.component.mapper.DeviceMapper;
import org.example.fhirdummy.component.mapper.MonitorMapper;
import org.example.fhirdummy.component.mapper.PatientMapper;
import org.example.fhirdummy.utility.Ids;
import org.example.fhirdummy.utility.JsonText;
import org.example.fhirdummy.utility.ProfileValidation;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Bundle.SearchEntryMode;
import org.hl7.fhir.r5.model.Observation;
import org.hl7.fhir.r5.model.OperationOutcome;
import org.hl7.fhir.r5.model.Reference;
import org.hl7.fhir.r5.model.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

/**
 * Builds a FHIR search bundle from a clinic's device connectivity data.
 * It validates mapped observations and optionally includes the patients, monitors, and implanted devices they reference.
 * Resources that fail validation are reported as OperationOutcome entries alongside the accepted results.
 */
@Service
public class ObservationService {
    public static final String INCLUDE_SUBJECT = "Observation:subject";
    public static final String INCLUDE_DEVICE = "Observation:device";
    public static final String INCLUDE_CIED_DEVICE = "Observation:cied-device";
    public static final Set<String> SUPPORTED_INCLUDES = Set.of(INCLUDE_SUBJECT, INCLUDE_DEVICE, INCLUDE_CIED_DEVICE);

    private final DataServiceClient client;
    private final FhirValidator validator;

    private final ConnectivityStatusMapper observationMapper;
    private final PatientMapper patientMapper;
    private final DeviceMapper deviceMapper;
    private final MonitorMapper monitorMapper;

    public ObservationService(
            DataServiceClient client, ConnectivityStatusMapper observationMapper, PatientMapper patientMapper,
            DeviceMapper deviceMapper, MonitorMapper monitorMapper, FhirValidator validator
    ) {
        this.client = client;
        this.observationMapper = observationMapper;
        this.patientMapper = patientMapper;
        this.deviceMapper = deviceMapper;
        this.monitorMapper = monitorMapper;
        this.validator = validator;
    }

    public Bundle getConnectivityBundle(String clinicId, Set<String> includes) {
        var source = client.getDeviceConnectivity(clinicId);
        if (source == null || !source.isObject()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid device connectivity data from the data service");
        }
        List<Observation> observations;
        try {
            observations = observationMapper.toFhir(source, clinicId);
        } catch (IllegalArgumentException | DateTimeParseException | FHIRException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid device connectivity data from the data service", exception);
        }

        List<Observation> conforming = new ArrayList<>();
        List<OperationOutcome> rejected = new ArrayList<>();
        for (Observation observation : observations) {
            List<String> errors = ProfileValidation.errors(validator.validateWithResult(observation));
            if (errors.isEmpty()) {
                conforming.add(observation);
            } else {
                rejected.add(outcome("Observation with counter " +
                        observation.getUserData(ConnectivityStatusMapper.COUNTER), errors));
            }
        }

        List<Resource> included = new ArrayList<>();
        if (!includes.isEmpty()) {
            includeReferenced(source, clinicId, includes, conforming, included, rejected);
        }

        Bundle result = new Bundle();
        result.setType(Bundle.BundleType.SEARCHSET);
        result.setTimestamp(new java.util.Date());
        result.setTotal(conforming.size());
        conforming.forEach(observation -> add(result, null, observation, SearchEntryMode.MATCH));
        included.forEach(resource -> add(result, resource.fhirType() + "/" + resource.getIdElement().getIdPart(),
                resource, SearchEntryMode.INCLUDE));
        rejected.forEach(outcome -> add(result, null, outcome, SearchEntryMode.OUTCOME));
        return result;
    }

    private void includeReferenced(
            JsonNode source, String clinicId, Set<String> includes, List<Observation> observations,
            List<Resource> included, List<OperationOutcome> rejected
    ) {
        Set<String> patientRefs = new LinkedHashSet<>();
        Set<String> monitorRefs = new LinkedHashSet<>();
        Set<String> deviceRefs = new LinkedHashSet<>();
        for (Observation observation : observations) {
            if (observation.getSubject().hasReference()) {
                patientRefs.add(observation.getSubject().getReference());
            }
            if (observation.getDevice().hasReference()) {
                monitorRefs.add(observation.getDevice().getReference());
            }
            for (var extension : observation.getExtension()) {
                if (extension.getValue() instanceof Reference device && device.hasReference()) {
                    deviceRefs.add(device.getReference());
                }
            }
        }

        for (JsonNode patientNode : source.path("patients")) {
            String merlinNetId = JsonText.optional(patientNode, "merlinNetId");
            String patientKey = "Patient/" + Ids.join(clinicId, merlinNetId);
            if (includes.contains(INCLUDE_SUBJECT) && patientRefs.contains(patientKey)) {
                tryInclude(patientKey, ProfileValidation.PATIENT_PROFILE,
                        () -> patientMapper.toFhir(patientNode, clinicId), included, rejected);
            }

            JsonNode transmitter = patientNode.path("transmitter");
            if (includes.contains(INCLUDE_DEVICE) && transmitter.isObject()) {
                String monitorKey = "Device/" + Ids.join(clinicId, merlinNetId,
                        JsonText.optional(transmitter, "transmitterModelNum"),
                        JsonText.optional(transmitter, "transmitterSerialNum"));
                if (monitorRefs.contains(monitorKey)) {
                    tryInclude(monitorKey, ProfileValidation.MONITOR_PROFILE,
                            () -> monitorMapper.toFhir(transmitter, clinicId, merlinNetId), included, rejected);
                }
            }

            if (!includes.contains(INCLUDE_CIED_DEVICE)) {
                continue;
            }
            for (JsonNode deviceNode : patientNode.path("devices")) {
                String deviceId = JsonText.identifier(deviceNode, "deviceId");
                String key = "Device/" + Ids.join(clinicId, merlinNetId, deviceId);
                if (deviceRefs.contains(key)) {
                    tryInclude(key, ProfileValidation.DEVICE_PROFILE,
                            () -> deviceMapper.toFhir(deviceNode, clinicId, merlinNetId, deviceId), included, rejected);
                }
            }
        }
    }

    private void tryInclude(
            String key, String profile, Supplier<? extends Resource> mapping, List<Resource> included,
            List<OperationOutcome> rejected
    ) {
        Resource resource;
        try {
            resource = mapping.get();
        } catch (IllegalArgumentException | DateTimeParseException | FHIRException exception) {
            rejected.add(outcome(key, List.of(exception.getMessage())));
            return;
        }
        resource.getMeta().addProfile(profile);
        List<String> errors = ProfileValidation.errors(validator.validateWithResult(resource));
        if (errors.isEmpty()) {
            included.add(resource);
        } else {
            rejected.add(outcome(key, errors));
        }
    }

    private static void add(Bundle bundle, String fullUrl, Resource resource, SearchEntryMode mode) {
        var entry = bundle.addEntry().setResource(resource);
        if (fullUrl != null) {
            entry.setFullUrl(fullUrl);
        }
        entry.getSearch().setMode(mode);
    }

    private static OperationOutcome outcome(String subject, List<String> errors) {
        var outcome = new OperationOutcome();
        for (String error : errors) {
            outcome.addIssue()
                    .setSeverity(OperationOutcome.IssueSeverity.ERROR)
                    .setCode(OperationOutcome.IssueType.INVALID)
                    .setDiagnostics(subject + " does not conform to the IG: " + error);
        }
        return outcome;
    }

    public Bundle getPatientConnectivityBundle(String clinicId, String patientId, Set<String> include) {
        return null;
    }
}
