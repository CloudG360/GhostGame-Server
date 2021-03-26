package net.cg360.spookums.server;

import net.cg360.spookums.server.util.data.Settings;

import java.awt.*;
import java.io.Console;
import java.io.File;
import java.io.IOException;

public class Server {

    private static Server instance;

    private Settings settings;
    private File dataPath;

    public Server(String[] args){
        dataPath = new File("./"); // Run from where jar is ran from. Maybe make it configurable?
    }



    public void prepareDatabases() {

    }


    public Settings getSettings() { return settings; }
    public File getDataPath() { return dataPath; }

    public static Server get() { return instance; }



    /**
     * Launches the server from the jar.
     * @param args arguments entered when running the jar.
     */
    public static void main(String[] args) throws IOException {

        if((System.console() == null) && (!GraphicsEnvironment.isHeadless())) { // No console window exists but one is supported.
            String filename = Server.class.getProtectionDomain().getCodeSource().getLocation().toString().substring(6);
            Runtime.getRuntime().exec(new String[]{"cmd","/c","start","cmd","/k","java -jar \"" + filename + "\""}); // Run the jar but in a cmd window.

        } else {
            instance = new Server(args);
            System.out.println("!!!  Stopped Server :^)  !!!"); // No logger prepared, use java's own methods (ew)
        }
    }

}
