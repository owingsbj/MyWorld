package com.gallantrealm.myworld.server.actions;

import com.gallantrealm.myworld.communication.ClientRequest;
import com.gallantrealm.myworld.communication.Connection;
import com.gallantrealm.myworld.communication.CreateObjectRequest;
import com.gallantrealm.myworld.communication.CreateObjectResponse;
import com.gallantrealm.myworld.communication.DataOutputStreamX;
import com.gallantrealm.myworld.model.WWObject;
import com.gallantrealm.myworld.model.WWUser;
import com.gallantrealm.myworld.model.WWWorld;

/**
 * Handles requests from clients to create new objects in the world.
 */
public class CreateObjectAction extends ServerAction {

	public WWUser doRequest(ClientRequest request, WWUser client, WWWorld world, Connection connection) throws Exception {
		CreateObjectRequest createObjectRequest = (CreateObjectRequest) request;

		// Add the object to the world
		WWObject object = createObjectRequest.getObject();
		long time = world.getWorldTime();
		object.setCreateTime(time);
		object.setLastModifyTime(time);
		object.setLastMoveTime(time);
		int objectId = world.addObject(object);

		// Respond providing the object id
		DataOutputStreamX sendStream = connection.getSendStream(TIMEOUT);
		sendStream.writeObject(new CreateObjectResponse(objectId));
		connection.send(TIMEOUT);

		return client;
	}

	public Class getHandledRequestType() {
		return CreateObjectRequest.class;
	}

}
