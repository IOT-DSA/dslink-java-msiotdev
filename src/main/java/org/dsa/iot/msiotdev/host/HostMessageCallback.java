package org.dsa.iot.msiotdev.host;

import org.dsa.iot.dslink.util.json.EncodingFormat;
import org.dsa.iot.dslink.util.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.function.Consumer;

@SuppressWarnings("Duplicates")
public class HostMessageCallback implements Consumer<JsonObject> {
    private static final Logger LOG = LoggerFactory.getLogger(HostMessageCallback.class);

    private IotHostController controller;

    public HostMessageCallback(IotHostController controller) {
        this.controller = controller;
    }

    @Override
    public void accept(JsonObject object) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Received " + new String(object.encodePrettily(EncodingFormat.JSON)) + " on device.");
        }

        String method = object.get("method");

        if ("subscribe".equals(method)) {
            String path = object.get("path");

            Map<String, HostSubscription> subs = controller.getSubscriptions();
            HostSubscription sub = subs.get(path);
            if (sub == null) {
                LOG.debug("Creating subscription for " + path);
                sub = new HostSubscription(controller, path);
                subs.put(path, sub);
            }
            sub.onListenerAdded();
        } else if ("unsubscribe".equals(method)) {
            String path = object.get("path");
            Map<String, HostSubscription> subs = controller.getSubscriptions();
            HostSubscription sub = subs.get(path);
            if (sub != null) {
                sub.onListenerRemoved();
            }
        } else if ("force-unsubscribe".equals(method)) {
            String path = object.get("path");
            Map<String, HostSubscription> subs = controller.getSubscriptions();
            HostSubscription sub = subs.get(path);
            if (sub != null) {
                boolean isEmpty = false;
                while (!isEmpty) {
                    isEmpty = sub.onListenerRemoved();
                }
            }
        } else if ("list".equals(method)) {
            String path = object.get("path");

            Map<String, HostLister> subs = controller.getLists();
            HostLister sub = subs.get(path);
            if (sub == null) {
                LOG.debug("Creating lister for " + path);
                sub = new HostLister(controller, path);
                subs.put(path, sub);
            }
            sub.onListenerAdded();
        } else if ("unlist".equals(method)) {
            String path = object.get("path");
            Map<String, HostLister> subs = controller.getLists();
            HostLister sub = subs.get(path);
            if (sub != null) {
                sub.onListenerRemoved();
            }
        }  else if ("force-unlist".equals(method)) {
            String path = object.get("path");
            Map<String, HostLister> subs = controller.getLists();
            HostLister sub = subs.get(path);
            if (sub != null) {
                boolean isEmpty = false;
                while (!isEmpty) {
                    isEmpty = sub.onListenerRemoved();
                }
            }
        } else if ("get-list-state".equals(method)) {
            String path = object.get("path");

            Map<String, HostLister> subs = controller.getLists();
            HostLister sub = subs.get(path);
            if (sub != null) {
                sub.getController().emit(sub.getCurrentState());
            }
        } else if ("get-subscribe-state".equals(method)) {
            String path = object.get("path");

            Map<String, HostSubscription> subs = controller.getSubscriptions();
            HostSubscription sub = subs.get(path);
            if (sub != null) {
                sub.getController().emit(sub.getCurrentState());
            }
        }
    }
}
