package net.cg360.spookums.server.network;

import net.cg360.spookums.server.Server;
import net.cg360.spookums.server.network.packet.generic.PacketInOutChatMessage;
import net.cg360.spookums.server.network.packet.generic.PacketInOutDisconnect;
import net.cg360.spookums.server.network.packet.info.PacketInProtocolCheck;
import net.cg360.spookums.server.network.packet.info.PacketOutProtocolError;
import net.cg360.spookums.server.network.packet.info.PacketOutProtocolSuccess;
import net.cg360.spookums.server.network.packet.info.PacketOutServerNotice;

public class VanillaProtocol {

    /* -- General Notes: --
     *
     * Format:
     * 1 byte - packet type
     * 2 bytes (short) - packet body size
     * ... bytes - body
     *
     * Types:
     *  - String:
     *      Stores a string type encoded in UTF-8
     *      - (Tracked) Includes a short type "length" prior to the text, indicating the amount of bytes taken to store it.
     *      - (Simple) Doesn't include a length prior to the string. Only used when the size can be inferred from the packet body length.
     *
     */

    public static final Short PROTOCOL_ID = 1;

    public static final int MAX_BUFFER_SIZE = 4096;
    public static final int MAX_PACKET_SIZE = 1024;
    public static final int TIMEOUT = 15000;


    // -- Packet Identifiers --

    // Protocol packets - These should not change in format, even after a large update for consistency.
    public static final byte PACKET_PROTOCOL_INVALID_PACKET = 0x00; // in/out - This should not be used at all! Packets with this ID are ignored silently.
    public static final byte PACKET_PROTOCOL_CHECK = 0x01; // in - Can be appened with new data. Includes vital protocol info. Nothing should be removed/reordered to accommodate for older clients.
    public static final byte PACKET_PROTOCOL_SUCCESS = 0x02; // out - confirms the client is compatible.
    public static final byte PACKET_PROTOCOL_ERROR = 0x03; // out - rejects the client for using an incompatible protocol. Returns the protocol version and the supported client version
    public static final byte PACKET_PROTOCOL_BATCH = 0x04; // Unused currently. Probably a good idea though.


    // Information Packets
    public static final byte PACKET_SERVER_PING_REQUEST = 0x11; // in - responded to with PACKET_SERVER_DETAIL | Accepted by the server even if a protocol check hasn't occurred.
    public static final byte PACKET_SERVER_DETAIL = 0x11; // out - JSON format - Like a ping, should only be extended. Includes stuff like name + logo if present.
    public static final byte PACKET_CLIENT_DETAIL = 0x12; // in - JSON format - Stores client OS, version, and other non-essential details. Could be use to split platforms
    public static final byte PACKET_SERVER_NOTICE = 0x13; // out - Used to display generic information to a user
    public static final byte PACKET_DISCONNECT_REASON = 0x14; // in/out - Sent by the server/client that's closing the connection.


    // Response/Generic Packets
    public static final byte PACKET_RESPONSE_WARNING = 0x15; // out - Used to respond to client packets with a warn status
    public static final byte PACKET_RESPONSE_SUCCESS = 0x16; // out - Used to respond to client packets with a info status
    public static final byte PACKET_RESPONSE_ERROR = 0x17; // out - Used to respond to client packets with a error status

    public static final byte PACKET_CHAT_MESSAGE = 0x18; // in/out - messages in may get some further formatting.


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


    public static void applyToRegistry(PacketRegistry packetRegistry) {
        packetRegistry
                .r(PACKET_PROTOCOL_INVALID_PACKET, null)
                .r(PACKET_PROTOCOL_CHECK, PacketInProtocolCheck.class)
                .r(PACKET_PROTOCOL_SUCCESS, PacketOutProtocolSuccess.class)
                .r(PACKET_PROTOCOL_ERROR, PacketOutProtocolError.class)
                .r(PACKET_PROTOCOL_BATCH, null)

                .r(PACKET_SERVER_PING_REQUEST, null)
                .r(PACKET_SERVER_DETAIL, null)
                .r(PACKET_CLIENT_DETAIL, null)
                .r(PACKET_SERVER_NOTICE, PacketOutServerNotice.class)
                .r(PACKET_DISCONNECT_REASON, PacketInOutDisconnect.class)

                .r(PACKET_RESPONSE_WARNING, null)
                .r(PACKET_RESPONSE_SUCCESS, null)
                .r(PACKET_RESPONSE_ERROR, null)
                .r(PACKET_CHAT_MESSAGE, PacketInOutChatMessage.class)

                .r(PACKET_LOGIN, null)
                .r(PACKET_CREATE_ACCOUNT, null)
                .r(PACKET_LOGIN_RESPONSE, null)

                .r(PACKET_GAME_JOIN_REQUEST, null)
                .r(PACKET_GAME_CREATE_REQUEST, null)
                .r(PACKET_GAME_RESPONSE, null)
                .r(PACKET_FETCH_GAME_LIST, null)
                .r(PACKET_REQUEST_GAME_DETAIL, null)
                .r(PACKET_GAME_DETAIL, null);
        Server.getMainLogger().info(
                String.format("Applied protocol version %s to %s packet registry.",
                        PROTOCOL_ID,
                        packetRegistry == PacketRegistry.get() ? "the primary" : "a"
                ));
    }

}
