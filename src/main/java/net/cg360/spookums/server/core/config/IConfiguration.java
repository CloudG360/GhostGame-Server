package net.cg360.spookums.server.core.config;

import net.cg360.spookums.server.core.data.keyvalue.Key;

public interface IConfiguration {

    <T> boolean write(Key<T> key, T value);
    <T> T getOrElse(Key<T> key, T orElse);
    default <T> T get(Key<T> key) {
        return getOrElse(key, null);
    }

    boolean isEditEnabled();

}
