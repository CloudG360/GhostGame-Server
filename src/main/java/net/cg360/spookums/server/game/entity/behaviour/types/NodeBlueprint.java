package net.cg360.spookums.server.game.entity.behaviour.types;

import net.cg360.spookums.server.util.Check;
import net.cg360.spookums.server.core.data.LockableSettings;
import net.cg360.spookums.server.core.data.Settings;
import net.cg360.spookums.server.core.data.keyvalue.Key;
import net.cg360.spookums.server.core.data.id.Identifier;

public class NodeBlueprint<T extends NodeBaseBehaviour> {

    public Identifier identifier;
    public Class<T> nodeClass;
    public Settings defaultSettings;

    public NodeBlueprint(Identifier identifier, Class<T> nodeClass, Settings defaultSettings) {
        Check.nullParam(identifier, "identifier");
        Check.nullParam(nodeClass, "nodeClass");

        this.identifier = identifier;
        this.nodeClass = nodeClass;

        this.defaultSettings = new LockableSettings(defaultSettings == null ? new Settings() : defaultSettings, true);
    }



    public Key<NodeBlueprint<T>> getKey() { return new Key<>(getIdentifier().getID()); }
    public Identifier getIdentifier() { return identifier; }
    public Class<T> getNodeClass() { return nodeClass; }
    public Settings getDefaultSettings() { return defaultSettings; }
}
