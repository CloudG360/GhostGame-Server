package net.cg360.spookums.server.network.packet.generic;

import net.cg360.spookums.server.network.VanillaProtocol;
import net.cg360.spookums.server.network.packet.type.PacketInOutCallbackStatus;

public class PacketInOutError extends PacketInOutCallbackStatus {

    @Override protected byte getPacketTypeID() {
        return VanillaProtocol.PACKET_RESPONSE_ERROR;
    }

}
