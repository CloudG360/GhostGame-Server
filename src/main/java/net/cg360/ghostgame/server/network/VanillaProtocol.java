package net.cg360.ghostgame.server.network;

public class VanillaProtocol {

    public static final Short[] SUPPORTED_PROTOCOLS = new Short[]{ 1 };


    // -- Packet Identifiers --

    //Protocol packets 0x0[0-7] - These should not change in format, even after a large update for consistency.
    public static final byte PACKET_PROTOCOL_SERVER_DETAIL = 0x00; // Like a ping, should only be extended. Includes stuff like name + logo if present.
    public static final byte PACKET_PROTOCOL_CLIENT_DETAIL = 0x01; // Informs based on OS, version, and other details.
    public static final byte PACKET_PROTOCOL_CHECK = 0x02; //in. Can be extended with new data. Includes vital protocol info. Nothing should be removed to accomodate for older clients.
    public static final byte PACKET_PROTOCOL_SUCCESS = 0x03;
    public static final byte PACKET_PROTOCOL_ERROR = 0x04;


    //Generic Packets 0x0[8-f]
    public static final byte PACKET_SERVER_NOTICE = 0x08; //out
    public static final byte PACKET_RESPONSE_WARNING = 0x07; //out
    public static final byte PACKET_RESPONSE_SUCCESS = 0x09; //out
    public static final byte PACKET_RESPONSE_ERROR = 0x0a; //out
    public static final byte PACKET_DISCONNECT_REASON = 0x0b; //Use before closing connection.

    //Account Management Packets 0x1[...]
    public static final byte PACKET_LOGIN = 0x10; //in
    public static final byte PACKET_LOGIN_RESPONSE = 0x11; //out - Could split into token packet + error packet.

    public static final byte PACKET_CREATE_ACCOUNT = 0x13; //in

    // Session Stuff
    public static final byte PACKET_SESSION_JOIN_REQUEST = 0x30;
    public static final byte PACKET_SESSION_CREATE_REQUEST = 0x31;




}
