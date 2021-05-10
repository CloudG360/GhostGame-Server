package net.cg360.spookums.server.network.packet.info;

import net.cg360.spookums.server.network.VanillaProtocol;
import net.cg360.spookums.server.network.packet.type.PacketInOutEmpty;

public class PacketInServerPingRequest extends PacketInOutEmpty {

    @Override
    protected byte getPacketTypeID() {
        return VanillaProtocol.PACKET_SERVER_PING_REQUEST;
    }

}
