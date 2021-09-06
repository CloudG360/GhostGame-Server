package net.cg360.spookums.server.network.user;

import net.cg360.spookums.server.Server;
import net.cg360.spookums.server.network.packet.NetworkPacket;

import java.util.UUID;

public class NetworkClient {

    protected UUID uuid;
    protected ConnectionState state;

    public NetworkClient(UUID uuid) {
        this.uuid = uuid;
        this.state = ConnectionState.OPEN;
    }

    public void send(NetworkPacket packet, boolean isUrgent) {
        if(state != ConnectionState.DISCONNECTED) {
            Server.get().getNetworkInterface().sendDataPacket(uuid, packet, isUrgent);
        }
    }




    public NetworkClient setState(ConnectionState state) {
        this.state = state;
        return this;
    }

    public UUID getID() { return uuid; }
    public ConnectionState getState() {return state;}
}
