package net.cg360.spookums.server.auth.record;

import net.cg360.spookums.server.network.user.ConnectionState;
import net.cg360.spookums.server.network.user.NetworkClient;
import net.cg360.spookums.server.util.clean.Check;

public class AuthenticatedClient {

    protected NetworkClient client;
    protected String username;
    protected AuthToken authentication;

    public AuthenticatedClient(NetworkClient client, String username, AuthToken token) {
        Check.nullParam(client, "client");
        Check.nullParam(username, "username");
        Check.nullParam(token, "token");

        if(client.getState() == ConnectionState.LOGGED_IN) throw new IllegalArgumentException("Network client must be logged in.");

        this.client = client;
        this.username = username;
        this.authentication = token;
    }


    public NetworkClient getClient() {
        return client;
    }

    public String getUsername() {
        return username;
    }

    public AuthToken getToken() {
        return authentication;
    }
}
