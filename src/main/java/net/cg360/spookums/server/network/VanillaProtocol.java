package net.cg360.spookums.server.network;

public class VanillaProtocol {

    public static final Short[] SUPPORTED_PROTOCOLS = new Short[]{ 1 };
    public static final Short CURRENT_PROTOCOL = 1;

    public static final byte PACKET_EXTENSION = 0x7f; //ew, why does java use *signed* bytes. Like who's realistically using negative numbers with bytes.

    // -- Packet Identifiers --

    //Protocol packets 0x0? - These should not change in format, even after a large update for consistency.
    public static final byte PACKET_PROTOCOL_CHECK = 0x01; //in. Can be appened with new data. Includes vital protocol info. Nothing should be removed to accomodate for older clients.
    public static final byte PACKET_PROTOCOL_SUCCESS = 0x02;
    public static final byte PACKET_PROTOCOL_ERROR = 0x03;
    public static final byte PACKET_PROTOCOL_BATCH = 0x04;


    //Infomation Packets 0x1+
    public static final byte PACKET_SERVER_DETAIL = 0x10; // Like a ping, should only be extended. Includes stuff like name + logo if present.
    public static final byte PACKET_CLIENT_DETAIL = 0x11; // Informs based on OS, version, and other details.

    //Generic Packets
    public static final byte PACKET_SERVER_NOTICE = 0x12; //out
    public static final byte PACKET_RESPONSE_WARNING = 0x13; //out
    public static final byte PACKET_RESPONSE_SUCCESS = 0x14; //out
    public static final byte PACKET_RESPONSE_ERROR = 0x15; //out
    public static final byte PACKET_DISCONNECT_REASON = 0x16; //Use before closing connection.

    //Account Management Packets
    public static final byte PACKET_LOGIN = 0x20; //in
    public static final byte PACKET_LOGIN_RESPONSE = 0x21; //out - Could split into token packet + error packet.
    public static final byte PACKET_CREATE_ACCOUNT = 0x22; //in

    // Session Stuff
    public static final byte PACKET_SESSION_JOIN_REQUEST = 0x30;
    public static final byte PACKET_SESSION_CREATE_REQUEST = 0x31;

    //




}
