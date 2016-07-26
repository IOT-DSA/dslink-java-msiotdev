package org.dsa.iot.msiotdev.host;

import com.microsoft.azure.iothub.*;

import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.provider.LoopProvider;
import org.dsa.iot.dslink.util.json.EncodingFormat;
import org.dsa.iot.dslink.util.json.JsonObject;
import org.dsa.iot.msiotdev.IotLinkHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class IotHostController {
    private static final Logger LOG = LoggerFactory.getLogger(IotHostController.class);

    private Node node;
    private DeviceClient deviceClient;
    private HostMessageCallback messageCallback;
    private Map<String, HostSubscription> subscriptions;
    private Map<String, HostLister>  lists;
    private IotLinkHandler handler;

    public IotHostController(final IotLinkHandler handler, final Node node) {
        this.node = node;
        this.messageCallback = new HostMessageCallback(this);
        this.subscriptions = new HashMap<>();
        this.lists = new HashMap<>();
        this.handler = handler;
    }

    public Node getNode() {
        return node;
    }

    public void init() throws Exception {
        LoopProvider.getProvider().schedule(() -> {
            String deviceConnection = node.getRoConfig("msiot_device_conn").getString();

            try {
                deviceClient = new DeviceClient(deviceConnection, IotHubClientProtocol.AMQPS);
                deviceClient.open();
                deviceClient.setMessageCallback(messageCallback, null);
            } catch (Exception e) {
                LOG.error("Failed to initialize host.", e);
            }
        });
    }

    public void emit(JsonObject object) {
        byte[] bytes = object.encode(EncodingFormat.MESSAGE_PACK);
        Message msg = new Message(bytes);
        msg.setExpiryTime(5000);
        deviceClient.sendEventAsync(msg, (responseStatus, callbackContext) -> {
        }, null);
    }

    public Map<String, HostLister> getLists() {
        return lists;
    }

    public Map<String, HostSubscription> getSubscriptions() {
        return subscriptions;
    }

    public void destroy() {
        try {
            deviceClient.close();
        } catch (IOException e) {
            LOG.warn("Failed to destroy device client.", e);
        }

        for (HostLister lister : lists.values()) {
            boolean isEmpty = false;
            while (!isEmpty) {
                isEmpty = lister.onListenerRemoved();
            }
        }

        for (HostSubscription sub : subscriptions.values()) {
            boolean isEmpty = false;
            while (!isEmpty) {
                isEmpty = sub.onListenerRemoved();
            }
        }

        lists.clear();
        subscriptions.clear();
    }

    public IotLinkHandler getHandler() {
        return handler;
    }
}
