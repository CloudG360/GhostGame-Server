package net.cg360.spookums.server.network.packet.type;

import net.cg360.spookums.server.network.packet.NetworkPacket;

public abstract class PacketInOutCallbackStatus extends NetworkPacket {

    //protected long callbackId = 0x00000000; todo: Add callback ids and a way to track them.
    protected int statusCode = 0x0000; // short
    protected String content = null;


    @Override
    protected int encodeBody() {
        this.getBodyData().reset();

        this.getBodyData().putUnsignedShort(statusCode);
        int strLen = content == null ? 0 : this.getBodyData().putUTF8String(content);

        return 2 + strLen;
    }

    @Override
    protected void decodeBody(int inboundSize) {
        this.getBodyData().reset();
        if(this.getBodyData().canReadBytesAhead(2)) {
            this.statusCode = this.getBodyData().getUnsignedShort();
            this.content = this.getBodyData().getUTF8String();

            // Ironic, an invalid error packet, just set some defaults
        } else {
            this.statusCode = 0xFFFF;
            this.content = "Unknown - Error packet was further malformed";
        }
    }

    public int getStatusCode() { return statusCode; }
    public String getContent() { return content; }

    public PacketInOutCallbackStatus setStatusCode(int statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public PacketInOutCallbackStatus setContent(String content) {
        this.content = content;
        return this;
    }


}
