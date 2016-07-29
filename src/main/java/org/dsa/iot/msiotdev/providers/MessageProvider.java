package org.dsa.iot.msiotdev.providers;

import org.dsa.iot.dslink.util.json.JsonObject;

public interface MessageProvider {
    HostMessageFacade getHostFacade(JsonObject config);
    ClientMessageFacade getClientFacade(JsonObject config);
}
