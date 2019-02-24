package com.gallantrealm.myworld.communication;

/**
 * Abstract class for all requests which are sent from the client to the server.
 */
public abstract class ClientRequest implements Sendable {
	static final long serialVersionUID = 0;

	public ClientRequest() {
	}

	public boolean hasResponse() {
		return true;
	}

}
