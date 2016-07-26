package org.dsa.iot.msiotdev.host;

import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.actions.ActionResult;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.util.handler.Handler;
import org.dsa.iot.msiotdev.IotLinkHandler;

public class CreateHostAction implements Handler<ActionResult> {
    private IotLinkHandler handler;

    public CreateHostAction(IotLinkHandler handler) {
        this.handler = handler;
    }

    @Override
    public void handle(ActionResult event) {
        String name = event.getParameter("name").getString();
        String deviceConnString = event.getParameter("connection").getString();

        String realName = Node.checkAndEncodeName(name);
        Node node = handler
                .getResponderLink()
                .getNodeManager()
                .createRootNode(realName)
                .setDisplayName(name)
                .setRoConfig("msiot_device_conn", new Value(deviceConnString))
                .setRoConfig("host", new Value(true))
                .build();

        handler.initializeHostNode(node);
    }
}
