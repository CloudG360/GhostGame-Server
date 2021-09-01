package net.cg360.spookums.server.network.netimpl.socket;

import net.cg360.spookums.server.Server;
import net.cg360.spookums.server.core.event.EventManager;
import net.cg360.spookums.server.core.event.type.network.ClientSocketStatusEvent;
import net.cg360.spookums.server.core.event.type.network.PacketEvent;
import net.cg360.spookums.server.network.PacketRegistry;
import net.cg360.spookums.server.network.VanillaProtocol;
import net.cg360.spookums.server.network.netimpl.NetworkInterface;
import net.cg360.spookums.server.network.packet.NetworkPacket;
import net.cg360.spookums.server.network.packet.generic.PacketInOutDisconnect;
import net.cg360.spookums.server.network.user.NetworkClient;
import net.cg360.spookums.server.util.NetworkBuffer;

import java.io.*;
import java.net.*;
import java.util.*;

public class NISocket implements NetworkInterface {

    protected ServerSocket netSocket;
    protected HashMap<UUID, Socket> clientSockets;
    protected HashMap<UUID, SocketListenerThread> clientThreads;

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
                        SocketListenerThread socketListenerThread = new SocketListenerThread(clientUUID, this);
                        NetworkClient client = new NetworkClient(clientUUID);

                        this.clientSockets.put(clientUUID, clientSocket);
                        this.clientThreads.put(clientUUID, socketListenerThread);

                        socketListenerThread.start();
                        EventManager.get().call(new ClientSocketStatusEvent.Open(client));

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
                synchronized (client.getInputStream()) {
                    DataInputStream in = new DataInputStream(client.getInputStream());
                    byte[] sizeBytes = new byte[2];

                    if(in.available() >= 2) {
                        int sizeByteCount = in.read(sizeBytes);
                        NetworkBuffer sizeBuf = NetworkBuffer.wrap(sizeBytes);
                        int packetSize = sizeByteCount == 2 ? sizeBuf.getUnsignedShort() : -1;

                        if (packetSize > 0) {

                            byte[] bodyBytes = new byte[packetSize];
                            in.readFully(bodyBytes);
                            NetworkBuffer bodyBuffer = NetworkBuffer.wrap(bodyBytes);

                            byte packetID = bodyBuffer.get();

                            Optional<Class<? extends NetworkPacket>> pk = PacketRegistry.get().getPacketTypeForID(packetID);

                            if (pk.isPresent()) {
                                NetworkBuffer buffer = NetworkBuffer.allocate(2 + bodyBuffer.capacity());

                                buffer.putUnsignedShort(packetSize);
                                buffer.put(bodyBytes);

                                Class<? extends NetworkPacket> clz = pk.get();
                                NetworkPacket packet = clz.newInstance().decode(buffer);

                                PacketEvent.In<?> packetEvent = new PacketEvent.In<>(clientNetID, packet);
                                Server.get().getServerEventManager().call(packetEvent);

                                collectedPackets.add(packet);

                            } else {
                                Server.getMainLogger().warn(String.format("Invalid packet received (Unrecognized type id: %s)", packetID));
                            }
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
            NetworkBuffer content = packet.encode();
            content.reset();

            PacketEvent.Out<?> packetEvent = new PacketEvent.Out<>(clientNetID, packet);
            Server.get().getServerEventManager().call(packetEvent);

            if(!packetEvent.isCancelled()) {
                byte[] sizeBytes = new byte[2];
                byte[] contents = new byte[content.capacity()-2];
                content.get(sizeBytes); // Split it into two, send the size first.
                content.get(contents);

                try {
                    DataOutputStream outputStream = new DataOutputStream(client.getOutputStream());
                    outputStream.write(sizeBytes);
                    outputStream.write(contents);


                } catch (SocketException err) {
                    err.printStackTrace();

                } catch (IOException ioErr) {
                    throw new RuntimeException("An IOException was raised whilst sending a packet: " + ioErr.getMessage());
                }
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
