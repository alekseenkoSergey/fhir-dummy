package org.example.fhirdummy.observation.service;

import ca.uhn.fhir.validation.FhirValidator;
import org.example.fhirdummy.emulator.client.DataServiceClient;
import org.example.fhirdummy.utility.ProfileValidation;
import org.example.fhirdummy.observation.mapper.ConnectivityStatusMapper;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Bundle.SearchEntryMode;
import org.hl7.fhir.r5.model.Device;
import org.hl7.fhir.r5.model.Observation;
import org.hl7.fhir.r5.model.OperationOutcome;
import org.hl7.fhir.r5.model.Reference;
import org.hl7.fhir.r5.model.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ObservationService {
    private final DataServiceClient client;
    private final ConnectivityStatusMapper mapper;
    private final FhirValidator validator;

    public ObservationService(DataServiceClient client, ConnectivityStatusMapper mapper, FhirValidator validator) {
        this.client = client;
        this.mapper = mapper;
        this.validator = validator;
    }

    public Bundle getConnectivityBundle(String clinicId) {
        var source = client.getDeviceConnectivity(clinicId);
        if (source == null || !source.isObject()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid device connectivity data from the data service");
        }
        Bundle mapped;
        try {
            mapped = mapper.toFhir(source, clinicId);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid device connectivity data from the data service", exception);
        }
        return conformingOnly(mapped);
    }

    private Bundle conformingOnly(Bundle mapped) {
        Map<String, Resource> resources = new LinkedHashMap<>();
        mapped.getEntry().forEach(entry -> resources.put(entry.getFullUrl(), entry.getResource()));

        Map<String, List<String>> ownErrors = new HashMap<>();
        resources.forEach((key, resource) ->
                ownErrors.put(key, ProfileValidation.errors(validator.validateWithResult(resource))));

        Set<String> conformingObservations = new LinkedHashSet<>();
        Set<String> included = new LinkedHashSet<>();
        List<OperationOutcome> rejected = new ArrayList<>();
        for (var entry : mapped.getEntry()) {
            if (entry.getSearch().getMode() != SearchEntryMode.MATCH) {
                continue;
            }
            String key = entry.getFullUrl();
            Set<String> dependencies = new LinkedHashSet<>();
            collectDependencies(entry.getResource(), resources, dependencies);

            List<String> errors = new ArrayList<>(ownErrors.get(key));
            for (String dependency : dependencies) {
                ownErrors.get(dependency).forEach(error -> errors.add(dependency + ": " + error));
            }
            if (errors.isEmpty()) {
                conformingObservations.add(key);
                included.addAll(dependencies);
            } else {
                rejected.add(outcome(key, errors));
            }
        }

        Bundle result = new Bundle();
        result.setType(Bundle.BundleType.SEARCHSET);
        result.setTimestamp(mapped.getTimestamp());
        result.setTotal(conformingObservations.size());
        conformingObservations.forEach(key -> add(result, key, resources.get(key), SearchEntryMode.MATCH));
        included.forEach(key -> add(result, key, resources.get(key), SearchEntryMode.INCLUDE));
        rejected.forEach(outcome -> add(result, null, outcome, SearchEntryMode.OUTCOME));
        return result;
    }

    private static void add(Bundle bundle, String fullUrl, Resource resource, SearchEntryMode mode) {
        var entry = bundle.addEntry().setResource(resource);
        if (fullUrl != null) {
            entry.setFullUrl(fullUrl);
        }
        entry.getSearch().setMode(mode);
    }

    private static OperationOutcome outcome(String key, List<String> errors) {
        var outcome = new OperationOutcome();
        for (String error : errors) {
            outcome.addIssue()
                    .setSeverity(OperationOutcome.IssueSeverity.ERROR)
                    .setCode(OperationOutcome.IssueType.INVALID)
                    .setDiagnostics(key + " does not conform to the IG: " + error);
        }
        return outcome;
    }

    private static void collectDependencies(Resource resource, Map<String, Resource> resources, Set<String> found) {
        for (String reference : references(resource)) {
            if (resources.containsKey(reference) && found.add(reference)) {
                collectDependencies(resources.get(reference), resources, found);
            }
        }
    }

    private static List<String> references(Resource resource) {
        List<String> references = new ArrayList<>();
        if (resource instanceof Observation observation) {
            addReference(references, observation.getSubject());
            observation.getPerformer().forEach(performer -> addReference(references, performer));
            addReference(references, observation.getDevice());
            observation.getExtension().forEach(extension -> {
                if (extension.getValue() instanceof Reference reference) {
                    addReference(references, reference);
                }
            });
        } else if (resource instanceof Device device) {
            device.getGateway().forEach(gateway -> addReference(references, gateway.getReference()));
        }
        return references;
    }

    private static void addReference(List<String> references, Reference reference) {
        if (reference != null && reference.hasReference()) {
            references.add(reference.getReference());
        }
    }
}
