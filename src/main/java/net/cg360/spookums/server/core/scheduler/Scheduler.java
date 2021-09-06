package net.cg360.spookums.server.core.scheduler;

import net.cg360.spookums.server.Server;
import net.cg360.spookums.server.core.scheduler.task.RunnableTypeTask;
import net.cg360.spookums.server.core.scheduler.task.SchedulerTask;
import net.cg360.spookums.server.util.clean.Check;

import java.util.*;

public class Scheduler extends SchedulingType {

    private final UUID schedulerID;

    protected final Set<Thread> activeThreads;
    protected final ArrayList<SchedulerTaskEntry> schedulerTasks;

    public Scheduler(int tickDelay) {
        this.schedulerID = UUID.randomUUID();

        this.tickDelay = Math.max(1, tickDelay);

        this.schedulerTasks = new ArrayList<>();
        this.activeThreads = Collections.synchronizedSet(new HashSet<>());
    }



    // -- Control --

    /** Enables ticking on the scheduler */
    public synchronized boolean startScheduler() { return this.startScheduler(true); }
    public synchronized boolean startScheduler(boolean tickWithServer) {
        if(!isRunning) {
            this.isRunning = true;
            if(tickWithServer) Server.get().getServerScheduler().syncSchedulers(this);
            return true;
        }
        return false;
    }

    /**
     * Removes scheduler's hook to the server tick whilst clearing the queue.
     * @return true if the scheduler was initially running and then stopped.
     */
    public synchronized boolean stopScheduler() {
        if(isRunning) {
            pauseScheduler();
            clearQueuedSchedulerTasks();
            return true;
        }
        return false;
    }

    /** Removes scheduler's hook to the server tick whilst clearing the queue */
    public synchronized void pauseScheduler() {
        this.isRunning = false;
        Server.get().getServerScheduler().desyncSchedulers(this);
    }

    /** Clears all the tasks queued in the scheduler. */
    public synchronized void clearQueuedSchedulerTasks() {
        for(SchedulerTaskEntry entry: new ArrayList<>(schedulerTasks)) {
            entry.getTask().cancel(); // For the runnable to use? idk
            this.schedulerTasks.remove(entry);
        }
    }



    // -- Ticking --
    // Methods used to tick a scheduler should only be triggered by the
    // main scheduler thread.

    /** Executes a scheduler tick, running any tasks due to run on this tick. */
    public synchronized void schedulerTick() {
        if(isRunning) {

            // To avoid stopping the scheduler from inside a task making it scream, use ArrayList wrapping
            for(SchedulerTaskEntry task: new ArrayList<>(schedulerTasks)) {
                long taskTick = task.getNextTick();

                if(taskTick == schedulerTick) {

                    // Cancelled tasks shouldn't be in the scheduler queue anyway.
                    if(!task.getTask().isCancelled()) {

                        if(task.isAsynchronous()) {
                            new Thread() {

                                @Override
                                public void run() {
                                    synchronized (activeThreads) { activeThreads.add(this); }

                                    try {
                                        task.getTask().run();

                                    } catch (Exception err) {
                                        Server.getMainLogger().error("Error thrown in a scheduler (asynchronous) task:");
                                        err.printStackTrace();
                                    }

                                    synchronized (activeThreads) { activeThreads.remove(this); }
                                }

                                @Override
                                public void interrupt() {
                                    synchronized (activeThreads) {
                                        activeThreads.remove(this);
                                    }
                                    super.interrupt();
                                }

                            }.start(); // Start async thread and move on.

                        } else {
                            // Run as sync. This task must complete before the next one
                            // is ran.
                            try {
                                task.getTask().run();

                            } catch (Exception err) {
                                Server.get().getLogger().error("Error thrown in a scheduler (synchronous) task:");
                                err.printStackTrace();
                            }
                        }


                        // Not cancelled by the call of #run() + it's a repeat task.
                        if(task.isRepeating() && (!task.getTask().isCancelled())) {
                            long targetTick = taskTick + task.getRepeatInterval();

                            SchedulerTaskEntry newTask = new SchedulerTaskEntry(task.getTask(), task.getRepeatInterval(), targetTick, task.isAsynchronous());
                            queueTaskEntry(newTask);
                        }
                    }

                } else if(taskTick > schedulerTick) {
                    // Upcoming task, do not remove from queue! :)
                    break;
                }

                schedulerTasks.remove(0); // Operate like a queue.
                // Remove from the start as long as it isn't an upcoming task.
                // If a task is somehow scheduled *before* the current tick, it should
                // be removed anyway.
            }
            schedulerTick++; // Tick after so tasks can be ran without a delay.
        }
    }



    // -- Task Control --

    protected synchronized void queueTaskEntry(SchedulerTaskEntry entry){
        if(entry.getNextTick() <= schedulerTick) throw new IllegalStateException("Task cannot be scheduled before the current tick.");

        int size = schedulerTasks.size();
        for(int i = 0; i < size; i++) {
            // Entry belongs before task? Insert into it's position
            if(schedulerTasks.get(i).getNextTick() > entry.getNextTick()) {
                this.schedulerTasks.add(i, entry);
                return;
            }
        }

        // Not added in loop. Append to the end.
        this.schedulerTasks.add(entry);
    }



    // -- Task Registering --

    public PendingEntryBuilder prepareTask(Runnable task) {
        SchedulerTask rTask = new RunnableTypeTask(task);
        return new PendingEntryBuilder(this, rTask);
    }

    public PendingEntryBuilder prepareTask(SchedulerTask task) {
        return new PendingEntryBuilder(this, task);
    }



    // -- Getters --

    /** The unique ID of this scheduler. */
    public UUID getSchedulerID() { return schedulerID; }
    /** @return a list of active async task threads */
    public Set<Thread> getActiveThreads() { return new HashSet<>(activeThreads); }



    // -- Setters --

    /** Sets the amount of server ticks that should pass between each scheduler tick. */
    public void setTickDelay(int tickDelay) {
        this.tickDelay = tickDelay;
    }



    public static class PendingEntryBuilder {

        protected final Scheduler scheduler;
        protected final SchedulerTask task;

        protected int interval;
        protected int delay;
        protected boolean isAsynchronous;

        protected PendingEntryBuilder(Scheduler scheduler, SchedulerTask task) {
            this.scheduler = Check.nullParam(scheduler, "scheduler");
            this.task = Check.nullParam(task, "task");

            this.interval = 0;
            this.delay = 0;
            this.isAsynchronous = false;
        }

        public SchedulerTask schedule() {
            synchronized (scheduler) {
                long nextTick = scheduler.schedulerTick + delay + 1;
                SchedulerTaskEntry entry = new SchedulerTaskEntry(task, interval, nextTick, isAsynchronous);
                scheduler.queueTaskEntry(entry);
            }
            return task;
        }


        public int getInterval() {
            return interval;
        }

        public int getDelay() {
            return delay;
        }

        public boolean isAsynchronous() {
            return isAsynchronous;
        }

        public PendingEntryBuilder setInterval(int interval) {
            this.interval = Check.inclusiveLowerBound(interval, 0, "interval");
            return this;
        }

        public PendingEntryBuilder setDelay(int delay) {
            this.delay = Check.inclusiveLowerBound(delay, 0, "delay");
            return this;
        }

        public PendingEntryBuilder setAsynchronous(boolean asynchronous) {
            this.isAsynchronous = asynchronous;
            return this;
        }
    }
}
