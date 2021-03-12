package net.cg360.spookums.server;

import net.cg360.spookums.server.util.data.Settings;

public class Server {

    private static Server instance;

    private Settings settings;

    public Server(String[] launchArguments){

    }

    public static Server get() { return instance; }

    /**
     * Launches the server from the jar.
     * @param args arguments entered when running the jar.
     */
    public static void main(String[] args){ instance = new Server(args); }

}
