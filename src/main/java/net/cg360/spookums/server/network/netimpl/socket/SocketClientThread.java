package net.cg360.spookums.server.network.netimpl.socket;

import net.cg360.spookums.server.network.netimpl.NetworkInterface;

import java.util.UUID;

public class SocketClientThread extends Thread {

    protected UUID clientUUID;
    protected NetworkInterface networkInterface;

    public SocketClientThread(UUID clientUUID, NetworkInterface netInf) {
        this.clientUUID = clientUUID;
        this.networkInterface = netInf;
    }

    @Override
    public void run() {

        while (networkInterface.isRunning()) {
            networkInterface.checkForInboundPackets(clientUUID);
        }

    }

    @Override
    public void interrupt() {

    }

}
