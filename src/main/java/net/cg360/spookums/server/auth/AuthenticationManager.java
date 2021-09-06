package net.cg360.spookums.server.auth;

import net.cg360.spookums.server.Server;
import net.cg360.spookums.server.auth.record.AuthToken;
import net.cg360.spookums.server.auth.record.AuthenticatedIdentity;
import net.cg360.spookums.server.network.packet.auth.PacketInLogin;
import net.cg360.spookums.server.network.packet.auth.PacketOutLoginResponse;
import net.cg360.spookums.server.network.user.NetworkClient;
import net.cg360.spookums.server.util.clean.ErrorUtil;

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
    private static final String SQL_CLEAR_USER_TOKENS = "DELETE FROM authentication WHERE username <= ?;";

    // 1 = username;
    private static final String SQL_FETCH_TOKEN = "SELECT token, expire FROM authentication WHERE username = ?;";

    // 1 = token;
    private static final String SQL_FETCH_USERNAME = "SELECT username, expire FROM authentication WHERE token = ?;";



    private static AuthenticationManager primaryInstance;

    protected HashMap<UUID, AuthenticatedIdentity> activeIdentities;

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

    public boolean deleteAllUsernameTokens(String username) {
        try {
            Connection connection = getCoreConnection();
            if(connection == null) return false;

            PreparedStatement s = connection.prepareStatement(SQL_CLEAR_USER_TOKENS);
            s.setObject(1, username);
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

    public Optional<AuthToken> fetchToken(String token, boolean clearIfExpired) {
        try {
            Connection connection = getCoreConnection();
            if(connection == null) return Optional.empty();

            if(clearIfExpired) {
                PreparedStatement c = connection.prepareStatement(SQL_EXPIRE_CHECK_TOKEN);
                c.setObject(1, System.currentTimeMillis());
                c.setObject(2, token);
                c.execute();
                ErrorUtil.quietlyClose(c);
            }

            PreparedStatement s = connection.prepareStatement(SQL_FETCH_TOKEN);
            s.setObject(1, token);
            Optional<AuthToken> a = processAuthTokenResults(s.executeQuery());

            ErrorUtil.quietlyClose(s);
            ErrorUtil.quietlyClose(connection);
            return a;

        } catch (SQLException err) {
            err.printStackTrace();
        }

        return Optional.empty();
    }

    public Optional<String> fetchUsername(String token, boolean failIfExpired) {
        try {
            Connection connection = getCoreConnection();
            if(connection == null) return Optional.empty();

            PreparedStatement s = connection.prepareStatement(SQL_FETCH_USERNAME);
            s.setObject(1, token);
            Optional<String> a = processAuthUserResults(s.executeQuery(), failIfExpired);

            ErrorUtil.quietlyClose(s);
            ErrorUtil.quietlyClose(connection);
            return a;

        } catch (SQLException err) {
            err.printStackTrace();
        }

        return Optional.empty();
    }

    public void processLoginPacket(PacketInLogin login, NetworkClient client) {
        new Thread(() -> {
            // If a token was submitted, use that as a login.
            if (login.getToken() != null) {
                Optional<String> u = this.fetchUsername(login.getToken(), true);
                u.ifPresent(s -> {
                    client.send(new PacketOutLoginResponse()
                                    .setUsername(s)
                                    .setToken(login.getToken())
                                    .setSuccessful(true),
                            true);
                });

                // If a user + pass is submitted, verify it matches and then reassign their token.
            } else if (login.getUsername() != null && login.getPassword() != null) {
                boolean correctCredentials = true;

                if(correctCredentials) {
                    AuthToken token = AuthToken.generateToken();
                    this.deleteAllUsernameTokens(login.getUsername());
                    this.publishToken(login.getUsername(), token);
                    client.send(new PacketOutLoginResponse()
                                    .setUsername(login.getUsername())
                                    .setToken(token.getAuthToken())
                                    .setSuccessful(true),
                            true);
                }
            }
        }).start();
    }


    public HashMap<UUID, AuthenticatedIdentity> getActiveIdentities() {
        return activeIdentities;
    }

    protected static Optional<AuthToken> processAuthTokenResults(ResultSet set) {

        try {
            if(set.next()) {
                String token = set.getString("token");
                long expire = set.getLong("expire");

                if ((token != null) && (expire != 0)) {
                    return Optional.of(new AuthToken(token, expire));
                }
            }
        } catch (SQLException ignored) { }

        return Optional.empty();
    }

    protected static Optional<String> processAuthUserResults(ResultSet set, boolean failIfExpired) {
        try {
            if(set.next()) {
                String username = set.getString("username");
                long expire = set.getLong("expire");

                if (username != null) {
                    if(failIfExpired && (System.currentTimeMillis() > expire)) return Optional.empty();
                    return Optional.of(username);
                }
            }

        } catch (SQLException ignored) { }

        return Optional.empty();
    }

    protected static Connection getCoreConnection() {
        return Server.get().getDBManager().access("core");
    }


    public static AuthenticationManager get() {
        return primaryInstance;
    }
}
