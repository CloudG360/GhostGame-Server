package net.cg360.spookums.server.core.data.json;

import net.cg360.spookums.server.util.clean.Check;

public final class Json<T> {

    protected Json<? extends JsonHolder> parent;
    protected T value;

    public Json(T value) {
        this.parent = null;
        this.value = value;
    }


    /** @return true if the parent of the element was set. */
    protected boolean setParent(Json<JsonHolder> parent) {
        Check.nullParam(parent, "parent");

        this.parent.getValue().removeChild(this);
        if(parent.getValue().acceptNewChild(this)) {
            this.parent = parent;
            return true;
        }

        return false;
    }

    public boolean hasParent() { return parent != null; }

    public T getValue() {
        return value;
    }

    public static <T> Json<T> from(T val) {
        return new Json<>(val);
    }
}
