package org.dsa.iot.msiotdev.client;

import com.microsoft.azure.eventhubs.EventHubClient;
import com.microsoft.azure.eventhubs.PartitionReceiver;
import com.microsoft.azure.iot.service.sdk.DeliveryAcknowledgement;
import com.microsoft.azure.iot.service.sdk.IotHubServiceClientProtocol;
import com.microsoft.azure.iot.service.sdk.Message;
import com.microsoft.azure.iot.service.sdk.ServiceClient;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.provider.LoopProvider;
import org.dsa.iot.dslink.util.json.EncodingFormat;
import org.dsa.iot.dslink.util.json.JsonObject;
import org.dsa.iot.msiotdev.IotLinkHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Date;
import java.util.concurrent.ScheduledFuture;

@SuppressWarnings("Duplicates")
public class IotClientController {
    private static final Logger LOG = LoggerFactory.getLogger(IotClientController.class);

    private IotLinkHandler handler;
    private Node node;
    private ServiceClient serviceClient;
    private EventHubClient eventHubClient;
    private ClientMessageHandler messageHandler;
    private String deviceId;

    public IotClientController(IotLinkHandler handler, Node node) {
        this.handler = handler;
        this.node = node;
        this.messageHandler = new ClientMessageHandler(this);
    }

    public void init() {
        LoopProvider.getProvider().schedule(() -> {
            try {
                deviceId = node.getRoConfig("msiot_device").getString();
                String connectionString = node.getRoConfig("msiot_conn").getString();
                String eventHubString = node.getRoConfig("msiot_event_conn").getString();
                serviceClient = ServiceClient.createFromConnectionString(connectionString, IotHubServiceClientProtocol.AMQPS);
                serviceClient.open();
                eventHubClient = EventHubClient.createFromConnectionStringSync(eventHubString);

                for (int i = 0; i < 4; i++) {
                    PartitionReceiver receiver = eventHubClient.createReceiverSync(
                            EventHubClient.DEFAULT_CONSUMER_GROUP_NAME,
                            String.valueOf(i),
                            PartitionReceiver.START_OF_STREAM
                    );

                    receiver.setReceiveTimeout(Duration.ofSeconds(3));
                    receiver.setPrefetchCount(999);
                    receiver.setReceiveHandler(messageHandler);
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

                node.addChild(brokerNode);
            } catch (Exception e) {
                LOG.error("Failed to initialize client controller.", e);
            }
        });
    }

    public void destroy() {
        try {
            eventHubClient.close();
        } catch (Exception e) {
            LOG.warn("Failed to destroy event hub client.", e);
        }

        try {
            serviceClient.close();
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

    public void emit(JsonObject object) {
        Message msg = new Message(object.encode(EncodingFormat.MESSAGE_PACK));
        Date now = new Date();
        msg.setExpiryTimeUtc(new Date(now.getTime() + 60 * 1000));
        msg.setDeliveryAcknowledgement(DeliveryAcknowledgement.Full);
        msg.setCorrelationId(java.util.UUID.randomUUID().toString());
        msg.setUserId(java.util.UUID.randomUUID().toString());
        serviceClient.sendAsync(deviceId, msg);
    }
}
