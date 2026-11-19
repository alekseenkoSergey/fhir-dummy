package org.example.fhirdummy.component.mapper;

import org.example.fhirdummy.utility.Ids;
import org.example.fhirdummy.utility.JsonText;
import org.hl7.fhir.r5.model.Device;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
public class MonitorMapper {
    public Device toFhir(JsonNode source, String clinicId, String merlinNetId) {
        if (source == null || !source.isObject()) {
            throw new IllegalArgumentException("Expected a transmitter object");
        }

        var device = new Device();
        String model = JsonText.required(source, "transmitterModelNum");
        String serial = JsonText.required(source, "transmitterSerialNum");
        device.setId(Ids.join(clinicId, merlinNetId, model, serial));
        device.setModelNumber(model);
        device.setSerialNumber(serial);
        return device;
    }
}
