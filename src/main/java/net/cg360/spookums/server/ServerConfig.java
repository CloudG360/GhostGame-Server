package net.cg360.spookums.server;

import net.cg360.spookums.server.core.data.LockableSettings;
import net.cg360.spookums.server.core.data.Settings;
import net.cg360.spookums.server.core.data.json.JsonObject;
import net.cg360.spookums.server.core.data.json.io.JsonIO;
import net.cg360.spookums.server.core.data.json.io.JsonUtil;
import net.cg360.spookums.server.core.data.json.io.error.JsonEmptyException;
import net.cg360.spookums.server.core.data.json.io.error.JsonFormatException;
import net.cg360.spookums.server.core.data.json.io.error.JsonParseException;
import net.cg360.spookums.server.core.data.keyvalue.Key;
import org.slf4j.Logger;

import java.io.*;

/**
 * The keys used within the server's main config
 */
public class ServerConfig {

    public static final Key<String> SERVER_IP = new Key<>("ip");
    public static final Key<Integer> SERVER_PORT = new Key<>("port");

    public static final Key<Boolean> LOG_UNSUPPORTED_PACKETS = new Key<>("log_unsupported_packets");
    public static final Key<Boolean> LOG_PACKET_IO = new Key<>("log_packet_io");
    public static final Key<Boolean> RUN_LAUNCH_TESTS = new Key<>("should_run_launch_tests");

    public static final Key<String> SERVER_NAME = new Key<>("name");
    public static final Key<String> DESCRIPTION = new Key<>("description");
    public static final Key<String> REGION = new Key<>("region");



    public static String DEFAULT_CONFIG =
            "{" + "\n" +
            String.format("     \"%s\": %s", SERVER_IP              .get(), "\"0.0.0.0\"") + "," + "\n" +
            String.format("     \"%s\": %s", SERVER_PORT            .get(), "22057") + "," + "\n" +

            String.format("     \"%s\": %s", LOG_UNSUPPORTED_PACKETS.get(), "true") + "," + "\n" +
            String.format("     \"%s\": %s", LOG_PACKET_IO          .get(), "false") + "," + "\n" +
            String.format("     \"%s\": %s", RUN_LAUNCH_TESTS       .get(), "false") + "," + "\n" +
            "" + "\n" +
            String.format("     \"%s\": %s", SERVER_NAME.get(), "\"Test Server\"") + "," + "\n" +
            String.format("     \"%s\": %s", DESCRIPTION.get(), "\"Unfinished business on an unfinished server. It'll be done soon! :)\"") + "," + "\n" +
            String.format("     \"%s\": %s", REGION     .get(), "\"en-gb\"") + " " + "\n" +
            "}";

    protected static int verifyAllKeys(Settings settings) {
        int replacements = 0;

        if(!checkSetting(settings, SERVER_IP, "0.0.0.0")) replacements++;
        if(!checkSetting(settings, SERVER_PORT, 22057)) replacements++;

        if(!checkSetting(settings, LOG_UNSUPPORTED_PACKETS, true)) replacements++;
        if(!checkSetting(settings, LOG_PACKET_IO, false)) replacements++;
        if(!checkSetting(settings, RUN_LAUNCH_TESTS, false)) replacements++;

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

        } catch (FileNotFoundException | JsonEmptyException err) {
            cfgLog.warn("No server configuration found! Creating a copy from default settings.");
            root = json.read(ServerConfig.DEFAULT_CONFIG);

            try {
                FileWriter writer = new FileWriter(cfgFile);
                BufferedWriter write = new BufferedWriter(writer);
                write.write(ServerConfig.DEFAULT_CONFIG);
                write.close();

            } catch (IOException err2) {
                cfgLog.error("Unable to write a new server configuration copy:");
                err2.printStackTrace();
            }

        } catch (JsonFormatException err) {
            cfgLog.error("Unable to parse json configuration! Using default settings: "+err.getMessage());
            root = json.read(ServerConfig.DEFAULT_CONFIG);

        } catch (JsonParseException err) {
            cfgLog.error("Unable to parse json configuration due to an internal error! Using default settings.");
            err.printStackTrace();
            root = json.read(ServerConfig.DEFAULT_CONFIG);
        }

        // Locking it as it really shouldn't be messed with.
        // If I add plugin support, I might change this ?   idk
        loadedSettings = JsonUtil.jsonToSettings(root, false);


        if(fillInDefaults) {
            cfgLog.warn("Filling in the blanks!");
            int replacements = verifyAllKeys(loadedSettings); //todo: print count of replacements

            cfgLog.warn("Using the defaults for "+replacements+" properties!");
        }

        cfgLog.warn("Loaded configuration!");
        return loadedSettings.lock();
    }

    // Just going to assume settings isn't null as this isn't
    // an important utility method.
    public static <T> boolean checkSetting(Settings settings, Key<T> key, T defaultValue) {
        if(settings.get(key) == null) {
            settings.set(key, defaultValue);
            return false; // invalid, replaced.
        }

        return true; // valid
    }
}
