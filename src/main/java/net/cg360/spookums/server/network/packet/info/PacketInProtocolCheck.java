package net.cg360.spookums.server.network.packet.info;

import net.cg360.spookums.server.network.VanillaProtocol;
import net.cg360.spookums.server.network.packet.NetworkPacket;

public class PacketInProtocolCheck extends NetworkPacket {

    protected boolean isValid;
    protected short protocolVersion;

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
    protected short encodeBody() {
        if(!this.isValid) throw new IllegalStateException("Attempting to encode a known malformed packet");

        this.getBodyData().clear();
        this.getBodyData().putShort(protocolVersion);

        return 2; // Update if more is added
    }

    @Override
    protected void decodeBody(short inboundSize) {
        this.isValid = false;

        // Ensure it has the protocol version at least.
        // Any checks to detect if the client is rejected should be
        // done outside the packet.
        if(inboundSize >= 2) {
            this.isValid = true;

            this.getBodyData().clear();
            this.protocolVersion = this.getBodyData().getShort();
        }
    }
}
