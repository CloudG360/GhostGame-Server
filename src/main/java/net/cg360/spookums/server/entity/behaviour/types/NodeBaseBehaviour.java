package net.cg360.spookums.server.entity.behaviour.types;

import net.cg360.spookums.server.entity.Entity;
import net.cg360.spookums.server.entity.behaviour.EntityBehaviourTree;
import net.cg360.spookums.server.util.data.Settings;

public abstract class NodeBaseBehaviour {

    private EntityBehaviourTree tree;
    protected Settings settings;


    protected abstract void create(Settings settings);

    public abstract NodeState tick(Entity entity);


    public final EntityBehaviourTree getTree() { return tree; }
    public Settings getSettings() { return settings; }
}
