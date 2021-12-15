package net.cg360.spookums.server.game.entity;

import net.cg360.spookums.server.core.data.id.Identifier;
import net.cg360.spookums.server.core.data.json.Json;
import net.cg360.spookums.server.core.data.json.JsonObject;
import net.cg360.spookums.server.game.entity.behaviour.EntityBehaviourTree;
import net.cg360.spookums.server.game.level.Floor;
import net.cg360.spookums.server.game.level.Map;
import net.cg360.spookums.server.network.packet.game.entity.PacketInOutEntityMove;
import net.cg360.spookums.server.network.packet.game.entity.PacketOutAddEntity;
import net.cg360.spookums.server.network.packet.game.entity.PacketOutRemoveEntity;
import net.cg360.spookums.server.util.clean.Check;
import net.cg360.spookums.server.util.math.Vector2;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public abstract class Entity {

    // 0 is reserved for the client's entity, always. Entities add 1 and THEN get.
    private static final AtomicLong entityCount = new AtomicLong(0); // Uniquely identifies entities as it ticks up. Increments on spawn.

    // Still considering. Might hardcode behaviours and implement behaviour trees later.
    //private EntityBehaviourTree behaviour;

    // The unique ID of the entity in a game session.
    protected long runtimeID;

    protected ArrayList<Player> visibleTo;

    protected Floor floor;
    protected Vector2 lastPosition;
    protected Vector2 position;

    //protected Vector2 acceleration; Really only used for projectiles?
    protected Vector2 velocity;


    public Entity(Floor floor, Vector2 position) {
        this.runtimeID = entityCount.addAndGet(1);

        this.visibleTo = new ArrayList<>();

        this.floor = Check.nullParam(floor, "floor");
        this.lastPosition = new Vector2(position);
        this.position = new Vector2(position);

        this.velocity = Vector2.ZERO;
    }



    // The entity type
    public abstract Identifier getTypeID();

    //TODO: Set type to JsonObject, I don't have the time to write a universal serializer rn.
    public abstract String serializePropertiesToJson();


    public void tick(int tickDelta) {
        // Physics
        this.lastPosition = position;
        this.position = this.position.add(this.velocity.mul(tickDelta, tickDelta));

        if(!this.lastPosition.equals(this.position)) {
            Vector2 delta = this.position.sub(this.lastPosition);
            PacketInOutEntityMove move = new PacketInOutEntityMove()
                    .setMovement(delta)
                    .setType(PacketInOutEntityMove.Type.DELTA);

            for(Player player: visibleTo) {
                player.getAuthClient().getClient().send(move, true);
            }
        }
    }


    public boolean showEntityTo(Player player) {
        if(!player.visibleEntities.containsKey(this.getRuntimeID())) {

            PacketOutAddEntity packetOutAddEntity = new PacketOutAddEntity()
                    .setEntityRuntimeID(this.getRuntimeID())
                    .setEntityTypeId(this.getTypeID())
                    .setPosition(this.getPosition())
                    .setPropertiesJSON(this.serializePropertiesToJson())
                    .setFloorNumber(this.getFloor().getFloorNumber());
            player.getAuthClient().getClient().send(packetOutAddEntity, true);

            player.visibleEntities.put(this.getRuntimeID(), this);
            this.visibleTo.add(player);
            return true;
        }

        return false;
    }

    public boolean hideEntityFrom(Player player) {
        if(player.visibleEntities.containsKey(this.getRuntimeID())) {

            PacketOutRemoveEntity packetOutRemoveEntity = new PacketOutRemoveEntity()
                    .setEntityRuntimeID(this.getRuntimeID());
            player.getAuthClient().getClient().send(packetOutRemoveEntity, true);

            player.visibleEntities.remove(this.getRuntimeID());
            this.visibleTo.remove(player);
            return true;
        }

        return false;
    }


    public long getRuntimeID() {
        return this.runtimeID;
    }

    public Vector2 getLastPosition() {
        return this.lastPosition;
    }

    public Vector2 getPosition() {
        return this.position;
    }

    public Vector2 getVelocity() {
        return this.velocity;
    }

    public Map getMap() {
        return this.floor.getMap();
    }

    public Floor getFloor() {
        return this.floor;
    }

}
