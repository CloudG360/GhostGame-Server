package net.cg360.spookums.server.network.user;

import java.util.UUID;

public class NetworkClient {

    protected UUID uuid;
    protected ConnectionState state;

    public NetworkClient(UUID uuid) {
        this.uuid = uuid;
        this.state = ConnectionState.OPEN;
    }

    public UUID getID() { return uuid; }
    public ConnectionState getState() {return state;}
}
