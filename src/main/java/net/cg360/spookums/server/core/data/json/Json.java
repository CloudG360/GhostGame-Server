package net.cg360.spookums.server.core.data.json;

import net.cg360.spookums.server.util.clean.Check;

public final class Json<T> {

    protected Json<? extends JsonHolder> parent;
    protected T value;

    private Json(T value) {
        this.parent = null;
        this.value = value;
    }


    /** @return true if the parent of the element was set. */
    protected boolean setParent(Json<JsonHolder> parent) {
        Check.nullParam(parent, "parent");

        this.parent = parent;
        return false;
    }

    public boolean hasParent() { return parent != null; }

    public T getValue() {
        return value;
    }

    @SuppressWarnings("unchecked")
    public static <T> Json<T> from(T val) {
        Json<T> json = new Json<>(val);

        if(json.getValue() instanceof JsonContainerReachback) {
            Json<? extends JsonContainerReachback> j1 = (Json<? extends JsonContainerReachback>) json;

            ((JsonContainerReachback) val).setSelfContainer(j1);
        }

        return json;
    }


    @Override
    public String toString() {
        return "  Json: " +
                "hasParent=(" + hasParent() + ")" +
                ", value=(" + value + ")  ";
    }
}
