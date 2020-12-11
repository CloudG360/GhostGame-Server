package net.cg360.ghostgame.server;

public class Server {

    private static Server instance;

    public Server(String[] launchArguments){

    }

    public static Server get() { return instance; }

    /**
     * Launches the server from the jar.
     * @param args arguments entered when running the jar.
     */
    public static void main(String[] args){ instance = new Server(args); }

}
