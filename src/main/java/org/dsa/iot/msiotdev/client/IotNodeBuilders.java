package org.dsa.iot.msiotdev.client;

import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeBuilder;

import java.util.ArrayList;
import java.util.List;

public class IotNodeBuilders {
    public static void applyMultiChildBuilders(IotClientFakeNode owner, List<NodeBuilder> builders) {
        List<Node> nodes = new ArrayList<>();
        for (NodeBuilder builder : builders) {
            Node node = builder.getChild();

            String go = owner.getDsaPath() + "/" + node.getName();
            IotNodeController nc = new IotNodeController(owner.getController(), node, go);
            nc.init();

            nodes.add(node);
        }

        owner.addChildren(nodes);
    }
}
