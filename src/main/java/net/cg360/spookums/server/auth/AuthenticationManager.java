package net.cg360.spookums.server.auth;

import net.cg360.spookums.server.Server;
import net.cg360.spookums.server.auth.record.AuthToken;
import net.cg360.spookums.server.auth.record.AuthenticatedIdentity;
import net.cg360.spookums.server.network.packet.auth.PacketInLogin;
import net.cg360.spookums.server.network.packet.auth.PacketOutLoginResponse;
import net.cg360.spookums.server.network.packet.generic.PacketInOutDisconnect;
import net.cg360.spookums.server.network.user.NetworkClient;
import net.cg360.spookums.server.util.SecretUtil;
import net.cg360.spookums.server.util.clean.ErrorUtil;
import net.cg360.spookums.server.util.clean.Pair;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class AuthenticationManager {

    // SQLite DB Info:
    // User information is split into 2 tables:
    // identity - holds the username, password hashes, the time the account was created, and other user data
    // authentication - holds username, token, and expiry | Used to login users automatically without storing the actual password on the client.

    private static final String SQL_CREATE_USER_LOOKUP_TABLE = "CREATE TABLE IF NOT EXISTS user_lookup (accountID TEXT PRIMARY KEY, username TEXT);";

    private static final String SQL_CREATE_IDENTITY_TABLE = "CREATE TABLE IF NOT EXISTS identity (accountID TEXT PRIMARY KEY, pwhash TEXT, salt TEXT, account_creation INTEGER);";

    // Integer supports longs.
    private static final String SQL_CREATE_AUTH_TABLE = "CREATE TABLE IF NOT EXISTS authentication (accountID TEXT PRIMARY KEY, token TEXT, expire INTEGER);";


    // -- AUTHENTICATION TABLE OPERATIONS

    // Input in the current system millis
    private static final String SQL_CLEAR_OUTDATED_KEYS = "DELETE FROM authentication WHERE expire <= ?;";

    // 1 = accountID;   2, 4 = token;   3, 5 = expire;
    private static final String SQL_ASSIGN_TOKEN = "INSERT INTO authentication (accountID, token, expire) VALUES (?, ?, ?) ON CONFLICT(accountID) DO UPDATE SET token=?, expire=?;";

    // 1 = current time;    2 = accountID;
    private static final String SQL_EXPIRE_CHECK_TOKEN = "DELETE FROM authentication WHERE expire <= ? AND accountID = ?;";

    // 1 = accountID;
    private static final String SQL_CLEAR_USER_TOKENS = "DELETE FROM authentication WHERE accountID <= ?;";

    // 1 = accountID;
    private static final String SQL_FETCH_TOKEN = "SELECT token, expire FROM authentication WHERE accountID = ?;";

    // 1 = accountID;
    private static final String SQL_FETCH_USERNAME = "SELECT accountID, expire FROM authentication WHERE token = ?;";


    // -- IDENTITY TABLE OPERATIONS

    //
    private static final String SQL_FETCH_PASS_AND_SALT = "SELECT pwhash, salt FROM identity WHERE accountID = ?;";




    private static AuthenticationManager primaryInstance;

    protected HashMap<UUID, AuthenticatedIdentity> activeNetIDIdentities;
    protected HashMap<String, AuthenticatedIdentity> activeUsernameIdentities;

    public AuthenticationManager() {
        this.activeNetIDIdentities = new HashMap<>();
        this.activeUsernameIdentities = new HashMap<>();
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
            s.addBatch(SQL_CREATE_USER_LOOKUP_TABLE);
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

    public Optional<Pair<String, Long>> fetchUsername(String token, boolean failIfExpired) {
        try {
            Connection connection = getCoreConnection();
            if(connection == null) return Optional.empty();

            PreparedStatement s = connection.prepareStatement(SQL_FETCH_USERNAME);
            s.setObject(1, token);
            Optional<Pair<String, Long>> a = processAuthUserResults(s.executeQuery(), failIfExpired);

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
                String token = login.getToken();
                Optional<Pair<String, Long>> u = this.fetchUsername(login.getToken(), true);

                u.ifPresent(i -> {
                    Pair<String, Long> loginPair = u.get();
                    String username = loginPair.getFirst();
                    long expire = loginPair.getSecond();
                    AuthenticatedIdentity identity = new AuthenticatedIdentity(client, username, new AuthToken(token, expire));

                    // The user is already logged into the server.
                    if(isIdentityLoaded(identity)) {
                        Server.get().getNetworkInterface().disconnectClient(client.getID(), new PacketInOutDisconnect("Your account is already logged onto the server!"));
                        return;
                    }

                    addAuthIdentity(identity);
                    client.send(new PacketOutLoginResponse()
                                    .setUsername(username)
                                    .setToken(token)
                                    .setSuccessful(true),
                            true);
                });

                // If a user + pass is submitted, verify it matches and then reassign their token.
            } else if (login.getUsername() != null && login.getPassword() != null) {


                if(correctCredentials) {
                    String username = login.getUsername();
                    AuthToken token = AuthToken.generateToken();
                    this.deleteAllUsernameTokens(username);
                    this.publishToken(username, token);
                    AuthenticatedIdentity identity = new AuthenticatedIdentity(client, username, token);

                    // The user is already logged into the server.
                    if(isIdentityLoaded(identity)) {
                        Server.get().getNetworkInterface().disconnectClient(client.getID(), new PacketInOutDisconnect("Your account is already logged onto the server!"));
                        return;
                    }

                    addAuthIdentity(identity);
                    client.send(new PacketOutLoginResponse()
                                    .setUsername(username)
                                    .setToken(token.getAuthToken())
                                    .setSuccessful(true),
                            true);
                }
            }
        }).start();
    }


    protected boolean isIdentityLoaded(AuthenticatedIdentity identity) {
        return activeNetIDIdentities.containsKey(identity.getClient().getID()) || activeUsernameIdentities.containsKey(identity.getUsername());
    }

    protected boolean addAuthIdentity(AuthenticatedIdentity identity) {
        if(!isIdentityLoaded(identity)) {
            this.activeNetIDIdentities.put(identity.getClient().getID(), identity);
            this.activeUsernameIdentities.put(identity.getUsername(), identity);
            return true;
        }
        return false;
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

    protected static Optional<Pair<String, Long>> processAuthUserResults(ResultSet set, boolean failIfExpired) {
        try {
            if(set.next()) {
                String username = set.getString("username");
                long expire = set.getLong("expire");

                if (username != null) {
                    if(failIfExpired && (System.currentTimeMillis() > expire)) return Optional.empty();
                    return Optional.of(Pair.of(username, expire));
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
