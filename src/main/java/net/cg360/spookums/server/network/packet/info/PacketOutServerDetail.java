package net.cg360.spookums.server.network.packet.info;

import net.cg360.spookums.server.network.VanillaProtocol;
import net.cg360.spookums.server.network.packet.NetworkPacket;

public class PacketOutServerDetail extends NetworkPacket {

    public static final int SERVER_DETAIL_FORMAT_VERSION = 1; // Independent from protocol as it

    // Note that these are for the general server a client would login to, not a
    // specific game session.
    protected int pingVersion; // The revision of the ServerDetail packet format | not available in constructor because why?
    protected String serverName;
    protected String serverRegion; // 5 characters max
    protected String serverDescription;
    //protected byte[] logoData;  Logos would be a nice touch but would take a lil
    //protected String logoURL;   bit to implement.

    public PacketOutServerDetail() {
        this(null, null, null);
    }

    public  PacketOutServerDetail(String serverName, String serverRegion, String serverDescription) {
        this.pingVersion = SERVER_DETAIL_FORMAT_VERSION;
        this.serverName = serverName;
        this.serverRegion = serverRegion;
        this.serverDescription = serverDescription;
    }



    @Override
    protected byte getPacketTypeID() {
        return VanillaProtocol.PACKET_SERVER_DETAIL;
    }

    @Override
    protected int encodeBody() {
        String name = (serverName == null) || (serverName.length() > 60) || (serverName.length() < 1) ? "Unidentified Server" : serverName;
        String region = (serverRegion == null) || (serverRegion.length() > 5) || (serverRegion.length() < 1) ? "?" : serverRegion;
        String description = serverDescription == null ? "Welcome to this server!" : serverDescription;

        this.getBodyData().putUnsignedByte(pingVersion);

        int size = 0;

        size += this.getBodyData().putSmallUTF8String(name);
        size += this.getBodyData().putSmallUTF8String(region);

        size += this.getBodyData().putUTF8String(description);

        return size + 1;
    }

    @Override
    protected void decodeBody(int inboundSize) {
        // nothing here silly.
    }
}
