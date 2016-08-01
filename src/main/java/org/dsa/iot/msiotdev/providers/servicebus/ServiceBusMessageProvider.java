package org.dsa.iot.msiotdev.providers.servicebus;

import com.microsoft.windowsazure.Configuration;
import com.microsoft.windowsazure.exception.ServiceException;
import com.microsoft.windowsazure.services.servicebus.ServiceBusConfiguration;
import com.microsoft.windowsazure.services.servicebus.ServiceBusContract;
import com.microsoft.windowsazure.services.servicebus.ServiceBusService;
import com.microsoft.windowsazure.services.servicebus.models.ListTopicsResult;
import com.microsoft.windowsazure.services.servicebus.models.TopicInfo;
import org.dsa.iot.dslink.util.json.JsonObject;
import org.dsa.iot.msiotdev.providers.ClientMessageFacade;
import org.dsa.iot.msiotdev.providers.HostMessageFacade;
import org.dsa.iot.msiotdev.providers.MessageProvider;
import org.dsa.iot.msiotdev.utils.ConnectionStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

public class ServiceBusMessageProvider implements MessageProvider {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceBusMessageProvider.class);

    @Override
    public HostMessageFacade getHostFacade(JsonObject config) {
        String deviceId = config.get("deviceId");
        String conn = config.get("connection");
        Configuration serviceBusConfig = getConnectionStringConfig(conn);
        ServiceBusContract service = ServiceBusService.create(serviceBusConfig);
        TopicInfo inputTopic = new TopicInfo("broker." + deviceId + ".input");
        TopicInfo outputTopic = new TopicInfo("broker." + deviceId + ".output");

        try {
            service.getTopic(inputTopic.getPath());
        } catch (Exception e) {
            try {
                service.createTopic(inputTopic);
            } catch (Exception e1) {
                LOG.warn("Failed to create host input topic.", e1);
            }
        }

        try {
            service.getTopic(outputTopic.getPath());
        } catch (Exception e) {
            try {
                service.createTopic(outputTopic);
            } catch (Exception e1) {
                LOG.warn("Failed to create host output topic.", e1);
            }
        }

        return new ServiceBusHostMessageFacade(
                service,
                deviceId
        );
    }

    @Override
    public ClientMessageFacade getClientFacade(JsonObject config) {
        String deviceId = config.get("deviceId");
        String conn = config.get("connection");
        Configuration serviceBusConfig = getConnectionStringConfig(conn);
        ServiceBusContract service = ServiceBusService.create(serviceBusConfig);

        TopicInfo inputTopic = new TopicInfo("broker." + deviceId + ".input");
        TopicInfo outputTopic = new TopicInfo("broker." + deviceId + ".output");

        try {
            service.getTopic(inputTopic.getPath());
        } catch (Exception e) {
            LOG.warn("Failed to get client input topic.", e);
        }

        try {
            service.getTopic(outputTopic.getPath());
        } catch (Exception e) {
            LOG.warn("Failed to get client output topic.", e);
        }

        return new ServiceBusClientMessageFacade(
                service,
                deviceId
        );
    }

    public Configuration getConnectionStringConfig(String conn) {
        Map<String, String> map = ConnectionStringUtils.parse(conn);
        String host = map.get("Endpoint");
        try {
            URI uri = new URI(host);
            String name = uri.getHost().split("\\.", 2)[0];

            return ServiceBusConfiguration.configureWithSASAuthentication(
                    name,
                    map.get("SharedAccessKeyName"),
                    map.get("SharedAccessKey"),
                    ".servicebus.windows.net"
            );
        } catch (URISyntaxException e) {
            LOG.error("Failed.", e);
            return null;
        }
    }
}
