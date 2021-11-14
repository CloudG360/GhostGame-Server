package net.cg360.spookums.server.core.data.json;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import net.cg360.spookums.server.util.clean.Check;

public final class JsonObject extends JsonContainerReachback implements JsonHolder  {

    protected BiMap<String, Json<?>> dictionary;
    protected BiMap<Json<?>, String> reverse;

    public JsonObject() {
        this.dictionary = HashBiMap.create();
        this.reverse = dictionary.inverse();
    }


    public Json<?> getChild(String key) {
        Check.nullParam(key, "key");
        if(key.trim().length() == 0) throw new IllegalArgumentException("key must not be empty");

        return dictionary.get(key.trim());
    }

    @Override
    public boolean removeChild(Json<?> child) {
        Check.nullParam(child, "child");

        if(reverse.containsKey(child)) {
            if(child.parent == this.getSelf()) child.parent = null;
            reverse.remove(child);
            return true;
        }

        return false;
    }

    public boolean removeChild(String name) {
        Check.nullParam(name, "name");

        if(dictionary.containsKey(name)) {
            Json<?> child = dictionary.remove(name);
            if(child.parent == this.getSelf()) child.parent = null;
            return true;
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    public boolean addChild(String name, Json<?> child) {
        Check.nullParam(name, "name");
        Check.nullParam(child, "child");

        if(this.dictionary.containsKey(name)) removeChild(name);
        this.dictionary.put(name, child);

        if(!(this.getSelf().getValue() instanceof JsonObject)) throw new IllegalStateException("Json array has none-array container");
        child.parent = (Json<? extends JsonObject>) this.getSelf();

        return false;
    }

    @Override
    public boolean hasChildren() {
        return false;
    }
}
