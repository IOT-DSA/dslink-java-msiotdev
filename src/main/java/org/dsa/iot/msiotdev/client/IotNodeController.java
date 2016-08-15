package org.dsa.iot.msiotdev.client;

import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeBuilder;
import org.dsa.iot.dslink.node.Permission;
import org.dsa.iot.dslink.node.Writable;
import org.dsa.iot.dslink.node.actions.*;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValuePair;
import org.dsa.iot.dslink.node.value.ValueType;
import org.dsa.iot.dslink.node.value.ValueUtils;
import org.dsa.iot.dslink.util.handler.Handler;
import org.dsa.iot.dslink.util.json.JsonArray;
import org.dsa.iot.dslink.util.json.JsonObject;
import org.dsa.iot.msiotdev.utils.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@SuppressWarnings({"Duplicates", "Convert2Lambda"})
public class IotNodeController {
    private static final Logger LOG = LoggerFactory.getLogger(IotNodeController.class);

    private IotClientController controller;
    private int listHandles = 0;
    private int subscribeHandles = 0;
    private boolean hasListHandle = false;
    private boolean hasSubscribeHandle = false;
    private IotClientFakeNode node;
    private String dsaPath;

    public IotNodeController(IotClientController controller, IotClientFakeNode node, String dsaPath) {
        this.controller = controller;
        this.node = node;
        this.dsaPath = dsaPath;

        node.setSerializable(false);
        node.setMetaData(this);
    }

    private boolean isInitialized = false;

    public void init() {
        if (isInitialized) {
            return;
        }
        isInitialized = true;

        LOG.debug("Initializing node controller for " + dsaPath);

        node.getListener().setOnListHandler(new Handler<Node>() {
            @Override
            public void handle(Node event) {
                listHandles++;
                checkListHandles();
            }
        });

        node.getListener().setNodeRemovedHandler(new Handler<Node>() {
            @Override
            public void handle(Node event) {
                LOG.info(dsaPath + " was removed.");
            }
        });

        node.getListener().setOnListClosedHandler(new Handler<Node>() {
            @Override
            public void handle(Node event) {
                listHandles--;
                checkListHandles();
            }
        });

        node.getListener().setOnSubscribeHandler(new Handler<Node>() {
            @Override
            public void handle(Node event) {
                subscribeHandles++;
                checkSubscribeHandles();
            }
        });

        node.getListener().setOnUnsubscribeHandler(new Handler<Node>() {
            @Override
            public void handle(Node event) {
                subscribeHandles--;
                checkSubscribeHandles();
            }
        });

        node.getListener().setValueHandler(new Handler<ValuePair>() {
            @Override
            public void handle(ValuePair event) {
            }
        });
    }

    public void checkListHandles() {
        if (listHandles <= 0) {
            if (hasListHandle) {
                closeListHandles();
                listHandles = 0;
            }
        } else {
            if (!hasListHandle) {
                startListHandles();
            }
        }
    }

    public void checkSubscribeHandles() {
        if (subscribeHandles <= 0) {
            if (hasSubscribeHandle) {
                closeSubscribeHandles();
            }
        } else {
            if (!hasSubscribeHandle) {
                startSubscribeHandles();
            }
        }
    }

    public void updateListData(JsonArray listArray, boolean isState) {
        isFirstUpdate = false;

        Set<String> toRemove = null;

        if (isState) {
            toRemove = new HashSet<>();

            if (node.getConfigurations() != null) {
                for (String key : node.getConfigurations().keySet()) {
                    toRemove.add("$" + key);
                }
            }

            if (node.getChildren() != null) {
                for (String key : node.getChildren().keySet()) {
                    toRemove.add(key);
                }
            }

            if (node.getAttributes() != null) {
                for (String key : node.getAttributes().keySet()) {
                    toRemove.add("@" + key);
                }
            }

            if (node.getValueType() != null) {
                toRemove.add("$type");
            }

            if (node.isHidden()) {
                toRemove.add("$hidden");
            }

            if (node.getDisplayName() != null) {
                toRemove.add("$name");
            }
        }

        List<NodeBuilder> childQueue = new ArrayList<>();
        for (Object o : listArray) {
            if (o instanceof JsonArray) {
                JsonArray m = (JsonArray) o;

                String key = m.get(0);

                if (toRemove != null) {
                    toRemove.remove(key);
                }

                Object mvalue;

                if (m.size() > 1) {
                    mvalue = m.get(1);
                } else {
                    mvalue = new JsonObject();
                }

                Value value = ValueUtils.toValue(mvalue);

                if (value == null) {
                    value = new Value((String) null);
                }

                //noinspection StatementWithEmptyBody
                if (key.equals("$is")) {
                    // Do Nothing.
                } else if (key.equals("$type")) {
                    ValueType type = ValueType.toValueType(value.getString());
                    if (!type.equals(node.getValueType())) {
                        node.setValueType(type);
                    }
                } else if (key.equals("$name")) {
                    if (node.getDisplayName() == null || !node.getDisplayName().equals(value.getString())) {
                        node.setDisplayName(value.getString());
                    }
                } else if (key.equals("$hasChildren")) {
                    node.setHasChildren(node.getHasChildren());
                } else if (key.equals("$invokable")) {
                    Permission perm = Permission.toEnum(value.getString());
                    Action act = getOrCreateAction(node, perm, false);
                    if (act.getPermission() == null || !act.getPermission().getJsonName().equals(perm.getJsonName())) {
                        act.setPermission(perm);
                    }
                } else if (key.equals("$columns")) {
                    if (mvalue instanceof JsonArray) {
                        JsonArray array = (JsonArray) mvalue;
                        Action act = getOrCreateAction(node, Permission.NONE, false);
                        iterateActionMetaData(act, array, true);
                    }
                } else if (key.equals("$writable")) {
                    String string = value.getString();
                    node.setWritable(Writable.toEnum(string));
                } else if (key.equals("$params")) {
                    if (mvalue instanceof JsonArray) {
                        JsonArray array = (JsonArray) mvalue;
                        Action act = getOrCreateAction(node, Permission.NONE, false);
                        iterateActionMetaData(act, array, false);
                    }
                } else if (key.equals("$hidden")) {
                    node.setHidden(value.getBool());
                } else if (key.equals("$result")) {
                    String string = value.getString();
                    Action act = getOrCreateAction(node, Permission.NONE, false);
                    if (act.getResultType() == null || !act.getResultType().getJsonName().equals(string)) {
                        act.setResultType(ResultType.toEnum(string));
                    }
                } else if (key.startsWith("$$")) {
                    String cname = key.substring(2);
                    if (!Values.isEqual(value, node.getRoConfig(cname))) {
                        node.setRoConfig(cname, value);
                    }
                } else if (key.startsWith("$")) {
                    String cname = key.substring(1);
                    if (!Values.isEqual(value, node.getConfig(cname))) {
                        node.setConfig(cname, value);
                    }
                } else if (key.startsWith("@")) {
                    String cname = key.substring(1);
                    if (!Values.isEqual(value, node.getAttribute(cname))) {
                        node.setAttribute(cname, value);
                    }
                } else {
                    IotClientFakeNode child = node.getCachedChild(key);

                    if (child == null) {
                        NodeBuilder builder = node.createChild(key);
                        if (mvalue instanceof JsonObject) {
                            JsonObject co = (JsonObject) mvalue;
                            for (Map.Entry<String, Object> entry : co) {
                                applyCreatedAttribute(builder, entry.getKey(), entry.getValue());
                            }
                        }

                        child = (IotClientFakeNode) builder.build();
                        if (child.getMetaData() == null) {
                            String childNodePath = node.getDsaPath();
                            if (!childNodePath.equals("/")) {
                                childNodePath += "/";
                            }
                            childNodePath += key;
                            IotNodeController nodeController = new IotNodeController(controller, child, childNodePath);
                            nodeController.init();
                            child.setMetaData(nodeController);
                        }

                        if (node.getCachedChild(key) == null) {
                            node.addChild(child);
                        }
                    } else {
                        if (mvalue instanceof JsonObject) {
                            JsonObject co = (JsonObject) mvalue;
                            for (Map.Entry<String, Object> entry : co) {
                                applyAttribute(child, entry.getKey(), entry.getValue(), true);
                            }
                        }
                    }

                    if (toRemove != null) {
                        toRemove.remove(key);
                    }
                }
            } else if (o instanceof JsonObject) {
                JsonObject obj = (JsonObject) o;
                if (obj.contains("change") && obj.get("change").equals("remove")) {
                    String key = obj.get("name");

                    if (key.startsWith("$$")) {
                        node.removeRoConfig(key.substring(2));
                    } else if (key.startsWith("$")) {
                        node.removeConfig(key.substring(1));
                    } else if (key.startsWith("@")) {
                        node.removeAttribute(key.substring(1));
                    } else {
                        node.removeChild(key);
                    }

                    if (toRemove != null) {
                        toRemove.remove(key);
                    }
                }
            }
        }

        if (childQueue.size() > 0) {
            IotNodeBuilders.applyMultiChildBuilders(node, childQueue);
        }

        if (toRemove != null) {
            for (String key : toRemove) {
                if (key.equals("$name")) {
                    node.setDisplayName(null);
                } else if (key.equals("$type")) {
                    node.setValueType(null);
                } else if (key.equals("$hidden")) {
                    node.setHidden(false);
                } else if (key.startsWith("$")) {
                    node.removeConfig(key.substring(1));
                } else if (key.startsWith("@")) {
                    node.removeAttribute(key.substring(1));
                } else {
                    node.removeChild(key);
                }
            }
        }

        checkListHandles();
    }

    public void applyCreatedAttribute(NodeBuilder n, String key, Object mvalue) {
        Value value = ValueUtils.toValue(mvalue);

        if (value == null) {
            value = new Value((String) null);
        }

        if (key.equals("$is")) {
            //node.setProfile(value.getString());
        } else if (key.equals("$type")) {
            n.setValueType(ValueType.toValueType(value.getString()));
        } else if (key.equals("$name")) {
            n.setDisplayName(value.getString());
        } else if (key.equals("$invokable")) {
            Permission perm = Permission.toEnum(value.getString());
            Action act = getOrCreateAction(n.getChild(), perm, true);
            act.setPermission(perm);
        } else if (key.equals("$columns")) {
            JsonArray array = (JsonArray) mvalue;
            Action act = getOrCreateAction(n.getChild(), Permission.NONE, true);
            iterateActionMetaData(act, array, true);
        } else if (key.equals("$writable")) {
            String string = value.getString();
            if (node.getWritable() == null || !node.getWritable().toJsonName().equals(string)) {
                node.setWritable(Writable.toEnum(string));
            }
        } else if (key.equals("$params")) {
            if (mvalue instanceof JsonArray) {
                JsonArray array = (JsonArray) mvalue;
                Action act = getOrCreateAction(node, Permission.NONE, false);
                iterateActionMetaData(act, array, false);
            }
        } else if (key.equals("$hidden")) {
            n.setHidden(value.getBool());
        } else if (key.equals("$result")) {
            String string = value.getString();
            Action act = getOrCreateAction(n.getChild(), Permission.NONE, true);
            act.setResultType(ResultType.toEnum(string));
        } else if (key.equals("$$password")) {
            if (value.getString() != null) {
                node.setPassword(value.getString().toCharArray());
            } else {
                node.setPassword(null);
            }
        } else if (key.equals("$hasChildren")) {
            if (value.getBool() != node.getHasChildren()) {
                node.setHasChildren(value.getBool());
            }
        } else if (key.startsWith("$$")) {
            n.setRoConfig(key.substring(2), value);
        } else if (key.startsWith("$")) {
            n.setConfig(key.substring(1), value);
        } else if (key.startsWith("@")) {
            n.setAttribute(key.substring(1), value);
        }
    }

    public void applyAttribute(Node n, String key, Object mvalue, boolean isChild) {
        Value value = ValueUtils.toValue(mvalue);

        if (value == null) {
            value = new Value((String) null);
        }

        if (key.equals("$is")) {
            //node.setProfile(value.getString());
        } else if (key.equals("$type")) {
            n.setValueType(ValueType.toValueType(value.getString()));
        } else if (key.equals("$name")) {
            n.setDisplayName(value.getString());
        } else if (key.equals("$invokable")) {
            Permission perm = Permission.toEnum(value.getString());
            Action act = getOrCreateAction(n, perm, isChild);
            act.setPermission(perm);
        } else if (key.equals("$columns")) {
            JsonArray array = (JsonArray) mvalue;
            Action act = getOrCreateAction(n, Permission.NONE, isChild);
            iterateActionMetaData(act, array, true);
        } else if (key.equals("$writable")) {
            String string = value.getString();
            n.setWritable(Writable.toEnum(string));
        } else if (key.equals("$params")) {
            JsonArray array = (JsonArray) mvalue;
            Action act = getOrCreateAction(n, Permission.NONE, isChild);
            iterateActionMetaData(act, array, false);
        } else if (key.equals("$hidden")) {
            n.setHidden(value.getBool());
        } else if (key.equals("$result")) {
            String string = value.getString();
            Action act = getOrCreateAction(n, Permission.NONE, isChild);
            act.setResultType(ResultType.toEnum(string));
        } else if (key.startsWith("$$")) {
            n.setRoConfig(key.substring(2), value);
        } else if (key.startsWith("$")) {
            n.setConfig(key.substring(1), value);
        } else if (key.startsWith("@")) {
            n.setAttribute(key.substring(1), value);
        }
    }

    public void updateValueData(JsonArray valueArray) {
        Value val = ValueUtils.toValue(valueArray.get(0), valueArray.get(1));

        if (val != null && (node.getValueType() == null || !val.getType().getRawName().equals(node.getValueType().getRawName()))) {
            node.setValueType(val.getType());
        }

        node.setValue(val);

        checkSubscribeHandles();
    }

    public void loadNow() {
        JsonObject object = new JsonObject();
        object.put("method", "get-list-state");
        object.put("path", dsaPath);
        controller.emit(object);
    }

    public void startListHandles() {
        hasListHandle = true;

        JsonObject object = new JsonObject();
        object.put("method", "list");
        object.put("path", dsaPath);
        controller.emit(object);
    }

    public void closeListHandles() {
        hasListHandle = false;
        listHandles = 0;

        JsonObject object = new JsonObject();
        object.put("method", "unlist");
        object.put("path", dsaPath);
        controller.emit(object);
    }

    public void startSubscribeHandles() {
        hasSubscribeHandle = true;
        JsonObject object = new JsonObject();
        object.put("method", "subscribe");
        object.put("path", dsaPath);
        controller.emit(object);
    }

    public void closeSubscribeHandles() {
        hasSubscribeHandle = false;
        subscribeHandles = 0;
        JsonObject object = new JsonObject();
        object.put("method", "unsubscribe");
        object.put("path", dsaPath);
        controller.emit(object);
    }

    private Action getOrCreateAction(Node node, Permission perm, boolean isChild) {
        Action action = node.getAction();
        if (action != null) {
            return action;
        }

        action = getRawAction(node, perm, isChild);
        node.setAction(action);
        return action;
    }

    private Action getRawAction(final Node node, Permission perm, final boolean isChild) {
        return new Action(perm, new Handler<ActionResult>() {
            @Override
            public void handle(final ActionResult event) {
                event.getTable().close();
            }
        });
    }

    private static void iterateActionMetaData(Action act,
                                              JsonArray array,
                                              boolean isCol) {

        ArrayList<Parameter> out = new ArrayList<>();
        for (Object anArray : array) {
            JsonObject data = (JsonObject) anArray;
            String name = data.get("name");

            if (out.stream().anyMatch((c) -> c.getName().equals(name))) {
                continue;
            }

            String type = data.get("type");
            ValueType valType = ValueType.toValueType(type);
            Parameter param = new Parameter(name, valType);

            String editor = data.get("editor");
            if (editor != null) {
                param.setEditorType(EditorType.make(editor));
            }
            Object def = data.get("default");
            if (def != null) {
                param.setDefaultValue(ValueUtils.toValue(def));
            }
            String placeholder = data.get("placeholder");
            if (placeholder != null) {
                param.setPlaceHolder(placeholder);
            }
            String description = data.get("description");
            if (description != null) {
                param.setDescription(description);
            }

            out.add(param);
        }

        if (!out.isEmpty()) {
            if (isCol) {
                act.setColumns(out);
            } else {
                act.setParams(out);
            }
        }
    }

    private boolean isFirstUpdate = true;

    public void deliver(String type, JsonObject object) {
        if ("list-state".equals(type) || "list".equals(type)) {
            JsonArray array = object.get("state");
            updateListData(array, "list-state".equals(type) || isFirstUpdate);
        } else if ("subscribe-state".equals(type) || "subscribe".equals(type)) {
            JsonArray array =  new JsonArray();
            array.add(object.get("value"));
            array.add(object.get("timestamp"));
            updateValueData(array);
        }
    }
}
