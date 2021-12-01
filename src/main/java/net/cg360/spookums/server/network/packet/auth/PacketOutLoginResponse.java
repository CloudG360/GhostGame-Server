package net.cg360.spookums.server.network.packet.auth;

import net.cg360.spookums.server.network.VanillaProtocol;
import net.cg360.spookums.server.network.packet.NetworkPacket;
import net.cg360.spookums.server.util.MicroBoolean;

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

    // Keep the IDs consistent with updates, skip index 0.
    protected MicroBoolean missingFields; // For creating an account

    public PacketOutLoginResponse() {
        this.statusCode = 0;
        this.token = null;
        this.username = null;
        this.missingFields = MicroBoolean.from((byte) 0x00);
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
        this.getBodyData().reset();
        if(this.isSuccess()) {
            return this.getBodyData().putSmallUTF8String(username)
                    + this.getBodyData().putSmallUTF8String(token);
        }
        return 0;
    }

    @Override
    protected void decodeBody(int inboundSize) { }



    public boolean isSuccess() {
        return (statusCode == 0) && (token != null) && (username != null);
    }

    public String getUsername() {
        return this.username;
    }

    public String getToken() {
        return this.token;
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

    public PacketOutLoginResponse setMissingFields(MicroBoolean missingFields) {
        this.missingFields = missingFields;
        return this;
    }



    public enum Status {

        SUCCESS(0),
        FAILURE_GENERAL(1),

        // Login
        INVALID_USERNAME(2), // Applies to login, updating account, and creating account (if username is taken)
        INVALID_PASSWORD(3),
        INVALID_TOKEN(3), // Password required for updating an account so n/a there
        TOO_MANY_ATTEMPTS(4),
        ALREADY_LOGGED_IN(5), // Updating an account refreshes the login so not sent.

        // Creating an account
        MISSING_FIELDS(6),
        TAKEN_USERNAME(7),
        TECHNICAL_SERVER_ERROR(8),
        GENERAL_REGISTER_ERROR(9),


        INVALID_PACKET(126),
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
                case 4:
                    return ALREADY_LOGGED_IN;
                case 5:
                    return MISSING_FIELDS;

                case 126:
                    return INVALID_PACKET;
                default:
                    return UNKNOWN;
            }
        }
    }
}
