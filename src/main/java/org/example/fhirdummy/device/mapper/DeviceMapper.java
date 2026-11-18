package org.example.fhirdummy.device.mapper;

import org.example.fhirdummy.utility.JsonText;
import org.hl7.fhir.r5.model.CodeType;
import org.hl7.fhir.r5.model.Device;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
public class DeviceMapper {
    public Device toFhir(JsonNode source) {
        if (!source.isObject()) {
            throw new IllegalArgumentException("Expected a device object");
        }

        var device = new Device();
        device.setManufacturer(JsonText.required(source, "deviceDescription"));
        device.setSerialNumber(JsonText.required(source, "deviceSerialNum"));
        device.setModelNumber(JsonText.required(source, "deviceModelNum"));

        // CardX requires a type, but the upstream payload does not provide one.
        device.addType().addExtension("http://hl7.org/fhir/StructureDefinition/data-absent-reason",
                new CodeType("unknown"));
        return device;
    }
}
