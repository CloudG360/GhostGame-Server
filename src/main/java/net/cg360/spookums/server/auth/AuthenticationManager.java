package net.cg360.spookums.server.auth;

import net.cg360.spookums.server.Server;
import net.cg360.spookums.server.auth.record.AuthToken;
import net.cg360.spookums.server.auth.record.AuthenticatedIdentity;
import net.cg360.spookums.server.auth.record.SecureIdentity;
import net.cg360.spookums.server.auth.state.AccountCreateState;
import net.cg360.spookums.server.core.event.handler.EventHandler;
import net.cg360.spookums.server.core.event.type.network.ClientSocketStatusEvent;
import net.cg360.spookums.server.network.packet.auth.PacketInLogin;
import net.cg360.spookums.server.network.packet.auth.PacketOutLoginResponse;
import net.cg360.spookums.server.network.packet.generic.PacketInOutDisconnect;
import net.cg360.spookums.server.network.user.NetworkClient;
import net.cg360.spookums.server.util.SecretUtil;
import net.cg360.spookums.server.util.clean.ErrorUtil;
import net.cg360.spookums.server.util.clean.Pair;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

public class AuthenticationManager {

    // SQLite DB Info:
    // User information is split into 2 tables:
    // identity - holds the username, password hashes, the time the account was created, and other user data
    // authentication - holds username, token, and expiry | Used to login users automatically without storing the actual password on the client.

    private static final String SQL_CREATE_USER_LOOKUP_TABLE = "CREATE TABLE IF NOT EXISTS user_lookup (accountID TEXT, username TEXT, PRIMARY KEY (accountID, username));";

    private static final String SQL_CREATE_IDENTITY_TABLE = "CREATE TABLE IF NOT EXISTS identity (accountID TEXT PRIMARY KEY, pwhash TEXT, salt TEXT, account_creation INTEGER);";

    // Integer supports longs.
    private static final String SQL_CREATE_AUTH_TABLE = "CREATE TABLE IF NOT EXISTS authentication (accountID TEXT PRIMARY KEY, token TEXT, expire INTEGER);";


    // -- IDENTITY / LOOKUP OPERATIONS

    private static final String SQL_FETCH_USERNAME_IDENTITY = "SELECT identity.accountID, identity.pwhash, identity.salt, identity.account_creation FROM identity INNER JOIN user_lookup on identity.accountID = user_lookup.accountID WHERE user_lookup.username = ?;";

    private static final String SQL_ASSIGN_NEW_IDENTITY = "INSERT INTO identity (accountID, pwhash, salt, account_creation) VALUES (?, ?, ?, ?);";

    private static final String SQL_ASSIGN_NEW_LOOKUP = "INSERT INTO user_lookup (accountID, username) VALUES (?, ?);";


    // -- AUTHENTICATION TABLE OPERATIONS

    // Input in the current system millis
    private static final String SQL_CLEAR_OUTDATED_TOKENS = "DELETE FROM authentication WHERE expire <= ?;";

    // 1 = accountID;   2, 4 = token;   3, 5 = expire;
    private static final String SQL_ASSIGN_TOKEN = "INSERT INTO authentication (accountID, token, expire) VALUES (?, ?, ?) ON CONFLICT(accountID) DO UPDATE SET token=?, expire=?;";

    // 1 = accountID; SQLite doesn't support INNER JOIN in delete statements so I have to use IN! :)
    private static final String SQL_CLEAR_USER_TOKENS = "DELETE FROM authentication WHERE authentication.accountID IN (SELECT user_lookup.accountID FROM user_lookup WHERE user_lookup.username = ?);";

    // 1 = username;
    private static final String SQL_FETCH_TOKEN = "SELECT authentication.token, authentication.expire FROM authentication INNER JOIN user_lookup ON user_lookup.accountID = authentication.accountID WHERE user_lookup.username = ?;";

    // 1 = accountID;
    private static final String SQL_FETCH_USERNAME = "SELECT user_lookup.username, authentication.expire FROM authentication INNER JOIN user_lookup on authentication.accountID = user_lookup.accountID WHERE authentication.token = ?;";



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

            PreparedStatement s = connection.prepareStatement(SQL_CLEAR_OUTDATED_TOKENS);
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



    public Pair<AccountCreateState, AuthToken> createNewIdentity(String username, String password) {
        try {
            // Check an account doesn't already exist.
            if(fetchSecureIdentity(username).isPresent()) return Pair.of(AccountCreateState.TAKEN, null);

            Connection connection = getCoreConnection();
            if(connection == null) Pair.of(AccountCreateState.DB_OFFLINE, null);

            String newAccountID = UUID.randomUUID().toString().toLowerCase(Locale.ROOT);
            SecretUtil.SaltyHash hashedPassword = SecretUtil.createSHA256Hash(password);
            String hashPw = hashedPassword.getHash();
            String hashSalt = hashedPassword.getSaltString().orElse("");

            //TODO: Create Identity record, and create a token
            PreparedStatement s = connection.prepareStatement(SQL_ASSIGN_NEW_LOOKUP);
            s.setObject(1, newAccountID);
            s.setObject(2, username);
            s.execute();
            ErrorUtil.quietlyClose(s);


            PreparedStatement sIdentity = connection.prepareStatement(SQL_ASSIGN_NEW_IDENTITY);
            sIdentity.setObject(1, newAccountID);
            sIdentity.setObject(2, hashPw);
            sIdentity.setObject(3, hashSalt);
            sIdentity.setObject(4, System.currentTimeMillis());
            sIdentity.execute();
            ErrorUtil.quietlyClose(sIdentity);
            ErrorUtil.quietlyClose(connection);

            AuthToken token = AuthToken.generateToken();
            publishToken(username, token);

            return Pair.of(AccountCreateState.CREATED, token);


        } catch (SQLException err) {
            err.printStackTrace();
            return Pair.of(AccountCreateState.ERRORED, null);
        }
    }

    public boolean publishToken(SecureIdentity identity, AuthToken token) {
        return publishTokenWithAccountID(identity.getAccountID(), token);
    }

    public boolean publishToken(String username, AuthToken token) {
        Optional<SecureIdentity> i =  fetchSecureIdentity(username);
        if(!i.isPresent()) return false;

        String accountID = i.get().getAccountID();
        return publishTokenWithAccountID(accountID, token);
    }

    protected boolean publishTokenWithAccountID(String accountID, AuthToken authToken) {
        try {
            Connection connection = getCoreConnection();
            if(connection == null) return false;

            String token = authToken.getAuthToken();
            long expire = authToken.getExpireTime();

            PreparedStatement s = connection.prepareStatement(SQL_ASSIGN_TOKEN);
            s.setObject(1, accountID);
            s.setObject(2, token);
            s.setObject(3, expire);
            s.setObject(4, token);
            s.setObject(5, expire);
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
                PreparedStatement c = connection.prepareStatement(SQL_CLEAR_OUTDATED_TOKENS);
                c.setObject(1, System.currentTimeMillis());
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

    public Optional<SecureIdentity> fetchSecureIdentity(String username) {
        try {
            Connection connection = getCoreConnection();
            if(connection == null) return Optional.empty();

            PreparedStatement s = connection.prepareStatement(SQL_FETCH_USERNAME_IDENTITY);
            s.setObject(1, username);
            Optional<SecureIdentity> a = processAuthIdentityResults(s.executeQuery());

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
                                    .setStatus(PacketOutLoginResponse.Status.SUCCESS),
                            true);
                });

                // If a user + pass is submitted, verify it matches and then reassign their token.
            } else if (login.getUsername() != null && login.getPassword() != null) {
                String username = login.getUsername();
                String password = login.getPassword();
                Optional<SecureIdentity> si = fetchSecureIdentity(login.getUsername());

                if(si.isPresent()) {
                    SecureIdentity remoteIdentity = si.get();
                    byte[] salt = remoteIdentity.getPasswordSalt().getBytes(StandardCharsets.UTF_8);
                    SecretUtil.SaltyHash replicaPassword = SecretUtil.createSHA256Hash(password, salt);

                    // Check if password is equal? If so, success!
                    if (replicaPassword.getHash().equalsIgnoreCase(remoteIdentity.getPasswordHash())) {

                        AuthToken token = AuthToken.generateToken();
                        this.deleteAllUsernameTokens(username);
                        this.publishToken(remoteIdentity, token);
                        AuthenticatedIdentity identity = new AuthenticatedIdentity(client, username, token);

                        // The user is already logged into the server.
                        if (isIdentityLoaded(identity)) {
                            Server.get().getNetworkInterface().disconnectClient(client.getID(), new PacketInOutDisconnect("Your account is already logged onto the server!"));
                            return;
                        }

                        addAuthIdentity(identity);
                        client.send(new PacketOutLoginResponse()
                                        .setUsername(username)
                                        .setToken(token.getAuthToken())
                                        .setStatus(PacketOutLoginResponse.Status.SUCCESS),
                                true);
                    } else {
                        client.send(new PacketOutLoginResponse()
                                        .setStatus(PacketOutLoginResponse.Status.INVALID_PASSWORD),
                                true);
                    }

                } else {
                    // Possibly needs a better implementation. Database errors result in this too.
                    client.send(new PacketOutLoginResponse()
                                    .setStatus(PacketOutLoginResponse.Status.INVALID_USERNAME),
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

    protected boolean removeAuth(UUID id) {
        AuthenticatedIdentity identity = this.activeNetIDIdentities.remove(id);
        if(identity != null) {
            this.activeUsernameIdentities.remove(identity.getUsername());
            return true;
        }
        return false;
    }



    @EventHandler
    public void onDisconnect(ClientSocketStatusEvent.Disconnect event) {
        removeAuth(event.getClient().getID());
    }



    protected static Optional<SecureIdentity> processAuthIdentityResults(ResultSet set) {

        try {
            if(set.next()) {
                String accountID = set.getString("accountID");
                String pwHash = set.getString("pwhash");
                String salt = set.getString("salt");
                long accountCreation = set.getLong("accountCreation");

                if ((accountID != null) && (pwHash != null) && (salt != null)) {
                    return Optional.of(new SecureIdentity(accountID, pwHash, salt, accountCreation));
                }
            }
        } catch (SQLException ignored) { }

        return Optional.empty();
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
