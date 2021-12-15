package net.cg360.spookums.server.game.manage;

import net.cg360.spookums.server.Server;
import net.cg360.spookums.server.ServerConfig;
import net.cg360.spookums.server.auth.record.AuthenticatedClient;
import net.cg360.spookums.server.core.data.Queue;
import net.cg360.spookums.server.core.scheduler.task.SchedulerTask;
import net.cg360.spookums.server.game.entity.Entity;
import net.cg360.spookums.server.game.level.Floor;
import net.cg360.spookums.server.game.level.Map;

import java.util.HashMap;
import java.util.UUID;

public class GameSession {

    protected SessionState sessionState;
    protected SchedulerTask tickTask;

    protected HashMap<UUID, AuthenticatedClient> userLookup;

    protected Map map;

    protected int timer;


    public GameSession() {

        this.sessionState = SessionState.QUEUEING;
        this.userLookup = new HashMap<>();

        this.map = new Map();

        this.timer = Server.get().getSettings().getOrDefault(ServerConfig.GAME_TIMER_LENGTH);
    }

    /**
     * Used to accept new players into a currently filling game from the queue.
     * @param potentialPlayers the queue to draw players from, used by a GameManager
     */
    protected void reviewQueue(Queue<AuthenticatedClient> potentialPlayers) {
        if(this.sessionState == SessionState.QUEUEING) {
            int maxPlayers = Server.get().getSettings().getOrDefault(ServerConfig.GAME_MAX_PLAYERS);

            // While players are in queue and not at max players
            while ((!potentialPlayers.isEmpty()) && (this.userLookup.size() < maxPlayers)) {
                AuthenticatedClient usr = potentialPlayers.dequeue();
                Server.get().getGameManager().removePlayerFromAllGames(usr.getClient().getID());

                this.userLookup.put(usr.getClient().getID(), usr);
            }
        }
    }

    protected boolean removePlayerFromGame(UUID player) {
        if(this.userLookup.containsKey(player)) {

            //TODO: Remove entity, check game logic

            this.userLookup.remove(player);
            return true;
        } else return false;
    }


    public void startGameLoop() {
        if(this.getSessionState() == SessionState.COUNTDOWN) {
            this.sessionState = SessionState.GAME_LOOP;
            this.tickTask = Server.get().getDefaultScheduler()
                    .prepareTask(this::tick)
                    .setInterval(1)
                    .schedule();
        }
    }

    public void stopGame() {
        if(this.getSessionState() == SessionState.GAME_LOOP) {
            this.sessionState = SessionState.POST_GAME;
            this.tickTask.cancel();

            //TODO: Remove all entities and process scores.
        }
    }

    // tick delta is there for skipping in the future if I add that.
    public void tick() {
        int tickDelta = 1;
        this.timer -= tickDelta;

        if(this.timer <= 0) {
            this.stopGame();
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




    public Map getMap() {
        return map;
    }

    public SessionState getSessionState() {
        return sessionState;
    }
}
