package net.cg360.spookums.server.core.scheduler;

import java.util.*;

public abstract class SchedulingType {

    protected long syncedTick;    // The last received server tick.
    protected volatile long schedulerTick; // The scheduler's actual tick. This depends on the tickrate
    protected volatile boolean isRunning;

    protected int tickDelay; // The amount of server ticks between each scheduler tick.

    protected SchedulingType() {
        this.syncedTick = 0;
        this.schedulerTick = 0;
        this.isRunning = false;

        this.tickDelay = 1;
    }


    public abstract boolean startScheduler();
    public abstract boolean stopScheduler();
    public abstract void pauseScheduler();

    public abstract void schedulerTick();

    /**
     * Ran to indicate a server tick has occurred, potentially triggering a server tick.
     * @return true is a scheduler tick is triggered as a result.
     */
    public final synchronized boolean serverTick() { // Should only be done on the main thread
        if (isRunning) {
            syncedTick++;

            // Check if synced is a multiple of the delay
            if ((syncedTick % tickDelay) == 0) {
                schedulerTick();
                return true;
            }
        }
        return false;
    }


    // -- Getters --

    /** @return the amount of server ticks this scheduler has been running for. */
    public long getSyncedTick() { return syncedTick; }
    /** @return the amount of ticks this scheduler has executed. */
    public long getSchedulerTick() { return schedulerTick; }
    /** @return the amount of server ticks between each scheduler tick. */
    public int getTickDelay() { return tickDelay; }

    public boolean isRunning() { return isRunning; }
}
