package org.dsa.iot.msiotdev.host;

import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.actions.ActionResult;
import org.dsa.iot.dslink.util.handler.Handler;

public class RemoveHostAction implements Handler<ActionResult> {
    private Node node;

    public RemoveHostAction(Node node) {
        this.node = node;
    }

    @Override
    public void handle(ActionResult event) {
        IotHostController controller = node.getMetaData();
        if (controller != null) controller.destroy();
        node.delete();
    }
}
