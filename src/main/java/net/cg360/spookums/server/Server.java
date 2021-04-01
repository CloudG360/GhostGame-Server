package net.cg360.spookums.server;

import net.cg360.spookums.server.log.ServerLogger;
import net.cg360.spookums.server.util.data.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.SimpleLogger;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class Server {

    protected static Server instance;

    protected Logger logger;

    protected Settings settings;
    protected File dataPath;


    public Server(String[] args){
        this.dataPath = new File("./"); // Configurable maybe?

        this.logger = new ServerLogger();
        SimpleLogger
        getLogger().info("Starting server...");

        while (true) {
            getLogger().info("test");
        }
    }



    public void prepareDatabases() {

    }


    public Logger getLogger() { return logger; }
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
