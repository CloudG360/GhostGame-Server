package net.cg360.spookums.server.core.data.keyvalue;

import net.cg360.spookums.server.util.Check;
import net.cg360.spookums.server.core.data.id.Identifier;

import java.util.Objects;

public final class Key<T> {

    private String key;

    public Key(Identifier key) {
        Check.nullParam(key, "Key Identifier");
        this.key = key.getID();
    }

    public Key(String key) {
        Check.nullParam(key, "Key String");
        String modifiedKey = key.trim().toLowerCase();

        if(modifiedKey.length() == 0) throw new IllegalArgumentException("Key cannot be made of only whitespace.");
        this.key = modifiedKey;
    }

    public String get() {
        return key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Key<?> key1 = (Key<?>) o;
        return Objects.equals(key, key1.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }
}
