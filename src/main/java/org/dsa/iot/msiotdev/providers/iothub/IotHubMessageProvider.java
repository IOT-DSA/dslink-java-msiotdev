package org.dsa.iot.msiotdev.providers.iothub;

import com.microsoft.azure.eventhubs.EventHubClient;
import com.microsoft.azure.iot.service.sdk.IotHubServiceClientProtocol;
import com.microsoft.azure.iot.service.sdk.ServiceClient;
import com.microsoft.azure.iothub.DeviceClient;
import com.microsoft.azure.iothub.IotHubClientProtocol;
import org.dsa.iot.dslink.util.json.JsonObject;
import org.dsa.iot.msiotdev.providers.ClientMessageFacade;
import org.dsa.iot.msiotdev.providers.HostMessageFacade;
import org.dsa.iot.msiotdev.providers.MessageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IotHubMessageProvider implements MessageProvider {
    private static final Logger LOG = LoggerFactory.getLogger(IotHubMessageProvider.class);


    @Override
    public HostMessageFacade getHostFacade(JsonObject config) {
        String deviceConnection = config.get("deviceConnection");

        DeviceClient deviceClient;
        try {
            deviceClient = new DeviceClient(deviceConnection, IotHubClientProtocol.MQTT);
            deviceClient.open();
            LOG.info("IoT Hub Device is ready.");

            return new IotHubHostMessageFacade(deviceClient);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ClientMessageFacade getClientFacade(JsonObject config) {
        try {
            String serviceConnectionString = config.get("connection");
            String eventConnectionString = config.get("eventConnection");
            String deviceId = config.get("deviceId");
            EventHubClient eventHubClient = EventHubClient.createFromConnectionStringSync(eventConnectionString);

            ServiceClient serviceClient = ServiceClient.createFromConnectionString(
                    serviceConnectionString,
                    IotHubServiceClientProtocol.AMQPS_WS
            );

            serviceClient.open();

            return new IotHubClientMessageFacade(eventHubClient, serviceClient, deviceId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
