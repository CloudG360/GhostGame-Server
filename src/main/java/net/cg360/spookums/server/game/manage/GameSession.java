package net.cg360.spookums.server.game.manage;

import net.cg360.spookums.server.Server;
import net.cg360.spookums.server.ServerConfig;
import net.cg360.spookums.server.auth.record.AuthenticatedClient;
import net.cg360.spookums.server.core.data.Queue;
import net.cg360.spookums.server.core.scheduler.Scheduler;
import net.cg360.spookums.server.core.scheduler.task.SchedulerTask;
import net.cg360.spookums.server.game.entity.Entity;
import net.cg360.spookums.server.game.entity.Player;
import net.cg360.spookums.server.game.level.Floor;
import net.cg360.spookums.server.game.level.Map;
import net.cg360.spookums.server.network.packet.game.PacketOutUpdateGameTimer;
import net.cg360.spookums.server.network.packet.game.entity.PacketInOutEntityMove;
import net.cg360.spookums.server.network.packet.game.info.PacketOutGameStatus;
import net.cg360.spookums.server.network.user.NetworkClient;
import net.cg360.spookums.server.util.clean.Check;
import net.cg360.spookums.server.util.math.Vector2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class GameSession {

    protected String sessionID;
    protected SessionState sessionState;
    protected Scheduler scheduler;

    protected boolean isPrivate;
    protected int minPlayers;
    protected int maxPlayers;

    protected SchedulerTask tickTask;
    protected SchedulerTask broadcastTimerTask;

    protected HashMap<UUID, AuthenticatedClient> userLookup;
    protected HashMap<UUID, Player> playerTiedEntities;

    protected Map map;

    protected int timer;


    /** @see GameManager#createGame(boolean, int, int) */
    protected GameSession(boolean isPrivate, int minPlayers, int maxPlayers) {
        Check.inclusiveLowerBound(minPlayers, 1, "minPlayers");
        Check.inclusiveLowerBound(maxPlayers, minPlayers, "maxPlayers (max < min)");

        this.sessionID = UUID.randomUUID().toString();
        this.sessionState = SessionState.QUEUEING;
        this.scheduler = new Scheduler(1);

        this.isPrivate = isPrivate;
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;

        this.tickTask = null;
        this.broadcastTimerTask = null;

        this.userLookup = new HashMap<>();
        this.playerTiedEntities = new HashMap<>();

        this.timer = 0;
    }


    protected GameSession postInit() {
        this.map = new Map(this);
        this.map.fillEmptyFloors();

        return this;
    }

    /**
     * Used to accept new players into a currently filling game from the queue.
     * @param potentialPlayers the queue to draw players from, used by a GameManager
     */
    protected void reviewQueue(Queue<AuthenticatedClient> potentialPlayers) {
        if((!isPrivate) && this.sessionState == SessionState.QUEUEING) { //TODO: Support queue joins from countdown stage

            // Ensure that either the minimum player count is queuing for a game or that some players are already in.
            if(this.userLookup.size() > 0 || potentialPlayers.getLength() >= minPlayers) {

                // While players are in queue and not at max players
                while ((!potentialPlayers.isEmpty()) && (this.userLookup.size() < maxPlayers)) {
                    AuthenticatedClient usr = potentialPlayers.dequeue();
                    Server.get().getGameManager().removePlayerFromAllGames(usr.getClient());

                    PacketOutGameStatus status = new PacketOutGameStatus()
                            .setType(PacketOutGameStatus.StatusType.GAME_JOIN)
                            .setGameID(this.getSessionID())
                            .setReason("Joined via queue");

                    usr.getClient().send(status, true);

                    this.userLookup.put(usr.getClient().getID(), usr);
                }

                this.initialize(); // Start countdown

                int tempXVariance = -7;

                for (AuthenticatedClient client: this.userLookup.values()) {
                    Player player = new Player(client, this.getMap().getFloors()[0], new Vector2(tempXVariance, 0)).init();
                    this.playerTiedEntities.put(client.getClient().getID(), player);

                    tempXVariance += 2;
                }

                for (Player client: this.playerTiedEntities.values()) {
                    for(Entity entity : this.getMap().getFloors()[0].getEntities()) {
                        entity.showTo(client);
                    }
                }
            }
        }
    }

    protected boolean removePlayerFromGame(NetworkClient player) {
        if(this.containsPlayer(player)) {

            //TODO: Remove entity, check game logic

            PacketOutGameStatus statusRemove = new PacketOutGameStatus()
                    .setType(PacketOutGameStatus.StatusType.GAME_DISCONNECT)
                    .setGameID(this.getSessionID());
            this.userLookup.get(player.getID()).getClient().send(statusRemove, true);

            this.userLookup.remove(player.getID());
            return true;
        } else return false;
    }


    public boolean containsPlayer(UUID player) {
        return this.userLookup.containsKey(player);
    }

    public boolean containsPlayer(NetworkClient player) {
        return this.userLookup.containsKey(player.getID());
    }


    public void initialize() {
        if(this.getSessionState() == SessionState.QUEUEING) {
            this.sessionState = SessionState.COUNTDOWN;

            this.timer = Server.get().getSettings().getOrDefault(ServerConfig.GAME_COUNTDOWN_LENGTH);

            this.tickTask = this.scheduler
                    .prepareTask(this::tickCountdown)
                    .setInterval(1)
                    .schedule();
            this.broadcastTimerTask = this.scheduler
                    .prepareTask(this::broadcastUpdatedTimer)
                    .setInterval(20)
                    .setDelay(20)
                    .schedule();

            this.scheduler.startScheduler(false); // Use GameManager's tick
        }
    }

    public void startGameLoop() {
        if(this.getSessionState() == SessionState.COUNTDOWN) {
            this.sessionState = SessionState.GAME_LOOP;

            if(this.tickTask != null)
                this.tickTask.cancel(); // Cancel a countdown timer

            this.timer = Server.get().getSettings().getOrDefault(ServerConfig.GAME_TIMER_LENGTH);

            this.tickTask = this.scheduler
                    .prepareTask(this::tickGame)
                    .setInterval(1)
                    .schedule();

            for(AuthenticatedClient client: this.userLookup.values()) {
                PacketOutGameStatus statusRemove = new PacketOutGameStatus()
                        .setType(PacketOutGameStatus.StatusType.GAME_STARTED)
                        .setGameID(this.getSessionID());
                client.getClient().send(statusRemove, true);
            }
        }
    }

    public void stopGame() {
        if(this.getSessionState() == SessionState.GAME_LOOP) {
            this.sessionState = SessionState.POST_GAME;

            this.tickTask.cancel();
            this.broadcastTimerTask.cancel();

            //TODO: Remove all entities and process scores.
            // Then send players the go-ahead to clear up

            //TODO PROCESS SCORES HERE

            for(AuthenticatedClient client: new ArrayList<>(this.userLookup.values())) {
                PacketOutGameStatus statusRemove = new PacketOutGameStatus()
                        .setType(PacketOutGameStatus.StatusType.GAME_CONCLUDED)
                        .setGameID(this.getSessionID());
                client.getClient().send(statusRemove, true);

                this.userLookup.remove(client.getClient().getID());
            }

            for(Floor floor: this.getMap().getFloors()) {
                for(Entity entity: floor.getEntities())
                    entity.destroy();
            }



        } else if(this.getSessionState() == SessionState.COUNTDOWN) {

            for(AuthenticatedClient client: new ArrayList<>(this.userLookup.values())) {
                this.removePlayerFromGame(client.getClient());
            }

            for(Floor floor: this.getMap().getFloors()) {
                for(Entity entity: floor.getEntities())
                    entity.destroy();
            }
        }

        if(!isPrivate) {
            Server.get().getGameManager().createGame(false); // Replace queue game
        }
    }

    public void tickCountdown() {
        int tickDelta = 1;
        this.timer -= tickDelta;

        if(this.timer <= 0) {
            this.scheduler.prepareTask(this::startGameLoop).schedule();
        }
    }

    // tick delta is there for skipping in the future if I add that.
    public void tickGame() {
        int tickDelta = 1;
        this.timer -= tickDelta;

        if(this.timer <= 0) {
            this.scheduler.prepareTask(this::stopGame).schedule();
            return;
        }


        // Tick all entities;
        for(Floor floor: this.getMap().getFloors()) {
            for(Entity entity : floor.getEntities()) {
                try {
                    entity.tick(tickDelta);
                } catch (Exception err) {
                    err.printStackTrace();
                }
            }
        }
    }


    public void broadcastUpdatedTimer() {

        for(AuthenticatedClient client: this.userLookup.values()) {
            PacketOutUpdateGameTimer packet = new PacketOutUpdateGameTimer().setTimerTicks(this.timer);
            client.getClient().send(packet, true);
        }

    }



    protected void processPlayerMovement(NetworkClient client, PacketInOutEntityMove movement) {

        if(this.sessionState == SessionState.GAME_LOOP) {
            // All clients move their own player with an id of 0
            if (movement.getEntityRuntimeID() == 0 && this.playerTiedEntities.containsKey(client.getID())) {
                Player target = this.playerTiedEntities.get(client.getID());

                if(movement.getType() == PacketInOutEntityMove.Type.DELTA) {
                    Vector2 newPos = target.getPosition().add(movement.getMovement());
                    target.setPosition(newPos);

                } else {
                    target.setPosition(movement.getMovement());
                }
            }
        }
    }


    public String getSessionID() {return this.sessionID;}
    public SessionState getSessionState() {return this.sessionState;}
    public Scheduler getScheduler() {return this.scheduler;}


    public boolean isPrivate() {return isPrivate;}
    public int getMinPlayers() {return minPlayers;}
    public int getMaxPlayers() {return maxPlayers;}

    public Map getMap() {return this.map;}

    public int getTimerTicks() {return this.timer;}
}
