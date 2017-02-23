package org.dsa.iot.msiotdev.providers.iothub;

import org.dsa.iot.dslink.util.json.JsonObject;
import org.dsa.iot.msiotdev.providers.ClientMessageFacade;

import com.microsoft.azure.eventhubs.EventHubClient;
import com.microsoft.azure.iothub.DeviceClient;
import com.microsoft.azure.iothub.Message;

public class IotHubClientMessageFacade extends IotHubMessageFacade implements ClientMessageFacade {
	
	private String hostDeviceId;
	
	public IotHubClientMessageFacade(DeviceClient device, EventHubClient eventHubClient, String hostDeviceId, int partitionCount) {
		super(device, eventHubClient, partitionCount);
		this.hostDeviceId = hostDeviceId;
		init();
	}
	
	@Override
	public void setMessageProperty(Message msg) {
		msg.setProperty("Destination", hostDeviceId);
	}

	@Override
	public void tagMessage(JsonObject object) {
		object.put("_destination", hostDeviceId);
	}

	@Override
	public boolean shouldHandleEvent(JsonObject object) {
		return hostDeviceId.equals(object.get("_source"));
	}

}
