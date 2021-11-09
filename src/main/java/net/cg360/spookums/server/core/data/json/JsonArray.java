package net.cg360.spookums.server.core.data.json;

import java.util.ArrayList;

public class JsonArray implements JsonHolder {

    protected ArrayList<Json<?>> children;

    @Override
    public boolean removeChild(Json<?> child) {
        children.remove(child);
        return false;
    }

    @Override
    public boolean acceptNewChild(Json<?> child) {
        if()
        return false;
    }

    @Override
    public boolean hasChildren() {
        return false;
    }
}
