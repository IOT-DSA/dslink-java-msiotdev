package org.dsa.iot.msiotdev.client;

import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeBuilder;
import org.dsa.iot.dslink.util.json.EncodingFormat;
import org.dsa.iot.dslink.util.json.JsonArray;
import org.dsa.iot.dslink.util.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class IotNodeBuilders {
    public static void applyMultiChildBuilders(IotClientFakeNode owner, List<NodeBuilder> builders) {
        List<Node> nodes = new ArrayList<>();
        for (NodeBuilder builder : builders) {
            Node node = builder.getChild();

            String go = owner.getDsaPath() + "/" + node.getName();

            if (go.startsWith("//")) {
                go = go.substring(1);
            }

            IotNodeController nc = new IotNodeController(owner.getController(), (IotClientFakeNode) node, go);
            nc.init();

            nodes.add(node);
        }

        owner.addChildren(nodes);

        for (Node node : nodes) {
            if (owner.getCachedChild(node.getName()) == null) {
                owner.addChild(node);
            }
        }
    }
}
