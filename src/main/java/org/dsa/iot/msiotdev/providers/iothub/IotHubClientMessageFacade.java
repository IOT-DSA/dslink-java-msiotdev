package org.dsa.iot.msiotdev.providers.iothub;

import com.microsoft.azure.eventhubs.EventHubClient;
import com.microsoft.azure.eventhubs.PartitionReceiver;
import com.microsoft.azure.iot.service.sdk.DeliveryAcknowledgement;
import com.microsoft.azure.iot.service.sdk.FeedbackReceiver;
import com.microsoft.azure.iot.service.sdk.Message;
import com.microsoft.azure.iot.service.sdk.ServiceClient;
import org.dsa.iot.commons.Container;
import org.dsa.iot.dslink.provider.LoopProvider;
import org.dsa.iot.dslink.util.json.EncodingFormat;
import org.dsa.iot.dslink.util.json.JsonObject;
import org.dsa.iot.msiotdev.providers.ClientMessageFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class IotHubClientMessageFacade implements ClientMessageFacade {
    private static final Logger LOG = LoggerFactory.getLogger(IotHubClientMessageFacade.class);

    private FeedbackReceiver feedbackReceiver;
    private EventHubClient eventHubClient;
    private ServiceClient serviceClient;
    private Consumer<JsonObject> consumer;
    private String deviceId;
    private List<ClientMessageHandler> messageHandlers;

    public IotHubClientMessageFacade(EventHubClient eventHubClient, ServiceClient serviceClient, String deviceId) {
        this.deviceId = deviceId;
        this.feedbackReceiver = serviceClient.getFeedbackReceiver(deviceId);
        this.eventHubClient = eventHubClient;
        this.serviceClient = serviceClient;
        this.messageHandlers = new ArrayList<>();

        try {
            feedbackReceiver.open();
        } catch (Exception e) {
            LOG.error("Failed to open feedback receiver.", e);
        }

        try {
            for (int i = 0; i < 4; i++) {
                PartitionReceiver receiver = eventHubClient.createReceiverSync(
                        EventHubClient.DEFAULT_CONSUMER_GROUP_NAME,
                        String.valueOf(i),
                        Instant.now()
                );

                receiver.setReceiveTimeout(Duration.ofSeconds(1));
                ClientMessageHandler handler = new ClientMessageHandler(this, receiver);
                messageHandlers.add(handler);
                handler.schedule();
            }
        } catch (Exception e) {
            LOG.warn("Failed to setup partition receivers.", e);
        }

        final Container<Runnable> task = new Container<>();
        task.setValue(() -> {
            if (feedbackReceiver == null) {
                return;
            }

            try {
                feedbackReceiver.receive();
            } catch (Exception e) {
                LOG.warn("Failed to handle feedback receive.", e);
            }

            LoopProvider.getProvider().schedule(task.getValue());
        });

        LoopProvider.getProvider().schedule(task.getValue());
    }

    @Override
    public void emit(JsonObject object) {
        Message msg = new Message(object.encode(EncodingFormat.MESSAGE_PACK));
        msg.setDeliveryAcknowledgement(DeliveryAcknowledgement.Full);
        msg.setCorrelationId(java.util.UUID.randomUUID().toString());
        msg.setUserId(java.util.UUID.randomUUID().toString());
        serviceClient.sendAsync(deviceId, msg);
    }

    @Override
    public void handle(Consumer<JsonObject> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void destroy() {
        for (ClientMessageHandler handler : messageHandlers) {
            handler.disable();
        }

        messageHandlers.clear();

        try {
            feedbackReceiver.close();
            feedbackReceiver = null;
        } catch (Exception e) {
            LOG.warn("Failed to destroy feedback receiver.", e);
        }

        try {
            eventHubClient.close();
            eventHubClient = null;
        } catch (Exception e) {
            LOG.warn("Failed to destroy event hub client.", e);
        }

        try {
            serviceClient.close();
            serviceClient = null;
        } catch (Exception e) {
            LOG.warn("Failed to destroy service client.", e);
        }
    }

    public Consumer<JsonObject> getConsumer() {
        return consumer;
    }
}
