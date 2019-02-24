package com.gallantrealm.myworld.server.actions;

import com.gallantrealm.myworld.communication.ClientRequest;
import com.gallantrealm.myworld.communication.Connection;
import com.gallantrealm.myworld.communication.DeleteObjectRequest;
import com.gallantrealm.myworld.model.WWUser;
import com.gallantrealm.myworld.model.WWWorld;

/**
 * Handles requests from clients to delete objects on the server.
 */
public class DeleteObjectAction extends ServerAction {

	@Override
	public WWUser doRequest(ClientRequest request, WWUser client, WWWorld world, Connection connection) throws Exception {
		DeleteObjectRequest deleteObjectRequest = (DeleteObjectRequest) request;
		int id = deleteObjectRequest.getObjectId();

		// if the object has a parent, delete the parent instead
		int parentId = world.objects[id].parentId;
		while (parentId != 0) {
			id = parentId;
			parentId = world.objects[id].parentId;
		}

		world.removeObject(id);

		return client;
	}

	@Override
	public Class getHandledRequestType() {
		return DeleteObjectRequest.class;
	}

}
