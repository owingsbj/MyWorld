package com.gallantrealm.myworld.server.actions;

import com.gallantrealm.myworld.communication.ClientRequest;
import com.gallantrealm.myworld.communication.Connection;
import com.gallantrealm.myworld.communication.MoveObjectRequest;
import com.gallantrealm.myworld.model.WWObject;
import com.gallantrealm.myworld.model.WWUser;
import com.gallantrealm.myworld.model.WWWorld;

/**
 * Handles requests to reposition an object. This includes setting its velocity and angular momentum.
 */
public class MoveObjectAction extends ServerAction {

	public WWUser doRequest(ClientRequest request, WWUser client, WWWorld world, Connection connection) throws Exception {
		MoveObjectRequest moveObjectRequest = (MoveObjectRequest) request;
		int objectId = moveObjectRequest.getObjectId();
		WWObject updateObject = moveObjectRequest.getObject();

		// Move the object with the same id in world
		world.moveObject(objectId, updateObject);

		// This action requires no response

		return client;
	}

	public Class getHandledRequestType() {
		return MoveObjectRequest.class;
	}

}
