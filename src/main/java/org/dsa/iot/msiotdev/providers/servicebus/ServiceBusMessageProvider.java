package org.dsa.iot.msiotdev.providers.servicebus;

import org.dsa.iot.dslink.util.json.JsonObject;
import org.dsa.iot.msiotdev.providers.ClientMessageFacade;
import org.dsa.iot.msiotdev.providers.HostMessageFacade;
import org.dsa.iot.msiotdev.providers.MessageProvider;

public class ServiceBusMessageProvider implements MessageProvider {
    @Override
    public HostMessageFacade getHostFacade(JsonObject config) {
        return null;
    }

    @Override
    public ClientMessageFacade getClientFacade(JsonObject config) {
        return null;
    }
}
