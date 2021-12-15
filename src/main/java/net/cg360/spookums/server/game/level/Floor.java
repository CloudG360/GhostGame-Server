package net.cg360.spookums.server.game.level;

import net.cg360.spookums.server.game.entity.Entity;
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


    public byte getFloorNumber() {
        return this.floorNumber;
    }

    public Map getMap() {
        return this.map;
    }

    public Entity[] getEntities() {
        return this.entityLookup.values().toArray(new Entity[0]);
    }
}
