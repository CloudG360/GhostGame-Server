package net.cg360.spookums.server.game.manage;

import net.cg360.spookums.server.Server;
import net.cg360.spookums.server.ServerConfig;
import net.cg360.spookums.server.auth.record.AuthenticatedClient;
import net.cg360.spookums.server.core.data.Queue;
import net.cg360.spookums.server.core.event.handler.EventHandler;
import net.cg360.spookums.server.core.event.type.flow.ServerStartedEvent;
import net.cg360.spookums.server.core.event.type.network.ClientSocketStatusEvent;
import net.cg360.spookums.server.core.event.type.network.PacketEvent;
import net.cg360.spookums.server.core.scheduler.Scheduler;
import net.cg360.spookums.server.network.VanillaProtocol;
import net.cg360.spookums.server.network.packet.game.info.PacketOutGameStatus;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

public class GameManager {

    protected Scheduler scheduler;

    protected Queue<AuthenticatedClient> clientQueue;
    protected HashMap<UUID, GameSession> currentSessions;

    public GameManager() {
        this.clientQueue = Queue.ofLength(Server.get().getSettings().getOrDefault(ServerConfig.MAX_GAME_QUEUE_LENGTH));
        this.currentSessions = new HashMap<>();
        this.scheduler = new Scheduler(1);
    }



    public int removePlayerFromAllGames(UUID clientID) {
        int gameRemovals = 0;
        for(GameSession session: currentSessions.values())
            if(session.removePlayerFromGame(clientID)) gameRemovals++;

        return gameRemovals;
    }



    public boolean doesQueueContainUser(UUID uuid) {
        Queue<AuthenticatedClient> checkQueue = Queue.copy(clientQueue);

        while (!checkQueue.isEmpty()) {
            AuthenticatedClient client = checkQueue.dequeue();
            if(client.getClient().getID().equals(uuid))
                return true;
        }

        return false;
    }


    @EventHandler
    public void onGamePacketRecieved(PacketEvent.In<?> e){
        // Give unauthed users the silent treatment, make debugging worse later! :)
        Optional<AuthenticatedClient> authCheck = Server.get().getAuthManager().getAuthenticatedClientForUUID(e.getClientNetID());
        if(!authCheck.isPresent()) return;

        AuthenticatedClient auth = authCheck.get();

        switch (e.getPacket().getPacketID()) {

            case VanillaProtocol.PACKET_GAME_SEARCH_REQUEST:
                {
                    if (this.doesQueueContainUser(e.getClientNetID())) {
                        PacketOutGameStatus pQueueStatus = new PacketOutGameStatus()
                                .setReason("You are already in the queue!")
                                .setType(PacketOutGameStatus.StatusType.QUEUE_REJECTED);
                        auth.getClient().send(pQueueStatus, true);
                        return;
                    }

                    this.clientQueue.enqueue(auth);
                    PacketOutGameStatus pQueueStatus = new PacketOutGameStatus()
                            .setGameID("queue-default")
                            .setType(PacketOutGameStatus.StatusType.QUEUE_JOINED);
                    auth.getClient().send(pQueueStatus, true);

                } break;


            case VanillaProtocol.PACKET_GAME_JOIN_REQUEST:
                {
                    PacketOutGameStatus pJoinReqStatus = new PacketOutGameStatus()
                            .setReason("This is currently not implemented!")
                            .setType(PacketOutGameStatus.StatusType.GAME_REJECTED);
                    auth.getClient().send(pJoinReqStatus, true);
                    return;

                } //break; -- if you remove the return

        }
    }

    @EventHandler
    public void onClientDisconnect(ClientSocketStatusEvent.Disconnect e) {
        this.removePlayerFromAllGames(e.getClient().getID());
    }

    @EventHandler
    public void onServerStart(ServerStartedEvent e) {
        Server.get().getDefaultScheduler().startScheduler();
    }

}
