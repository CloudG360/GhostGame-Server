package net.cg360.spookums.server.core.data;

import net.cg360.spookums.server.core.data.keyvalue.Key;
import net.cg360.spookums.server.core.data.keyvalue.Value;

import java.util.HashMap;
import java.util.Map;

/**
 * A way of storing settings/properties with
 * the option to lock them.
 *
 * Uses String keys rather than Identifier keys.
 */
public class Settings {

    protected Map<Key<?>, Value<?>> dataMap;


    public Settings () {
        this.dataMap = new HashMap<>();
    }

    /** Used to duplicate a Settings instance.*/
    protected Settings(Settings duplicate) {
        this.dataMap = new HashMap<>(duplicate.dataMap);
    }



    /** Sets a key within the settings if they are unlocked. */
    public <T> Settings set(Key<T> key, T value) {
        dataMap.put(key, new Value<>(value));
        return this;
    }


    /**
     * Returns a property with the same type as the key. If not
     * present, the object from the 2nd parameter is returned.
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrElse(Key<T> id, T orElse) {
        Value<?> v = dataMap.get(id);
        return v == null ? orElse : ((Value<T>) v).get();
    }

    /**
     * Returns a property with the same type as the key. If not
     * present, null is returned.
     */
    public <T> T get(Key<T> id) {
        return getOrElse(id, null);
    }

    /** @return a copy of these settings which is unlocked. */
    public Settings getCopy() {
        return new Settings(this);
    }

    public Key<?>[] getKeys(){
        return dataMap.keySet().toArray(new Key<?>[0]);
    }

}
