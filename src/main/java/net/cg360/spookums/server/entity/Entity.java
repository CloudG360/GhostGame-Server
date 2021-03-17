package net.cg360.spookums.server.entity;

import net.cg360.spookums.server.entity.behaviour.EntityBehaviourTree;
import net.cg360.spookums.server.math.Vector2;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class Entity {

    private static AtomicInteger entityCount; // Uniquely identifies entities as it ticks up. Increments on spawn.

    private EntityBehaviourTree behaviour;

    protected int runtimeID;

    protected Vector2 lastPosition;
    protected Vector2 position;
    protected Vector2 acceleration;
    protected Vector2 velocity;

    public Entity() {

    }

    public void tick() {

    }

    public abstract int getTypeID();
}
