package net.cg360.spookums.server.auth;

import net.cg360.spookums.server.Server;
import net.cg360.spookums.server.auth.record.AuthToken;
import net.cg360.spookums.server.auth.record.AuthenticatedClient;
import net.cg360.spookums.server.auth.record.StoredIdentity;
import net.cg360.spookums.server.auth.state.AccountCreateState;
import net.cg360.spookums.server.auth.state.AccountLoginState;
import net.cg360.spookums.server.auth.state.FetchState;
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

import static net.cg360.spookums.server.auth.state.AccountCreateState.*;

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
        Pair<FetchState, StoredIdentity> i =  this.fetchStoredIdentity(username);
        if(i.getFirst() != FetchState.SUCCESS) return false;

        String accountID = i.getSecond().getAccountID();
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
    public Pair<FetchState, AuthToken> fetchStoredTokenWithToken(String token, boolean clearIfExpired) {
        try {
            Connection connection = getCoreConnection();
            if(connection == null) return Pair.of(FetchState.DB_OFFLINE, null);

            if(clearIfExpired) {
                PreparedStatement c = connection.prepareStatement(SQL_CLEAR_OUTDATED_TOKENS);
                c.setObject(1, System.currentTimeMillis());
                c.execute();
                ErrorUtil.quietlyClose(c);
            }

            PreparedStatement s = connection.prepareStatement(SQL_FETCH_TOKEN);
            s.setObject(1, token);
            Pair<FetchState, AuthToken> a = processAuthTokenResults(s.executeQuery());

            ErrorUtil.quietlyClose(s);
            ErrorUtil.quietlyClose(connection);
            return a;

        } catch (SQLException err) {
            err.printStackTrace();
            return Pair.of(FetchState.ERRORED, null);
        }
    }

    //TODO: Clean up that return type ffs.
    /**
     * Fetches a users username based on their token. Used
     * during login to quickly verify if their token is valid.
     *
     * @param failIfExpired if the token is expired, should it return an empty
     */
    public Pair<FetchState, Pair<String, Long>> fetchUsernameFromToken(String token, boolean failIfExpired) {
        try {
            Connection connection = getCoreConnection();
            if(connection == null) return Pair.of(FetchState.DB_OFFLINE, null);

            PreparedStatement s = connection.prepareStatement(SQL_FETCH_USERNAME);
            s.setObject(1, token);
            Pair<FetchState, Pair<String, Long>> a = processAuthUserResults(s.executeQuery(), failIfExpired);

            ErrorUtil.quietlyClose(s);
            ErrorUtil.quietlyClose(connection);
            return a;

        } catch (SQLException err) {
            err.printStackTrace();
            return Pair.of(FetchState.ERRORED, null);
        }

    }


    /**
     * Fetches a secure identity from the database
     * @param username the username to fetch for
     * @return a SecureIdentity record
     */
    public Pair<FetchState, StoredIdentity> fetchStoredIdentity(String username) {
        try {
            Connection connection = getCoreConnection();
            if(connection == null) return Pair.of(FetchState.DB_OFFLINE, null);

            PreparedStatement s = connection.prepareStatement(SQL_FETCH_USERNAME_IDENTITY);
            s.setObject(1, username);
            Pair<FetchState, StoredIdentity> a = processAuthIdentityResults(s.executeQuery());

            ErrorUtil.quietlyClose(s);
            ErrorUtil.quietlyClose(connection);
            return a;

        } catch (SQLException err) {
            err.printStackTrace();
            return Pair.of(FetchState.ERRORED, null);
        }
    }




    public Pair<AccountLoginState, AuthenticatedClient> authenticateClient(NetworkClient client, String token) {
        if(isClientLoggedIn(client))
            return Pair.of(AccountLoginState.ALREADY_LOGGED_IN, null);

        // Fetch the username tied to a token. If one is found, a valid token exists as it auto-expires them.
        Pair<FetchState, Pair<String, Long>> u = this.fetchUsernameFromToken(token, true);

        // Remapping database fetch \/\/\/
        if(u.getFirst() != FetchState.SUCCESS) {
            switch (u.getFirst()) {
                case NOT_FOUND:  return Pair.of(AccountLoginState.DENIED, null);
                case DB_OFFLINE: return Pair.of(AccountLoginState.DB_OFFLINE, null);
                case ERRORED:
                default:         return Pair.of(AccountLoginState.ERRORED, null);
            }

        }

        // Extract the data, create a new auth client from it.
        Pair<String, Long> usernameExpire = u.getSecond();
        if(isUserLoggedIn(usernameExpire.getFirst()))
            return Pair.of(AccountLoginState.ALREADY_LOGGED_IN, null);

        AuthenticatedClient authClient = new AuthenticatedClient(client, usernameExpire.getFirst(), new AuthToken(token, usernameExpire.getSecond()));
        this.addAuthenticatedClient(authClient);

        return Pair.of(AccountLoginState.SUCCESS, authClient);
    }


    public Pair<AccountLoginState, AuthenticatedClient> authenticateClient(NetworkClient client, String username, String password) {
        if(isClientLoggedIn(client) || isUserLoggedIn(username))
            return Pair.of(AccountLoginState.ALREADY_LOGGED_IN, null);

        //TODO: Login!
        // Note that tokens are not removed from the server when a new one is generated.
        // This is so multiple devices can login.

        Pair<FetchState, StoredIdentity> identityPair = this.fetchStoredIdentity(username);

        if(identityPair.getFirst() != FetchState.SUCCESS) {
            switch (identityPair.getFirst()) {
                // Denied. They shouldn't know if it's the username or password that's wrong for better security.
                case NOT_FOUND:  return Pair.of(AccountLoginState.DENIED, null);
                case DB_OFFLINE: return Pair.of(AccountLoginState.DB_OFFLINE, null);
                case ERRORED:
                default:         return Pair.of(AccountLoginState.ERRORED, null);
            }
        }

        StoredIdentity identity = identityPair.getSecond();
        byte[] passSalt = identity.getPasswordSalt().getBytes(StandardCharsets.UTF_8);

        String passIn = SecretUtil.createSHA256Hash(password, passSalt).getHash();
        String passExist = identity.getPasswordHash();

        // Hash of password in does not match the stored password hash. It must be wrong :D
        if(!passIn.equals(passExist))
            return Pair.of(AccountLoginState.DENIED, null);

        AuthToken token = AuthToken.generateToken();
        if(publishTokenWithAccountID(identity,  token))
            return Pair.of(AccountLoginState.ERRORED, null);

        AuthenticatedClient authClient = new AuthenticatedClient(client, username, token);
        this.addAuthenticatedClient(authClient);
        return Pair.of(AccountLoginState.SUCCESS, authClient);
    }


    // write docs for this but yeah, stored identities are user data. AuthenticatedClients are actively logged in users.
    public Pair<AccountCreateState, StoredIdentity> createStoredIdentity(NetworkClient client, String username, String password) {
        if(Check.isNull(client) || Check.isNull(username) || Check.isNull(password))
            return Pair.of(AccountCreateState.ERRORED, null);

        // Check an account doesn't already exist, remapping it to the AccountCreateState values for everything other than NOT_FOUND
        Pair<FetchState, StoredIdentity> identityPair = fetchStoredIdentity(username);
        switch (identityPair.getFirst()) {
            case DB_OFFLINE: return Pair.of(DB_OFFLINE, null);
            case ERRORED:    return Pair.of(ERRORED, null);
            case SUCCESS:    return Pair.of(TAKEN, null);
        }

        String newAccountID = UUID.randomUUID().toString();
        SecretUtil.SaltyHash hashbrown_haha = SecretUtil.createSHA256Hash(password, SecretUtil.generateSalt(8));
        String passHash = hashbrown_haha.getHash();
        String passSalt = hashbrown_haha.getSalt().isPresent()
                ? new String(hashbrown_haha.getSalt().get(), StandardCharsets.UTF_8)
                : "";
        long accountCreateTime = System.currentTimeMillis();


        try {
            Connection connection = getCoreConnection();
            if(connection == null) return Pair.of(DB_OFFLINE, null);

            PreparedStatement statementCreateIdentity = connection.prepareStatement(SQL_ASSIGN_NEW_IDENTITY);
            statementCreateIdentity.setObject(1, newAccountID); // account id
            statementCreateIdentity.setObject(2, passHash); // pass hash
            statementCreateIdentity.setObject(3, passSalt); // pass salt
            statementCreateIdentity.setObject(4, accountCreateTime); // create time
            statementCreateIdentity.execute();

            ErrorUtil.quietlyClose(statementCreateIdentity);


            PreparedStatement statementLinkIdentity = connection.prepareStatement(SQL_ASSIGN_NEW_LOOKUP);
            statementLinkIdentity.setObject(1, newAccountID); // account id
            statementLinkIdentity.setObject(2, username); // username
            statementLinkIdentity.execute();

            ErrorUtil.quietlyClose(statementLinkIdentity);

            ErrorUtil.quietlyClose(connection);
            return Pair.of(
                    CREATED,
                    new StoredIdentity(newAccountID, passHash, passSalt, accountCreateTime)
            );

        } catch (SQLException err) {
            err.printStackTrace();
            return Pair.of(ERRORED, null);
        }
    }




    public void processRegisterPacket(PacketInUpdateAccount reg, NetworkClient client) {
        this.scheduler.prepareTask(() -> {
            if(reg.isCreatingNewAccount()) {
                String username = reg.getNewUsername();
                String password = reg.getNewPassword();

                Pair<AccountCreateState, StoredIdentity> accountState = this.createStoredIdentity(client, username, password);
                PacketOutLoginResponse loginResponse = new PacketOutLoginResponse();

                switch (accountState.getFirst()) {
                    case CREATED:
                        Pair<AccountLoginState, AuthenticatedClient> aClient = authenticateClient(client, username, password);

                        if(aClient.getFirst() == AccountLoginState.SUCCESS) {
                            AuthenticatedClient authClient = aClient.getSecond();
                            loginResponse.setStatus(PacketOutLoginResponse.Status.SUCCESS);

                            loginResponse.setUsername(authClient.getUsername());
                            loginResponse.setToken(authClient.getToken().getAuthToken());

                        } else {
                            switch (aClient.getFirst()) {
                                case ERRORED: loginResponse.setStatus(PacketOutLoginResponse.Status.GENERAL_LOGIN_ERROR);
                                case DB_OFFLINE: loginResponse.setStatus(PacketOutLoginResponse.Status.TECHNICAL_SERVER_ERROR);
                                case ALREADY_LOGGED_IN: loginResponse.setStatus(PacketOutLoginResponse.Status.ALREADY_LOGGED_IN);
                            }

                            Server.getLogger(Server.AUTH_LOG).error(String.format(
                                    "%s failed to register an account with the name %s (registered but not logged in | %s)",
                                    client.getID().toString(),
                                    username,
                                    aClient.getFirst().toString()
                            ));
                        }
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

            switch (login.getMode()) {
                case 0: // username + password
                    break;

                case 1: // token
                    String token = login.getToken();
                    Pair<FetchState, Pair<String, Long>> u = this.fetchUsernameFromToken(login.getToken(), true);

                    if(u.getFirst() == FetchState.SUCCESS){
                        Pair<String, Long> loginPair = u.getSecond();
                        String username = loginPair.getFirst();
                        long expire = loginPair.getSecond();

                        AuthenticatedClient identity = new AuthenticatedClient(client, username, new AuthToken(token, expire));
                        this.addAuthenticatedClient(identity);
                        //TODO: Send successful result.

                    } else {
                        //TODO: Send failed result
                    }
                    break;

                default:
                    Server.getLogger(Server.AUTH_LOG).warn("Login packet in has an unknown login type");
                    break;
            }

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
        return isClientLoggedIn(identity.getClient()) || isUserLoggedIn(identity.getUsername());
    }


    protected boolean isClientLoggedIn(NetworkClient client) {
        return activeNetIDIdentities.containsKey(client.getID());
    }


    protected boolean isUserLoggedIn(String username) {
        return activeAuthenticatedClients.containsKey(username);
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



    protected static Pair<FetchState, StoredIdentity> processAuthIdentityResults(ResultSet set) {

        try {
            if(set.next()) {
                String accountID = set.getString("accountID");
                String pwHash = set.getString("pwhash");
                String salt = set.getString("salt");
                long accountCreation = set.getLong("account_creation");

                if ((accountID != null) && (pwHash != null) && (salt != null)) {
                    return Pair.of(FetchState.SUCCESS, new StoredIdentity(accountID, pwHash, salt, accountCreation));
                } else Server.getLogger(Server.AUTH_LOG).warn("Incomplete StoredIdentity found in database!");
            }

            return Pair.of(FetchState.NOT_FOUND, null);

        } catch (SQLException err) {
            err.printStackTrace();
            return Pair.of(FetchState.ERRORED, null);
        }
    }

    protected static Pair<FetchState, AuthToken> processAuthTokenResults(ResultSet set) {

        try {
            if(set.next()) {
                String token = set.getString("token");
                long expire = set.getLong("expire");

                if ((token != null) && (expire != 0))
                    return Pair.of(FetchState.SUCCESS, new AuthToken(token, expire));
            }

            return Pair.of(FetchState.NOT_FOUND, null);

        } catch (SQLException err) {
            err.printStackTrace();
            return Pair.of(FetchState.ERRORED, null);
        }
    }

    protected static Pair<FetchState, Pair<String, Long>> processAuthUserResults(ResultSet set, boolean failIfExpired) {
        try {
            if(set.next()) {
                String username = set.getString("username");
                long expire = set.getLong("expire");

                if (username != null) {
                    if(failIfExpired && (System.currentTimeMillis() > expire)) return Pair.of(FetchState.NOT_FOUND, null);
                    return Pair.of(FetchState.SUCCESS, Pair.of(username, expire));
                }
            }

            return Pair.of(FetchState.NOT_FOUND, null);

        } catch (SQLException err) {
            err.printStackTrace();
            return Pair.of(FetchState.ERRORED, null);
        }
    }



    protected static Connection getCoreConnection() {
        return Server.get().getDBManager().access("core");
    }


    public static AuthenticationManager get() {
        return primaryInstance;
    }
}
