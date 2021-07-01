package net.cg360.spookums.server.network.packet;

import net.cg360.spookums.server.Server;
import net.cg360.spookums.server.network.VanillaProtocol;
import net.cg360.spookums.server.util.NetworkBuffer;

import java.nio.ByteBuffer;

// Hi me o/
// Create a NetworkHelper that replaces the ByteBuffer, keeping reading + writing
// of values consistent.
// Needs to be in place on both the Java and C# side. Should use bitwise operators to the data

public abstract class NetworkPacket {

    private NetworkBuffer body;
    private byte packetID;

    protected int bodySize; // It's an int in java but an unsigned short in the packet :)

    public NetworkPacket(){
        this.body = NetworkBuffer.wrap(new byte[VanillaProtocol.MAX_PACKET_SIZE - 3]); // 3 bytes are reserved for meta.
        this.packetID = getPacketTypeID();
        this.bodySize = 0;
    }

    protected abstract byte getPacketTypeID();
    protected abstract int encodeBody(); // Takes data and puts it into the body buffer. returns: body size
    protected abstract void decodeBody(int inboundSize); // Takes data from the body buffer and converts it to fields.

    public final NetworkBuffer encode() {
        NetworkBuffer data = NetworkBuffer.wrap(new byte[VanillaProtocol.MAX_PACKET_SIZE]);

        bodySize = encodeBody();
        int size = bodySize + 1;
        data.put(packetID);
        data.putUnsignedShort(size); // TODO: maybe catch and throw another exception to make a size issue clearer.

        this.body.reset(); // Go to the start of the body.

        for(short i = 0; i < size; i++) {
            data.put(body.get()); // Copy bytes from body up to the size
        }

        return data;
    }

    public final NetworkPacket decode(NetworkBuffer fullPacket) {
        fullPacket.reset(); // Ensure buffers are ready for reading.
        this.body.reset();

        this.packetID = fullPacket.get();
        this.bodySize = fullPacket.getUnsignedShort() - 1; // Really should be converted to an int if it's unsigned

        for(int i = 0; i < bodySize; i++) {
            this.body.put(fullPacket.get()); // Copy bytes
        }

        this.body.reset();
        decodeBody(this.bodySize);
        return this;
    }

    public final NetworkBuffer getBodyData() { return body; }
    public final byte getPacketID() { return packetID; }
    public final int getBodySize() { return bodySize; }



    public String toCoreString() {
        return "(" +
                "ID=" + packetID +
                "| size=" + bodySize +
                ")";
    }
}
