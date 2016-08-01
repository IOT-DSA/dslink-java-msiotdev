package org.dsa.iot.msiotdev.providers.servicebus;

import com.google.common.io.ByteStreams;
import com.microsoft.windowsazure.exception.ServiceException;
import com.microsoft.windowsazure.services.servicebus.ServiceBusContract;
import com.microsoft.windowsazure.services.servicebus.models.BrokeredMessage;
import com.microsoft.windowsazure.services.servicebus.models.ReceiveSubscriptionMessageResult;
import com.microsoft.windowsazure.services.servicebus.models.SubscriptionInfo;
import org.dsa.iot.dslink.provider.LoopProvider;
import org.dsa.iot.dslink.util.json.EncodingFormat;
import org.dsa.iot.dslink.util.json.JsonObject;
import org.dsa.iot.msiotdev.providers.ClientMessageFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.function.Consumer;

public class ServiceBusClientMessageFacade implements ClientMessageFacade {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceBusHostMessageFacade.class);

    private ServiceBusContract service;
    private String deviceId;
    private Consumer<JsonObject> consumer;
    private boolean canceled = false;

    public ServiceBusClientMessageFacade(ServiceBusContract service, String deviceId) {
        this.service = service;
        this.deviceId = deviceId;

        SubscriptionInfo info = new SubscriptionInfo("broker." + deviceId + ".clientOutputSubscription");
        String outputTopic = "broker." + deviceId + ".output";

        try {
            service.getSubscription(info.getName(), outputTopic);
        } catch (Exception getE) {
            if (!getE.getMessage().contains("409 Conflict")) {
                try {
                    service.createSubscription(outputTopic, info);
                } catch (Exception e) {
                    if (!e.getMessage().contains(" already exists")) {
                        LOG.warn("Failed to create subscription.", e);
                    }
                }
            }
        }

        LoopProvider.getProvider().schedule(this::receiveTick);
    }

    public void receiveTick() {
        receive();

        if (!canceled) {
            LoopProvider.getProvider().schedule(this::receiveTick);
        }
    }

    public void receive() {
        try {
            ReceiveSubscriptionMessageResult result = service.receiveSubscriptionMessage(
                    "broker." + deviceId + ".output",
                    "broker." + deviceId + ".clientOutputSubscription"
            );

            BrokeredMessage msg = result.getValue();
            if (msg != null && msg.getBody() != null) {
                byte[] bytes = ByteStreams.toByteArray(msg.getBody());
                JsonObject obj = new JsonObject(EncodingFormat.MESSAGE_PACK, bytes);
                if (consumer != null) {
                    consumer.accept(obj);
                }
            }
        } catch (ServiceException | IOException e) {
            LOG.warn("Failed to receive data.", e);
        }
    }

    @Override
    public void emit(JsonObject object) {
        byte[] bytes = object.encode(EncodingFormat.MESSAGE_PACK);
        BrokeredMessage msg = new BrokeredMessage(bytes);
        try {
            service.sendTopicMessage("broker." + deviceId + ".input", msg);
        } catch (ServiceException e) {
            LOG.error("Failed to send " + new String(object.encode(EncodingFormat.JSON)) + " to service bus client.", e);
        }
    }

    @Override
    public void handle(Consumer<JsonObject> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void destroy() {
        canceled = true;
    }
}
