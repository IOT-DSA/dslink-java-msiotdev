package org.dsa.iot.msiotdev.client;

import com.microsoft.azure.eventhubs.EventData;
import com.microsoft.azure.eventhubs.PartitionReceiver;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.provider.LoopProvider;
import org.dsa.iot.dslink.util.json.EncodingFormat;
import org.dsa.iot.dslink.util.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ClientMessageHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ClientMessageHandler.class);

    private IotClientController controller;
    private PartitionReceiver receiver;
    private boolean canceled = false;

    public ClientMessageHandler(IotClientController controller, PartitionReceiver receiver) {
        this.controller = controller;
        this.receiver = receiver;
    }

    public void receive() {
        // LOG.debug("Attempting to fetch events from partition " + receiver.getPartitionId() + ".");

        try {
            Iterable<EventData> datas = receiver.receive(4).get(
                    1,
                    TimeUnit.SECONDS
            );
            if (datas != null) {
                for (EventData data : datas) {
                    handleEvent(data);
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Failed to fetch events from partition " + receiver.getPartitionId(), e);
        } catch (TimeoutException ignored) {
        }

        schedule();
    }

    public void schedule() {
        LoopProvider.getProvider().schedule(() -> {
            if (!canceled) {
                receive();
            }
        });
    }

    public void handleEvent(EventData data) {
        try {
            JsonObject object = new JsonObject(EncodingFormat.MESSAGE_PACK, data.getBody());

            if (LOG.isDebugEnabled()) {
                LOG.debug("Received event " + new String(object.encodePrettily(EncodingFormat.JSON)) + " from hub.");
            }

            String type = object.get("type");
            String path = object.get("path");

            if (path != null) {
                Node node = controller.resolveNode(path);

                if (node != null) {
                    IotNodeController nodeController = node.getMetaData();

                    if (nodeController != null) {
                        nodeController.deliver(type, object);
                    }
                } else {
                    LOG.debug("Failed to find node at " + path);
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to handle client event.", e);
        }
    }

    public void disable() {
        canceled = true;
    }
}
