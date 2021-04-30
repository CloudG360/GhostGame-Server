package net.cg360.spookums.server.game.entity.behaviour.types;

import net.cg360.spookums.server.game.entity.Entity;
import net.cg360.spookums.server.game.entity.behaviour.EntityBehaviourTree;
import net.cg360.spookums.server.core.data.Settings;

public abstract class NodeBaseBehaviour {

    private EntityBehaviourTree tree;
    protected Settings settings;


    protected abstract void create(Settings settings);

    public abstract NodeState tick(Entity entity);


    public final EntityBehaviourTree getTree() { return tree; }
    public Settings getSettings() { return settings; }
}
