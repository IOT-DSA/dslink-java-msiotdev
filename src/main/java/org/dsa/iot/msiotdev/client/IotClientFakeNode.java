package org.dsa.iot.msiotdev.client;

import org.dsa.iot.dslink.link.Linkable;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeBuilder;
import org.dsa.iot.dslink.node.SubscriptionManager;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueType;

public class IotClientFakeNode extends Node {
    private IotClientController controller;
    private String dsaPath;

    public IotClientFakeNode(String name, Node parent, Linkable link, IotClientController controller, String dsaPath) {
        super(name, parent, link);
        this.controller = controller;
        this.dsaPath = dsaPath;
    }

    @Override
    public Node getChild(String name) {
        Node child = super.getChild(name);
        if (child == null) {
            child = createChild(name).getChild();
            IotNodeController nodeController = new IotNodeController(
                    controller,
                    (IotClientFakeNode) child,
                    ((IotClientFakeNode) child).dsaPath
            );

            nodeController.init();
            nodeController.loadNow();

            addChild(child);
        }

        if (child instanceof IotClientFakeNode) {
            ((IotNodeController) child.getMetaData()).init();
        }

        return child;
    }

    @Override
    public boolean hasChild(String name) {
        return true;
    }

    @Override
    public NodeBuilder createChild(String name, String profile) {
        NodeBuilder b = new NodeBuilder(this, new IotClientFakeNode(
                name,
                this,
                getLink(),
                controller,
                (dsaPath.equals("/") ? "" : dsaPath) + "/" + name
        ));

        if (profile != null) {
            b.setProfile(profile);
        }
        return b;
    }

    public IotClientFakeNode getCachedChild(String name) {
        return (IotClientFakeNode) super.getChild(name);
    }

    @Override
    public Node removeChild(String name) {
        return super.removeChild(name);
    }

    @Override
    public void delete() {
        super.delete();
    }

    @Override
    public Value setConfig(String name, Value value) {
        if (name.startsWith("$")) {
            name = name.substring(1);
        }

        return super.setConfig(name, value);
    }

    @Override
    public Value setAttribute(String name, Value value) {
        if (name.startsWith("@") && !name.startsWith("@@")) {
            name = name.substring(1);
        }

        return super.setAttribute(name, value);
    }

    public String getDsaPath() {
        return dsaPath;
    }

    public IotClientController getController() {
        return controller;
    }

    @Override
    public void clearChildren() {
        super.clearChildren();
    }

    @Override
    public Node addChild(Node node) {
        return super.addChild(node);
    }
}
