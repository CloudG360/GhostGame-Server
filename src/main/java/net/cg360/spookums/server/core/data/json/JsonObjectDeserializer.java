package net.cg360.spookums.server.core.data.json;

import com.google.gson.JsonElement;

public abstract class JsonObjectDeserializer<T> {


    public abstract JsonSerializableObject<T> getObject(JsonElement objectRoot);

}
