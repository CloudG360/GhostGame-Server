package net.cg360.spookums.server.network.packet.auth;

import net.cg360.spookums.server.network.VanillaProtocol;
import net.cg360.spookums.server.network.packet.NetworkPacket;

// Format:
// IF successful:
//     byte - statusCode
//     small var-string - username
//     small var-string - token
// ELSE:
//     just send nothing
public class PacketOutLoginResponse extends NetworkPacket {

    /**
     * == CODES:
     * 0 = Success/Invalid (Depending on if info follows)
     *
     * 1 = Provided Username does not exist.
     * 2 = Provided Password/Token is invalid/incorrect.
     * 3 = Too many attempts.
     */
    protected byte statusCode;

    protected String username;
    protected String token;

    public PacketOutLoginResponse() {
        this.statusCode = 0;
        this.token = null;
        this.username = null;
    }

    public PacketOutLoginResponse(String username, String token, Status status) {
        this(username, token, status.getStatusCode());
    }

    public PacketOutLoginResponse(String username, String token, byte status) {
        this.statusCode = status;
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
        return (statusCode == 0) && (token != null) && (username != null);
    }

    public String getUsername() {
        return username;
    }

    public String getToken() {
        return token;
    }

    public Status getStatusCode() {
        return Status.getEquivalantCode(statusCode);
    }



    public PacketOutLoginResponse setUsername(String username) {
        this.username = username;
        return this;
    }

    public PacketOutLoginResponse setToken(String token) {
        this.token = token;
        return this;
    }

    public PacketOutLoginResponse setStatus(byte statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public PacketOutLoginResponse setStatus(Status statusCode) {
        this.statusCode = statusCode.getStatusCode();
        return this;
    }



    public enum Status {

        SUCCESS(0),
        FAILURE_GENERAL(0),

        // Login
        INVALID_USERNAME(1),
        INVALID_PASSWORD(2),
        INVALID_TOKEN(2),
        TOO_MANY_ATTEMPTS(3),


        UNKNOWN(127);


        private byte codeNum;
        Status(int codeNum) {
            this.codeNum = (byte) codeNum;
        }

        public byte getStatusCode() {
            return codeNum;
        }

        public static Status getEquivalantCode(byte code) {
            switch (code) {
                case 0:
                    return SUCCESS; // Success/Generic fail
                case 1:
                    return INVALID_USERNAME;
                case 2:
                    return INVALID_PASSWORD; // Pass/Token
                case 3:
                    return TOO_MANY_ATTEMPTS;

                default:
                    return UNKNOWN;
            }
        }
    }
}
