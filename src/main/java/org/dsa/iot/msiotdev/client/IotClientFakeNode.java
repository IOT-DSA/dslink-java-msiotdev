package org.dsa.iot.msiotdev.client;

import org.dsa.iot.dslink.link.Linkable;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeBuilder;
import org.dsa.iot.dslink.node.value.Value;

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
            child = createChild(name).build();
            IotNodeController nodeController = new IotNodeController(
                    controller,
                    (IotClientFakeNode) child,
                    ((IotClientFakeNode) child).dsaPath
            );

            nodeController.init();
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
        try {
            throw new RuntimeException();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return super.removeChild(name);
    }

    @Override
    public void delete() {
        super.delete();

        try {
            throw new RuntimeException();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Value setConfig(String name, Value value) {
        if (name.equals("disconnectedTs")) {
            try {
                throw new RuntimeException();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return super.setConfig(name, value);
    }

    public String getDsaPath() {
        return dsaPath;
    }

    public IotClientController getController() {
        return controller;
    }
}
