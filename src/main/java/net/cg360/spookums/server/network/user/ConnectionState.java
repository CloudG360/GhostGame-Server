package net.cg360.spookums.server.network.user;

public enum ConnectionState {

    OPEN, // Just opened
    PROTOCOL, // Protocol info recieved
    CONNECTED, // Responded with a positive protocol
    LOGGED_IN,
    DISCONNECTED

}
