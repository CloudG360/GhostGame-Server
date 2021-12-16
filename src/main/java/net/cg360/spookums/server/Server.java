package net.cg360.spookums.server;

import net.cg360.spookums.server.auth.record.AuthToken;
import net.cg360.spookums.server.auth.AuthenticationManager;
import net.cg360.spookums.server.core.data.LockableSettings;
import net.cg360.spookums.server.core.data.Queue;
import net.cg360.spookums.server.core.data.json.JsonArray;
import net.cg360.spookums.server.core.data.json.JsonObject;
import net.cg360.spookums.server.core.data.json.io.JsonIO;
import net.cg360.spookums.server.core.data.json.io.error.ConfigFormatException;
import net.cg360.spookums.server.core.event.EventManager;
import net.cg360.spookums.server.core.event.handler.EventHandler;
import net.cg360.spookums.server.core.event.handler.Priority;
import net.cg360.spookums.server.core.event.type.flow.ServerStartedEvent;
import net.cg360.spookums.server.core.event.type.network.ClientSocketStatusEvent;
import net.cg360.spookums.server.core.event.type.network.PacketEvent;
import net.cg360.spookums.server.core.scheduler.CommandingScheduler;
import net.cg360.spookums.server.core.scheduler.Scheduler;
import net.cg360.spookums.server.db.DatabaseManager;
import net.cg360.spookums.server.game.manage.GameManager;
import net.cg360.spookums.server.network.PacketRegistry;
import net.cg360.spookums.server.network.VanillaProtocol;
import net.cg360.spookums.server.network.netimpl.NetworkInterface;
import net.cg360.spookums.server.network.netimpl.socket.NISocket;
import net.cg360.spookums.server.network.packet.auth.PacketInLogin;
import net.cg360.spookums.server.network.packet.auth.PacketInUpdateAccount;
import net.cg360.spookums.server.network.packet.auth.PacketOutLoginResponse;
import net.cg360.spookums.server.network.packet.generic.PacketInOutDisconnect;
import net.cg360.spookums.server.network.packet.info.PacketInProtocolCheck;
import net.cg360.spookums.server.network.packet.info.PacketOutProtocolError;
import net.cg360.spookums.server.network.packet.info.PacketOutProtocolSuccess;
import net.cg360.spookums.server.network.packet.info.PacketOutServerDetail;
import net.cg360.spookums.server.network.user.ConnectionState;
import net.cg360.spookums.server.network.user.NetworkClient;
import net.cg360.spookums.server.util.type.MicroBoolean;
import net.cg360.spookums.server.util.Patterns;
import net.cg360.spookums.server.util.clean.Check;
import org.slf4j.Logger;
import org.slf4j.impl.SimpleLoggerFactory;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

/**
 * The main class of the server, collecting all the managers together
 * and starting the server.
 */
public class Server {

    public static final int MSPT = 1000 / 20; // Millis per tick.

    protected static Server instance;
    protected static SimpleLoggerFactory loggerFactory = new SimpleLoggerFactory();

    public static final String BASE_LOG = "Server";
    public static final String BOOT_LOG = "Server/Boot";
    public static final String NET_LOG = "Server/Network";
    public static final String AUTH_LOG = "Server/Auth";
    public static final String DB_LOG = "Server/Database";
    public static final String S7R_LOG = "Server/Scheduler";
    public static final String EVNT_LOG = "Server/Events";
    public static final String B7R_LOG = "Game/Behaviour";
    public static final String TEST_LOG = "Test"; // Server.TEST_LOG + "/Thingy"


    protected File dataPath;
    protected LockableSettings settings;

    protected boolean isRunning;

    protected Thread netServerThread;
    protected Thread netClientsThread;

    protected Logger logger;

    protected CommandingScheduler serverScheduler;
    protected Scheduler defaultScheduler;

    protected EventManager serverEventManager;
    protected DatabaseManager databaseManager;
    protected AuthenticationManager authenticationManager;
    protected GameManager gameManager;

    protected PacketRegistry packetRegistry;


    // -- Network --

    protected NetworkInterface networkInterface;


    public Server() {
        this.dataPath = new File("./"); // Configurable maybe?
        this.isRunning = false;

        this.logger = new SimpleLoggerFactory().getLogger("Server");
    }



    public synchronized void start() {
        if(!this.isRunning) {

            try {
                this.isRunning = true;

                Logger dbLog = Server.getLogger(Server.DB_LOG);
                Logger btLog = Server.getLogger(Server.BOOT_LOG);
                Logger netLog = Server.getLogger(Server.NET_LOG);

                btLog.info("Starting server...");

                this.settings = ServerConfig.loadServerConfiguration(this, true);


                // -- Core Components --
                this.serverScheduler = new CommandingScheduler();
                this.defaultScheduler = new Scheduler(1);
                this.serverEventManager = new EventManager();
                this.databaseManager = new DatabaseManager();
                this.authenticationManager = new AuthenticationManager();
                this.gameManager = new GameManager();

                // -- Core Registries --
                this.packetRegistry = new PacketRegistry();


                // Attempt to claim the primary instances.
                boolean resultScheduler = this.serverScheduler.setAsPrimaryInstance();
                boolean resultEventManager = this.serverEventManager.setAsPrimaryInstance();
                boolean resultDatabaseManager = this.databaseManager.setAsPrimaryInstance();
                boolean resultAuthManager = this.authenticationManager.setAsPrimaryInstance();
                boolean resultGameManager = this.gameManager.setAsPrimaryInstance();
                boolean resultPacketRegistry = this.packetRegistry.setAsPrimaryInstance();


                if(resultScheduler && resultEventManager && resultDatabaseManager && resultAuthManager && resultGameManager && resultPacketRegistry){
                    btLog.info("Claimed primary instances! This is the main server! :)");
                }

                this.getEventManager().addListener(this);
                this.serverEventManager.addListener(this.authenticationManager);
                this.serverEventManager.addListener(this.gameManager);


                // Tests && Setup
                btLog.info("Running through pre-scheduler activities...");
                btLog.info("These are ran on the main thread to expect a wait!\n");

                dbLog.info("Created/repaired database authentication tables? " + getAuthManager().createTables());
                dbLog.info("Deleted any expired tokens ahead of time? " + getAuthManager().deleteAllOutdatedTokens());


                if(this.getSettings().getOrDefault(ServerConfig.RUN_LAUNCH_TESTS)) runLaunchTests();


                btLog.info("Completed pre-scheduler activities.\n");
                // Main server operation \/\/


                this.serverScheduler.startScheduler();
                this.defaultScheduler.startScheduler();
                btLog.info("Started the schedulers! ");


                btLog.info("Starting network threads...");
                this.networkInterface = new NISocket();

                String serverIP = this.getSettings().getOrDefault(ServerConfig.SERVER_IP);
                int port = this.getSettings().getOrDefault(ServerConfig.SERVER_PORT);

                if(!serverIP.matches(Patterns.IPV4_ADDRESS))
                    throw new ConfigFormatException("The property 'server-ip' must be an IPv4 address!");
                // Ports up to 1024 are reserved mostly so let's start from 1024.
                Check.inclusiveLowerBound(port, 1024, "config.port");


                this.netServerThread = new Thread() {

                    @Override
                    public void run() {
                        networkInterface.openServerBlocking(serverIP, port);
                        netClientsThread.interrupt();
                        netLog.info("Stopped down the network server thread.");
                    }

                    @Override
                    public void interrupt() {
                        netClientsThread.interrupt();
                        super.interrupt();
                    }
                };

                this.netServerThread.start();
                btLog.info("Starting network server thread!");

                VanillaProtocol.applyToRegistry(this.packetRegistry);

                this.getEventManager().call(new ServerStartedEvent());

                // Scheduler ticking is done here now.
                // TODO: Account for variation in ticks otherwise clients will become desynchronized with the server.
                //       While ticking is less important on the client, it could cause unexpected behaviour.
                while (this.isRunning) {
                    serverScheduler.serverTick();
                    this.wait(MSPT);
                }

            } catch (Exception err) {
                logger.info("Error whilst running server... :<");
                err.printStackTrace();
                this.isRunning = false;
            }

        } else {
            throw new IllegalStateException("This server is already running!");
        }
    }


    protected void runLaunchTests() {
        test_databaseControl();
        test_jsonParsing();
        test_queueFillAndEmpty();
    }







    public void test_databaseControl() {
        Logger log = Server.getLogger(Server.TEST_LOG + "/Database");
        log.info("Created test token to expire now? " + getAuthManager().publishTokenWithUsername(
                "CG360_",
                new AuthToken(AuthToken.generateTokenString(), System.currentTimeMillis())
        ));
    }


    public void test_jsonParsing() {
        Logger log = Server.getLogger(Server.TEST_LOG + "/JsonValueParser");

        JsonIO json = new JsonIO();
        String jsonArrayTest = "{ 'this': [ 'should', 1, 2.0, -3.50000, -345, true, TRUE, trUE, FALSE ], 'work':'fine' }";
        String jsonObjectTest = "{ 'this': { 'bool': trUE, 'number': 34.6 }, 'work':'fine' }";

        // Test 1 - Array
        log.info("Parsing "+jsonArrayTest);
        JsonObject rootArrayTest = new JsonIO().read(jsonArrayTest);
        log.info(Arrays.toString(
                ((JsonArray) rootArrayTest.getChild("this").getValue()).getChildren()
        ));

        // Test 2 - Object
        log.info("Parsing "+jsonObjectTest);
        JsonObject rootObjTest = json.read(jsonObjectTest);
        log.info(((JsonObject) rootObjTest.getChild("this").getValue()).getChild("bool").getValue().toString());
        log.info(((JsonObject) rootObjTest.getChild("this").getValue()).getChild("number").getValue().toString());
    }

    public void test_queueFillAndEmpty() {
        Logger log = Server.getLogger(Server.TEST_LOG + "/Queue");
        Queue<Integer> testQueue = Queue.ofLength(1024);
        log.info("Filling queue of size 1024 (round 1)");

        int elementsQueue = 0;
        while (!testQueue.isFull()) {
            testQueue.enqueue(elementsQueue);
            elementsQueue++;
        }

        log.info(String.format("Filled the queue with %s amount of entries", elementsQueue));
        log.info(String.format("Attempting to dequeue %s amount of entries", elementsQueue));

        int elementsDequeue = 0;
        while (!testQueue.isEmpty()) {
            testQueue.dequeue();
            elementsDequeue++;
        }

        log.info(String.format("Dequeued %s amount of entries", elementsDequeue));

        log.info("Filling queue of size 1024 (round 2)");

        elementsQueue = 0;
        while (!testQueue.isFull()) {
            testQueue.enqueue(elementsQueue);
            elementsQueue++;
        }

        log.info(String.format("Filled the queue with %s amount of entries", elementsQueue));
        log.info(String.format("Attempting to dequeue %s amount of entries", elementsQueue));

        elementsDequeue = 0;
        while (!testQueue.isEmpty()) {
            testQueue.dequeue();
            elementsDequeue++;
        }

        log.info(String.format("Dequeued %s amount of entries", elementsDequeue));
    }


    @EventHandler
    public void onClientConnect(ClientSocketStatusEvent.Open event) {
        Server.getLogger(Server.NET_LOG).info("Connection | " + event.getClient().getID().toString());
    }

    @EventHandler
    public void onClientDisconnect(ClientSocketStatusEvent.LoggedIn event) {
        Server.getLogger(Server.NET_LOG).info(String.format("Logged in | %s (%s)", event.getClient().getID().toString(), event.getUsername()));
    }

    @EventHandler
    public void onClientDisconnect(ClientSocketStatusEvent.Disconnect event) {
        Server.getLogger(Server.NET_LOG).info("Disconnected | " + event.getClient().getID().toString());
    }

    @EventHandler(ignoreIfCancelled = true, priority = Priority.HIGHEST)
    public void onPacketIn(PacketEvent.In<?> event) {

        if(this.settings.getOrDefault(ServerConfig.LOG_PACKET_IO)) {
            Server.getLogger(Server.NET_LOG).info(String.format("IN | %s << %s %s",
                    event.getClientNetID().toString(),
                    event.getPacket().toCoreString(),
                    event.getPacket().toString())
            );
        }

        UUID id = event.getClientNetID();
        Optional<NetworkClient> cl = this.getNetworkInterface().getClient(event.getClientNetID());
        if(!cl.isPresent()) return;
        NetworkClient client = cl.get();

        switch (event.getPacket().getPacketID()) {


            case VanillaProtocol.PACKET_PROTOCOL_CHECK:
                if(client.getState() == ConnectionState.OPEN) {
                    if(!(event.getPacket() instanceof PacketInProtocolCheck)) return;
                    client.setState(ConnectionState.PROTOCOL);
                    this.serverEventManager.call(new ClientSocketStatusEvent.Protocol(client));

                    PacketInProtocolCheck protocolCheck = (PacketInProtocolCheck) event.getPacket();

                    if(protocolCheck.isValid()) {

                        if(VanillaProtocol.PROTOCOL_ID == protocolCheck.getProtocolVersion()) {
                            client.send(new PacketOutProtocolSuccess(), true);
                            client.setState(ConnectionState.CONNECTED);
                            this.serverEventManager.call(new ClientSocketStatusEvent.Connected(client));

                        } else {
                            String append = (VanillaProtocol.PROTOCOL_ID < protocolCheck.getProtocolVersion())
                                    ? "Client is newer than the server."
                                    : "Client is older than the server.";

                            client.send(new PacketOutProtocolError(VanillaProtocol.PROTOCOL_ID, VanillaProtocol.SUPPORTED_VERSION_STRING), true);
                            networkInterface.disconnectClient(id, null);
                            Server.getLogger(NET_LOG).info(
                                    String.format("Client %s attempted to connect with an unsupported protocol. %s", id.toString(), append)
                            );
                        }

                    } else {
                        networkInterface.disconnectClient(id, new PacketInOutDisconnect("Invalid network version! How'd you manage that? :)"));
                        Server.getLogger(NET_LOG).warn(String.format("Client %s attempted to connect with an invalid protocol version check.", id.toString()));
                    }

                } else {
                    Server.getLogger(NET_LOG).warn(String.format("Client %s sent protocol check packet at an unexpected point. Ignoring.", id.toString()));
                }

                break;



            case VanillaProtocol.PACKET_SERVER_PING_REQUEST:
                String name = this.settings.getOrDefault(ServerConfig.SERVER_NAME);
                String region = this.settings.getOrDefault(ServerConfig.REGION);
                String description = this.settings.getOrDefault(ServerConfig.DESCRIPTION);

                client.send(new PacketOutServerDetail(name, region, description), true);
                break;



            case VanillaProtocol.PACKET_LOGIN:
                if(isClientCompatible(client)) {
                    if(!(event.getPacket() instanceof PacketInLogin)) return;
                    PacketInLogin login = (PacketInLogin) event.getPacket();

                    if(login.isValid()) {
                        // Start login checks within a thread.
                        // It's a database action so we don't want to slow down incoming packets.
                        authenticationManager.processLoginPacket(login, client);

                    } else {
                        client.send(new PacketOutLoginResponse()
                                    .setStatus(PacketOutLoginResponse.Status.INVALID_PACKET),
                                true);
                    }
                }
                break;



            case VanillaProtocol.PACKET_UPDATE_ACCOUNT:
                if(isClientCompatible(client)) {
                    if(!(event.getPacket() instanceof PacketInUpdateAccount)) return;
                    PacketInUpdateAccount update = (PacketInUpdateAccount) event.getPacket();
                    MicroBoolean missingValues = update.getMissingFields();

                    if(update.isValid()) {

                        this.authenticationManager.processRegisterPacket(update, client);

                    } else {
                        client.send(
                                new PacketOutLoginResponse()
                                        .setStatus(PacketOutLoginResponse.Status.MISSING_FIELDS)
                                        .setMissingFields(missingValues),
                                true
                        );
                    }
                }
                break;

            // When adding new packets, remember to include an isClientCompatible()
            // This ensures the protocol is compatible.


            // when handled elsewhere, add your packets in here.
            case VanillaProtocol.PACKET_GAME_SEARCH_REQUEST:
                break;

            case VanillaProtocol.PACKET_PROTOCOL_INVALID_PACKET:
            default:
                if(settings.getOrDefault(ServerConfig.LOG_UNSUPPORTED_PACKETS))
                    Server.getLogger(NET_LOG).warn(
                            String.format("Client %s sent a packet with an invalid/unregistered ID.", id.toString())
                    );

                break;



        }
    }

    @EventHandler(ignoreIfCancelled = true, priority = Priority.HIGHEST)
    public void onPacketOut(PacketEvent.Out<?> event) {
        if(this.settings.getOrDefault(ServerConfig.LOG_PACKET_IO)) {
            Server.getLogger(Server.NET_LOG).info(String.format("OUT | %s >> %s %s",
                    event.getClientNetID().toString(),
                    event.getPacket().toCoreString(),
                    event.getPacket().toString())
            );
        }
    }



    public LockableSettings getSettings() { return settings; }
    public File getDataPath() { return dataPath; }
    public boolean isRunning() { return isRunning; }

    public CommandingScheduler getServerScheduler() { return serverScheduler; }
    public Scheduler getDefaultScheduler() { return defaultScheduler; }

    public EventManager getEventManager() { return serverEventManager; }
    public DatabaseManager getDBManager() { return databaseManager; }
    public AuthenticationManager getAuthManager() { return authenticationManager; }
    public GameManager getGameManager() { return gameManager; }

    public NetworkInterface getNetworkInterface() { return networkInterface; }

    public static Server get() { return instance; }
    public static Logger getLogger(String name) { return Server.loggerFactory.getLogger(name); }

    protected static boolean isClientCompatible(NetworkClient client) {
        // It's past the protocol checks so it's compatible with the server.
        return (client.getState() == ConnectionState.CONNECTED) || (client.getState() == ConnectionState.LOGGED_IN);
    }




    /**
     * Launches the server from the jar.
     * @param args arguments entered when running the jar.
     */
    public static void main(String[] args) throws IOException {
        List<String> argsList = Arrays.asList(args);


        // Sourced from -> https://stackoverflow.com/questions/7704405/how-do-i-make-my-java-application-open-a-console-terminal-window
        // Thanks StackOverflow <3_<3
        if((System.console() == null) && (!GraphicsEnvironment.isHeadless()) && (!argsList.contains("-headless"))) {
            String filename = Server.class.getProtectionDomain().getCodeSource().getLocation().toString().substring(6);
            Runtime.getRuntime().exec(new String[]{"cmd", "/c", "start", "cmd", "/k","java -jar \"" + filename + "\""}); // Run the jar but in a cmd window.

        } else {
            instance = new Server(); // args are not used. Use config instead.
            instance.start();
            System.out.println("!!!  Stopped Server :^)  !!!"); // No logger prepared, use java's own methods (ew)
            System.exit(0);
        }
    }

}
