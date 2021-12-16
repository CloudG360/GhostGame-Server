package net.cg360.spookums.server;

import net.cg360.spookums.server.core.data.LockableSettings;
import net.cg360.spookums.server.core.data.Settings;
import net.cg360.spookums.server.core.data.json.JsonObject;
import net.cg360.spookums.server.core.data.json.io.JsonIO;
import net.cg360.spookums.server.core.data.json.io.JsonUtil;
import net.cg360.spookums.server.core.data.json.io.error.JsonEmptyException;
import net.cg360.spookums.server.core.data.json.io.error.JsonFormatException;
import net.cg360.spookums.server.core.data.json.io.error.JsonParseException;
import net.cg360.spookums.server.core.data.keyvalue.DefaultKey;
import org.slf4j.Logger;

import java.io.*;

/**
 * The DefaultKeys used within the server's main config
 */
public class ServerConfig {

    public static final DefaultKey<String> SERVER_IP = new DefaultKey<>("ip", "0.0.0.0");
    public static final DefaultKey<Integer> SERVER_PORT = new DefaultKey<>("port", 22057);
    public static final DefaultKey<Integer> CONNECTION_TIMEOUT = new DefaultKey<>("connection_timeout", 10000);

    public static final DefaultKey<Boolean> LOG_UNSUPPORTED_PACKETS = new DefaultKey<>("log_unsupported_packets", true);
    public static final DefaultKey<Boolean> LOG_PACKET_IO = new DefaultKey<>("log_packet_io", false);
    public static final DefaultKey<Boolean> RUN_LAUNCH_TESTS = new DefaultKey<>("should_run_launch_tests", false);

    public static final DefaultKey<String> SERVER_NAME = new DefaultKey<>("name", "Ghost Game Server");
    public static final DefaultKey<String> DESCRIPTION = new DefaultKey<>("description", "No description provided");
    public static final DefaultKey<String> REGION = new DefaultKey<>("region", "en-gb");

    public static final DefaultKey<Integer> MAX_GAME_QUEUE_LENGTH = new DefaultKey<>("max_game_queue_length",  1024);
    public static final DefaultKey<Integer> GAME_MIN_PLAYERS = new DefaultKey<>("game_min_players",  2);
    public static final DefaultKey<Integer> GAME_MAX_PLAYERS = new DefaultKey<>("game_max_players",  8);
    public static final DefaultKey<Integer> GAME_TIMER_LENGTH = new DefaultKey<>("game_timer_length",  300 * 20); // x20 as it's in ticks
    public static final DefaultKey<Integer> GAME_COUNTDOWN_LENGTH = new DefaultKey<>("game_countdown_length",  15 * 20);

    public static final DefaultKey<Long> AUTH_TOKEN_TIMEOUT = new DefaultKey<>("auth_token_timeout", 1000L * 60 * 60 * 24 * 30);


    public static String DEFAULT_CONFIG =
            "{" + "\n" +
                    "    " + formatLine(SERVER_IP) + "," + "\n" +
                    "    " + formatLine(SERVER_PORT) + "," + "\n" +
                    "    " + formatLine(CONNECTION_TIMEOUT) + "," + "\n" +

                    "    " + formatLine(LOG_UNSUPPORTED_PACKETS) + "," + "\n" +
                    "    " + formatLine(LOG_PACKET_IO) + "," + "\n" +
                    "    " + formatLine(RUN_LAUNCH_TESTS) + "," + "\n" +

                    "    " + formatLine(SERVER_NAME) + "," + "\n" +
                    "    " + formatLine(DESCRIPTION) + "," + "\n" +
                    "    " + formatLine(REGION) + "," + "\n" +

                    "    " + formatLine(MAX_GAME_QUEUE_LENGTH) + "," + "\n" +
                    "    " + formatLine(GAME_MIN_PLAYERS) + "," + "\n" +
                    "    " + formatLine(GAME_MAX_PLAYERS) + "," + "\n" +
                    "    " + formatLine(GAME_TIMER_LENGTH) + "," + "\n" +
                    "    " + formatLine(GAME_COUNTDOWN_LENGTH) + "," + "\n" +


                    "    " + formatLine(AUTH_TOKEN_TIMEOUT) + "\n" + // When extending, add comma!
            "}";

    protected static int verifyAllDefaultKeys(Settings settings) {
        int replacements = 0;

        if(isSettingNull(settings, SERVER_IP)) replacements++;
        if(isSettingNull(settings, SERVER_PORT)) replacements++;
        if(isSettingNull(settings, CONNECTION_TIMEOUT)) replacements++;

        if(isSettingNull(settings, LOG_UNSUPPORTED_PACKETS)) replacements++;
        if(isSettingNull(settings, LOG_PACKET_IO)) replacements++;
        if(isSettingNull(settings, RUN_LAUNCH_TESTS)) replacements++;

        if(isSettingNull(settings, SERVER_NAME)) replacements++;
        if(isSettingNull(settings, DESCRIPTION)) replacements++;
        if(isSettingNull(settings, REGION)) replacements++;

        if(isSettingNull(settings, MAX_GAME_QUEUE_LENGTH)) replacements++;
        if(isSettingNull(settings, GAME_MIN_PLAYERS)) replacements++;
        if(isSettingNull(settings, GAME_MAX_PLAYERS)) replacements++;
        if(isSettingNull(settings, GAME_TIMER_LENGTH)) replacements++;
        if(isSettingNull(settings, GAME_COUNTDOWN_LENGTH)) replacements++;

        if(isSettingNull(settings, AUTH_TOKEN_TIMEOUT)) replacements++;

        return replacements;
    }

    public static LockableSettings loadServerConfiguration(Server server, boolean fillInDefaults) {
        Logger cfgLog = Server.getLogger("Server/Config");
        cfgLog.info("Loading configuration...");

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
            cfgLog.info("Filling in the blanks!");
            int replacements = verifyAllDefaultKeys(loadedSettings); //todo: print count of replacements

            cfgLog.info("Using the defaults for "+replacements+" properties!");
        }

        cfgLog.info("Loaded configuration!");
        return loadedSettings.lock();
    }

    // Just going to assume settings isn't null as this isn't
    // an important utility method.
    public static <T> boolean isSettingNull(Settings settings, DefaultKey<T> key) {
        if(settings.get(key) == null) {
            settings.set(key, key.getDefaultValue());
            return true; // invalid, replaced.
        }

        return false; // valid
    }

    // A little method to cleanup the default config
    @SuppressWarnings("unchecked")
    public static <T> String formatLine(DefaultKey<T> key) {

        Object value = key.getDefaultValue();

        // String can't be extended, this is fine.
        if(key.getDefaultValue() instanceof String)
            value = "\"" + key.getDefaultValue() + "\"";

        return String.format("\"%s\": %s", key.get(), value);
    }
}
