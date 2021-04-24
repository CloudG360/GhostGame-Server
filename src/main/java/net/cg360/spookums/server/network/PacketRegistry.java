package net.cg360.spookums.server.network;

import net.cg360.spookums.server.network.packet.NetworkPacket;
import net.cg360.spookums.server.util.Check;

import java.util.HashMap;
import java.util.Optional;

// While it's not essential register a packet here, only packets
// found here are processed when recieved by the server.
public class PacketRegistry {

    private static PacketRegistry primaryInstance = null;

    protected HashMap<Character, Class<? extends NetworkPacket>> packetTypes;

    public PacketRegistry() {
        this.packetTypes = new HashMap<>();
    }

    public boolean setAsPrimaryInstance() {
        if(primaryInstance == null) {
            primaryInstance = this;
            return true;
        }
        return false;
    }



    public Optional<Class<? extends NetworkPacket>> getPacketTypeForID(char id) {
        return Optional.ofNullable(this.packetTypes.get(id));
    }

    // Chaining
    public PacketRegistry r(char id, Class<? extends NetworkPacket> type) {
        registerPacketType(id, type);
        return this;
    }

    public boolean registerPacketType(char id, Class<? extends NetworkPacket> type) {
        if(type == null) return false;

        if(!this.packetTypes.containsKey(id)) {
            this.packetTypes.put(id, type);
            return true;
        }
        return false;
    }



    public static PacketRegistry get() {
        return primaryInstance;
    }
}
