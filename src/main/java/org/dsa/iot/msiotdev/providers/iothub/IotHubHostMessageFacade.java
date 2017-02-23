package org.dsa.iot.msiotdev.providers.iothub;

import org.dsa.iot.dslink.util.json.JsonObject;
import org.dsa.iot.msiotdev.providers.HostMessageFacade;

import com.microsoft.azure.eventhubs.EventHubClient;
import com.microsoft.azure.iothub.DeviceClient;
import com.microsoft.azure.iothub.Message;

public class IotHubHostMessageFacade extends IotHubMessageFacade implements HostMessageFacade{
	
	private String deviceId;
	
	public IotHubHostMessageFacade(DeviceClient device, EventHubClient eventHubClient, String deviceId, int partitionCount) {
		super(device, eventHubClient, partitionCount);
		this.deviceId = deviceId;
		init();
	}

	@Override
	public void setMessageProperty(Message msg) {
		msg.setProperty("Source", deviceId);
	}
	
	@Override
	public void tagMessage(JsonObject object) {
		object.put("_source", deviceId);
	}

	@Override
	public boolean shouldHandleEvent(JsonObject object) {
		return deviceId != null && deviceId.equals(object.get("_destination"));
	}

}
