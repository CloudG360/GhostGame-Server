package net.cg360.spookums.server.core.data.json;

public interface JsonHolder {

    /**
     * Removes a child from a holder of children.
     * @return true if the child was found.
     */
    boolean removeChild(Json<?> child);

    /** @return true if the holding object has at least one child. */
    boolean hasChildren();

}
