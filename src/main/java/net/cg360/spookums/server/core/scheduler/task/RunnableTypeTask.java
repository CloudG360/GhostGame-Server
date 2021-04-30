package net.cg360.spookums.server.core.scheduler.task;


import net.cg360.spookums.server.util.Check;

public final class RunnableTypeTask extends SchedulerTask {

    private final Runnable taskRunnable;

    public RunnableTypeTask(Runnable taskRunnable) {
        Check.nullParam(taskRunnable, "taskRunnable");
        this.taskRunnable = taskRunnable;
    }

    @Override
    public void run() {
        taskRunnable.run();
    }
}
