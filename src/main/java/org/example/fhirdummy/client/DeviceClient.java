package org.example.fhirdummy.client;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.example.fhirdummy.dto.DeviceRequest;
import org.hl7.fhir.r4.model.Device;
import org.springframework.stereotype.Service;

@Service
public class DeviceClient {
    private final FhirCrudClient<Device> client;

    public DeviceClient(IGenericClient client) {
        this.client = new FhirCrudClient<>(client, Device.class);
    }

    public Device create(DeviceRequest request) {
        Device device = new Device();
        apply(device, request);
        return client.create(device);
    }

    public Device get(String id) {
        return client.get(id);
    }

    public Device update(String id, DeviceRequest request) {
        return client.update(id, device -> apply(device, request));
    }

    public void delete(String id) {
        client.delete(id);
    }

    private void apply(Device device, DeviceRequest request) {
        device.setStatus(Device.FHIRDeviceStatus.fromCode(request.status()));
        device.setManufacturer(request.manufacturer());
        device.setModelNumber(request.modelNumber());
    }
}
