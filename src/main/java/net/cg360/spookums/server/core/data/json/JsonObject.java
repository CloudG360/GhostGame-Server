package net.cg360.spookums.server.core.data.json;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import net.cg360.spookums.server.util.clean.Check;

public final class JsonObject implements JsonHolder {

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
        if(reverse.containsKey(child)) {
            reverse.remove(child);
            return true;
        }

        return false;
    }

    @Override
    public boolean acceptNewChild(Json<?> child) {
        return false;
    }

    @Override
    public boolean hasChildren() {
        return false;
    }
}
