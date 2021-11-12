package net.cg360.spookums.server.core.data.json;

import net.cg360.spookums.server.util.clean.Check;

import java.util.ArrayList;
import java.util.Optional;

public class JsonArray implements JsonHolder {

    protected ArrayList<Json<?>> children;

    /**
     * Fetches a child from the list at a given index
     * @param index the index to draw from
     * @return the child, if found.
     */
    public Optional<Json<?>> getChild(int index) {
        Check.inclusiveBounds(index, 0, children.size() - 1, "index");
        return Optional.ofNullable(children.get(index));
    }

    /**
     * Fetched a child from the list under the condition that it matches
     * the specified type.
     * @return the child
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<Json<T>> getChild(int index, Class<T> type) {
        Optional<Json<?>> ret = getChild(index);

        if(ret.isPresent()) {
            Json<?> child = ret.get();
            if(type.isInstance(child.getValue())) {
                return Optional.of((Json<T>) child);
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean removeChild(Json<?> child) {
        Check.nullParam(child, "child");
        return children.remove(child);
    }

    @Override
    public boolean acceptNewChild(Json<?> child) {
        Check.nullParam(child, "child");
        children.add(child);
        return true;
    }

    @Override
    public boolean hasChildren() {
        return children.size() > 0;
    }
}
