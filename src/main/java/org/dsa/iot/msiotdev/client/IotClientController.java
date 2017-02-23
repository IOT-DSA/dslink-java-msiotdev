package org.dsa.iot.msiotdev.client;

import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.util.json.EncodingFormat;
import org.dsa.iot.dslink.util.json.JsonObject;
import org.dsa.iot.msiotdev.IotLinkHandler;
import org.dsa.iot.msiotdev.providers.ClientMessageFacade;
import org.dsa.iot.msiotdev.providers.MessageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("Duplicates")
public class IotClientController {
    private static final Logger LOG = LoggerFactory.getLogger(IotClientController.class);

    private IotLinkHandler handler;
    private ClientMessageFacade facade;
    private Node node;
    private JsonObject config;
    private MessageProvider provider;

    public IotClientController(IotLinkHandler handler, Node node, MessageProvider provider, JsonObject config) {
        this.handler = handler;
        this.node = node;
        this.config = config;
        this.provider = provider;
        node.setMetaData(this);
    }

    public void init() {
        if (facade == null) {
            facade = provider.getClientFacade(config);
        }

        facade.handle(object -> {
            String type = object.get("type");
            String path = object.get("path");

            if (path != null) {
                Node node = resolveNode(path);

                if (node != null) {
                    IotNodeController nodeController = node.getMetaData();

                    if (nodeController != null) {
                        nodeController.deliver(type, object);
                    }
                } else {
                    LOG.debug("Failed to find node at " + path);
                }
            }
        });

        try {
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
    }

    public void destroy() {
        if (facade != null) {
            facade.destroy();
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
        if (LOG.isDebugEnabled()) {
            LOG.debug("Sending " + new String(object.encodePrettily(EncodingFormat.JSON)) + " to host.");
        }
        LOG.info("Sending " + new String(object.encodePrettily(EncodingFormat.JSON)) + " to host.");

        facade.emit(object);
    }
}
