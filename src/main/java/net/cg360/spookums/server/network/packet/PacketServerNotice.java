package net.cg360.spookums.server.network.packet;

import net.cg360.spookums.server.network.VanillaProtocol;
import net.cg360.spookums.server.util.Check;

import java.nio.charset.StandardCharsets;

public class PacketServerNotice extends NetworkPacket {

    public byte type;
    public String text;

    public PacketServerNotice(byte type, String text) {
        Check.nullParam(text, "text");

        this.type = type;
        this.text = text;
    }

    public PacketServerNotice(Type type, String text) {
        this(type.getTypeID(), text);
    }



    @Override
    protected byte getPacketTypeID() {
        return VanillaProtocol.PACKET_SERVER_NOTICE;
    }

    @Override
    protected short encodeBody() {
        this.getBodyData().clear();
        this.getBodyData().put(type);

        // Usually strings would have a length short in-front, however it can
        // be assumed from the packet length so it isn't used here!
        byte[] bytes = this.text.getBytes(StandardCharsets.UTF_8);
        this.getBodyData().put(bytes);

        return (short) (bytes.length + 1); // Return the size of the body
    }

    @Override
    protected void decodeBody() {
        // Not a server-bound packet so do nothing.
    }



    public enum Type {
        INFO_BOX( 0),
        BANNER( 1),
        NOTIFICATION(2);

        private final byte typeID;
        Type(int typeID) {
            this.typeID = (byte) typeID;
        }

        public byte getTypeID() {
            return typeID;
        }
    }
}
