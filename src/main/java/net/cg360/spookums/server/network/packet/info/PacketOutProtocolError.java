package net.cg360.spookums.server.network.packet.info;

import net.cg360.spookums.server.network.VanillaProtocol;
import net.cg360.spookums.server.network.packet.NetworkPacket;

import java.nio.charset.StandardCharsets;

// Acts as a disconnect packet in a way for incompatible clients.
public class PacketOutProtocolError extends NetworkPacket {

    protected int requiredProtocolVersion; // Send 0 if the check packet was malformed.
    protected String requiredClientVersionString; // Simple String

    public PacketOutProtocolError() { this(VanillaProtocol.PROTOCOL_ID, null); }

    public PacketOutProtocolError(int protocolVersion, String requiredClientVersionString) {
        this.requiredProtocolVersion = protocolVersion;
        this.requiredClientVersionString = requiredClientVersionString;
    }

    @Override
    protected byte getPacketTypeID() {
        return VanillaProtocol.PACKET_PROTOCOL_ERROR;
    }

    // Really this isn't needed as it's an In Packet but eh.
    @Override
    protected int encodeBody() {

        String targetString = this.requiredClientVersionString == null
                ? "Unknown Version"
                : this.requiredClientVersionString;

        this.getBodyData().reset();
        this.getBodyData().putUnsignedShort(requiredProtocolVersion);
        int strSize = this.getBodyData().putUTF8String(targetString);

        return strSize + 1; // Update if more is added
    }

    @Override
    protected void decodeBody(int inboundSize) {
        this.getBodyData().reset();
        this.requiredProtocolVersion = this.getBodyData().getUnsignedShort();
        this.requiredClientVersionString = this.getBodyData().getUTF8String();
    }
}
