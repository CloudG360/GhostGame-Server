package net.cg360.spookums.server.auth;

import net.cg360.spookums.server.Server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

public class AuthenticationManager {

    // SQLite DB Info:
    // User information is split into 2 tables:
    // identity - holds the username, password hashes, the time the account was created, and other user data
    // authentication - holds username, token, and expirey | Used to login users automatically without storing the actual password on the client.


    private static final String SQL_CREATE_IDENTITY_TABLE = "CREATE TABLE IF NOT EXISTS identity (username TEXT PRIMARY KEY, pwhash TEXT, salt TEXT, account_creation INTEGER);";

    // Integer supports longs.
    private static final String SQL_CREATE_AUTH_TABLE = "CREATE TABLE IF NOT EXISTS authentication (username TEXT PRIMARY KEY, token TEXT, expire INTEGER);";

    // Input in the current system millis
    private static final String SQL_CLEAR_OUTDATED_KEYS = "DELETE FROM authentication WHERE expire <= ?;";

    // 1 = uuid;   2, 4 == token;   3, 5 = expire
    private static final String SQL_ASSIGN_TOKEN = "INSERT INTO authentication (username, token, expire) VALUES (?, ?, ?) ON CONFLICT(username) DO UPDATE SET token=?, expire=?;";

    // 1 = uuid;   2, 4 == token;   3, 5 = expire
    private static final String SQL_FETCH_TOKEN = "SELECT ();";


    private static AuthenticationManager primaryInstance;

    protected HashMap<UUID, UserIdentity> activeIdentities;


    public AuthenticationManager() {
        this.activeIdentities = new HashMap<>();
    }

    public boolean setAsPrimaryInstance(){
        if(primaryInstance == null) {
            primaryInstance = this;
            return true;
        }
        return false;
    }



    public void createTables() {
        try {
            Server.get().getDBManager().access("core").prepareStatement(SQL_CREATE_IDENTITY_TABLE);
            Server.get().getDBManager().access("core").prepareStatement(SQL_CREATE_AUTH_TABLE);
        } catch (SQLException err) {
            err.printStackTrace();
        }
    }


    // Ran at the start of the server, cleans up the database.
    public void deleteOutdatedForeignTokens() {
        try {
            Server.get().getDBManager().access("core").prepareStatement(SQL_CLEAR_OUTDATED_KEYS).setObject(1, System.currentTimeMillis());
        } catch (SQLException err) {
            err.printStackTrace();
        }
    }

    // NOTE TO SELF: Owner is NOT the socket UUID.
    public void publishToken(String username, AuthToken authToken) {
        try {
            PreparedStatement s = Server.get().getDBManager().access("core").prepareStatement(SQL_ASSIGN_TOKEN);
            s.setObject(1, username);
            s.setObject(2, authToken.getAuthToken());
            s.setObject(3, authToken.getExpireTime());
            s.setObject(4, authToken.getAuthToken());
            s.setObject(5, authToken.getExpireTime());

        } catch (SQLException err) {
            err.printStackTrace();
        }
    }

    public Optional<AuthToken> fetchToken(String username) {

    }


    public static AuthenticationManager get() {
        return primaryInstance;
    }
}
