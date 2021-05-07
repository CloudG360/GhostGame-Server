package net.cg360.spookums.server.network.netimpl.socket;

import net.cg360.spookums.server.Server;
import net.cg360.spookums.server.core.event.type.network.PacketEvent;
import net.cg360.spookums.server.network.PacketRegistry;
import net.cg360.spookums.server.network.VanillaProtocol;
import net.cg360.spookums.server.network.netimpl.NetworkInterface;
import net.cg360.spookums.server.network.packet.NetworkPacket;
import net.cg360.spookums.server.network.packet.generic.PacketInOutDisconnect;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

public class NISocket implements NetworkInterface {

    protected ServerSocket netSocket;
    protected HashMap<UUID, Socket> clientSockets;
    protected HashMap<UUID, SocketClientThread> clientThreads;

    protected boolean isRunning = false;

    public NISocket() {
        this.netSocket = null;
        this.clientSockets = new HashMap<>();
        this.clientThreads = new HashMap<>();
    }

    @Override
    public void openServerBlocking(String hostname, int port) {
        if(!isRunning) {

            try {
                InetAddress address = Inet4Address.getByName(hostname);

                try {
                    this.isRunning = true;
                    this.netSocket = new ServerSocket(port, 50, address);
                    this.clientSockets = new HashMap<>();

                    while (isRunning) {
                        Socket clientSocket = this.netSocket.accept();
                        clientSocket.setKeepAlive(true);
                        clientSocket.setSoTimeout(VanillaProtocol.TIMEOUT);
                        clientSocket.setReceiveBufferSize(VanillaProtocol.MAX_BUFFER_SIZE);
                        clientSocket.setSendBufferSize(VanillaProtocol.MAX_BUFFER_SIZE);

                        UUID clientUUID = UUID.randomUUID();
                        SocketClientThread socketClientThread = new SocketClientThread(clientUUID, this);

                        this.clientSockets.put(clientUUID, clientSocket);
                        this.clientThreads.put(clientUUID, socketClientThread);

                        socketClientThread.start();
                    }

                    this.closeServer();

                } catch (Exception socketError) {
                    if(this.netSocket != null) this.closeServer();
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
        if(!isRunning) return;
        if((!netSocket.isClosed()) && netSocket.isBound())  {
            PacketInOutDisconnect pkDisconnect = new PacketInOutDisconnect("The server you were connected to has closed.");

            for(UUID uuid: getClientNetIDs()) {
                disconnectClient(uuid, pkDisconnect);
            }

            // How do I even counter that? Idk
            try { netSocket.close(); }
            catch (Exception err) { err.printStackTrace(); }
            this.isRunning = false;
        }
    }

    @Override
    public ArrayList<NetworkPacket> checkForInboundPackets(UUID clientNetID) {
        if(!isRunning()) return new ArrayList<>();
        ArrayList<NetworkPacket> collectedPackets = new ArrayList<>();

        if(isClientConnected(clientNetID)) {

            Socket client;
            synchronized (clientSockets) { client = clientSockets.get(clientNetID); }

            try {
                DataInputStream in = new DataInputStream(client.getInputStream());
                byte[] headerBytes = new byte[3];
                int headerSize = in.read(headerBytes);
                ByteBuffer headerBuffer = ByteBuffer.wrap(headerBytes);

                if (headerSize == 3) { // There's not a complete header present
                    byte typeID = headerBuffer.get();
                    short size = headerBuffer.getShort();

                    byte[] bodyBytes = new byte[size];
                    int actualBodySize = in.read(bodyBytes);
                    ByteBuffer bodyBuffer = ByteBuffer.wrap(bodyBytes);

                    if (actualBodySize == size) {

                        Optional<Class<? extends NetworkPacket>> pk = PacketRegistry.get().getPacketTypeForID(typeID);

                        if (pk.isPresent()) {
                            ByteBuffer buffer = ByteBuffer.allocate(headerBuffer.capacity() + bodyBuffer.capacity());

                            buffer.put(headerBytes);
                            buffer.put(bodyBytes);

                            Class<? extends NetworkPacket> clz = pk.get();
                            NetworkPacket packet = clz.newInstance().decode(buffer);

                            PacketEvent.In<?> packetEvent = new PacketEvent.In<>(clientNetID, packet);
                            Server.get().getServerEventManager().call(packetEvent);

                            collectedPackets.add(packet);

                        } else {
                            Server.getMainLogger().warn(String.format("Invalid packet received (Unrecognized type id: %s)", typeID));
                        }
                    }
                }

            } catch (IOException socketErr) {
                disconnectClient(clientNetID, new PacketInOutDisconnect("An error occurred | "+socketErr.getMessage()));

            } catch (InstantiationException | IllegalAccessException err) {
                err.printStackTrace();
                Server.getMainLogger().error("A packet type is broken in this case! Submit a bug report. :)");
            }

        }

        return collectedPackets;
    }

    @Override
    public synchronized void sendDataPacket(UUID clientNetID, NetworkPacket packet, boolean isUrgent) {
        if(!isRunning) return;
        if(isClientConnected(clientNetID)) {
            Socket client = clientSockets.get(clientNetID);
            ByteBuffer content = packet.encode();
            content.clear();

            byte[] contents = new byte[content.capacity()];
            content.get(contents);

            try {
                DataOutputStream outputStream = new DataOutputStream(client.getOutputStream());
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
        if(!isRunning) return;
        for(UUID uuid: getClientNetIDs()) sendDataPacket(uuid, packet, isUrgent);
    }

    @Override
    public synchronized void disconnectClient(UUID clientNetID, PacketInOutDisconnect disconnectPacket) {
        if(!isRunning) return;
        if(clientSockets.containsKey(clientNetID)) {
            Socket conn = clientSockets.get(clientNetID);

            if(conn.isConnected() && (!conn.isClosed())) {

                try { sendDataPacket(clientNetID, disconnectPacket, true); }
                catch (RuntimeException err) { Server.getMainLogger().warn("Client disconnected with a IOException"); }

                try { conn.close(); }
                catch (Exception err) { err.printStackTrace(); }
            }

            //Server.get().getServerEventManager().call(); Call an event to indicate a client has been disconnected.

            clientSockets.remove(clientNetID);
        }
    }

    @Override
    public synchronized boolean isClientConnected(UUID clientNetId) {
        if(!isRunning) return false;
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
        if(!isRunning) return new UUID[0];
        return clientSockets.keySet().toArray(new UUID[0]);
    }
}
