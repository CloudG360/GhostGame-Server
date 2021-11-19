package net.cg360.spookums.server;

import net.cg360.spookums.server.core.data.LockableSettings;
import net.cg360.spookums.server.core.data.Settings;
import net.cg360.spookums.server.core.data.json.JsonObject;
import net.cg360.spookums.server.core.data.json.io.JsonIO;
import net.cg360.spookums.server.core.data.json.io.JsonUtil;
import net.cg360.spookums.server.core.data.json.io.error.JsonFormatException;
import net.cg360.spookums.server.core.data.json.io.error.JsonParseException;
import net.cg360.spookums.server.core.data.keyvalue.Key;
import org.slf4j.Logger;

import java.io.*;

/**
 * The keys used within the server's main config
 */
public class ServerConfig {

    public static final Key<Boolean> LOG_UNSUPPORTED_PACKETS = new Key<>("log_unsupported_packets");
    public static final Key<Boolean> LOG_PACKET_IO = new Key<>("log_packet_io");
    public static final Key<Boolean> RUN_TESTS = new Key<>("run_tests");

    public static final Key<String> SERVER_NAME = new Key<>("name");
    public static final Key<String> DESCRIPTION = new Key<>("description");
    public static final Key<String> REGION = new Key<>("region");



    public static String DEFAULT_CONFIG =
            "{" + "\n" +
                    String.format("'%s': %s", LOG_UNSUPPORTED_PACKETS.get(), "true") + "," + "\n" +
                    String.format("'%s': %s", LOG_PACKET_IO          .get(), "true") + "," + "\n" +
                    String.format("'%s': %s", RUN_TESTS              .get(), "true") + "," + "\n" +
                    "" + "\n" +
                    String.format("'%s': %s", SERVER_NAME.get(), "true") + "," + "\n" +
                    String.format("'%s': %s", DESCRIPTION.get(), "true") + "," + "\n" +
                    String.format("'%s': %s", REGION     .get(), "true") + " " + "\n" +
            "}";

    protected static int verifyAllKeys(Settings settings) {
        int replacements = 0;

        if(!checkSetting(settings, LOG_UNSUPPORTED_PACKETS, true)) replacements++;
        if(!checkSetting(settings, LOG_PACKET_IO, true)) replacements++;
        if(!checkSetting(settings, RUN_TESTS, true)) replacements++;

        if(!checkSetting(settings, SERVER_NAME, "Test Server")) replacements++;
        if(!checkSetting(settings, DESCRIPTION, "Unfinished business on an unfinished server. It'll be done soon! :)")) replacements++;
        if(!checkSetting(settings, REGION, "en-gb")) replacements++;

        return replacements;
    }

    public static LockableSettings loadServerConfiguration(Server server, boolean fillInDefaults) {
        Logger cfgLog = Server.getLogger("Server/Config");
        cfgLog.warn("Loading configuration...");

        File cfgFile = new File(server.getDataPath(), "server_config.json");
        JsonIO json = new JsonIO();
        LockableSettings loadedSettings;
        JsonObject root;

        try {
            root = json.read(cfgFile);

        } catch (FileNotFoundException err) {
            cfgLog.warn("No server configuration found! Creating a copy from default settings.");
            root = json.read(ServerConfig.DEFAULT_CONFIG);

            try {
                FileWriter writer = new FileWriter(cfgFile);
                BufferedWriter write = new BufferedWriter(writer);
                write.write(ServerConfig.DEFAULT_CONFIG);
            } catch (IOException err2) {
                cfgLog.error("Unable to write a new server configuration copy:");
                err2.printStackTrace();
            }

        } catch (JsonFormatException | JsonParseException err) {
            cfgLog.error("Unable to parse json configuration! Using default settings.");
            err.printStackTrace();
            root = json.read(ServerConfig.DEFAULT_CONFIG);
        }

        // Locking it as it really shouldn't be messed with.
        // If I add plugin support, I might change this ?   idk
        loadedSettings = JsonUtil.jsonToSettings(root, false);

        cfgLog.warn("Filling in the blanks!");
        if(fillInDefaults) verifyAllKeys(loadedSettings);

        cfgLog.warn("Loaded configuration!");
        return loadedSettings.lock();
    }

    // Just going to assume settings isn't null as this isn't
    // an important utility method.
    public static <T> boolean checkSetting(Settings settings, Key<T> key, T type) {
        if(settings.get(key) == null) {
            settings.set(key, type);
            return false; // invalid, replaced.
        }

        return true; // valid
    }
}
