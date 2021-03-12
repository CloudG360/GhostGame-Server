package net.cg360.spookums.server.network.netimpl;

import net.cg360.spookums.server.network.packet.NetworkPacket;

public interface NetworkInterface {

    void start(String hostname, int port);

    void sendDataPacket(NetworkPacket packet, boolean isUrgent); //TODO: Add destination parameter
    void broadcastDataPacket(NetworkPacket packet, boolean isUrgent);

    //default void disconnectClient(){ disconnectClient(); } //TODO: Add disconnect packet default
    void disconnectClient(NetworkPacket packet); //Disconnects on serverside. Recommended to use this rather than sending a disconnect packet alone.

    boolean isClientConnected(String clientNetworkIdentifier);

    String[] getNetworkIdentifiers(); //TODO: Change to array. Should list all client ids.
    void getNetworkIdentities(); //TODO: Change to array. Should list all clients.

}
