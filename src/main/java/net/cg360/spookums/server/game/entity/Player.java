package net.cg360.spookums.server.game.entity;

import net.cg360.spookums.server.auth.record.AuthenticatedClient;
import net.cg360.spookums.server.core.data.Queue;
import net.cg360.spookums.server.core.data.id.Identifier;
import net.cg360.spookums.server.game.level.Floor;
import net.cg360.spookums.server.network.packet.game.entity.PacketOutAddEntity;
import net.cg360.spookums.server.network.packet.game.entity.PacketOutRemoveEntity;
import net.cg360.spookums.server.util.Constants;
import net.cg360.spookums.server.util.math.Vector2;

import java.awt.Color;
import java.util.HashMap;

public class Player extends Entity {

    //TODO: IMPORTANT !!! When destroying a player entity, remove it from visibleTo lists on all entities. !!!

    protected static final Queue<Color> COLOUR_QUEUE = Queue.ofLength(8);

    static {
        Player.COLOUR_QUEUE
                .enqueue(new Color(232, 55, 35)) // red
                .enqueue(new Color(232, 202, 35)) // yellow
                .enqueue(new Color(71, 204, 22)) // cg green
                .enqueue(new Color(22, 180, 204)) // light blue

                .enqueue(new Color(255, 128,0)) // orange
                .enqueue(new Color(22, 204, 119)) // turquoise
                .enqueue(new Color(92, 22, 204)) // conquest purple
                .enqueue(new Color(224, 88, 204)) // pink
        ;

    }

    protected AuthenticatedClient client;
    protected HashMap<Long, Entity> visibleEntities;

    protected Color colour;


    public Player(AuthenticatedClient client, Floor floor, Vector2 position) {
        super(floor, position);
        this.client = client;
        this.visibleEntities = new HashMap<>();

        this.colour = Player.COLOUR_QUEUE.cycle();
    }


    @Override
    public Player init() {
        return (Player) super.init();
    }

    @Override
    public Identifier getTypeID() {
        return Constants.NAMESPACE.id("player");
    }

    @Override
    public String serializePropertiesToJson() {
        return String.format(
                "{\"username\":\"%s\", \"colour\":\"%s\"}",
                client.getUsername(),
                Integer.toHexString(this.getColour().getRGB())
        );
    }

    @Override
    public boolean showTo(Player player) {
        //Overrides behaviour for when this is a player's own entity.
        if(this.getAuthClient() == player.getAuthClient()) {
            if ((!isDestroyed) && (!player.visibleEntities.containsKey(this.getRuntimeID()))) {

                PacketOutAddEntity packetOutAddEntity = new PacketOutAddEntity()
                        .setEntityRuntimeID(0)
                        .setEntityTypeId(this.getTypeID())
                        .setPosition(this.getPosition())
                        .setPropertiesJSON(this.serializePropertiesToJson())
                        .setFloorNumber(this.getFloor().getFloorNumber());
                player.getAuthClient().getClient().send(packetOutAddEntity, true);

                player.visibleEntities.put(this.getRuntimeID(), this);
                this.visibleTo.add(player);
                return true;
            }
            return false;

        } else return super.showTo(player);
    }


    @Override
    public boolean hideFrom(Player player) {
        if(this.getAuthClient() == player.getAuthClient()) {
            if(player.visibleEntities.containsKey(this.getRuntimeID())) {

                PacketOutRemoveEntity packetOutRemoveEntity = new PacketOutRemoveEntity()
                        .setEntityRuntimeID(0);
                player.getAuthClient().getClient().send(packetOutRemoveEntity, true);

                player.visibleEntities.remove(this.getRuntimeID());
                this.visibleTo.remove(player);
                return true;
            }
            return false;

        } else return super.hideFrom(player);
    }



    public AuthenticatedClient getAuthClient() {
        return client;
    }

    public Color getColour() {
        return colour;
    }
}
