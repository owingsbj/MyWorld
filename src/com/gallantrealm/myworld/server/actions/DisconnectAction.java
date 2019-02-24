package com.gallantrealm.myworld.server.actions;

import com.gallantrealm.myworld.communication.ClientRequest;
import com.gallantrealm.myworld.communication.Connection;
import com.gallantrealm.myworld.communication.DisconnectRequest;
import com.gallantrealm.myworld.model.WWUser;
import com.gallantrealm.myworld.model.WWWorld;

/**
 * Disconnects a client from the server.
 */
public class DisconnectAction extends ServerAction {

	public WWUser doRequest(ClientRequest request, WWUser user, WWWorld world, Connection connection) throws Exception {
		user.disconnect();
		return user;
	}

	public Class getHandledRequestType() {
		return DisconnectRequest.class;
	}

}
