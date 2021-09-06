package net.cg360.spookums.server;

import net.cg360.spookums.server.auth.AuthToken;
import net.cg360.spookums.server.auth.AuthenticationManager;
import net.cg360.spookums.server.core.data.json.JsonTypeRegistry;
import net.cg360.spookums.server.core.event.EventManager;
import net.cg360.spookums.server.core.event.handler.EventHandler;
import net.cg360.spookums.server.core.event.handler.Priority;
import net.cg360.spookums.server.core.event.type.network.ClientSocketStatusEvent;
import net.cg360.spookums.server.core.event.type.network.PacketEvent;
import net.cg360.spookums.server.core.scheduler.CommandingScheduler;
import net.cg360.spookums.server.core.data.Settings;
import net.cg360.spookums.server.db.DatabaseManager;
import net.cg360.spookums.server.network.PacketRegistry;
import net.cg360.spookums.server.network.VanillaProtocol;
import net.cg360.spookums.server.network.netimpl.NetworkInterface;
import net.cg360.spookums.server.network.netimpl.socket.NISocket;
import net.cg360.spookums.server.network.packet.generic.PacketInOutDisconnect;
import net.cg360.spookums.server.network.packet.info.PacketInProtocolCheck;
import net.cg360.spookums.server.network.packet.info.PacketOutProtocolError;
import net.cg360.spookums.server.network.packet.info.PacketOutProtocolSuccess;
import net.cg360.spookums.server.network.packet.info.PacketOutServerDetail;
import net.cg360.spookums.server.network.user.ConnectionState;
import net.cg360.spookums.server.network.user.NetworkClient;
import org.slf4j.Logger;
import org.slf4j.impl.SimpleLoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class Server {

    public static final int MSPT = 1000 / 20; // Millis per tick.

    //TODO: Move to config:
    protected static boolean logUnregisteredPacketsSent = true;
    protected static String serverName = "Test Server";
    protected static String serverDesc = "It's not yet finished! - me, sometime in 2021";
    protected static String serverRegion = "gb-en";


    protected static Server instance;

    protected File dataPath;
    protected Settings settings;

    protected boolean isRunning;

    protected Thread netServerThread;
    protected Thread netClientsThread;

    protected Logger logger;
    protected CommandingScheduler serverScheduler;
    protected EventManager serverEventManager;
    protected DatabaseManager databaseManager;
    protected AuthenticationManager authenticationManager;

    protected PacketRegistry packetRegistry;
    protected JsonTypeRegistry jsonTypeRegistry;


    // -- Network --

    protected NetworkInterface networkInterface;
    protected HashMap<UUID, NetworkClient> networkClients;


    public Server(String[] args) {
        this.dataPath = new File("./"); // Configurable maybe?
        this.settings = new Settings();

        this.isRunning = false;

        this.logger = new SimpleLoggerFactory().getLogger("Server");
        getLogger().info("Preparing server...");


        // -- Core Components --

        this.serverScheduler = new CommandingScheduler();
        this.serverEventManager = new EventManager();
        this.databaseManager = new DatabaseManager();
        this.authenticationManager = new AuthenticationManager();


        // -- Core Registries --

        this.packetRegistry = new PacketRegistry();
        this.jsonTypeRegistry = new JsonTypeRegistry();

    }



    public synchronized void start() {
        if(!this.isRunning) {

            try {
                this.isRunning = true;
                getLogger().info("Starting server...");

                // Attempt to claim the primary instances.
                boolean resultScheduler = this.serverScheduler.setAsPrimaryInstance();
                boolean resultEventManager = this.serverEventManager.setAsPrimaryInstance();
                boolean resultDatabaseManager = this.databaseManager.setAsPrimaryInstance();
                boolean resultAuthManager = this.authenticationManager.setAsPrimaryInstance();
                boolean resultPacketRegistry = this.packetRegistry.setAsPrimaryInstance();
                boolean resultJsonTypeRegistry = this.jsonTypeRegistry.setAsPrimaryInstance();

                if(resultScheduler && resultEventManager && resultDatabaseManager && resultAuthManager && resultPacketRegistry && resultJsonTypeRegistry){
                    getLogger().info("Claimed primary instances! This is the main server! :)");
                }


                // Tests && Setup
                getLogger().info("Running through pre-scheduler activities...");
                getLogger().info("These are ran on the main thread to expect a wait!\n");


                getLogger().info("[DB] Created/repaired authentication tables? " + getAuthManager().createTables());
                getLogger().info("[DB TEST] Created test token to expire now? " + getAuthManager().publishToken(
                        "CG360_",
                        new AuthToken(AuthToken.generateTokenString(), System.currentTimeMillis())
                ));

                getLogger().info("[DB] Deleted any expired tokens ahead of time? " + getAuthManager().deleteOutdatedTokens());


                getLogger().info("Completed pre-scheduler activities.\n");
                // Main server operation \/\/


                this.serverScheduler.startScheduler();
                getLogger().info("Started the scheduler! :)");


                getLogger().info("Starting network threads...");
                networkInterface = new NISocket();
                networkClients = new HashMap<>();
                this.netServerThread = new Thread() {

                    @Override
                    public void run() {
                        networkInterface.openServerBlocking("127.0.0.1", 22057);
                        netClientsThread.interrupt();
                        getLogger().info("Stopped down the network server thread.");
                    }

                    @Override
                    public void interrupt() {
                        netClientsThread.interrupt();
                        super.interrupt();
                    }
                };

                this.netServerThread.start();
                getLogger().info("Starting network server thread!");

                this.getServerEventManager().addListener(this);

                VanillaProtocol.applyToRegistry(this.packetRegistry);

                // Scheduler ticking is done here now.
                // TODO: Account for variation in ticks otherwise clients will become desynchronized with the server.
                //       While ticking is less important on the client, it could cause unexpected behaviour.
                while (this.isRunning) {
                    serverScheduler.serverTick();
                    this.wait(MSPT);
                }

            } catch (Exception err) {
                getLogger().info("Error whilst running server... :<");
                err.printStackTrace();
                this.isRunning = false;
            }

        } else {
            throw new IllegalStateException("This server is already running!");
        }
    }


    @EventHandler
    public void onClientConnect(ClientSocketStatusEvent.Open event) {
        getLogger().info("Connection | " + event.getClient().getID().toString());
        this.networkClients.put(event.getClient().getID(), event.getClient());
    }

    @EventHandler
    public void onClientDisconnect(ClientSocketStatusEvent.Disconnect event) {
        getLogger().info("Disconnected | " + event.getClient().getID().toString());
        this.networkClients.remove(event.getClient().getID(), event.getClient());
    }

    @EventHandler(ignoreIfCancelled = true, priority = Priority.HIGHEST)
    public void onPacketIn(PacketEvent.In<?> event) {
        getLogger().info(String.format("IN | %s << %s %s",
                event.getClientNetID().toString(),
                event.getPacket().toCoreString(),
                event.getPacket().toString())
        );

        UUID id = event.getClientNetID();
        NetworkClient client = this.networkClients.get(event.getClientNetID());
        if(client == null) return;

        switch (event.getPacket().getPacketID()) {

            case VanillaProtocol.PACKET_PROTOCOL_CHECK:
                if(client.getState() == ConnectionState.OPEN) {
                    if(!(event.getPacket() instanceof PacketInProtocolCheck)) return;
                    client.setState(ConnectionState.PROTOCOL);
                    this.serverEventManager.call(new ClientSocketStatusEvent.Protocol(client));

                    PacketInProtocolCheck protocolCheck = (PacketInProtocolCheck) event.getPacket();

                    if(protocolCheck.isValid()) {

                        if(VanillaProtocol.PROTOCOL_ID == protocolCheck.getProtocolVersion()) {
                            networkInterface.sendDataPacket(id, new PacketOutProtocolSuccess(), true);
                            client.setState(ConnectionState.CONNECTED);
                            this.serverEventManager.call(new ClientSocketStatusEvent.Connected(client));

                        } else {
                            String append = (VanillaProtocol.PROTOCOL_ID < protocolCheck.getProtocolVersion()) ? "Client is newer than the server." : "Client is older than the server.";
                            networkInterface.sendDataPacket(id, new PacketOutProtocolError(VanillaProtocol.PROTOCOL_ID, VanillaProtocol.SUPPORTED_VERSION_STRING), true);
                            networkInterface.disconnectClient(id, null);
                            getLogger().info(String.format("Client %s attempted to connect with an unsupported protocol. %s", id.toString(), append));
                        }

                    } else {
                        networkInterface.disconnectClient(id, new PacketInOutDisconnect("Invalid network version! How'd you manage that? :)"));
                        getLogger().warn(String.format("Client %s attempted to connect with an invalid protocol version check.", id.toString()));
                    }

                } else {
                    getLogger().warn(String.format("Client %s sent protocol check packet at an unexpected point. Ignoring.", id.toString()));
                }

                break;


            case VanillaProtocol.PACKET_REQUEST_GAME_DETAIL:
                networkInterface.sendDataPacket(id, new PacketOutServerDetail(serverName, serverRegion, serverDesc), true);
                break;



            case VanillaProtocol.PACKET_LOGIN:
                if(isClientCompatible(client)) {
                    //TODO: Add authenticatedProfiles
                }
                break;

            // When adding new packets, remember to include an isClientCompatible()
            // This ensures the protocol is compatible.

            case VanillaProtocol.PACKET_PROTOCOL_INVALID_PACKET:
            default:
                if(logUnregisteredPacketsSent) {
                    getLogger().warn(String.format("Client %s sent a packet with an invalid/unregistered ID.", id.toString()));
                }
                break;



        }
    }

    @EventHandler(ignoreIfCancelled = true, priority = Priority.HIGHEST)
    public void onPacketOut(PacketEvent.Out<?> event) {
        getLogger().info(String.format("OUT | %s >> %s %s",
                event.getClientNetID().toString(),
                event.getPacket().toCoreString(),
                event.getPacket().toString())
        );
    }



    public Settings getSettings() { return settings; }
    public File getDataPath() { return dataPath; }

    public boolean isRunning() { return isRunning; }

    public Logger getLogger() { return logger; }
    public CommandingScheduler getServerScheduler() { return serverScheduler; }
    public EventManager getServerEventManager() { return serverEventManager; }
    public DatabaseManager getDBManager() { return databaseManager; }
    public AuthenticationManager getAuthManager() {return authenticationManager;}

    public static Server get() { return instance; }
    public static Logger getMainLogger() { return get().getLogger(); }

    protected static boolean isClientCompatible(NetworkClient client) {
        return (client.getState() == ConnectionState.CONNECTED) || (client.getState() == ConnectionState.LOGGED_IN);
    }


    /**
     * Launches the server from the jar.
     * @param args arguments entered when running the jar.
     */
    public static void main(String[] args) throws IOException {
        List<String> argsList = Arrays.asList(args);

        // Checks:
        // - Is no terminal window present?
        // - Does the user environment support a graphical terminal?
        // - Headless flag *is not* present.
        if((System.console() == null) && (!GraphicsEnvironment.isHeadless()) && (!argsList.contains("-headless"))) {
            String filename = Server.class.getProtectionDomain().getCodeSource().getLocation().toString().substring(6);
            Runtime.getRuntime().exec(new String[]{"cmd","/c","start","cmd","/k","java -jar \"" + filename + "\""}); // Run the jar but in a cmd window.

        } else {
            instance = new Server(args);
            instance.start();
            System.out.println("!!!  Stopped Server :^)  !!!"); // No logger prepared, use java's own methods (ew)
            System.exit(0);
        }
    }

}
