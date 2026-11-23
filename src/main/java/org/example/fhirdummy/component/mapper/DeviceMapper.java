package org.example.fhirdummy.component.mapper;

import static org.example.fhirdummy.utility.Constants.ID_DELIMITER;
import org.example.fhirdummy.utility.JsonText;
import org.hl7.fhir.common.hapi.validation.support.NpmPackageValidationSupport;
import org.hl7.fhir.r5.model.CodeType;
import org.hl7.fhir.r5.model.Coding;
import org.hl7.fhir.r5.model.Device;
import org.hl7.fhir.r5.model.StringType;
import org.hl7.fhir.r5.model.ValueSet;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.List;

@Component
public class DeviceMapper {
    private static final String DATA_ABSENT_REASON = "http://hl7.org/fhir/StructureDefinition/data-absent-reason";
    private static final String DEVICE_TYPES_VALUE_SET = "http://hl7.org/fhir/uv/cardx-cied/ValueSet/MDCIDCDeviceTypes";
    private final List<Coding> deviceTypes;

    public DeviceMapper(NpmPackageValidationSupport validationSupport) {
        var valueSet = (ValueSet) validationSupport.fetchValueSet(DEVICE_TYPES_VALUE_SET);
        if (valueSet == null) {
            throw new IllegalStateException("Missing CIED device types ValueSet in the IG package");
        }
        deviceTypes = valueSet.getCompose().getInclude().stream()
                .flatMap(include -> include.getConcept().stream()
                        .map(concept -> new Coding(
                                        include.getSystem(),
                                        concept.getCode(),
                                        concept.getDisplay()
                                )
                        )
                )
                .toList();
    }

    public Device toFhir(JsonNode source, String clinicId, String merlinNetId, String deviceId) {
        if (source == null || !source.isObject()) {
            throw new IllegalArgumentException("Expected a device object");
        }

        var device = new Device();
        String id = clinicId + ID_DELIMITER + merlinNetId + ID_DELIMITER + deviceId;
        device.setId(id);
        device.addIdentifier().setValue(id);
        device.setManufacturerElement(textOrUnknown(source, "deviceDescription"));
        device.setSerialNumberElement(textOrUnknown(source, "deviceSerialNum"));
        device.setModelNumberElement(textOrUnknown(source, "deviceModelNum"));

        String deviceType = JsonText.optional(source, "deviceType");
        var type = device.addType();
        if (deviceType == null) {
            type.addExtension(DATA_ABSENT_REASON, new CodeType("unknown"));
        } else {
            Coding coding = deviceTypes.stream()
                    .filter(candidate -> deviceType.equals(candidate.getDisplay()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unsupported deviceType: " + deviceType));
            type.addCoding(coding.copy());
        }
        return device;
    }

    private StringType textOrUnknown(JsonNode source, String field) {
        var element = new StringType(JsonText.optional(source, field));
        if (!element.hasValue()) {
            element.addExtension(DATA_ABSENT_REASON, new CodeType("unknown"));
        }
        return element;
    }
}
