package net.cg360.spookums.server.network.packet.generic;

import net.cg360.spookums.server.network.VanillaProtocol;
import net.cg360.spookums.server.network.packet.NetworkPacket;

import java.nio.charset.StandardCharsets;

public class PacketChatMessage extends NetworkPacket {

    protected String text;

    public PacketChatMessage() { this(null); }
    public PacketChatMessage(String text) {
        this.text = text;
    }


    @Override
    protected byte getPacketTypeID() {
        return VanillaProtocol.PACKET_CHAT_MESSAGE;
    }

    @Override
    protected short encodeBody() {
        String selectedText = this.text == null ? " " : this.text;
        byte[] encodedText = selectedText.getBytes(StandardCharsets.UTF_8);
        short size = (short) encodedText.length;

        this.getBodyData().clear();
        this.getBodyData().put(encodedText);

        return size;
    }

    @Override
    protected void decodeBody(short inboundSize) {

        if(inboundSize == 0) {
            this.text = " ";

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
