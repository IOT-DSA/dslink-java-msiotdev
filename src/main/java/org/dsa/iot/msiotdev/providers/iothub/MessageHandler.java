package org.dsa.iot.msiotdev.providers.iothub;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import org.dsa.iot.dslink.provider.LoopProvider;
import org.dsa.iot.dslink.util.json.EncodingFormat;
import org.dsa.iot.dslink.util.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.microsoft.azure.eventhubs.EventData;
import com.microsoft.azure.eventhubs.PartitionReceiver;

public class MessageHandler {
	private static final Logger LOG = LoggerFactory.getLogger(MessageHandler.class);

    private IotHubMessageFacade facade;
    private PartitionReceiver receiver;
    private boolean canceled = false;

    public MessageHandler(IotHubMessageFacade facade, PartitionReceiver receiver) {
        this.facade = facade;
        this.receiver = receiver;
    }

    public void receive() {
         LOG.debug("Attempting to fetch events from partition " + receiver.getPartitionId() + ".");

        try {
            Iterable<EventData> datas = receiver.receive(1).get(
                    2,
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
                if (!facade.shouldHandleEvent(object)) {
                	return;
                }

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Received event " + new String(object.encodePrettily(EncodingFormat.JSON)) + " from hub.");
                }
                LOG.info("Received event " + new String(object.encodePrettily(EncodingFormat.JSON)) + " from hub.");

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
