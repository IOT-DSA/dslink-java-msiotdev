package org.dsa.iot.msiotdev.client;

import com.microsoft.azure.eventhubs.EventHubClient;
import com.microsoft.azure.eventhubs.PartitionReceiver;
import com.microsoft.azure.iot.service.sdk.*;
import org.dsa.iot.commons.Container;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.provider.LoopProvider;
import org.dsa.iot.dslink.util.json.EncodingFormat;
import org.dsa.iot.dslink.util.json.JsonObject;
import org.dsa.iot.msiotdev.IotLinkHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("Duplicates")
public class IotClientController {
    private static final Logger LOG = LoggerFactory.getLogger(IotClientController.class);

    private IotLinkHandler handler;
    private Node node;
    private ServiceClient serviceClient;
    private EventHubClient eventHubClient;
    private String deviceId;
    private List<ClientMessageHandler> messageHandlers;
    private FeedbackReceiver feedbackReceiver;

    public IotClientController(IotLinkHandler handler, Node node) {
        this.handler = handler;
        this.node = node;
    }

    public void init() {
        if (messageHandlers != null && !messageHandlers.isEmpty()) {
            for (ClientMessageHandler handler : messageHandlers) {
                handler.disable();
            }
            messageHandlers.clear();
        }

        messageHandlers = new ArrayList<>();
        LoopProvider.getProvider().schedule(() -> {
            try {
                deviceId = node.getRoConfig("msiot_device").getString();
                String connectionString = node.getRoConfig("msiot_conn").getString();
                String eventHubString = node.getRoConfig("msiot_event_conn").getString();
                serviceClient = ServiceClient.createFromConnectionString(connectionString, IotHubServiceClientProtocol.AMQPS);
                serviceClient.open();
                feedbackReceiver = serviceClient.getFeedbackReceiver(deviceId);
                feedbackReceiver.open();

                final Container<Runnable> task = new Container<>();
                task.setValue(() -> {
                    if (feedbackReceiver == null) {
                        return;
                    }

                    try {
                        feedbackReceiver.receive();
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }

                    LoopProvider.getProvider().schedule(task.getValue());
                });

                LoopProvider.getProvider().schedule(task.getValue());

                eventHubClient = EventHubClient.createFromConnectionStringSync(eventHubString);

                for (int i = 0; i < 4; i++) {
                    PartitionReceiver receiver = eventHubClient.createReceiverSync(
                            EventHubClient.DEFAULT_CONSUMER_GROUP_NAME,
                            String.valueOf(i),
                            Instant.now()
                    );

                    receiver.setReceiveTimeout(Duration.ofSeconds(1));
                    ClientMessageHandler handler = new ClientMessageHandler(IotClientController.this, receiver);
                    messageHandlers.add(handler);
                    handler.schedule();
                }

                IotClientFakeNode brokerNode = new IotClientFakeNode(
                        "broker",
                        node,
                        node.getLink(),
                        this,
                        "/"
                );

                IotNodeController controller = new IotNodeController(IotClientController.this, brokerNode, "/");
                controller.init();
                controller.loadNow();

                node.addChild(brokerNode);
            } catch (Exception e) {
                LOG.error("Failed to initialize client controller.", e);
            }
        });
    }

    public void destroy() {
        try {
            feedbackReceiver.close();
            feedbackReceiver = null;
        } catch (Exception e) {
            LOG.warn("Failed to destroy feedback receiver.", e);
        }

        try {
            eventHubClient.close();
            eventHubClient = null;
        } catch (Exception e) {
            LOG.warn("Failed to destroy event hub client.", e);
        }

        try {
            serviceClient.close();
            serviceClient = null;
        } catch (Exception e) {
            LOG.warn("Failed to destroy service client.", e);
        }
    }

    public IotLinkHandler getHandler() {
        return handler;
    }

    public Node getNode() {
        return node;
    }

    public Node resolveNode(String path) {
        Node root = node.getChild("broker");

        if ("/".equals(path)) {
            return root;
        }

        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        String[] parts = path.split("/");

        Node current = this.node.getChild("broker");

        for (String part : parts) {
            if (current == null || current.getChildren() == null) {
                return null;
            }
            current = current.getChildren().get(part);
        }

        return current;
    }

    public EventHubClient getEventHubClient() {
        return eventHubClient;
    }

    public void emit(JsonObject object) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Sending " + new String(object.encodePrettily(EncodingFormat.JSON)) + " to device.");
        }

        Message msg = new Message(object.encode(EncodingFormat.MESSAGE_PACK));
        msg.setDeliveryAcknowledgement(DeliveryAcknowledgement.Full);
        msg.setCorrelationId(java.util.UUID.randomUUID().toString());
        msg.setUserId(java.util.UUID.randomUUID().toString());
        try {
            serviceClient.send(deviceId, msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
