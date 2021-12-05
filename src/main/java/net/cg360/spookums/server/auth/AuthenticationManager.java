package net.cg360.spookums.server.auth;

import net.cg360.spookums.server.Server;
import net.cg360.spookums.server.auth.record.AuthToken;
import net.cg360.spookums.server.auth.record.AuthenticatedClient;
import net.cg360.spookums.server.auth.record.StoredIdentity;
import net.cg360.spookums.server.auth.state.AccountCreateState;
import net.cg360.spookums.server.core.event.handler.EventHandler;
import net.cg360.spookums.server.core.event.type.network.ClientSocketStatusEvent;
import net.cg360.spookums.server.core.scheduler.Scheduler;
import net.cg360.spookums.server.network.packet.auth.PacketInLogin;
import net.cg360.spookums.server.network.packet.auth.PacketInUpdateAccount;
import net.cg360.spookums.server.network.packet.auth.PacketOutLoginResponse;
import net.cg360.spookums.server.network.user.NetworkClient;
import net.cg360.spookums.server.util.SecretUtil;
import net.cg360.spookums.server.util.clean.Check;
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

    protected Scheduler scheduler;

    protected HashMap<UUID, AuthenticatedClient> activeNetIDIdentities;
    protected HashMap<String, AuthenticatedClient> activeAuthenticatedClients;

    public AuthenticationManager() {
        this.activeNetIDIdentities = new HashMap<>();
        this.activeAuthenticatedClients = new HashMap<>();

        this.scheduler = new Scheduler(1);
        this.scheduler.startScheduler();
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


    /**
     * A task typically ran on server startup, its role
     * is to clear any currently outdated tokens on the
     * database. This does not affect users currently
     * connected to the server.
     * @return true if the action was successful.
     */
    public boolean deleteAllOutdatedTokens() {
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

    /**
     * Deletes all existing tokens for a specific username, whether
     * they're outdated or not.
     * @return true if the action was successful.
     */
    public boolean deleteAllTokensFromUsername(String username) {
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


    /**
     * Publishes a token using a cached SecureIdentity
     * @param identity
     * @param token
     * @return true if the token was pushed to the database.
     */
    public boolean publishTokenWithAccountID(StoredIdentity identity, AuthToken token) {
        return this.publishTokenWithAccountID(identity.getAccountID(), token);
    }

    /**
     * Publishes a token based on a username, fetching a SecureIdentity
     * used to extract an account identifier.
     * @param username
     * @param token
     * @return true if the token was pushed to the database.
     */
    public boolean publishTokenWithUsername(String username, AuthToken token) {
        Optional<StoredIdentity> i =  this.fetchStoredIdentity(username);
        if(!i.isPresent()) return false;

        String accountID = i.get().getAccountID();
        return this.publishTokenWithAccountID(accountID, token);
    }

    /**
     * Publishes a token using a unique account identifier
     * @param accountID
     * @param authToken
     * @return true if the token was pushed to the database.
     */
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


    /**
     * Fetches a token from the database, getting its expire time.
     * @param token
     * @param clearIfExpired
     * @return
     */
    public Optional<AuthToken> fetchStoredTokenWithToken(String token, boolean clearIfExpired) {
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

    /**
     * Fetches a users username based on their token. Used
     * during login to quickly verify if their token is valid.
     *
     * @param failIfExpired if the token is expired, should it return an empty
     */
    public Optional<Pair<String, Long>> fetchUsernameFromToken(String token, boolean failIfExpired) {
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


    /**
     * Fetches a secure identity from the database
     * @param username the username to fetch for
     * @return a SecureIdentity record
     */
    public Optional<StoredIdentity> fetchStoredIdentity(String username) {
        try {
            Connection connection = getCoreConnection();
            if(connection == null) return Optional.empty();

            PreparedStatement s = connection.prepareStatement(SQL_FETCH_USERNAME_IDENTITY);
            s.setObject(1, username);
            Optional<StoredIdentity> a = processAuthIdentityResults(s.executeQuery());

            ErrorUtil.quietlyClose(s);
            ErrorUtil.quietlyClose(connection);
            return a;

        } catch (SQLException err) {
            err.printStackTrace();
        }

        return Optional.empty();
    }



    public void processRegisterPacket(PacketInUpdateAccount reg, NetworkClient client) {
        //TODO: Do this.

        this.scheduler.prepareTask(() -> {
            if(reg.isCreatingNewAccount()) {
                String username = reg.getNewUsername();
                String password = reg.getNewPassword();

                Pair<AccountCreateState, AuthenticatedClient> accountState = Pair.of(null, null);
                        //this.createNewIdentityAndLogin(client, username, password);



                PacketOutLoginResponse loginResponse = new PacketOutLoginResponse();

                switch (accountState.getFirst()) {
                    case CREATED:
                        loginResponse.setStatus(PacketOutLoginResponse.Status.SUCCESS);

                        AuthenticatedClient loginIdentity = accountState.getSecond();
                        loginResponse.setUsername(loginIdentity.getUsername());
                        loginResponse.setToken(loginIdentity.getToken().getAuthToken());
                        break;

                    case TAKEN:
                        loginResponse.setStatus(PacketOutLoginResponse.Status.TAKEN_USERNAME);
                        Server.getLogger(Server.AUTH_LOG).warn(String.format(
                                "%s failed to register an account with the name %s (taken)",
                                client.getID().toString(),
                                username
                        ));
                        break;

                    case DB_OFFLINE:
                        loginResponse.setStatus(PacketOutLoginResponse.Status.TECHNICAL_SERVER_ERROR);
                        Server.getLogger(Server.AUTH_LOG).warn(String.format(
                                "%s failed to register an account with the name %s (db offline)",
                                client.getID().toString(),
                                username
                        ));
                        break;

                    case ERRORED:
                        loginResponse.setStatus(PacketOutLoginResponse.Status.GENERAL_REGISTER_ERROR);
                        Server.getLogger(Server.AUTH_LOG).warn(String.format(
                                "%s failed to register an account with the name %s (error)",
                                client.getID().toString(),
                                username
                        ));
                        break;

                }

                client.send(loginResponse, true);

            } else {

                Server.getLogger(Server.NET_LOG).warn("Protocol issue! - updating existing accounts is not yet implemented server-side");
                client.send(
                        new PacketOutLoginResponse()
                                .setStatus(PacketOutLoginResponse.Status.FAILURE_GENERAL),
                        true
                );

            }

        }).setAsynchronous(true).schedule();
    }



    public void processLoginPacket(PacketInLogin login, NetworkClient client) {
        this.scheduler.prepareTask(() -> {
            /*
            // If a token was submitted, use that as a login.
            if (login.getToken() != null) {
                String token = login.getToken();
                Optional<Pair<String, Long>> u = this.fetchUsername(login.getToken(), true);

                u.ifPresent(i -> {
                    Pair<String, Long> loginPair = u.get();
                    String username = loginPair.getFirst();
                    long expire = loginPair.getSecond();

                    AuthenticatedClient identity = new AuthenticatedClient(client, username, new AuthToken(token, expire));
                    addAuthenticatorIdentity(identity);
                });

                // If a user + pass is submitted, verify it matches and then reassign their token.
            } else if (login.getUsername() != null && login.getPassword() != null) {
                String username = login.getUsername();
                String password = login.getPassword();
                Optional<StoredIdentity> si = fetchSecureIdentity(login.getUsername());

                if(si.isPresent()) {
                    StoredIdentity remoteIdentity = si.get();
                    byte[] salt = remoteIdentity.getPasswordSalt().getBytes(StandardCharsets.UTF_8);
                    SecretUtil.SaltyHash replicaPassword = SecretUtil.createSHA256Hash(password, salt);

                    // Check if password is equal? If so, success!
                    if (replicaPassword.getHash().equalsIgnoreCase(remoteIdentity.getPasswordHash())) {

                        AuthToken token = AuthToken.generateToken();
                        this.deleteAllUsernameTokens(username);
                        this.publishToken(remoteIdentity, token);

                        AuthenticatedClient identity = new AuthenticatedClient(client, username, token);
                        addAuthenticatorIdentity(identity);

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
             */
        }).setAsynchronous(true).schedule();
    }


    protected boolean isAuthenticatedClientActive(AuthenticatedClient identity) {
        return activeNetIDIdentities.containsKey(identity.getClient().getID()) || activeAuthenticatedClients.containsKey(identity.getUsername());
    }




    protected boolean addAuthenticatedClient(AuthenticatedClient identity) {
        if(!isAuthenticatedClientActive(identity)) {
            this.activeNetIDIdentities.put(identity.getClient().getID(), identity);
            this.activeAuthenticatedClients.put(identity.getUsername(), identity);
            return true;
        }
        return false;
    }

    protected boolean removeAuthenticatedClient(UUID id) {
        AuthenticatedClient identity = this.activeNetIDIdentities.remove(id);
        if(identity != null) {
            this.activeAuthenticatedClients.remove(identity.getUsername());
            return true;
        }
        return false;
    }



    @EventHandler
    public void onDisconnect(ClientSocketStatusEvent.Disconnect event) {
        removeAuthenticatedClient(event.getClient().getID());
    }



    protected static Optional<StoredIdentity> processAuthIdentityResults(ResultSet set) {

        try {
            if(set.next()) {
                String accountID = set.getString("accountID");
                String pwHash = set.getString("pwhash");
                String salt = set.getString("salt");
                long accountCreation = set.getLong("accountCreation");

                if ((accountID != null) && (pwHash != null) && (salt != null)) {
                    return Optional.of(new StoredIdentity(accountID, pwHash, salt, accountCreation));
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
