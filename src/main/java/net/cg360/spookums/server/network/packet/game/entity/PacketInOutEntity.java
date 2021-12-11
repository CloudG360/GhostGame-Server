package net.cg360.spookums.server.network.packet.game.entity;

import net.cg360.spookums.server.network.packet.NetworkPacket;
import net.cg360.spookums.server.util.NetworkBuffer;

public abstract class PacketInOutEntity extends NetworkPacket {

    protected long entityRuntimeID;


    @Override
    protected int encodeBody() {
        this.getBodyData().reset();
        this.getBodyData().putUnsignedInt(entityRuntimeID);
        return 4;
    }

    @Override
    protected void decodeBody(int inboundSize) {
        this.getBodyData().reset();
        if(this.getBodyData().canReadBytesAhead(NetworkBuffer.INT_BYTE_COUNT))
            this.entityRuntimeID = this.getBodyData().getUnsignedInt();
    }


    public long getRuntimeID() {
        return entityRuntimeID;
    }
}
