package net.cg360.spookums.server.network.packet;

import java.nio.ByteBuffer;

public abstract class NetworkPacket {

    public static final short MAX_BUFFER_SIZE = 1024;

    private ByteBuffer body;
    private byte packetID;

    protected short bodySize;

    public NetworkPacket(){
        this.body = ByteBuffer.wrap(new byte[MAX_BUFFER_SIZE - 3]); // 3 bytes are reserved for meta.
        this.packetID = genPacketID();
        this.bodySize = 0;
    }

    protected abstract byte genPacketID();
    protected abstract short encodeBody(); // returns: body size
    protected abstract short decodeBody(); // returns: body size

    public final ByteBuffer encode() {
        ByteBuffer data = ByteBuffer.wrap(new byte[MAX_BUFFER_SIZE]);

        short size = encodeBody();
        data.put(packetID);
        data.putShort(size);

        this.body.clear(); // Go to the start of the body.

        for(short i = 0; i < size; i++) {
            data.put(body.get()); // Copy bytes from body up to the size
        }

        return data;
    }

    public final NetworkPacket decode(ByteBuffer fullPacket) {
        data.clear();
        packetID = fullPacket.get();
        bodySize = fullPacket.getShort(); // Really should be converted to an int if it's unsigned

        data.p

        for(int i = 0; i < bodySize; i++) {

        }
    }

    public ByteBuffer getBodyData() { return body; }
    public byte getPacketID() { return packetID; }
    public short getBodySize() { return bodySize; }
}
