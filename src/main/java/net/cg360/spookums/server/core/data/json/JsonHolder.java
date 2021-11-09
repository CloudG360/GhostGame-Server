package net.cg360.spookums.server.core.data.json;

public interface JsonHolder {

    /**
     * Removes a child from a holder of children.
     * @return true if the child was found.
     */
    boolean removeChild(Json<?> child);

    /**
     * Adds or sets a child in the parent object.
     * @return true if the child is successfully added/updated.
     */
    boolean acceptNewChild(Json<?> child);

    /** @return true if the holding object has at least one child. */
    boolean hasChildren();

}
