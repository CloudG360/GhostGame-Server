package net.cg360.spookums.server.game.level;

import net.cg360.spookums.server.game.entity.Entity;
import net.cg360.spookums.server.game.manage.GameSession;

import java.util.HashMap;

public class Map {

    protected GameSession session;
    protected Floor[] floors;


    public Map(GameSession session) {
        this.session = session;
        this.floors = new Floor[16];
    }

    //TODO: Replace with proper map gen.
    public int fillEmptyFloors() {
        int filledFloors = 0;

        for(byte i = 0; i < floors.length; i++) {
            if(floors[i] == null) {
                floors[i] = new Floor(this, i);
                filledFloors++;
            }
        }

        return filledFloors;
    }


    public GameSession getSession() {
        return session;
    }

    public Floor[] getFloors() {
        Floor[] immutableCopy = new Floor[floors.length];
        System.arraycopy(this.floors, 0, immutableCopy, 0, this.floors.length);
        return immutableCopy;
    }
}
