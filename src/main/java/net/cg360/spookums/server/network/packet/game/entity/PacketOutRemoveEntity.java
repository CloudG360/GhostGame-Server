package net.cg360.spookums.server.network.packet.game.entity;

import net.cg360.spookums.server.network.VanillaProtocol;
import net.cg360.spookums.server.util.NetworkBuffer;
import net.cg360.spookums.server.util.math.Vector2;

public class PacketOutRemoveEntity extends PacketInOutEntity {

    // inherited: entity runtime id

    public PacketOutRemoveEntity() {
        this.entityRuntimeID = -1; // Invalid as the ID is uint, throw error.
    }

    @Override
    protected byte getPacketTypeID() {
        return VanillaProtocol.PACKET_ENTITY_REMOVE;
    }

    @Override
    protected int encodeBody() {
        return super.encodeBody();
    }

    @Override
    protected void decodeBody(int inboundSize) {
        super.decodeBody(inboundSize);
    }
}
