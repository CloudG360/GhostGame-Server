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
    protected short encodeBody() {
        String selectedText = this.text == null ? DEFAULT_OUTBOUND_TEXT : this.text;
        byte[] encodedText = selectedText.getBytes(StandardCharsets.UTF_8);
        short size = (short) encodedText.length;

        this.getBodyData().clear();
        this.getBodyData().put(encodedText);

        return size;
    }

    @Override
    protected void decodeBody(short inboundSize) {

        if(inboundSize == 0) {
            this.text = DEFAULT_INBOUND_TEXT;

        } else {
            byte[] target = new byte[inboundSize];
            this.getBodyData().get(target);

            this.text = new String(target, StandardCharsets.UTF_8);
        }

        this.getBodyData().clear();
    }


    public String getText() { return text; }



    @Override
    public String toString() {
        return "Content: {" +
                "text='" + text + "'" +
                "}";
    }
}
