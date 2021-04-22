package net.cg360.spookums.server.network.netimpl;

import net.cg360.spookums.server.Server;
import net.cg360.spookums.server.network.VanillaProtocol;
import net.cg360.spookums.server.network.packet.NetworkPacket;
import net.cg360.spookums.server.network.packet.generic.PacketDisconnect;

import java.io.IOException;
import java.io.OutputStream;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class NISocket implements NetworkInterface {

    protected ServerSocket netSocket;
    protected HashMap<UUID, Socket> clientSockets;

    protected boolean isRunning = false;

    @Override
    public void openServerBlocking(String hostname, int port) {

        if(!isRunning) {

            try {
                InetAddress address = Inet4Address.getByName(hostname);

                try (ServerSocket socket = new ServerSocket(port, 50, address)) {
                    this.isRunning = true;
                    this.netSocket = socket;
                    this.clientSockets = new HashMap<>();

                    while (isRunning) {
                        Socket clientSocket = this.netSocket.accept();
                        clientSocket.setKeepAlive(true);
                        clientSocket.setSoTimeout(VanillaProtocol.TIMEOUT);
                        clientSocket.setReceiveBufferSize(NetworkPacket.MAX_BUFFER_SIZE);
                        clientSocket.setSendBufferSize(NetworkPacket.MAX_BUFFER_SIZE);
                        this.clientSockets.put(UUID.randomUUID(), clientSocket);
                    }

                } catch (Exception socketError) {
                    socketError.printStackTrace();
                }

            } catch (Exception addressErr) {
                addressErr.printStackTrace();
            }

            this.isRunning = false;
        }
    }

    @Override
    public synchronized void closeServer() {
        if((!netSocket.isClosed()) && netSocket.isBound())  {
            PacketDisconnect pkDisconnect = new PacketDisconnect("The server you were connected to has closed.");

            for(UUID uuid: getClientNetIDs()) {
                disconnectClient(uuid, pkDisconnect);
            }

            // How do I even counter that? Idk
            try { netSocket.close(); }
            catch (Exception err) { err.printStackTrace(); }
        }
    }

    @Override
    public synchronized ArrayList<NetworkPacket> checkForInboundPackets(UUID user) {
        return null;
    }

    @Override
    public synchronized HashMap<UUID, ArrayList<NetworkPacket>> checkForInboundPackets() {
        return null;
    }

    @Override
    public synchronized void sendDataPacket(UUID clientNetID, NetworkPacket packet, boolean isUrgent) {

        if(isClientConnected(clientNetID)) {
            Socket client = clientSockets.get(clientNetID);
            ByteBuffer content = packet.encode();
            content.clear();

            byte[] contents = new byte[content.capacity()];
            content.get(contents);

            try {
                OutputStream outputStream = client.getOutputStream();
                outputStream.write(contents);

            } catch (SocketException err) {
                err.printStackTrace();

            } catch (IOException ioErr) {
                throw new RuntimeException("An IOException was raised whilst sending a packet: "+ioErr.getMessage());
            }
        }
    }

    @Override
    public synchronized void broadcastDataPacket(NetworkPacket packet, boolean isUrgent) {
        for(UUID uuid: getClientNetIDs()) sendDataPacket(uuid, packet, isUrgent);
    }

    @Override
    public synchronized void disconnectClient(UUID uuid, PacketDisconnect disconnectPacket) {

        if(clientSockets.containsKey(uuid)) {
            Socket conn = clientSockets.get(uuid);

            if(conn.isConnected() && (!conn.isClosed())) {

                try { sendDataPacket(uuid, disconnectPacket, true); }
                catch (RuntimeException err) { Server.getMainLogger().warn("Client disconnected with a IOException"); }

                try { conn.close(); }
                catch (Exception err) { err.printStackTrace(); }
            }

            //Server.get().getServerEventManager().call(); Call an event to indicate a client has been disconnected.

            clientSockets.remove(uuid);
        }
    }

    @Override
    public synchronized boolean isClientConnected(UUID clientNetId) {
        if(clientSockets.containsKey(clientNetId)) {
            Socket socket = clientSockets.get(clientNetId);

            if(!socket.isClosed()) {
                return true;
            } else {
                // Clean-up dead connection
                // I might change this but probably not.
                disconnectClient(clientNetId);
            }
        }
        return false;
    }

    @Override
    public synchronized boolean isRunning() {
        return isRunning;
    }

    @Override
    public synchronized UUID[] getClientNetIDs() {
        return clientSockets.keySet().toArray(new UUID[0]);
    }
}
