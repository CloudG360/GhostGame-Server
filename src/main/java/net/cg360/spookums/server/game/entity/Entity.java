package net.cg360.spookums.server.game.entity;

import net.cg360.spookums.server.game.entity.behaviour.EntityBehaviourTree;
import net.cg360.spookums.server.game.level.Floor;
import net.cg360.spookums.server.game.level.Map;
import net.cg360.spookums.server.util.clean.Check;
import net.cg360.spookums.server.util.math.Vector2;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public abstract class Entity {

    // 0 is reserved for the client's entity, always. Entities add 1 and THEN get.
    private static final AtomicLong entityCount = new AtomicLong(0); // Uniquely identifies entities as it ticks up. Increments on spawn.

    // Still considering. Might hardcode behaviours and implement behaviour trees later.
    //private EntityBehaviourTree behaviour;

    // The unique ID of the entity in a game session.
    protected long runtimeID;

    protected Floor floor;
    protected Vector2 lastPosition;
    protected Vector2 position;

    //protected Vector2 acceleration; Really only used for projectiles?
    protected Vector2 velocity;

    public Entity(Floor floor, Vector2 position) {
        this.runtimeID = entityCount.addAndGet(1);

        this.floor = Check.nullParam(floor, "floor");
        this.lastPosition = new Vector2(position);
        this.position = new Vector2(position);

        this.velocity = Vector2.zero();
    }

    public void tick() {
        // Physics
        this.lastPosition = position;
        this.position.add(this.velocity);
    }

    // The entity type
    public abstract String getTypeID();



    public Map getMap() {
        return floor.getMap();
    }

    public Floor getFloor() {
        return floor;
    }

}
