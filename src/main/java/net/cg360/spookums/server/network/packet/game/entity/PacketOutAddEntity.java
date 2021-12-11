package net.cg360.spookums.server.network.packet.game.entity;

import net.cg360.spookums.server.network.VanillaProtocol;
import net.cg360.spookums.server.util.Constants;
import net.cg360.spookums.server.util.NetworkBuffer;
import net.cg360.spookums.server.util.math.Vector2;

public class PacketOutAddEntity extends PacketInOutEntity {

    // inherited: entity runtime id
    protected String entityTypeId;
    protected Vector2 position;
    protected short floorNumber;

    protected String propertiesJSON;

    public PacketOutAddEntity() {
        this.entityRuntimeID = -1; // Invalid as the ID is uint, throw error.
        this.entityTypeId = null;
        this.position = null;
        this.floorNumber = -1; // Invalid, unsigned byte for floor number

        this.propertiesJSON = "{}"; // providing a default as this isn't essential
    }

    @Override
    protected byte getPacketTypeID() {
        return VanillaProtocol.PACKET_ENTITY_ADD;
    }

    @Override
    protected int encodeBody() {
        int total = super.encodeBody();

        total += this.getBodyData().putSmallUTF8String(this.entityTypeId);
        total += this.getBodyData().putVector2(this.position);
        total += 1; this.getBodyData().putUnsignedByte(floorNumber);
        total += this.getBodyData().putUnboundUTF8String(this.propertiesJSON);

        return total;
    }


    // why I implemented the decoding in java, idk.
    @Override
    protected void decodeBody(int inboundSize) {
        super.decodeBody(inboundSize);

        // Check if type id length can be read
        if(this.getBodyData().canReadBytesAhead(1)) {
            this.entityTypeId = this.getBodyData().getSmallUTF8String();

            if(this.getBodyData().canReadBytesAhead(NetworkBuffer.VECTOR2_BYTE_COUNT + 1)) {
                this.position = this.getBodyData().getVector2();
                this.floorNumber = this.getBodyData().getUnsignedByte();

                this.propertiesJSON = this.getBodyData().countBytesRemaining() > 0
                        ? this.getBodyData().getUnboundUTF8String(this.getBodyData().countBytesRemaining())
                        : "{}";
            }
        }
    }
}
