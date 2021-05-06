package net.cg360.spookums.server.network.netimpl;

import net.cg360.spookums.server.network.packet.NetworkPacket;
import net.cg360.spookums.server.network.packet.generic.PacketInOutDisconnect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

/**
 *  The bare minimum interface to send data
 */
public interface NetworkInterface {

    void openServerBlocking(String hostname, int port);
    void closeServer();

    ArrayList<NetworkPacket> checkForInboundPackets(UUID clientNetID);

    void sendDataPacket(UUID clientNetID, NetworkPacket packet, boolean isUrgent);
    void broadcastDataPacket(NetworkPacket packet, boolean isUrgent); // Bulk method

    default void disconnectClient(UUID clientNetID) { disconnectClient(clientNetID, new PacketInOutDisconnect(null)); }
    void disconnectClient(UUID clientNetID, PacketInOutDisconnect disconnectPacket); // Closes the socked

    boolean isClientConnected(UUID clientNetId);
    boolean isRunning();
    UUID[] getClientNetIDs();

}
