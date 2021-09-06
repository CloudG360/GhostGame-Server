package net.cg360.spookums.server.core.event.type.network;

import net.cg360.spookums.server.core.event.type.Event;
import net.cg360.spookums.server.network.packet.NetworkPacket;
import net.cg360.spookums.server.util.clean.Check;

import java.util.UUID;

public abstract class PacketEvent<P extends NetworkPacket> extends Event.Cancellable {

    protected UUID clientNetID;
    protected P packet;

    public PacketEvent(UUID clientNetID, P packet) {
        Check.nullParam(clientNetID, "clientNetID");
        Check.nullParam(packet, "packet");

        this.clientNetID = clientNetID;
        this.packet = packet;
    }


    public UUID getClientNetID() { return clientNetID; }
    public P getPacket() { return packet; }



    public static class In<P extends NetworkPacket> extends PacketEvent<P> {
        public In(UUID clientNetID, P packet) { super(clientNetID, packet); }
    }
    public static class Out<P extends NetworkPacket> extends PacketEvent<P> {
        public Out(UUID clientNetID, P packet) { super(clientNetID, packet); }
    }

}
