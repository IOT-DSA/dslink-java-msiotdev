package org.dsa.iot.msiotdev.client;

import com.microsoft.azure.eventhubs.EventData;
import com.microsoft.azure.eventhubs.PartitionReceiveHandler;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.util.json.EncodingFormat;
import org.dsa.iot.dslink.util.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientMessageHandler extends PartitionReceiveHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ClientMessageHandler.class);

    private IotClientController controller;

    public ClientMessageHandler(IotClientController controller) {
        super(999);
        this.controller = controller;
    }

    public IotClientController getController() {
        return controller;
    }

    @Override
    public void onReceive(Iterable<EventData> events) {
        if (events != null) {
            for (EventData data : events) {
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
    }

    @Override
    public void onError(Throwable error) {
        LOG.warn("Received error from client message handler.", error);
    }
}
