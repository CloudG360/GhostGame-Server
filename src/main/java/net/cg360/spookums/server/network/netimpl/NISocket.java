package net.cg360.spookums.server.network.netimpl;

import net.cg360.spookums.server.network.packet.NetworkPacket;

public class NISocket implements NetworkInterface {


    @Override
    public void start(String hostname, int port) {

    }

    @Override
    public void sendDataPacket(NetworkPacket packet, boolean isUrgent) {

    }

    @Override
    public void broadcastDataPacket(NetworkPacket packet, boolean isUrgent) {

    }

    @Override
    public void disconnectClient(NetworkPacket packet) {

    }

    @Override
    public boolean isClientConnected(String clientNetworkIdentifier) {
        return false;
    }

    @Override
    public String[] getNetworkIdentifiers() {
        return new String[0];
    }

    @Override
    public void getNetworkIdentities() {

    }
}
