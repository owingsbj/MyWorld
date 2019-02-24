package com.gallantrealm.myworld.server.actions;

import com.gallantrealm.myworld.communication.ClientRequest;
import com.gallantrealm.myworld.communication.Connection;
import com.gallantrealm.myworld.communication.UpdateObjectRequest;
import com.gallantrealm.myworld.model.WWObject;
import com.gallantrealm.myworld.model.WWUser;
import com.gallantrealm.myworld.model.WWWorld;

/**
 * Handles requests to update object properties.
 */
public class UpdateObjectAction extends ServerAction {

	@Override
	public WWUser doRequest(ClientRequest request, WWUser client, WWWorld world, Connection connection) throws Exception {
		UpdateObjectRequest updateObjectRequest = (UpdateObjectRequest) request;
		int objectId = updateObjectRequest.getObjectId();
		WWObject updateObject = updateObjectRequest.getObject();

		// Time stamp the replacement object
		long time = world.getWorldTime();
		updateObject.setLastModifyTime(time);
		updateObject.setLastMoveTime(time);

		// Replace the object with the same id in world
		world.updateObject(objectId, updateObject);

		return client;
	}

	@Override
	public Class getHandledRequestType() {
		return UpdateObjectRequest.class;
	}

}
