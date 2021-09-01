package net.cg360.spookums.server.network.packet.info;

import net.cg360.spookums.server.network.VanillaProtocol;
import net.cg360.spookums.server.network.packet.NetworkPacket;

public class PacketInClientDetail extends NetworkPacket {

    @Override
    protected byte getPacketTypeID() {
        return VanillaProtocol.PACKET_CLIENT_DETAIL;
    }

    @Override
    protected int encodeBody() {
        return 0;
    }

    @Override
    protected void decodeBody(int inboundSize) {
        //TODO: Decode client detail and log statistics.
    }
}
