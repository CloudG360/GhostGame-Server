package net.cg360.ghostgame.server.network.packet;

import java.nio.ByteBuffer;

public abstract class NetworkPacket {

    private ByteBuffer data;

    public NetworkPacket(int dataBufferSize){
        this.data = ByteBuffer.wrap(new byte[dataBufferSize]);
    }

    public abstract byte getPacketID();
    protected abstract void encodeBody();
    protected abstract void decodeBody();

    public final void encode() {

    }

    public final void decode() {
        data.reset();
        byte packetType = data.get();
    }

    public ByteBuffer getData() { return data; }
}
