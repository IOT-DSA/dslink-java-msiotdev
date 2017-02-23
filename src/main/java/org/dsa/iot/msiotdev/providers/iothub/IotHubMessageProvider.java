package org.dsa.iot.msiotdev.providers.iothub;

import com.microsoft.azure.eventhubs.EventHubClient;
import com.microsoft.azure.iothub.DeviceClient;
import com.microsoft.azure.iothub.IotHubClientProtocol;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.dsa.iot.dslink.util.json.JsonObject;
import org.dsa.iot.msiotdev.providers.ClientMessageFacade;
import org.dsa.iot.msiotdev.providers.HostMessageFacade;
import org.dsa.iot.msiotdev.providers.MessageFacade;
import org.dsa.iot.msiotdev.providers.MessageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IotHubMessageProvider implements MessageProvider {
    private static final Logger LOG = LoggerFactory.getLogger(IotHubMessageProvider.class);


    @Override
    public HostMessageFacade getHostFacade(JsonObject config) {
    	try {
        	String deviceConnection = config.get("deviceConnection");
        	String eventConnectionString = config.get("eventConnection");
        	int partitionCount = config.get("partitionCount");
        	
        	String[] connStringAttrs = deviceConnection.split(";");
            String deviceId = null;
            for (String attr : connStringAttrs) {
            	if (attr.startsWith(DeviceClient.DEVICE_ID_ATTRIBUTE)) {
                    String urlEncodedDeviceId = attr.substring(DeviceClient.DEVICE_ID_ATTRIBUTE.length());
                    try
                    {
                        deviceId = URLDecoder.decode(urlEncodedDeviceId, DeviceClient.CONNECTION_STRING_CHARSET.name());
                        break;
                    }
                    catch (UnsupportedEncodingException e)
                    {
                        // should never happen, since the encoding is hard-coded.
                        throw new IllegalStateException(e);
                    }
                }
            }

        	EventHubClient eventHubClient = EventHubClient.createFromConnectionStringSync(eventConnectionString);
       
            DeviceClient deviceClient = new DeviceClient(deviceConnection, IotHubClientProtocol.MQTT);
            deviceClient.open();
            LOG.info("IoT Hub Device is ready.");
            

            return new IotHubHostMessageFacade(deviceClient, eventHubClient, deviceId, partitionCount);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ClientMessageFacade getClientFacade(JsonObject config) {
    	try {
        	String deviceConnection = config.get("deviceConnection");
        	String eventConnectionString = config.get("eventConnection");
        	int partitionCount = config.get("partitionCount");
        	String deviceId = config.get("hostDeviceId");

        	EventHubClient eventHubClient = EventHubClient.createFromConnectionStringSync(eventConnectionString);
        	DeviceClient deviceClient;
       
            deviceClient = new DeviceClient(deviceConnection, IotHubClientProtocol.MQTT);
            deviceClient.open();
            LOG.info("IoT Hub Device is ready.");

            return new IotHubClientMessageFacade(deviceClient, eventHubClient, deviceId, partitionCount);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
