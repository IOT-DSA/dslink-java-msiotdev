package org.dsa.iot.msiotdev.client;

import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.actions.ActionResult;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.util.handler.Handler;
import org.dsa.iot.msiotdev.IotLinkHandler;

public class CreateClientAction implements Handler<ActionResult> {
    private IotLinkHandler handler;

    public CreateClientAction(IotLinkHandler handler) {
        this.handler = handler;
    }

    @Override
    public void handle(ActionResult event) {
        String name = event.getParameter("name").getString();
        String deviceId = event.getParameter("targetDeviceId").getString();
        String connectionString = event.getParameter("connection").getString();
        String eventConnectionString = event.getParameter("eventConnection").getString();

        String realName = Node.checkAndEncodeName(name);
        Node node = handler
                .getResponderLink()
                .getNodeManager()
                .createRootNode(realName)
                .setDisplayName(name)
                .setRoConfig("msiot_device", new Value(deviceId))
                .setRoConfig("msiot_conn", new Value(connectionString))
                .setRoConfig("msiot_event_conn", new Value(eventConnectionString))
                .setRoConfig("client", new Value(true))
                .build();

        handler.initializeClientNode(node);
    }
}
