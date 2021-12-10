package net.cg360.spookums.server.core.data.keyvalue;

import net.cg360.spookums.server.core.data.id.Identifier;
import net.cg360.spookums.server.util.clean.Check;

import java.util.Objects;

public final class DefaultKey<T> extends Key<T> {

    private final T defaultValue;

    public DefaultKey(Identifier key, T defaultValue) {
        super(key);
        Check.nullParam(defaultValue, "defaultValue");
        this.defaultValue = defaultValue;
    }

    public DefaultKey(String key, T defaultValue) {
        super(key);
        Check.nullParam(defaultValue, "defaultValue");
        this.defaultValue = defaultValue;
    }

    public Key<T> toKey() {
        return new Key<T>(this.get());
    }

    public T getDefaultValue() {
        return defaultValue;
    }
}
