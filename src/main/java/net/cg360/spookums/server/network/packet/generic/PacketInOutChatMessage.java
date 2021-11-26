package net.cg360.spookums.server.network.packet.generic;

import net.cg360.spookums.server.network.VanillaProtocol;
import net.cg360.spookums.server.network.packet.NetworkPacket;

import java.nio.charset.StandardCharsets;

public class PacketInOutChatMessage extends NetworkPacket {

    protected String text;

    public PacketInOutChatMessage() { this(null); }
    public PacketInOutChatMessage(String text) {
        this.text = text;
    }


    @Override
    protected byte getPacketTypeID() {
        return VanillaProtocol.PACKET_CHAT_MESSAGE;
    }

    @Override
    protected int encodeBody() {
        String selectedText = this.text == null ? " " : this.text;

        this.getBodyData().reset();
        return this.getBodyData().putUnboundUTF8String(selectedText);
    }

    @Override
    protected void decodeBody(int inboundSize) {

        if(inboundSize == 0) {
            this.text = " ";

        } else {
            this.text = this.getBodyData().getUnboundUTF8String(inboundSize);
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
