package org.dsa.iot.msiotdev.client;

import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.actions.ActionResult;
import org.dsa.iot.dslink.util.handler.Handler;

public class RemoveClientAction implements Handler<ActionResult> {
    private Node node;

    public RemoveClientAction(Node node) {
        this.node = node;
    }

    @Override
    public void handle(ActionResult event) {
        IotClientController controller = node.getMetaData();
        controller.destroy();
        node.delete();
    }
}
