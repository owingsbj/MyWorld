package com.gallantrealm.myworld.server.actions;

import com.gallantrealm.myworld.communication.ClientRequest;
import com.gallantrealm.myworld.communication.ConnectRequest;
import com.gallantrealm.myworld.communication.ConnectResponse;
import com.gallantrealm.myworld.communication.Connection;
import com.gallantrealm.myworld.communication.DataOutputStreamX;
import com.gallantrealm.myworld.communication.ServerResponse;
import com.gallantrealm.myworld.model.WWUser;
import com.gallantrealm.myworld.model.WWWorld;
import com.gallantrealm.myworld.server.exits.ConnectExit;

/**
 * This action handles requests for connection by clients. It validates that the user is defined on this server, and
 * returns the world time (to allow all clients connected to the server to be synchronized).
 */
public class ConnectAction extends ServerAction {

	public WWUser doRequest(ClientRequest request, WWUser user, WWWorld world, Connection connection) throws Exception {
		ConnectRequest connectRequest = (ConnectRequest) request;
		long worldTime = world.getWorldTime();
		String[] pMessage = new String[1];
		user = ConnectExit.getAuthenticatedUser(world, connectRequest.getUserid(), connectRequest.getCredentials(), pMessage);
		if (user == null) {

			// Send a response that we are not connected
			DataOutputStreamX sendStream = connection.getSendStream(TIMEOUT);
			ServerResponse response = new ConnectResponse(false, pMessage[0], 0, 0, 0);
			sendStream.writeObject(response);
			connection.send(TIMEOUT);

		} else {
			user.connect();

			// Send a response that we are connected, with the current world time
			DataOutputStreamX sendStream = connection.getSendStream(TIMEOUT);
			ServerResponse response = new ConnectResponse(true, "", world.getUserId(user), world.getWorldTime(), worldTime - connectRequest.getCurrentClientTime());
			sendStream.writeObject(response);
			connection.send(TIMEOUT);
		}

		return user;
	}

	public Class getHandledRequestType() {
		return ConnectRequest.class;
	}

}
