package net.cg360.spookums.server.network.packet.game.info;

import net.cg360.spookums.server.network.VanillaProtocol;
import net.cg360.spookums.server.network.packet.NetworkPacket;

public class PacketOutGameStatus extends NetworkPacket {

    protected byte type;
    protected String gameID; // for if the type == 2 or 3
    protected String reason; // if rejected, this is filled.

    public PacketOutGameStatus(){
        this.type = 0;
        this.gameID = "";
        this.reason = "";
    }

    @Override
    protected byte getPacketTypeID() {
        return VanillaProtocol.PACKET_GAME_STATUS;
    }

    @Override
    protected int encodeBody() {
        return 0;
    }

    @Override
    protected void decodeBody(int inboundSize) {
        // nothing!
    }


    public PacketOutGameStatus setGameID(String gameID) {
        this.gameID = gameID;
        return this;
    }

    public PacketOutGameStatus setType(StatusType type) {
        this.type = type.getId();
        return this;
    }

    public PacketOutGameStatus setType(byte type) {
        this.type = type;
        return this;
    }

    public PacketOutGameStatus setReason(String reason) {
        this.reason = reason;
        return this;
    }

    public enum StatusType {
        QUEUE_JOINED(1), QUEUE_REJECTED(2),
        GAME_JOIN(3), GAME_REJECTED(4), GAME_JOIN_AS_SPECTATOR(5),
        GAME_DISCONNECT(6),


        UNKNOWN(127);


        private byte id;
        StatusType(int id) {
            this.id = (byte) id;
        }

        public byte getId() {
            return id;
        }


        public static StatusType getTypeFromID(int i) {
            switch (i) {
                case 1:
                    return QUEUE_JOINED;
                case 2:
                    return QUEUE_REJECTED;
                case 3:
                    return GAME_JOIN;
                case 4:
                    return GAME_REJECTED;
                case 5:
                    return GAME_JOIN_AS_SPECTATOR;
                case 6:
                    return GAME_DISCONNECT;
                default:
                    return UNKNOWN;
            }
        }
    }

}
