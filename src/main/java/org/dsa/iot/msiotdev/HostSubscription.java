package org.dsa.iot.msiotdev;

import org.dsa.iot.dslink.node.value.SubscriptionValue;
import org.dsa.iot.dslink.node.value.ValueUtils;
import org.dsa.iot.dslink.util.handler.Handler;
import org.dsa.iot.dslink.util.json.JsonObject;

public class HostSubscription {
    private IotHostController controller;
    private String path;
    private boolean isActive = false;
    private int listeners = 0;
    private ValueHandler valueHandler;

    public HostSubscription(IotHostController controller, String path) {
        this.controller = controller;
        this.path = path;
        this.valueHandler = new ValueHandler();
    }

    public IotHostController getController() {
        return controller;
    }

    public String getPath() {
        return path;
    }

    public void activate() {
        isActive = true;
        RequesterSubscribeContainer container = controller.getHandler().getSubscribeContainer();
        container.subscribe(path, valueHandler);
    }

    public void deactivate() {
        isActive = false;

        RequesterSubscribeContainer container = controller.getHandler().getSubscribeContainer();
        container.unsubscribe(path, valueHandler);
    }

    public void onListenerAdded() {
        listeners++;

        if (!isActive) {
            activate();
        }
    }

    public boolean onListenerRemoved() {
        listeners--;

        if (listeners <= 0) {
            listeners = 0;
            deactivate();
            controller.getSubscriptions().remove(path);
            return true;
        }
        return false;
    }

    public JsonObject getCurrentState() {
        RequesterSubscribeContainer container = controller.getHandler().getSubscribeContainer();

        SubscriptionValue event = container.getCurrentState(path);

        if (event != null) {
            JsonObject object = new JsonObject();
            object.put("type", "subscribe-state");
            object.put("path", path);
            object.put("value", ValueUtils.toValue(event.getValue()));
            object.put("timestamp", event.getValue().getTimeStamp());
            return object;
        }
        return null;
    }

    public class ValueHandler implements Handler<SubscriptionValue> {
        @Override
        public void handle(SubscriptionValue event) {
            JsonObject object = new JsonObject();
            object.put("type", "subscribe");
            object.put("path", path);
            object.put("value", ValueUtils.toValue(event.getValue()));
            object.put("timestamp", event.getValue().getTimeStamp());
            controller.emit(object);
        }
    }
}
