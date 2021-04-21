package net.cg360.spookums.server.network;

public class VanillaProtocol {

    public static final Short[] SUPPORTED_PROTOCOLS = new Short[]{ 1 };
    public static final Short CURRENT_PROTOCOL = 1;


    // -- Packet Identifiers --

    // Protocol packets - These should not change in format, even after a large update for consistency.
    public static final byte PACKET_PROTOCOL_CHECK = 0x01; // in - Can be appened with new data. Includes vital protocol info. Nothing should be removed/reordered to accommodate for older clients.
    public static final byte PACKET_PROTOCOL_SUCCESS = 0x02; // out - confirms the client is compatible.
    public static final byte PACKET_PROTOCOL_ERROR = 0x03;
    public static final byte PACKET_PROTOCOL_BATCH = 0x04; // Unused currently. Probably a good idea though.


    // Information Packets
    public static final byte PACKET_SERVER_PING_REQUEST = 0x11; // in - responded to with PACKET_SERVER_DETAIL
    public static final byte PACKET_SERVER_DETAIL = 0x11; // out - JSON format - Like a ping, should only be extended. Includes stuff like name + logo if present.
    public static final byte PACKET_CLIENT_DETAIL = 0x12; // in - JSON format - Stores client OS, version, and other non-essential details. Could be use to split platforms

    // Generic Packets
    public static final byte PACKET_SERVER_NOTICE = 0x12; // out - Used to display generic information to a user
    public static final byte PACKET_RESPONSE_WARNING = 0x13; // out - Used to respond to client packets with a warn status
    public static final byte PACKET_RESPONSE_SUCCESS = 0x14; // out - Used to respond to client packets with a info status
    public static final byte PACKET_RESPONSE_ERROR = 0x15; // out - Used to respond to client packets with a error status
    public static final byte PACKET_DISCONNECT_REASON = 0x16; // in/out - Sent by the server/client that's closing the connection.

    // Account Management Packets
    public static final byte PACKET_LOGIN = 0x20; // in - User attempts to login to their account
    public static final byte PACKET_CREATE_ACCOUNT = 0x22; // in - User attempts to create an account (Returns a login response packet)
    public static final byte PACKET_LOGIN_RESPONSE = 0x24; // out - Could split into token (success) packet + error (failure) packet.


    // Session Stuff
    public static final byte PACKET_GAME_JOIN_REQUEST = 0x30; // in - Client's intent to join a game. Will return a PACKET_SESSION_RESPONSE type packet
    public static final byte PACKET_GAME_CREATE_REQUEST = 0x31; // in - Client's intent to create their own game with it's settings included.
    public static final byte PACKET_GAME_RESPONSE = 0x32; // out - Returns a specific game token or an error message

    public static final byte PACKET_FETCH_GAME_LIST = 0x33; // in - Requests a list of games (Responded to with a few PACKET_GAME_DETAIL's)
    public static final byte PACKET_REQUEST_GAME_DETAIL = 0x34; // in - Requests the details of a specific game
    public static final byte PACKET_GAME_DETAIL = 0x35; // out - Sends details of the game to the client

}
