package org.example.fhirdummy.mapper;

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
        device.setManufacturer(text(source, "deviceDescription"));
        device.setSerialNumber(text(source, "deviceSerialNum"));
        device.setModelNumber(text(source, "deviceModelNum"));

        // CardX requires a type, but the upstream payload does not provide one.
        device.addType().addExtension("http://hl7.org/fhir/StructureDefinition/data-absent-reason",
                new CodeType("unknown"));
        return device;
    }

    private static String text(JsonNode source, String field) {
        JsonNode value = source.path(field);
        if (!value.isString() || value.asString().isBlank()) {
            throw new IllegalArgumentException("Expected nonempty text for field " + field);
        }
        return value.asString().trim();
    }
}
