package net.cg360.spookums.server.core.event.type.network;

import net.cg360.spookums.server.core.event.type.Event;
import net.cg360.spookums.server.network.user.ConnectionState;
import net.cg360.spookums.server.network.user.NetworkClient;
import net.cg360.spookums.server.util.clean.Check;

// Not cancellable as these events are indicators for achieved states. In order to block
// a client from reaching these states, other events should be used.
public class ClientSocketStatusEvent extends Event {

    protected NetworkClient clientNetID;

    protected ClientSocketStatusEvent(NetworkClient clientNetID) {
        Check.nullParam(clientNetID, "clientNetID");
        this.clientNetID = clientNetID;
    }

    public NetworkClient getClient() {
        return clientNetID;
    }

    public static NetworkClient checkState(NetworkClient client, ConnectionState requiredState) {
        if(client.getState() != requiredState) throw new IllegalArgumentException("NetworkClient must be in the '"+requiredState.name()+"' state.");
        return client;
    }

    public static class Open extends ClientSocketStatusEvent {
        public Open(NetworkClient clientNetID) {
            super(checkState(clientNetID, ConnectionState.OPEN));
        }
    }

    public static class Protocol extends ClientSocketStatusEvent {
        public Protocol(NetworkClient clientNetID) {
            super(checkState(clientNetID, ConnectionState.PROTOCOL));
        }
    }

    public static class Connected extends ClientSocketStatusEvent {
        public Connected(NetworkClient clientNetID) {
            super(checkState(clientNetID, ConnectionState.CONNECTED));
        }
    }

    public static class LoggedIn extends ClientSocketStatusEvent {

        protected String username;

        public LoggedIn(NetworkClient clientNetID, String username) {
            super(checkState(clientNetID, ConnectionState.LOGGED_IN));
            this.username = username;
        }

        public String getUsername() {
            return username;
        }
    }

    public static class Disconnect extends ClientSocketStatusEvent {
        public Disconnect(NetworkClient clientNetID) {
            super(checkState(clientNetID, ConnectionState.DISCONNECTED));
        }
    }
}
