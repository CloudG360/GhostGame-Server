package net.cg360.spookums.server.network.packet.info;

import net.cg360.spookums.server.network.VanillaProtocol;
import net.cg360.spookums.server.network.packet.NetworkPacket;

public class PacketInProtocolCheck extends NetworkPacket {

    protected boolean isValid;
    protected int protocolVersion;

    public PacketInProtocolCheck(short protocolVersion) {
        this.isValid = true;
        this.protocolVersion = protocolVersion;
    }

    @Override
    protected byte getPacketTypeID() {
        return VanillaProtocol.PACKET_PROTOCOL_CHECK;
    }

    // Really this isn't needed as it's an In Packet but eh.
    @Override
    protected int encodeBody() {
        if(!this.isValid) throw new IllegalStateException("Attempting to encode a known malformed packet");

        this.getBodyData().reset();
        this.getBodyData().putUnsignedShort(protocolVersion);

        return 2; // Update if more is added
    }

    @Override
    protected void decodeBody(int inboundSize) {
        this.isValid = false;

        // Ensure it has the protocol version at least.
        // Any checks to detect if the client is rejected should be
        // done outside the packet.
        if(inboundSize >= 2) {
            this.isValid = true;

            this.getBodyData().reset();
            this.protocolVersion = this.getBodyData().getUnsignedShort();
        }
    }
}
