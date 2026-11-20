package org.example.fhirdummy.component.mapper;

import static org.example.fhirdummy.utility.Constants.ID_DELIMITER;
import org.example.fhirdummy.utility.JsonText;
import org.hl7.fhir.r5.model.CodeType;
import org.hl7.fhir.r5.model.Device;
import org.hl7.fhir.r5.model.StringType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
public class DeviceMapper {
    private static final String DATA_ABSENT_REASON = "http://hl7.org/fhir/StructureDefinition/data-absent-reason";

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
            // TODO: Clarify the mapping of the extracted upstream deviceType to CardX codes. Use Other until agreed.
            type.addCoding().setSystem("urn:iso:std:iso:11073:10101")
                    .setCode("753670").setDisplay("MDC_IDC_ENUM_DEV_TYPE_Other");
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
