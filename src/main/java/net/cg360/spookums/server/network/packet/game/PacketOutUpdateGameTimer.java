package net.cg360.spookums.server.network.packet.game;

import net.cg360.spookums.server.network.VanillaProtocol;
import net.cg360.spookums.server.network.packet.NetworkPacket;
import net.cg360.spookums.server.util.NetworkBuffer;

public class PacketOutUpdateGameTimer extends NetworkPacket {

    protected int timerTicks;

    public PacketOutUpdateGameTimer() {
        this.timerTicks = 0;
    }

    @Override
    protected byte getPacketTypeID() {
        return VanillaProtocol.PACKET_TIMER_UPDATE;
    }

    @Override
    protected int encodeBody() {
        return this.getBodyData().putUnsignedInt(timerTicks) ? NetworkBuffer.INT_BYTE_COUNT : 0;
    }

    @Override
    protected void decodeBody(int inboundSize) {

    }


    public PacketOutUpdateGameTimer setTimerTicks(int timerTicks) {
        this.timerTicks = Math.max(timerTicks, 0);
        return this;
    }
}
