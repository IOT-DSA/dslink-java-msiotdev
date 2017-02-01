package org.dsa.iot.msiotdev.providers.iothub;

import com.microsoft.azure.iothub.DeviceClient;
import com.microsoft.azure.iothub.IotHubMessageResult;
import com.microsoft.azure.iothub.Message;
import org.dsa.iot.dslink.util.json.EncodingFormat;
import org.dsa.iot.dslink.util.json.JsonObject;
import org.dsa.iot.msiotdev.providers.HostMessageFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.function.Consumer;

public class IotHubHostMessageFacade implements HostMessageFacade {
    private static final Logger LOG = LoggerFactory.getLogger(IotHubHostMessageFacade.class);


    private DeviceClient device;
    private Consumer<JsonObject> consumer;

    public IotHubHostMessageFacade(DeviceClient device) {
        this.device = device;

        device.setMessageCallback((message, callbackContext) -> {
            if (consumer != null) {
                byte[] bytes = message.getBytes();
                JsonObject object = new JsonObject(EncodingFormat.MESSAGE_PACK, bytes);
                consumer.accept(object);
            }
            return IotHubMessageResult.COMPLETE;
        }, null);

        emit(new JsonObject() {{
            put("event", "ready");
        }});
    }

    @Override
    public void emit(JsonObject object) {
        Message message = new Message(object.encode(EncodingFormat.MESSAGE_PACK));
        device.sendEventAsync(message, (responseStatus, callbackContext) -> {
        }, null);
    }

    @Override
    public void handle(Consumer<JsonObject> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void destroy() {
        if (device != null) {
            try {
                device.close();
            } catch (IOException ignored) {
            }
        }
    }
}
