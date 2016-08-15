package org.dsa.iot.msiotdev.providers.iothub;

import com.microsoft.azure.eventhubs.EventData;
import com.microsoft.azure.eventhubs.PartitionReceiver;
import org.dsa.iot.dslink.provider.LoopProvider;
import org.dsa.iot.dslink.util.Objects;
import org.dsa.iot.dslink.util.json.EncodingFormat;
import org.dsa.iot.dslink.util.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public class ClientMessageHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ClientMessageHandler.class);

    private IotHubClientMessageFacade facade;
    private PartitionReceiver receiver;
    private boolean canceled = false;

    public ClientMessageHandler(IotHubClientMessageFacade facade, PartitionReceiver receiver) {
        this.facade = facade;
        this.receiver = receiver;
    }

    public void receive() {
         LOG.debug("Attempting to fetch events from partition " + receiver.getPartitionId() + ".");

        try {
            Iterable<EventData> datas = receiver.receive(2).get(
                    1,
                    TimeUnit.SECONDS
            );

            if (datas != null) {
                for (EventData data : datas) {
                    handleEvent(data);
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Failed to fetch events from partition " + receiver.getPartitionId(), e);
        } catch (TimeoutException ignored) {
        }

        schedule();
    }

    public void schedule() {
        LoopProvider.getProvider().schedule(() -> {
            if (!canceled) {
                receive();
            }
        });
    }

    public void handleEvent(EventData data) {
        LoopProvider.getProvider().schedule(() -> {
            try {
                JsonObject object = new JsonObject(EncodingFormat.MESSAGE_PACK, data.getBody());

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Received event " + new String(object.encodePrettily(EncodingFormat.JSON)) + " from hub.");
                }

                if (facade != null) {
                    Consumer<JsonObject> consumer = facade.getConsumer();

                    if (consumer != null) {
                        consumer.accept(object);
                    }
                }
            } catch (Exception e) {
                LOG.error("Failed to handle client event.", e);
            }
        });
    }

    public void disable() {
        canceled = true;
    }
}
