package net.cg360.spookums.server.core.data.json;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public final class JsonObject implements JsonHolder {

    protected BiMap<String, Json<?>> dictionary;
    protected BiMap<Json<?>, String> reverse;

    public JsonObject() {
        this.dictionary = HashBiMap.create();
        this.reverse = dictionary.inverse();
    }


    public Json<?> getChild(String key) {

    }

    @Override
    public boolean removeChild(Json<?> child) {
        if(reverse.containsKey(child)) {
            reverse.remove(child);
            return true;
        }

        return false;
    }
}
