package org.dsa.iot.msiotdev.client;

import com.microsoft.azure.eventhubs.EventData;
import com.microsoft.azure.eventhubs.EventHubClient;
import com.microsoft.azure.eventhubs.PartitionReceiveHandler;
import com.microsoft.azure.eventhubs.PartitionReceiver;
import com.microsoft.azure.servicebus.ServiceBusException;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.provider.LoopProvider;
import org.dsa.iot.dslink.util.json.EncodingFormat;
import org.dsa.iot.dslink.util.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientMessageHandler extends PartitionReceiveHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ClientMessageHandler.class);

    private IotClientController controller;
    private PartitionReceiver receiver;
    private int counter = 0;

    public ClientMessageHandler(IotClientController controller, PartitionReceiver receiver) {
        super(999);
        this.controller = controller;
        this.receiver = receiver;
    }

    public IotClientController getController() {
        return controller;
    }

    @Override
    public void onReceive(Iterable<EventData> events) {
        if (events != null) {
            for (EventData data : events) {
                counter++;

                JsonObject object = new JsonObject(EncodingFormat.MESSAGE_PACK, data.getBody());
                String type = object.get("type");
                String path = object.get("path");

                if (path != null) {
                    Node node = controller.resolveNode(path);

                    if (node != null) {
                        IotNodeController nodeController = node.getMetaData();

                        if (nodeController != null) {
                            nodeController.deliver(type, object);
                        }
                    }
                }
            }
        }

        if (counter >= 900) {
            counter = 0;

            try {
                receiver.close().get();
            } catch (Exception ignored) {}

            try {
                receiver = controller.getEventHubClient().createReceiverSync(
                        EventHubClient.DEFAULT_CONSUMER_GROUP_NAME,
                        receiver.getPartitionId(),
                        PartitionReceiver.START_OF_STREAM
                );
            } catch (ServiceBusException e) {
                LOG.error("Failed to resubscribe.", e);
            }

            LoopProvider.getProvider().schedule(() -> receiver.setReceiveHandler(ClientMessageHandler.this));
        }
    }

    @Override
    public void onError(Throwable error) {
        LOG.warn("Received error from client message handler.", error);
    }
}
