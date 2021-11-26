package net.cg360.spookums.server.network.packet.info;

import net.cg360.spookums.server.Server;
import net.cg360.spookums.server.network.VanillaProtocol;
import net.cg360.spookums.server.network.packet.NetworkPacket;
import net.cg360.spookums.server.util.clean.Check;

import java.nio.charset.StandardCharsets;

/**
 * <h3>Format:</h3>
 * 1 byte - Notice Display Type
 * x byte(s) - UTF-8 String data (length = body size - 1)
 */
public class PacketOutServerNotice extends NetworkPacket {

    public byte type;
    public String text;


    public PacketOutServerNotice() {
        this.type = 0;
        this.text = null;
    }

    public PacketOutServerNotice(byte type, String text) {
        this.setType(type);
        this.setText(text);
    }

    public PacketOutServerNotice(Type type, String text) {
        this(type.getTypeID(), text);
    }




    @Override
    protected byte getPacketTypeID() {
        return VanillaProtocol.PACKET_SERVER_NOTICE;
    }

    @Override
    protected int encodeBody() {
        if(Check.isNull(this.text)) {
            this.text = "...";
        }

        this.getBodyData().reset();
        this.getBodyData().put(this.type);

        // Using a "simple" string rather than a "tracked" string.
        int size = this.getBodyData().putUnboundUTF8String(this.text);

        return (short) (size + 1); // Return the size of the body
    }

    @Override
    protected void decodeBody(int inboundSize) {
        Server.getLogger(Server.NET_LOG).warn("Attempted to decode the outbound packet: PacketServerNotice");
        // Not a server-bound packet so do nothing.
    }



    public byte getType() { return this.type; }
    public String getText() { return this.text; }


    public void setType(byte type) {
        if((this.type > 3) || (this.type < 0))
            Server.getLogger(Server.NET_LOG).warn("Unrecognized type ID in PacketServerNotice (should be from 0-3)");
        this.type = type;
    }
    public void setType(Type type) { this.type = type.getTypeID(); }
    public void setText(String text) { this.text = text; }


    @Override
    public String toString() {
        return "Content: {" +
                "type=" + this.type +
                ", text='" + this.text + "'" +
                "}";
    }



    public enum Type {
        INFO_BOX( 0),     // A simple info prompt is displayed to the client.
        BANNER( 1),       // Visible as a bar at the top of any main-menu page in the client.
        NOTIFICATION(2),  // Sends a simple notification to the client. Usually has a small pop-up when received.
        HIDDEN(3);        // Invisible to the client user. Could be used to pass small amounts of data?

        private final byte typeID;
        Type(int typeID) {
            this.typeID = (byte) typeID;
        }

        public byte getTypeID() {
            return typeID;
        }
    }
}
