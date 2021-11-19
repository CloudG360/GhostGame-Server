package net.cg360.spookums.server.core.data.json.io;

import net.cg360.spookums.server.core.data.LockableSettings;
import net.cg360.spookums.server.core.data.Settings;
import net.cg360.spookums.server.core.data.json.Json;
import net.cg360.spookums.server.core.data.json.JsonArray;
import net.cg360.spookums.server.core.data.json.JsonHolder;
import net.cg360.spookums.server.core.data.json.JsonObject;
import net.cg360.spookums.server.core.data.json.io.error.ConfigFormatException;
import net.cg360.spookums.server.core.data.keyvalue.Key;
import net.cg360.spookums.server.util.clean.Check;

import java.util.IllegalFormatException;

public final class JsonUtil {

    // It is checked! Java is dumb! :)
    public static LockableSettings jsonToSettings(JsonObject parent, boolean isLocked) {
        Check.nullParam(parent, "parent");
        LockableSettings settings = new LockableSettings();

        for(String key : parent.getKeys()) {
            Json<?> element = parent.getChild(key);

            Check.nullParam(key, "key");
            Check.nullParam(element, "element{wrapper}");
            Check.nullParam(element.getValue(), "element");

            String fKey = key.trim().toLowerCase();
            if(fKey.length() == 0) throw new ConfigFormatException("Empty keys are not allowed");

            Object saveObj = element.getValue();

            if(saveObj instanceof JsonObject) {
                saveObj = jsonToSettings((JsonObject) saveObj, isLocked);

            // Json Array: Get the values in object[] form.
            } else if (saveObj instanceof JsonArray) {
                saveObj = ((JsonArray) saveObj).getChildrenValues();
            }

            settings.set(Key.of(fKey), saveObj);
        }

        return isLocked? settings.lock() : settings;
    }

    // public static Settings settingsToJson() {}     // Requires object attrib mapping.

}
