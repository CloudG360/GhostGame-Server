package net.cg360.spookums.server.core.data.json;

import com.google.gson.JsonElement;

import java.util.Optional;

public abstract class JsonSerializableObject<T> {

    protected T wrappedObject;

    public JsonSerializableObject(T wrappedObject) {
        this.wrappedObject = wrappedObject;
    }

    public abstract Optional<JsonElement> serializeWrappedObject();


    public T getWrappedObject() {
        return wrappedObject;
    }
}
