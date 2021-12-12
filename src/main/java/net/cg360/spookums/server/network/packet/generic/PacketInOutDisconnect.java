package net.cg360.spookums.server.network.packet.generic;

import net.cg360.spookums.server.network.VanillaProtocol;
import net.cg360.spookums.server.network.packet.NetworkPacket;

import java.nio.charset.StandardCharsets;

// Proposed change:
// Have a format which only contains a disconnect "code"
// to make it optionally shorter. (But keep the full te

/**
 * <h3>Format:</h3>
 * x byte(s) - UTF-8 String data (length = body size)
 */
public class PacketInOutDisconnect extends NetworkPacket {

    public static final String DEFAULT_INBOUND_TEXT = "The client has disconnected. (No Reason Specified)";
    public static final String DEFAULT_OUTBOUND_TEXT = "You have been disconnected from the host server.";

    protected String text;


    public PacketInOutDisconnect() {
        this.text = null;
    }

    public PacketInOutDisconnect(String text) {
        this.text = text;
    }



    @Override
    protected byte getPacketTypeID() {
        return VanillaProtocol.PACKET_DISCONNECT_REASON;
    }

    @Override
    protected int encodeBody() {
        String selectedText = this.text == null ? DEFAULT_OUTBOUND_TEXT : this.text;

        this.getBodyData().reset();
        return this.getBodyData().putUTF8String(selectedText);
    }

    @Override
    protected void decodeBody(int inboundSize) {

        if(inboundSize < 2) {
            this.text = DEFAULT_INBOUND_TEXT;

        } else {
            this.text = this.getBodyData().getUTF8String();
        }

        this.getBodyData().reset();
    }


    public String getText() { return this.text; }



    @Override
    public String toString() {
        return "Content: {" +
                "text='" + text + "'" +
                "}";
    }
}
