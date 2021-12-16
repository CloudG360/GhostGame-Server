package net.cg360.spookums.server.network;

import net.cg360.spookums.server.Server;
import net.cg360.spookums.server.network.packet.auth.PacketInLogin;
import net.cg360.spookums.server.network.packet.auth.PacketInUpdateAccount;
import net.cg360.spookums.server.network.packet.auth.PacketOutLoginResponse;
import net.cg360.spookums.server.network.packet.game.PacketOutUpdateGameTimer;
import net.cg360.spookums.server.network.packet.game.entity.PacketInOutEntityMove;
import net.cg360.spookums.server.network.packet.game.entity.PacketOutAddEntity;
import net.cg360.spookums.server.network.packet.game.entity.PacketOutRemoveEntity;
import net.cg360.spookums.server.network.packet.game.info.PacketInQueueSearchRequest;
import net.cg360.spookums.server.network.packet.game.info.PacketOutGameStatus;
import net.cg360.spookums.server.network.packet.generic.PacketInOutChatMessage;
import net.cg360.spookums.server.network.packet.generic.PacketInOutDisconnect;
import net.cg360.spookums.server.network.packet.generic.PacketInOutError;
import net.cg360.spookums.server.network.packet.generic.PacketInOutWarn;
import net.cg360.spookums.server.network.packet.info.*;

public class VanillaProtocol {

    public static final Short PROTOCOL_ID = 1;
    public static final String SUPPORTED_VERSION_STRING = "v1.0.0";

    public static final int MAX_BUFFER_SIZE = 8192;
    public static final int MAX_PACKET_SIZE = 4096;
    //public static final int TIMEOUT = 10000;  -- Use ServerConfig value.


    // -- Packet Identifiers --

    // Protocol Connection:
    // Client sends version -> Server responds with success or failure

    // Protocol packets - These should not change in format, even after a large update for consistency.
    public static final byte PACKET_PROTOCOL_INVALID_PACKET = 0x00; // in/out - This should not be used at all! Packets with this ID are ignored silently.
    public static final byte PACKET_PROTOCOL_CHECK = 0x01; // in - Can be appened with new data. Includes vital protocol info. Nothing should be removed/reordered to accommodate for older clients.
    public static final byte PACKET_PROTOCOL_SUCCESS = 0x02; // out - confirms the client is compatible.
    public static final byte PACKET_PROTOCOL_ERROR = 0x03; // out - rejects the client for using an incompatible protocol. Returns the protocol version and the supported client version
    public static final byte PACKET_PROTOCOL_BATCH = 0x04; // Unused currently. Probably a good idea though.


    // Information Packets
    public static final byte PACKET_SERVER_PING_REQUEST = 0x10; // in - responded to with PACKET_SERVER_DETAIL | Accepted by the server even if a protocol check hasn't occurred.
    public static final byte PACKET_SERVER_DETAIL = 0x11; // out - Like a ping, should only be extended. Includes stuff like name + logo if present.
    public static final byte PACKET_CLIENT_DETAIL = 0x12; // in - Stores client OS, version, and other non-essential details. Could be use to split platforms
    public static final byte PACKET_SERVER_NOTICE = 0x13; // out - Used to display generic information to a user
    public static final byte PACKET_DISCONNECT_REASON = 0x14; // in/out - Sent by the server/client that's closing the connection.


    // Response/Generic Packets
    public static final byte PACKET_RESPONSE_WARNING = 0x15; // out - Used to respond to client packets with a warn status
    public static final byte PACKET_RESPONSE_SUCCESS = 0x16; // out - Used to respond to client packets with a info status
    public static final byte PACKET_RESPONSE_ERROR = 0x17; // in/out - Used to respond to client or server packets with a error status.

    public static final byte PACKET_CHAT_MESSAGE = 0x18; // in/out - A chat message!


    // Account Management Packets
    public static final byte PACKET_LOGIN = 0x20; // in - User attempts to login to their account with either a token or login details.
    public static final byte PACKET_UPDATE_ACCOUNT = 0x22; // in - User attempts to create/update an account (Returns a new login in a login response packet)
    public static final byte PACKET_LOGIN_RESPONSE = 0x24; // out - Could split into token (success) packet + error (failure) packet.


    // Session Stuff
    public static final byte PACKET_GAME_JOIN_REQUEST = 0x30; // in - Client's intent to join a *specific* game. Will return a PACKET_GAME_STATUS type packet
    public static final byte PACKET_GAME_SEARCH_REQUEST = 0x31; // in - Client's intent to join a *new* game. Can result in placing them in a queue.
    // BREAKING NEXT -> public static final byte PACKET_GAME_CREATE_REQUEST = 0x31; // in - Client's intent to create their own game with it's settings included.

    // out - Update's a client on their status in relation to the game. It can place the client in the queue, mark it as a spectator,
    // or mark it as an active player. It can also indicate that a player has been kicked or denied from a game, no-matter their current status.
    public static final byte PACKET_GAME_STATUS = 0x32;

    // These will be utilised differently to the final game.
    // Currently, their only use is to offer spectating support.
    //public static final byte PACKET_FETCH_GAME_LIST = 0x33; // in - Requests a list of games (Responded to with a few PACKET_GAME_DETAIL's)
    public static final byte PACKET_REQUEST_GAME_DETAIL = 0x34; // in - Requests the details of a specific game
    public static final byte PACKET_GAME_DETAIL = 0x35; // out - Sends details of the game to the client


    public static final byte PACKET_ENTITY_ADD = 0x50; // out - Spawns an entity on the client side based on its runtime id.
    public static final byte PACKET_ENTITY_REMOVE = 0x51; // out - Removes an entity from the client side based on its runtime id
    public static final byte PACKET_ENTITY_MOVE = 0x52; // out - Moves an entity on the client. Big, small, whatever, done in one packet.

    public static final byte PACKET_TIMER_UPDATE = 0x60;


    //TODO: As you'll be looking at this before the C# project
    //      Instead of storing the next tick a task will run, store an
    //      offset of ticks. This can be decremented as each task is already
    //      looped through recursively. It should eliminate timing issues.



    public static void applyToRegistry(PacketRegistry packetRegistry) {
        packetRegistry
                .r(PACKET_PROTOCOL_INVALID_PACKET, null)
                .r(PACKET_PROTOCOL_CHECK, PacketInProtocolCheck.class)
                .r(PACKET_PROTOCOL_SUCCESS, PacketOutProtocolSuccess.class)
                .r(PACKET_PROTOCOL_ERROR, PacketOutProtocolError.class)
                .r(PACKET_PROTOCOL_BATCH, null)

                .r(PACKET_SERVER_PING_REQUEST, PacketInServerPingRequest.class)
                .r(PACKET_SERVER_DETAIL, PacketOutServerDetail.class)
                .r(PACKET_CLIENT_DETAIL, PacketInClientDetail.class)
                .r(PACKET_SERVER_NOTICE, PacketOutServerNotice.class)
                .r(PACKET_DISCONNECT_REASON, PacketInOutDisconnect.class)

                .r(PACKET_RESPONSE_WARNING, PacketInOutWarn.class)
                .r(PACKET_RESPONSE_SUCCESS, null)
                .r(PACKET_RESPONSE_ERROR, PacketInOutError.class)
                .r(PACKET_CHAT_MESSAGE, PacketInOutChatMessage.class)

                .r(PACKET_LOGIN, PacketInLogin.class)
                .r(PACKET_UPDATE_ACCOUNT, PacketInUpdateAccount.class)
                .r(PACKET_LOGIN_RESPONSE, PacketOutLoginResponse.class)

                //.r(PACKET_GAME_JOIN_REQUEST, PacketInGameJoinRequest.class)
                .r(PACKET_GAME_SEARCH_REQUEST, PacketInQueueSearchRequest.class)
                .r(PACKET_GAME_STATUS, PacketOutGameStatus.class)
                //.r(PACKET_FETCH_GAME_LIST, null)
                .r(PACKET_REQUEST_GAME_DETAIL, null)
                .r(PACKET_GAME_DETAIL, null)

                .r(PACKET_ENTITY_ADD, PacketOutAddEntity.class)
                .r(PACKET_ENTITY_REMOVE, PacketOutRemoveEntity.class)
                .r(PACKET_ENTITY_MOVE, PacketInOutEntityMove.class)

                .r(PACKET_TIMER_UPDATE, PacketOutUpdateGameTimer.class)
        ;

        Server.getLogger(Server.NET_LOG).info(
                String.format("Applied protocol version %s to %s packet registry.",
                        PROTOCOL_ID,
                        packetRegistry == PacketRegistry.get() ? "the primary" : "a"
                ));
    }

}
