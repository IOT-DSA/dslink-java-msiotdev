package org.dsa.iot.msiotdev;

import org.dsa.iot.dslink.methods.responses.ListResponse;
import org.dsa.iot.dslink.node.value.ValueUtils;
import org.dsa.iot.dslink.util.handler.Handler;
import org.dsa.iot.dslink.util.json.JsonObject;

public class HostLister {
    private IotHostController controller;
    private String path;
    private boolean isActive = false;
    private int listeners = 0;
    private UpdateHandler updateHandler;

    public HostLister(IotHostController controller, String path) {
        this.controller = controller;
        this.path = path;
        this.updateHandler = new UpdateHandler();
    }

    public IotHostController getController() {
        return controller;
    }

    public String getPath() {
        return path;
    }

    public void activate() {
        isActive = true;
        RequesterListContainer container = controller.getHandler().getListContainer();
        container.subscribe(path, updateHandler);
    }

    public void deactivate() {
        isActive = false;

        RequesterListContainer container = controller.getHandler().getListContainer();
        container.unsubscribe(path, updateHandler);
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
            controller.getLists().remove(path);
            return true;
        }
        return false;
    }

    public JsonObject getCurrentState() {
        RequesterListContainer container = controller.getHandler().getListContainer();

        ListResponse event = container.getCurrentState(path);

        if (event != null) {
            JsonObject object = new JsonObject();
            object.put("type", "list-state");
            object.put("path", path);
            object.put("state", ValueUtils.toValue(event.getJsonResponse(null).get("updates")));
            return object;
        }

        return null;
    }

    public class UpdateHandler implements Handler<ListResponse> {
        @Override
        public void handle(ListResponse event) {
            JsonObject object = new JsonObject();
            object.put("type", "list");
            object.put("path", path);
            object.put("state", ValueUtils.toValue(event.getJsonResponse(null).get("updates")));
            controller.emit(object);
        }
    }
}