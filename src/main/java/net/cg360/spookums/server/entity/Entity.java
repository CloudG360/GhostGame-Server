package net.cg360.spookums.server.entity;

import net.cg360.spookums.server.entity.behaviour.EntityBehaviourTree;
import net.cg360.spookums.server.math.Vector3;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class Entity {

    private static AtomicInteger entityCount; // Uniquely identifies entities as it ticks up. Increments on spawn.

    private EntityBehaviourTree behaviour;

    protected int runtimeID;

    protected Vector3 lastPosition;
    protected Vector3 position;
    protected Vector3 acceleration;
    protected Vector3 velocity;

    public Entity() {

    }

    public abstract int getTypeID();
}
