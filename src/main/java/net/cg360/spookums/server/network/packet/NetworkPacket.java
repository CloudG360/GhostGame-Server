package net.cg360.spookums.server.network.packet;

import java.nio.ByteBuffer;

public abstract class NetworkPacket {

    public static final short MAX_BUFFER_SIZE = 1024;

    private ByteBuffer body;
    private byte packetID;

    protected short bodySize;

    public NetworkPacket(){
        this.body = ByteBuffer.wrap(new byte[MAX_BUFFER_SIZE - 3]); // 3 bytes are reserved for meta.
        this.packetID = getPacketTypeID();
        this.bodySize = 0;
    }

    protected abstract byte getPacketTypeID();
    protected abstract short encodeBody(); // Takes data and puts it into the body buffer. returns: body size
    protected abstract void decodeBody(short inboundSize); // Takes data from the body buffer and converts it to fields.

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
        fullPacket.clear(); // Ensure buffers are ready for reading.
        this.body.clear();

        this.packetID = fullPacket.get();
        this.bodySize = fullPacket.getShort(); // Really should be converted to an int if it's unsigned

        for(int i = 0; i < bodySize; i++) {
            this.body.put(fullPacket.get()); // Copy bytes
        }

        this.body.clear();
        decodeBody(this.bodySize);
        return this;
    }

    public ByteBuffer getBodyData() { return body; }
    public byte getPacketID() { return packetID; }
    public short getBodySize() { return bodySize; }
}
