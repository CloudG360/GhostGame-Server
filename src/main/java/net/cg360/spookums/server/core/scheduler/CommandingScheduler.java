package net.cg360.spookums.server.core.scheduler;

import net.cg360.spookums.server.Server;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class CommandingScheduler extends SchedulingType {

    private static CommandingScheduler primaryInstance;

    private final Set<Scheduler> children;

    public CommandingScheduler() {
        this.children = Collections.synchronizedSet(new HashSet<>());
    }

    public boolean setAsPrimaryInstance() {
        if(primaryInstance == null) {
            primaryInstance = this;
            return true;
        }
        return false;
    }


    public void syncSchedulers(Scheduler... children) {
        this.children.addAll(Arrays.asList(children));
    }

    public void desyncSchedulers(Scheduler... children) {
        Arrays.asList(children).forEach(this.children::remove);
    }


    @Override
    public boolean startScheduler() {
        if(!isRunning) {
            this.isRunning = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean stopScheduler() {
        if(isRunning)  {
            pauseScheduler();
            this.children.clear();
            return true;
        }
        return false;
    }

    @Override
    public void pauseScheduler() {
        this.isRunning = false;
    }

    @Override
    public void schedulerTick() {

        if(isRunning) {
            // Must duplicate the set to allow schedulers to remove themselves.
            for (Scheduler scheduler : new HashSet<>(children)) {
                scheduler.schedulerTick();
            }
        }
    }
}
