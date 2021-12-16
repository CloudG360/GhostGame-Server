package net.cg360.spookums.server.network.packet.game.entity;

import net.cg360.spookums.server.network.VanillaProtocol;
import net.cg360.spookums.server.util.NetworkBuffer;
import net.cg360.spookums.server.util.math.Vector2;

public class PacketInOutEntityMove extends PacketInOutEntity {

    protected short type;
    protected Vector2 movement;

    public PacketInOutEntityMove() {
        this.type = 0;
        this.movement = Vector2.ZERO;
    }

    @Override
    protected byte getPacketTypeID() {
        return VanillaProtocol.PACKET_ENTITY_MOVE;
    }

    @Override
    protected int encodeBody() {
        int total = super.encodeBody();
        total += this.getBodyData().putUnsignedByte(this.type) ? 1 : 0;
        total += this.getBodyData().putVector2(this.movement);

        return total;
    }

    @Override
    protected void decodeBody(int inboundSize) {
        super.decodeBody(inboundSize);

        if(this.getBodyData().canReadBytesAhead(1)) {
            this.type = this.getBodyData().getUnsignedByte();

            if(this.getBodyData().canReadBytesAhead(NetworkBuffer.VECTOR2_BYTE_COUNT)) {
                this.movement = this.getBodyData().getVector2();
            }
        }
    }


    public PacketInOutEntityMove setMovement(Vector2 movement) {
        this.movement = movement;
        return this;
    }

    public PacketInOutEntityMove setType(Type type) {
        this.type = type.getId();
        return this;
    }

    public PacketInOutEntityMove setType(short type) {
        this.type = type;
        return this;
    }


    public byte getTypeId() {
        return (byte) type;
    }

    public Type getType() {
        return Type.getTypeFromID((byte) this.type);
    }

    public Vector2 getMovement() {
        return movement;
    }

    public enum Type {
        DELTA(0),
        ABSOLUTE(1),
        UNKNOWN(127);

        protected final byte id;

        Type(int id) {
            this.id = (byte) id;
        }

        public byte getId() { return id; }

        public static Type getTypeFromID(byte id) {
            switch (id) {
                case 0:
                    return DELTA;

                case 1:
                    return ABSOLUTE;

                default:
                    return UNKNOWN;
            }
        }
    }

}
