package org.dsa.iot.msiotdev;

import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.DSLinkHandler;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeManager;
import org.dsa.iot.dslink.node.Permission;
import org.dsa.iot.dslink.node.actions.Action;
import org.dsa.iot.dslink.node.actions.Parameter;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueType;
import org.dsa.iot.dslink.util.Objects;
import org.dsa.iot.dslink.util.json.JsonObject;
import org.dsa.iot.msiotdev.client.CreateClientAction;
import org.dsa.iot.msiotdev.client.IotClientController;
import org.dsa.iot.msiotdev.client.RemoveClientAction;
import org.dsa.iot.msiotdev.client.SendTestAction;
import org.dsa.iot.msiotdev.host.*;
import org.dsa.iot.msiotdev.providers.MessageProvider;
import org.dsa.iot.msiotdev.providers.iothub.IotHubMessageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IotLinkHandler extends DSLinkHandler {
    private static final Logger LOG = LoggerFactory.getLogger(IotLinkHandler.class);

    private DSLink requesterLink;
    private DSLink responderLink;
    private RequesterSubscribeContainer subscribeContainer;
    private RequesterListContainer listContainer;

    @Override
    public void onRequesterInitialized(DSLink link) {
        super.onRequesterInitialized(link);

        this.requesterLink = link;
        this.subscribeContainer = new RequesterSubscribeContainer(requesterLink);
        this.listContainer = new RequesterListContainer(requesterLink);

        LOG.info("Requester Initialized.");
        fullInitialize();
    }

    @Override
    public void onResponderInitialized(DSLink link) {
        super.onResponderInitialized(link);

        this.responderLink = link;

        LOG.info("Responder Initialized.");
        fullInitialize();
    }

    @Override
    public void onResponderConnected(DSLink link) {
        super.onResponderConnected(link);
        LOG.info("Responder Connected.");

        fullInitialize();
    }

    @Override
    public void onRequesterConnected(DSLink link) {
        super.onRequesterConnected(link);
        LOG.info("Requester Connected.");
        fullInitialize();
    }

    @Override
    public boolean isRequester() {
        return true;
    }

    @Override
    public boolean isResponder() {
        return true;
    }

    public DSLink getRequesterLink() {
        return requesterLink;
    }

    public DSLink getResponderLink() {
        return responderLink;
    }

    public RequesterSubscribeContainer getSubscribeContainer() {
        return subscribeContainer;
    }

    public RequesterListContainer getListContainer() {
        return listContainer;
    }

    private boolean hasInitialized = false;

    public void fullInitialize() {
        if (hasInitialized) {
            return;
        }

        if (requesterLink == null || responderLink == null) {
            return;
        }

        hasInitialized = true;
        LOG.info("doing fullInitialize");

        NodeManager nodeManager = responderLink.getNodeManager();
        Node superRoot = nodeManager.getSuperRoot();
        Objects.getThreadPool().setMaximumPoolSize(256);
        Objects.getDaemonThreadPool().setMaximumPoolSize(256);
        Objects.getDaemonThreadPool().setCorePoolSize(32);

        {
            Action action = new Action(Permission.CONFIG, new CreateHostAction(this))
                    .addParameter(new Parameter("name", ValueType.STRING).setPlaceHolder("My Host"))
                    .addParameter(new Parameter("deviceConnection", ValueType.STRING).setPlaceHolder("HostName=x;DeviceId=y;SharedAccessKey=z"))
                    .addParameter(new Parameter("eventConnection", ValueType.STRING).setPlaceHolder("Endpoint=x;SharedAccessKeyName=y;SharedAccessKey=z"))
                    .addParameter(new Parameter("numberOfPartitions", ValueType.NUMBER).setDefaultValue(new Value(2)));

            superRoot
                    .createChild("createHostDevice")
                    .setDisplayName("Create IoT Host Device")
                    .setAction(action)
                    .setSerializable(false)
                    .build();
        }

        {
            Action action = new Action(Permission.CONFIG, new CreateClientAction(this))
                    .addParameter(new Parameter("name", ValueType.STRING).setPlaceHolder("My Client"))
                    .addParameter(new Parameter("deviceConnection", ValueType.STRING).setPlaceHolder("HostName=x;DeviceId=y;SharedAccessKey=z"))
                    .addParameter(new Parameter("eventConnection", ValueType.STRING).setPlaceHolder("Endpoint=x;SharedAccessKeyName=y;SharedAccessKey=z"))
                    .addParameter(new Parameter("hostDeviceId", ValueType.STRING))
                    .addParameter(new Parameter("numberOfPartitions", ValueType.NUMBER).setDefaultValue(new Value(2)));

            superRoot
                    .createChild("createClientDevice")
                    .setDisplayName("Create IoT Client Device")
                    .setAction(action)
                    .setSerializable(false)
                    .build();
        }

        for (Node node : superRoot.getChildren().values()) {
            if (node.getRoConfig("host") != null && node.getRoConfig("host").getBool()) {
                initializeHostNode(node);
            } else if (node.getRoConfig("client") != null && node.getRoConfig("client").getBool()) {
                initializeClientNode(node);
            }
        }
    }

    public void initializeHostNode(Node node) {
        {
            Action action = new Action(Permission.CONFIG, new RemoveHostAction(node));

            node
                    .createChild("remove")
                    .setDisplayName("Remove")
                    .setAction(action)
                    .setSerializable(false)
                    .build();
        }

        JsonObject config = new JsonObject();

        if (node.getRoConfig("msiot_device_conn") != null) {
            config.put("deviceConnection", node.getRoConfig("msiot_device_conn").getString());
        }

        if (node.getRoConfig("msiot_event_conn") != null) {
            config.put("eventConnection", node.getRoConfig("msiot_event_conn").getString());
        }
        
        if (node.getRoConfig("msiot_partition_count") != null) {
            config.put("partitionCount", node.getRoConfig("msiot_partition_count").getNumber().intValue());
        }

        MessageProvider provider;

        provider = new IotHubMessageProvider();

        IotHostController controller = new IotHostController(this, node, provider, config);
        try {
            controller.init();
        } catch (Exception e) {
            LOG.error("Failed to initialize host controller.", e);
        }
    }

    public void initializeClientNode(Node node) {
        {
            Action action = new Action(Permission.CONFIG, new RemoveClientAction(node));

            node
                    .createChild("remove")
                    .setDisplayName("Remove")
                    .setAction(action)
                    .setSerializable(false)
                    .build();
        }
        {
            Action action = new Action(Permission.CONFIG, new SendTestAction(node));

            node
                    .createChild("sendMessage")
                    .setDisplayName("sendMessage")
                    .setAction(action)
                    .setSerializable(false)
                    .build();
        }

        JsonObject config = new JsonObject();
        
        if (node.getRoConfig("msiot_device_conn") != null) {
            config.put("deviceConnection", node.getRoConfig("msiot_device_conn").getString());
        }

        if (node.getRoConfig("msiot_event_conn") != null) {
            config.put("eventConnection", node.getRoConfig("msiot_event_conn").getString());
        }
        
        if (node.getRoConfig("msiot_partition_count") != null) {
            config.put("partitionCount", node.getRoConfig("msiot_partition_count").getNumber().intValue());
        }

        if (node.getRoConfig("msiot_device") != null) {
            config.put("hostDeviceId", node.getRoConfig("msiot_device").getString());
        }

        MessageProvider provider;

        provider = new IotHubMessageProvider();

        IotClientController controller = new IotClientController(this, node, provider, config);
        try {
            controller.init();
        } catch (Exception e) {
            LOG.error("Failed to initialize client controller.", e);
        }
    }
}
