package net.cg360.spookums.server.game.level;

import net.cg360.spookums.server.game.entity.Entity;
import net.cg360.spookums.server.game.manage.GameSession;
import net.cg360.spookums.server.util.clean.Check;

import java.util.HashMap;

public class Floor {

    protected final Map map;
    protected final byte floorNumber;

    protected HashMap<Long, Entity> entityLookup;

    public Floor(Map map, byte floorNumber) {
        Check.inclusiveLowerBound(floorNumber, 0, "floorNumber");

        this.map = map;
        this.floorNumber = floorNumber;

        this.entityLookup = new HashMap<>();
    }

    public boolean addEntity(Entity entity) {
        if(entity.getFloor() == this && !this.entityLookup.containsKey(entity.getRuntimeID())) {
            this.entityLookup.put(entity.getRuntimeID(), entity);
            return true;
        }
        return false;
    }

    public boolean removeEntity(Entity entity) {
        if(this.entityLookup.containsKey(entity.getRuntimeID())) {
            if (entity.isDestroyed()) {
                this.entityLookup.remove(entity.getRuntimeID());

            } else entity.destroy();

            return true;
        }
        return false;
    }


    public byte getFloorNumber() {
        return this.floorNumber;
    }

    public Map getMap() {
        return this.map;
    }

    public GameSession getSession() {
        return this.getMap().getSession();
    }

    public Entity[] getEntities() {
        return this.entityLookup.values().toArray(new Entity[0]);
    }
}
