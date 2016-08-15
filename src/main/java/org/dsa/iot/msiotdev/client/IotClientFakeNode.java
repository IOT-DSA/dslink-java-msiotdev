package org.dsa.iot.msiotdev.client;

import org.dsa.iot.dslink.link.Linkable;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeBuilder;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class IotClientFakeNode extends Node {
    private static final Logger LOG = LoggerFactory.getLogger(IotClientFakeNode.class);

    private IotClientController controller;
    private String dsaPath;

    public IotClientFakeNode(String name, Node parent, Linkable link, IotClientController controller, String dsaPath) {
        super(name, parent, link);
        this.controller = controller;
        this.dsaPath = dsaPath;

        setSerializable(false);
        setProfile("node");
    }

    @Override
    public Node getChild(String name) {
        try {
            IotClientFakeNode child = getCachedChild(name);

            if (child == null) {
                child = (IotClientFakeNode) createChild(name).build();
            }

            if (child.getMetaData() == null) {
                IotNodeController nodeController = new IotNodeController(
                        controller,
                        child,
                        child.dsaPath
                );

                nodeController.init();
                nodeController.loadNow();
                child.setMetaData(nodeController);
            } else {
                ((IotNodeController) child.getMetaData()).init();
            }

            return child;
        } catch (Exception e) {
            LOG.error("Failed to fetch child '" + name + "' of " + getPath(), e);
            return null;
        }
    }

    @Override
    public boolean hasChild(String name) {
        IotClientFakeNode node = getCachedChild(name);

        if (node == null) {
            Objects.getDaemonThreadPool().execute(() -> getChild(name));
            return false;
        } else {
            return true;
        }
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
    public Map<String, Value> clearAttributes() {
        LOG.info("Clear attributes for " + dsaPath);
        return super.clearAttributes();
    }

    @Override
    public Map<String, Value> clearConfigs() {
        LOG.info("Clear configs for " + dsaPath);
        return super.clearConfigs();
    }

    @Override
    public Node addChild(Node node) {
        if (getCachedChild(node.getName()) == null) {
            return super.addChild(node);
        } else {
            return getCachedChild(node.getName());
        }
    }
}
