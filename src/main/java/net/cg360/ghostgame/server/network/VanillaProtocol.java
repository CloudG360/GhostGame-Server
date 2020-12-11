package net.cg360.ghostgame.server.network;

public class VanillaProtocol {

    public static final Short[] SUPPORTED_PROTOCOLS = new Short[]{ 1 };


    // -- Packet Identifiers --

    //Protocol packets 0x0[<2..] - These should not change in format, even after a large update for consistency.
    public static final byte PACKET_PROTOCOL_CHECK = 0x00;
    public static final byte PACKET_PROTOCOL_SUCCESS = 0x01;
    public static final byte PACKET_PROTOCOL_ERROR = 0x02;

    //Generic Packets 0x0[3+..]
    public static final byte PACKET_SERVER_NOTICE = 0x06;
    public static final byte PACKET_RESPONSE_WARNING = 0x07;
    public static final byte PACKET_RESPONSE_SUCCESS = 0x08; //out
    public static final byte PACKET_RESPONSE_ERROR = 0x09; //out

    //Account Management Packets 0x1[...]
    public static final byte PACKET_LOGIN = 0x10; //in
    public static final byte PACKET_TOKEN = 0x11; //out

    public static final byte PACKET_CREATE_ACCOUNT = 0x13; //in

}
