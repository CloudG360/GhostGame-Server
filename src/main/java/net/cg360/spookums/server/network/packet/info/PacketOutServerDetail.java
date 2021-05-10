package net.cg360.spookums.server.network.packet.info;

import net.cg360.spookums.server.network.VanillaProtocol;
import net.cg360.spookums.server.network.packet.NetworkPacket;
import net.cg360.spookums.server.util.Check;

import java.nio.charset.StandardCharsets;

public class PacketOutServerDetail extends NetworkPacket {

    public static final int SERVER_DETAIL_FORMAT_VERSION = 1; // Independent from protocol as it

    // Note that these are for the general server a client would login to, not a
    // specific game session.
    protected byte pingVersion; // The revision of the ServerDetail packet format | not available in constructor because why?
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
    protected short encodeBody() {
        String name = (serverName == null) || (serverName.length() > 60) || (serverName.length() < 1) ? "Unidentified Server" : serverName;
        String region = (serverRegion == null) || (serverRegion.length() > 5) || (serverRegion.length() < 1) ? "?" : serverRegion;
        String description = serverDescription == null ? "Welcome to this server!" : serverDescription;

        byte[] encodeName = name.getBytes(StandardCharsets.UTF_8); // 1 byte for byte count.
        byte[] encodeRegion = region.getBytes(StandardCharsets.UTF_8); // 1 byte for byte count
        byte[] encodeDescription = description.getBytes(StandardCharsets.UTF_8); // 2 bytes (short) for byte count.

        this.getBodyData().put(pingVersion);

        this.getBodyData().put((byte) encodeName.length);
        this.getBodyData().put(encodeName);

        this.getBodyData().put((byte) encodeRegion.length);
        this.getBodyData().put(encodeRegion);

        this.getBodyData().putShort((short) encodeDescription.length);
        this.getBodyData().put(encodeDescription);

        return 0;
    }

    @Override
    protected void decodeBody(short inboundSize) {
        // nothing here silly.
    }
}
