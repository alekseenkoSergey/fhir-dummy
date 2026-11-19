package org.example.fhirdummy.component.mapper;

import static org.example.fhirdummy.utility.Constants.ID_DELIMITER;
import org.example.fhirdummy.utility.JsonText;
import org.hl7.fhir.r5.model.Device;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
public class DeviceMapper {
    public Device toFhir(JsonNode source, String clinicId, String merlinNetId, String deviceId) {
        if (source == null || !source.isObject()) {
            throw new IllegalArgumentException("Expected a device object");
        }

        var device = new Device();
        String id = clinicId + ID_DELIMITER + merlinNetId + ID_DELIMITER + deviceId;
        device.setId(id);
        device.addIdentifier().setValue(id);
        device.setManufacturer(JsonText.required(source, "deviceDescription"));
        device.setSerialNumber(JsonText.required(source, "deviceSerialNum"));
        device.setModelNumber(JsonText.required(source, "deviceModelNum"));

        String deviceType = JsonText.optional(source, "deviceType");
        // TODO: Clarify the mapping of the extracted upstream deviceType to CardX codes. Use Other until agreed.
        device.addType().addCoding().setSystem("urn:iso:std:iso:11073:10101")
                .setCode("753670").setDisplay("MDC_IDC_ENUM_DEV_TYPE_Other");
        return device;
    }
}
