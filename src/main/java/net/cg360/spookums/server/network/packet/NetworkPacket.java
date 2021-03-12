package net.cg360.spookums.server.network.packet;

import java.nio.ByteBuffer;

public abstract class NetworkPacket {

    private ByteBuffer data;

    public NetworkPacket(int dataBufferSize){
        this.data = ByteBuffer.wrap(new byte[dataBufferSize + 3]);
    }

    public abstract byte getPacketID();
    protected abstract void encodeBody();
    protected abstract void decodeBody();

    public final void encode() {
        this.data.clear();
        this.data.put(getPacketID());
        this.data.putShort((short) 0); //Skip packet size marker for later.

        this.data.mark();
        encodeBody();

        this.data.
    }

    public final void decode() {
        data.reset();
        byte packetType = data.get();
    }

    public ByteBuffer getData() { return data; }
}
