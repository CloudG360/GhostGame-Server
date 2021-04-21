package net.cg360.spookums.server.core.event.type;

import net.cg360.spookums.server.core.event.BaseEvent;

/**
 * An implementation of the Cancellable interface applied to
 * an Event. Can be used as a base
 */
public abstract class CancellableEvent extends BaseEvent implements Cancellable {

    private boolean isCancelled = false;

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void setCancelled(boolean isCancelled) {
        this.isCancelled = isCancelled;
    }
}
