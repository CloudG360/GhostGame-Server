package net.cg360.spookums.server;

import net.cg360.spookums.server.core.event.EventManager;
import net.cg360.spookums.server.core.log.ServerLogger;
import net.cg360.spookums.server.core.scheduler.Scheduler;
import net.cg360.spookums.server.core.data.Settings;
import org.slf4j.Logger;
import org.slf4j.impl.SimpleLoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class Server {

    public static final int MSPT = 1000 / 20; // Millis per tick.

    protected static Server instance;

    protected Logger logger;

    protected Thread networkThread;
    protected Thread schedulerThread;

    protected Scheduler serverScheduler;
    protected EventManager serverEventManager;

    protected File dataPath;
    protected Settings settings;


    public Server(String[] args) {
        this.dataPath = new File("./"); // Configurable maybe?
        this.settings = new Settings();

        this.logger = new SimpleLoggerFactory().getLogger("Server");
        getLogger().info("Starting server...");


        this.serverScheduler = new Scheduler(0);
        this.serverScheduler.setAsPrimaryInstance();

        this.serverEventManager = new EventManager();
        this.serverEventManager.setAsPrimaryManager();


        getLogger().info("Starting server scheduler!");
        this.schedulerThread = new Thread() {

            private boolean loop = true;

            @Override
            public void run() {

                try {

                    while (loop) {
                        serverScheduler.serverTick();
                        this.wait(MSPT);
                    }

                } catch (InterruptedException err) {
                    getLogger().info("Shutting down main scheduler- this better be during shutdown :^)");
                }
            }

            @Override
            public void interrupt() {
                this.loop = false;
                super.interrupt();
            }
        };

        this.serverScheduler.startScheduler();
        this.schedulerThread.start();
        getLogger().info("Started the scheduler!! :)");
    }




    public Logger getLogger() { return logger; }

    public Scheduler getServerScheduler() { return serverScheduler; }
    public EventManager getServerEventManager() { return serverEventManager; }

    public Settings getSettings() { return settings; }
    public File getDataPath() { return dataPath; }

    public static Server get() { return instance; }
    public static Logger getMainLogger() { return get().getLogger(); }



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
            System.out.println("!!!  Stopped Server :^)  !!!"); // No logger prepared, use java's own methods (ew)
        }
    }

}
