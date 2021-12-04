package net.cg360.spookums.server.network.packet.type;

import net.cg360.spookums.server.network.packet.NetworkPacket;

public abstract class PacketInOutEmpty extends NetworkPacket {

    @Override protected final int encodeBody() {
        // Hacky fix to get empty packets to send
        // I don't have the time to fix it right now but this defo needs fixing!
        this.getBodyData().putUnsignedByte(0x00);
        return 1;
    }
    @Override protected final void decodeBody(int inboundSize) { }

}
