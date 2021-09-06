package net.cg360.spookums.server.core.config;

import com.google.gson.*;
import net.cg360.spookums.server.Server;
import net.cg360.spookums.server.core.data.LockableSettings;
import net.cg360.spookums.server.core.data.Settings;
import net.cg360.spookums.server.core.data.json.JsonTypeRegistry;
import net.cg360.spookums.server.core.data.keyvalue.Key;
import net.cg360.spookums.server.util.clean.Check;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Iterator;
import java.util.Optional;

public class Configuration implements IConfiguration {

    protected File sourceFile;
    protected LockableSettings contents;

    protected boolean isInitialized;
    protected boolean modified;

    public Configuration(File source, Settings content) {
        this(source);
        this.contents = new LockableSettings(content, false);
    }

    public Configuration(File source) {
        Check.nullParam(source, "source");

        this.sourceFile = source;
        this.contents = new LockableSettings();

        this.isInitialized = false;
        this.modified = false;
    }

    /**
     * Loads the state of the source file into the object. Does not save
     * the current state of the object to a file prior to the call.
     *
     * @param shouldMerge should the data pulled from the file be merged with the current config,
     * @return true if the config was loaded, false if a source wasn't present or loaded incorectly.
     */
    public boolean loadSource(boolean shouldMerge) {
        // File exists
        if(sourceFile.exists() && sourceFile.isFile()) {

            try {
                FileReader reader = new FileReader(sourceFile);
                BufferedReader r = new BufferedReader(reader);
                Iterator<String> lines = r.lines().iterator();

                String jsonIn = "";
                while (lines.hasNext()) {
                    jsonIn = jsonIn.concat(lines.next());
                }

                JsonElement element = JsonParser.parseString(jsonIn);
                Optional<Settings> newSettings = JsonTypeRegistry.get().deserialize(element, Settings.class);

                if(newSettings.isPresent()) {
                    Settings source = newSettings.get();
                    boolean lockState = contents.isLocked();

                    if (!shouldMerge) {
                        this.contents = new LockableSettings();
                    } else {
                        this.contents = this.contents.getUnlockedCopy(); // Ensure unlocked.
                    }

                    for(Key<?> key : source.getKeys()) {

                    }

                } else {
                    Server.getMainLogger().warn("Unable to parse settings. The internal type parser must be broken :)");
                    return false;
                }




            } catch (FileNotFoundException errNotFound){
                return false;
            } catch (JsonSyntaxException | JsonIOException errJson) {
                Server.getMainLogger().error("Malformed Json in configuration source: "+errJson.getMessage());
                return false;
            }

        }
        return false;
    }

    public void saveConfiguration() {

    }

    @Override
    public <T> boolean write(Key<T> key, T value) {
        return false;
    }

    @Override
    public <T> T getOrElse(Key<T> key, T orElse) {
        return null;
    }

    @Override
    public boolean isEditEnabled() {
        return !contents.isLocked();
    }

    public boolean isModified() {
        return modified;
    }
}
