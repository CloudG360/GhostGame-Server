package net.cg360.spookums.server.core.event.type;

public interface CancellableType {

    boolean isCancelled();

    default void setCancelled() { setCancelled(true); }
    void setCancelled(boolean isCancelled);

}
