package com.gallantrealm.myworld.server.actions;

import com.gallantrealm.myworld.communication.ClientRequest;
import com.gallantrealm.myworld.communication.Connection;
import com.gallantrealm.myworld.communication.ThrustObjectRequest;
import com.gallantrealm.myworld.model.WWObject;
import com.gallantrealm.myworld.model.WWUser;
import com.gallantrealm.myworld.model.WWWorld;

/**
 * Handles requests from clients to apply force to an object. This can cause the object to change in its velocity and
 * angular momentum. It is typically used to move the users avatar.
 */
public class ThrustObjectAction extends ServerAction {

	@Override
	public WWUser doRequest(ClientRequest request, WWUser client, WWWorld world, Connection connection) throws Exception {
		ThrustObjectRequest thrustObjectRequest = (ThrustObjectRequest) request;
		int objectId = thrustObjectRequest.getObjectId();
		WWObject updateObject = thrustObjectRequest.getObject();

		// Update the object's thrust, thrust velocity, torque, and torque velocity with the same id in world
		world.thrustObject(objectId, updateObject);

		// This action requires no response

		return client;
	}

	@Override
	public Class getHandledRequestType() {
		return ThrustObjectRequest.class;
	}

}
