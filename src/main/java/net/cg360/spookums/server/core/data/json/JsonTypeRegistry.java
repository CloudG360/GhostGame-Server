package net.cg360.spookums.server.core.data.json;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.cg360.spookums.server.Server;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Optional;

public class JsonTypeRegistry {

    private static JsonTypeRegistry primaryRegistry;

    protected HashMap<Class<?>, Class<JsonSerializableObject<?>>> serializers;
    protected HashMap<Class<?>, JsonObjectDeserializer<?>> deserializers;

    public JsonTypeRegistry() {
        this.serializers = new HashMap<>();
        this.deserializers = new HashMap<>();
    }

    public boolean setAsPrimaryInstance() {
        if(primaryRegistry == null) {
            primaryRegistry = this;
            return true;
        }
        return false;
    }

    public <T> JsonElement serialize(T object) {
        Optional<JsonSerializableObject<T>> wrapper = wrapWithSerializer(object);
        if(wrapper.isPresent()) {
            JsonSerializableObject<T> wrap = wrapper.get();
            Optional<JsonElement> constructed = wrap.serializeWrappedObject();
            return constructed.orElseGet(JsonObject::new);
        }
        return new JsonObject();
    }

    public <T> Optional<T> deserialize(JsonElement data, Class<T> expectedType) {
        Optional<JsonObjectDeserializer<T>> wrapper = wrapWithDeserializer(expectedType);
        if(wrapper.isPresent()) {
            JsonObjectDeserializer<T> wrap = wrapper.get();
            T constructed = wrap.getObject(data).getWrappedObject();
            return Optional.of(constructed);
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<JsonSerializableObject<T>> wrapWithSerializer(T object) {
        Class<T> type = (Class<T>) object.getClass();
        if(this.serializers.containsKey(type)) {
            Class<JsonSerializableObject<?>> t = this.serializers.get(type);

            try {
                Constructor<JsonSerializableObject<?>> c = t.getConstructor(type);
                return Optional.of((JsonSerializableObject<T>) c.newInstance(object));

            } catch (Exception err) {
                Server.getMainLogger().warn("Error creating serializer. Broken hm?");
            }
        }

        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<JsonObjectDeserializer<T>> wrapWithDeserializer(Class<T> expectedType) {
        if(this.deserializers.containsKey(expectedType)) {
            JsonObjectDeserializer<T> deserializer = (JsonObjectDeserializer<T>) this.deserializers.get(expectedType);
            return Optional.of(deserializer);
        }
        return Optional.empty();
    }




    public static JsonTypeRegistry get() {
        return primaryRegistry;
    }
}
