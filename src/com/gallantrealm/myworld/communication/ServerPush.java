package com.gallantrealm.myworld.communication;

/**
 * Abstract class for messages sent from the server to the clients to update clients on the state of the server.
 */
public abstract class ServerPush implements Sendable {
	static final long serialVersionUID = 0;

	public ServerPush() {
	}

}
