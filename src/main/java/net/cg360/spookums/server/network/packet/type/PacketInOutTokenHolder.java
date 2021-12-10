package net.cg360.spookums.server.network.packet.type;

import net.cg360.spookums.server.network.packet.NetworkPacket;

import java.nio.charset.StandardCharsets;

public abstract class PacketInOutTokenHolder extends NetworkPacket {

    public static final String NO_TOKEN = "N/A";

    protected String token;

    public PacketInOutTokenHolder() { this(null); }
    public PacketInOutTokenHolder(String token) {
        this.token = token;
    }

    @Override
    protected int encodeBody() {
        String finalToken = this.token == null ? NO_TOKEN : this.token;
        byte[] encodedToken = finalToken.getBytes(StandardCharsets.US_ASCII);

        this.getBodyData().reset();
        this.getBodyData().put((byte) encodedToken.length); // tokens should be small. Use byte.
        //this.getBodyData().put(encodedToken);

        return (short) (encodedToken.length + 1); // array length + token bytes
    }

    public String getToken() { return this.token; }
    public void setToken(String token) { this.token = token; }
}
