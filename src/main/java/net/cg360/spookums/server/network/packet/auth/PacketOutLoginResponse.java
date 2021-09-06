package net.cg360.spookums.server.network.packet.auth;

import net.cg360.spookums.server.network.VanillaProtocol;
import net.cg360.spookums.server.network.packet.NetworkPacket;

// Format:
// IF successful:
//     small var-string - username
//     small var-string - token
// ELSE:
//     just send nothing
public class PacketOutLoginResponse extends NetworkPacket {

    protected boolean isSuccess;
    protected String username;
    protected String token;

    public PacketOutLoginResponse() {
        this.isSuccess = false;
        this.token = null;
        this.username = null;
    }

    public PacketOutLoginResponse(String username, String token) {
        this.isSuccess = true;
        this.username = username;
        this.token = token;
    }

    @Override
    protected byte getPacketTypeID() {
        return VanillaProtocol.PACKET_LOGIN_RESPONSE;
    }

    @Override
    protected int encodeBody() {
        getBodyData().reset();
        if(isSuccess()) {
            return getBodyData().putSmallUTF8String(username)
                    + getBodyData().putSmallUTF8String(token);
        }
        return 0;
    }

    @Override
    protected void decodeBody(int inboundSize) {}



    public boolean isSuccess() {
        return isSuccess && (token != null) && (username != null);
    }

    public String getToken() {
        return token;
    }


    public PacketOutLoginResponse setSuccessful(boolean success) {
        isSuccess = success;
        return this;
    }

    public PacketOutLoginResponse setUsername(String username) {
        this.username = username;
        return this;
    }

    public PacketOutLoginResponse setToken(String token) {
        this.token = token;
        return this;
    }
}
