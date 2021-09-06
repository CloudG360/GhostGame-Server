package net.cg360.spookums.server.auth;

import net.cg360.spookums.server.Server;
import net.cg360.spookums.server.util.ErrorUtil;

import java.sql.*;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

public class AuthenticationManager {

    // SQLite DB Info:
    // User information is split into 2 tables:
    // identity - holds the username, password hashes, the time the account was created, and other user data
    // authentication - holds username, token, and expiry | Used to login users automatically without storing the actual password on the client.


    private static final String SQL_CREATE_IDENTITY_TABLE = "CREATE TABLE IF NOT EXISTS identity (username TEXT PRIMARY KEY, pwhash TEXT, salt TEXT, account_creation INTEGER);";

    // Integer supports longs.
    private static final String SQL_CREATE_AUTH_TABLE = "CREATE TABLE IF NOT EXISTS authentication (username TEXT PRIMARY KEY, token TEXT, expire INTEGER);";

    // Input in the current system millis
    private static final String SQL_CLEAR_OUTDATED_KEYS = "DELETE FROM authentication WHERE expire <= ?;";

    // 1 = username;   2, 4 = token;   3, 5 = expire;
    private static final String SQL_ASSIGN_TOKEN = "INSERT INTO authentication (username, token, expire) VALUES (?, ?, ?) ON CONFLICT(username) DO UPDATE SET token=?, expire=?;";

    // 1 = current time;    2 = username;
    private static final String SQL_EXPIRE_CHECK_TOKEN = "DELETE FROM authentication WHERE expire <= ? AND username = ?;";

    // 1 = username;
    private static final String SQL_FETCH_TOKEN = "SELECT username, token, expire FROM authentication WHERE username = ?;";


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

    private Connection getCoreConnection() {
        return Server.get().getDBManager().access("core");
    }



    public boolean createTables() {
        try {
            Connection connection = getCoreConnection();
            if(connection == null) return false;

            Statement s = connection.createStatement();
            s.addBatch(SQL_CREATE_IDENTITY_TABLE);
            s.addBatch(SQL_CREATE_AUTH_TABLE);
            s.executeBatch();

            ErrorUtil.quietlyClose(s);
            ErrorUtil.quietlyClose(connection);
            return true;

        } catch (Exception err) {
            err.printStackTrace();
        }

        return false;
    }


    // Ran at the start of the server, cleans up the database.
    public boolean deleteOutdatedTokens() {
        try {
            Connection connection = getCoreConnection();
            if(connection == null) return false;

            PreparedStatement s = connection.prepareStatement(SQL_CLEAR_OUTDATED_KEYS);
            s.setObject(1, System.currentTimeMillis());
            s.execute();

            ErrorUtil.quietlyClose(s);
            ErrorUtil.quietlyClose(connection);
            return true;

        } catch (SQLException err) {
            err.printStackTrace();
        }

        return false;
    }

    public boolean publishToken(String username, AuthToken authToken) {
        try {
            Connection connection = getCoreConnection();
            if(connection == null) return false;

            PreparedStatement s = connection.prepareStatement(SQL_ASSIGN_TOKEN);
            s.setObject(1, username);
            s.setObject(2, authToken.getAuthToken());
            s.setObject(3, authToken.getExpireTime());
            s.setObject(4, authToken.getAuthToken());
            s.setObject(5, authToken.getExpireTime());
            s.execute();

            ErrorUtil.quietlyClose(s);
            ErrorUtil.quietlyClose(connection);
            return true;

        } catch (SQLException err) {
            err.printStackTrace();
        }
        return false;
    }

    public Optional<AuthToken> fetchToken(String username, boolean clearIfExpired) {
        try {
            Connection connection = getCoreConnection();
            if(connection == null) return Optional.empty();

            if(clearIfExpired) {
                PreparedStatement c = connection.prepareStatement(SQL_EXPIRE_CHECK_TOKEN);
                c.setObject(1, System.currentTimeMillis());
                c.setObject(2, username);
                c.execute();
                ErrorUtil.quietlyClose(c);
            }

            PreparedStatement s = connection.prepareStatement(SQL_FETCH_TOKEN);
            s.setObject(1, username);
            Optional<AuthToken> a = processAuthTokenResults(s.executeQuery());

            ErrorUtil.quietlyClose(s);
            ErrorUtil.quietlyClose(connection);
            return a;

        } catch (SQLException err) {
            err.printStackTrace();
        }

        return Optional.empty();
    }



    protected static Optional<AuthToken> processAuthTokenResults(ResultSet set) {

        try {
            String token = set.getString("token");
            long expire = set.getLong("expire");

            if((token != null) && (expire != 0)) {
                return Optional.of(new AuthToken(token, expire));
            }

        } catch (SQLException ignored) { }

        return Optional.empty();
    }


    public static AuthenticationManager get() {
        return primaryInstance;
    }
}
