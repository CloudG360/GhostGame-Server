package net.cg360.spookums.server.entity.behaviour.types;

import com.google.gson.JsonObject;
import net.cg360.spookums.server.entity.Entity;
import net.cg360.spookums.server.entity.behaviour.EntityBehaviourTree;

public abstract class NodeBaseBehaviour {

    private EntityBehaviourTree tree;

    public abstract NodeState tick(Entity entity);

    protected final boolean loadObjectFromJson(JsonObject object){
        return loadParametersFromJson(object);
    }

    protected abstract boolean loadParametersFromJson(JsonObject object);

    public final EntityBehaviourTree getTree() { return tree; }
}
