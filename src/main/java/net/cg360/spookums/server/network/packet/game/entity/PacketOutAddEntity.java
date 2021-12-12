package net.cg360.spookums.server.network.packet.game.entity;

import net.cg360.spookums.server.core.data.id.Identifier;
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
        total += 1; this.getBodyData().putUnsignedByte(this.floorNumber);
        total += this.getBodyData().putUTF8String(this.propertiesJSON);

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

                this.propertiesJSON = this.getBodyData().getUTF8String();
            }
        }
    }


    public String getEntityTypeId() {
        return entityTypeId;
    }

    public Vector2 getPosition() {
        return position;
    }

    public short getFloorNumber() {
        return floorNumber;
    }

    public String getPropertiesJSON() {
        return propertiesJSON;
    }


    public PacketOutAddEntity setEntityRuntimeID(long entityRuntimeID) {
        this.entityRuntimeID = entityRuntimeID;
        return this;
    }

    public PacketOutAddEntity setEntityTypeId(String entityTypeId) {
        this.entityTypeId = entityTypeId;
        return this;
    }

    public PacketOutAddEntity setEntityTypeId(Identifier entityTypeId) {
        this.entityTypeId = entityTypeId.toString();
        return this;
    }

    public PacketOutAddEntity setPosition(Vector2 position) {
        this.position = position;
        return this;
    }

    public PacketOutAddEntity setFloorNumber(short floorNumber) {
        this.floorNumber = floorNumber;
        return this;
    }

    public PacketOutAddEntity setPropertiesJSON(String propertiesJSON) {
        this.propertiesJSON = propertiesJSON;
        return this;
    }
}
