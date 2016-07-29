package org.dsa.iot.msiotdev.providers;

import org.dsa.iot.dslink.util.json.JsonObject;

import java.util.function.Consumer;

public interface MessageFacade {
    void emit(JsonObject object);
    void handle(Consumer<JsonObject> consumer);
    void destroy();
}
