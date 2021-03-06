package org.dsa.iot.msiotdev.host;

import org.dsa.iot.dslink.methods.requests.SetRequest;
import org.dsa.iot.dslink.methods.responses.SetResponse;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueUtils;
import org.dsa.iot.dslink.util.handler.Handler;
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
        LOG.info("Received " + new String(object.encodePrettily(EncodingFormat.JSON)) + " on device.");

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

            if (sub == null) {
                LOG.debug("Creating lister for " + path);
                sub = new HostLister(controller, path);
                subs.put(path, sub);
            }

            JsonObject state = sub.getCurrentState();
            if (state != null) {
                sub.getController().emit(state);
            }
        } else if ("get-subscribe-state".equals(method)) {
            String path = object.get("path");

            Map<String, HostSubscription> subs = controller.getSubscriptions();
            HostSubscription sub = subs.get(path);

            if (sub == null) {
                LOG.debug("Creating subscription for " + path);
                sub = new HostSubscription(controller, path);
                subs.put(path, sub);
            }

            JsonObject state = sub.getCurrentState();
            if (state != null) {
                sub.getController().emit(state);
            }
        } else if ("set".equals(method)) {
        	String path = object.get("path");
        	Value val = ValueUtils.toValue(object.get("value"));
        	SetRequest request = new SetRequest(path, val);
        	controller.getHandler().getRequesterLink().getRequester().set(request, new Handler<SetResponse>() {
				@Override
				public void handle(SetResponse event) {
				}
        	});
        	
        } else if ("test".equals(method)) {
        	Number num = object.get("number");
        	JsonObject obj = new JsonObject();
        	obj.put("type", "test");
        	obj.put("number", num);
        	controller.emit(obj);
        }
    }
}
