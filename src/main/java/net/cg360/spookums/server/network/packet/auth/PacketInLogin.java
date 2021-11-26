package net.cg360.spookums.server.network.packet.auth;

import net.cg360.spookums.server.network.VanillaProtocol;
import net.cg360.spookums.server.network.packet.NetworkPacket;

// Packet Format:
// 1 byte - mode (0 = login mode, 1 = token mode)
// IF mode == 0:
//     1 small var-string - username
//     1 small var-string - password
// IF mode == 1:
//     1 small var-string - token
// ELSE:
//     Mark the packet as invalid by nulling it's values.
//     This lets modified login packets accept their own modes
//     easily.
public class PacketInLogin extends NetworkPacket {

    protected byte mode;

    protected String username;
    protected String password;
    protected String token;

    public PacketInLogin() {
        this.mode = 0;
        this.username = null;
        this.password = null;
        this.token = null;
    }

    @Override
    protected byte getPacketTypeID() {
        return VanillaProtocol.PACKET_LOGIN;
    }

    @Override
    protected int encodeBody() { return 0; }

    @Override
    protected void decodeBody(int inboundSize) {
        this.getBodyData().reset();
        if(this.getBodyData().canReadBytesAhead(1)) {
            mode = getBodyData().get();

            switch (mode) {
                case 0:
                    if(this.getBodyData().canReadBytesAhead(1)) username = getBodyData().getSmallUTF8String();
                    if(this.getBodyData().canReadBytesAhead(1)) password = getBodyData().getSmallUTF8String();
                        break;
                case 1:
                    if(this.getBodyData().canReadBytesAhead(1)) token = getBodyData().getSmallUTF8String();
                    break;

                default:
                    break;
            }
        }
    }


    public byte getMode() {
        return mode;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getToken() {
        return token;
    }

    public boolean isValid() {
        return ((username != null) && (password != null)) || (token != null);
    }

}
