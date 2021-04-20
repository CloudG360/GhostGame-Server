package net.cg360.spookums.server.scheduler;

import net.cg360.spookums.server.scheduler.task.SchedulerTask;
import net.cg360.spookums.server.util.Check;

public final class SchedulerTaskEntry {

    private final SchedulerTask task;
    private final int repeatInterval;
    private final long nextTick;
    private final boolean isAsynchronous;

    protected SchedulerTaskEntry(SchedulerTask task, int repeatInterval, long nextTick, boolean isAsynchronous) {
        Check.nullParam(task, "task");

        this.task = task;
        this.repeatInterval = repeatInterval;
        this.nextTick = nextTick;
        this.isAsynchronous = isAsynchronous;
    }

    public SchedulerTask getTask() { return task; }

    public boolean isRepeating() { return repeatInterval > 0; }
    public boolean isAsynchronous() { return isAsynchronous; }

    public int getRepeatInterval() { return repeatInterval; }
    public long getNextTick() { return nextTick; }

}
