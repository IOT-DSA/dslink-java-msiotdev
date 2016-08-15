package org.dsa.iot.msiotdev.host;

import org.dsa.iot.dslink.methods.responses.ListResponse;
import org.dsa.iot.dslink.util.handler.Handler;
import org.dsa.iot.dslink.util.json.JsonArray;
import org.dsa.iot.dslink.util.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class HostLister {
    private static final Logger LOG = LoggerFactory.getLogger(HostLister.class);

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

        LOG.debug("Activate list for " + path);
        RequesterListContainer container = controller.getHandler().getListContainer();
        container.subscribe(path, updateHandler);
    }

    public void deactivate() {
        isActive = false;

        LOG.debug("Deactivate list for " + path);

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

        if (event == null) {
            CompletableFuture<ListResponse> future = new CompletableFuture<>();
            Handler<ListResponse> handler = future::complete;
            container.subscribe(path, handler);
            try {
                event = future.get(5, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                LOG.warn("Timed out trying to get the current list state of " + path + ": Timed out.");
            }
            container.unsubscribe(path, handler);
        }

        if (event != null) {
            JsonArray updates = event.getJsonResponse(null).get("updates");

            if (updates != null && updates.size() > 0) {
                JsonObject object = new JsonObject();
                object.put("type", "list-state");
                object.put("path", path);
                object.put("state", updates);
                return object;
            }
        }

        return null;
    }

    public class UpdateHandler implements Handler<ListResponse> {
        @Override
        public void handle(ListResponse event) {
            LOG.debug("Received list update for " + path);

            JsonArray updates = event.getJsonResponse(null).get("updates");

            if (updates != null && updates.size() > 0) {
                JsonObject object = new JsonObject();
                object.put("type", "list");
                object.put("path", path);
                object.put("state", updates);
                controller.emit(object);
            }
        }
    }
}
