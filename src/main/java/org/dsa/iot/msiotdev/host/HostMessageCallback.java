package org.dsa.iot.msiotdev.host;

import com.microsoft.azure.iothub.IotHubMessageResult;
import com.microsoft.azure.iothub.Message;
import com.microsoft.azure.iothub.MessageCallback;
import org.dsa.iot.dslink.util.json.EncodingFormat;
import org.dsa.iot.dslink.util.json.JsonObject;

import java.util.Map;

@SuppressWarnings("Duplicates")
public class HostMessageCallback implements MessageCallback {
    private final IotHostController controller;

    public HostMessageCallback(IotHostController controller) {
        this.controller = controller;
    }

    @Override
    public IotHubMessageResult execute(Message message, Object callbackContext) {
        byte[] bytes = message.getBytes();
        JsonObject object = new JsonObject(EncodingFormat.MESSAGE_PACK, bytes);
        String method = object.get("method");

        if ("subscribe".equals(method)) {
            String path = object.get("path");

            Map<String, HostSubscription> subs = controller.getSubscriptions();
            HostSubscription sub = subs.get(path);
            if (sub == null) {
                sub = new HostSubscription(controller, path);
            }
            sub.onListenerAdded();
            return IotHubMessageResult.COMPLETE;
        } else if ("unsubscribe".equals(method)) {
            String path = object.get("path");
            Map<String, HostSubscription> subs = controller.getSubscriptions();
            HostSubscription sub = subs.get(path);
            if (sub != null) {
                sub.onListenerRemoved();
                return IotHubMessageResult.COMPLETE;
            } else {
                return IotHubMessageResult.REJECT;
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
                return IotHubMessageResult.COMPLETE;
            } else {
                return IotHubMessageResult.REJECT;
            }
        } else if ("list".equals(method)) {
            String path = object.get("path");

            Map<String, HostLister> subs = controller.getLists();
            HostLister sub = subs.get(path);
            if (sub == null) {
                sub = new HostLister(controller, path);
            }
            sub.onListenerAdded();
            return IotHubMessageResult.COMPLETE;
        } else if ("unlist".equals(method)) {
            String path = object.get("path");
            Map<String, HostLister> subs = controller.getLists();
            HostLister sub = subs.get(path);
            if (sub != null) {
                sub.onListenerRemoved();
                return IotHubMessageResult.COMPLETE;
            } else {
                return IotHubMessageResult.REJECT;
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
                return IotHubMessageResult.COMPLETE;
            } else {
                return IotHubMessageResult.REJECT;
            }
        } else if ("get-list-state".equals(method)) {
            String path = object.get("path");

            Map<String, HostLister> subs = controller.getLists();
            HostLister sub = subs.get(path);
            if (sub != null) {
                sub.getController().emit(sub.getCurrentState());
                return IotHubMessageResult.COMPLETE;
            } else {
                return IotHubMessageResult.REJECT;
            }
        } else if ("get-subscribe-state".equals(method)) {
            String path = object.get("path");

            Map<String, HostSubscription> subs = controller.getSubscriptions();
            HostSubscription sub = subs.get(path);
            if (sub != null) {
                sub.getController().emit(sub.getCurrentState());
                return IotHubMessageResult.COMPLETE;
            } else {
                return IotHubMessageResult.REJECT;
            }
        } else {
            return IotHubMessageResult.REJECT;
        }
    }
}
