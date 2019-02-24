package com.gallantrealm.myworld.server.actions;

import com.gallantrealm.myworld.communication.ClientRequest;
import com.gallantrealm.myworld.communication.Connection;
import com.gallantrealm.myworld.model.WWUser;
import com.gallantrealm.myworld.model.WWWorld;

/**
 * This is the superclass for all actions on the server that are performed on behalf of a client.
 */
public abstract class ServerAction {
	static final int TIMEOUT = 10000; // wait 10 secs for clients to respond

	public abstract Class getHandledRequestType();

	public abstract WWUser doRequest(ClientRequest request, WWUser client, WWWorld world, Connection connection) throws Exception;
}
