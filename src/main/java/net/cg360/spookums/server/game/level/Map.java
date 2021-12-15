package net.cg360.spookums.server.game.level;

import net.cg360.spookums.server.game.entity.Entity;

import java.util.HashMap;

public class Map {

    protected Floor[] floors;


    public Map() {
        this.floors = new Floor[16];
    }


    public Floor[] getFloors() {
        Floor[] immutableCopy = new Floor[floors.length];
        System.arraycopy(this.floors, 0, immutableCopy, 0, this.floors.length);
        return immutableCopy;
    }
}
