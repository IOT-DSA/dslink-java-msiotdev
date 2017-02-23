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
        String deviceConnString = event.getParameter("deviceConnection").getString();
        String eventConnectionString = event.getParameter("eventConnection").getString();
        int numPartitions = event.getParameter("numberOfPartitions").getNumber().intValue();

        String realName = Node.checkAndEncodeName(name);
        Node node = handler
                .getResponderLink()
                .getNodeManager()
                .createRootNode(realName)
                .setDisplayName(name)
                .setRoConfig("msiot_device_conn", new Value(deviceConnString))
                .setRoConfig("msiot_event_conn", new Value(eventConnectionString))
                .setRoConfig("msiot_partition_count", new Value(numPartitions))
                .setRoConfig("host", new Value(true))
                .build();

        handler.initializeHostNode(node);
    }
}
