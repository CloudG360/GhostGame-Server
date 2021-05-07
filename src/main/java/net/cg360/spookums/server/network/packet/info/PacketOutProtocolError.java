package net.cg360.spookums.server.network.packet.info;

import net.cg360.spookums.server.network.VanillaProtocol;
import net.cg360.spookums.server.network.packet.NetworkPacket;

import java.nio.charset.StandardCharsets;

public class PacketOutProtocolError extends NetworkPacket {

    protected short requiredProtocolVersion; // Send 0 if the check packet was malformed.
    protected String requiredClientVersionString; // Simple String

    public PacketOutProtocolError(short protocolVersion, String requiredClientVersionString) {
        this.requiredProtocolVersion = protocolVersion;
        this.requiredClientVersionString = requiredClientVersionString;
    }

    @Override
    protected byte getPacketTypeID() {
        return VanillaProtocol.PACKET_PROTOCOL_ERROR;
    }

    // Really this isn't needed as it's an In Packet but eh.
    @Override
    protected short encodeBody() {

        String targetString = requiredClientVersionString == null ? "Unknown Version" : requiredClientVersionString;
        byte[] clientStringData = targetString.getBytes(StandardCharsets.UTF_8);

        this.getBodyData().clear();
        this.getBodyData().putShort(requiredProtocolVersion);
        this.getBodyData().put(clientStringData);

        return (short) (clientStringData.length + 1); // Update if more is added
    }

    @Override
    protected void decodeBody(short inboundSize) {

        this.getBodyData().clear();
        this.requiredProtocolVersion = this.getBodyData().getShort();

        byte[] targetBuffer = new byte[inboundSize - 1];
        this.getBodyData().get(targetBuffer);
        this.requiredClientVersionString = new String(targetBuffer, StandardCharsets.UTF_8);
    }
}
