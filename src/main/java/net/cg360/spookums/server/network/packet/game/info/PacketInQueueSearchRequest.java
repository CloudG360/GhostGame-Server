package net.cg360.spookums.server.network.packet.game.info;

import net.cg360.spookums.server.network.VanillaProtocol;
import net.cg360.spookums.server.network.packet.type.PacketInOutEmpty;

public class PacketInQueueSearchRequest extends PacketInOutEmpty {

    //TODO: In the future, add filters

    @Override
    protected byte getPacketTypeID() {
        return VanillaProtocol.PACKET_GAME_SEARCH_REQUEST;
    }
}
