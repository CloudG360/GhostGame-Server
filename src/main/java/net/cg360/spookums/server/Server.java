package net.cg360.spookums.server;

import net.cg360.spookums.server.util.data.Settings;

import java.io.File;

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
    public static void main(String[] args){ instance = new Server(args); }

}
