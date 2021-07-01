package net.cg360.spookums.server.network.packet.type;

import net.cg360.spookums.server.network.packet.NetworkPacket;

import java.nio.charset.StandardCharsets;

public abstract class PacketInTokenHolder extends NetworkPacket {

    public static final String NO_TOKEN = "N/A";

    protected String token;

    public PacketInTokenHolder() { this(null); }
    public PacketInTokenHolder(String token) {
        this.token = token;
    }

    @Override
    protected int encodeBody() {
        String finalToken = token == null ? NO_TOKEN : token;
        byte[] encodedToken = finalToken.getBytes(StandardCharsets.UTF_8);

        this.getBodyData().reset();
        this.getBodyData().put((byte) encodedToken.length); // tokens should be small. Use byte.
        //this.getBodyData().put(encodedToken);

        return (short) (encodedToken.length + 1); // array length + token bytes
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}
