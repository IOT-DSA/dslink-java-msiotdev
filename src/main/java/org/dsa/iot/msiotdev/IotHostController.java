package org.dsa.iot.msiotdev;

import com.microsoft.azure.iot.service.exceptions.IotHubException;
import com.microsoft.azure.iot.service.sdk.Device;
import com.microsoft.azure.iot.service.sdk.RegistryManager;
import com.microsoft.azure.iothub.*;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.util.json.EncodingFormat;
import org.dsa.iot.dslink.util.json.JsonObject;
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
        String deviceId = node.getRoConfig("msiot_device").getString();
        String connectionString = node.getRoConfig("msiot_conn").getString();
        RegistryManager registryManager = RegistryManager.createFromConnectionString(connectionString);

        try {
            Device device = Device.createFromId(deviceId, null, null);
            registryManager.addDevice(device);
        } catch (IotHubException e) {
            registryManager.getDevice(deviceId);
        }

        deviceClient = new DeviceClient(connectionString, IotHubClientProtocol.AMQPS);
        deviceClient.open();
        deviceClient.setMessageCallback(messageCallback, null);
    }

    public void emit(JsonObject object) {
        byte[] bytes = object.encode(EncodingFormat.MESSAGE_PACK);
        Message msg = new Message(bytes);
        deviceClient.sendEventAsync(msg, null, null);
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
    }

    public IotLinkHandler getHandler() {
        return handler;
    }
}
