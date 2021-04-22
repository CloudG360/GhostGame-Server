package net.cg360.spookums.server.network.netimpl;

import net.cg360.spookums.server.network.packet.NetworkPacket;
import net.cg360.spookums.server.network.packet.generic.PacketDisconnect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

/**
 *  The bare minimum interface to send data
 */
public interface NetworkInterface {

    void openServerBlocking(String hostname, int port);
    void closeServer();

    ArrayList<NetworkPacket> checkForInboundPackets(UUID user);
    HashMap<UUID, ArrayList<NetworkPacket>> checkForInboundPackets(); // Bulk method

    void sendDataPacket(UUID clientNetID, NetworkPacket packet, boolean isUrgent);
    void broadcastDataPacket(NetworkPacket packet, boolean isUrgent); // Bulk method

    default void disconnectClient(UUID uuid) { disconnectClient(uuid, new PacketDisconnect(null)); }
    void disconnectClient(UUID uuid, PacketDisconnect disconnectPacket); // Closes the socked

    boolean isClientConnected(UUID clientNetId);
    boolean isRunning();
    UUID[] getClientNetIDs();

}
