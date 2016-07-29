package org.dsa.iot.msiotdev.host;

import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.provider.LoopProvider;
import org.dsa.iot.dslink.util.json.EncodingFormat;
import org.dsa.iot.dslink.util.json.JsonObject;
import org.dsa.iot.msiotdev.IotLinkHandler;
import org.dsa.iot.msiotdev.providers.HostMessageFacade;
import org.dsa.iot.msiotdev.providers.MessageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class IotHostController {
    private static final Logger LOG = LoggerFactory.getLogger(IotHostController.class);

    private Node node;
    private HostMessageCallback messageCallback;
    private Map<String, HostSubscription> subscriptions;
    private Map<String, HostLister>  lists;
    private IotLinkHandler handler;
    private JsonObject config;
    private HostMessageFacade facade;
    private MessageProvider provider;

    public IotHostController(final IotLinkHandler handler, final Node node, final MessageProvider provider, final JsonObject config) {
        this.node = node;
        this.messageCallback = new HostMessageCallback(this);
        this.subscriptions = new HashMap<>();
        this.lists = new HashMap<>();
        this.handler = handler;
        this.config = config;
        this.provider = provider;
    }

    public Node getNode() {
        return node;
    }

    public void init() throws Exception {
        LoopProvider.getProvider().schedule(() -> {
            if (facade == null) {
                facade = provider.getHostFacade(config);
            }

            facade.handle(messageCallback);
        });
    }

    public void emit(JsonObject object) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Emitting event " + new String(object.encodePrettily(EncodingFormat.JSON)) + " from host.");
        }

        facade.emit(object);
    }

    public Map<String, HostLister> getLists() {
        return lists;
    }

    public Map<String, HostSubscription> getSubscriptions() {
        return subscriptions;
    }

    public void destroy() {
        try {
            facade.destroy();
        } catch (Exception e) {
            LOG.warn("Failed to destroy host facade.", e);
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
