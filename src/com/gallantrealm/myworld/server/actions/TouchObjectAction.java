package com.gallantrealm.myworld.server.actions;

import com.gallantrealm.myworld.communication.ClientRequest;
import com.gallantrealm.myworld.communication.Connection;
import com.gallantrealm.myworld.communication.TouchObjectRequest;
import com.gallantrealm.myworld.model.WWObject;
import com.gallantrealm.myworld.model.WWUser;
import com.gallantrealm.myworld.model.WWWorld;

/**
 * Handles a client request to "touch" an object. This invokes objectTouchedEvents on the behaviors of the object.
 */
public class TouchObjectAction extends ServerAction {

	@Override
	public WWUser doRequest(ClientRequest request, WWUser user, WWWorld world, Connection connection) throws Exception {
		TouchObjectRequest touchObjectRequest = (TouchObjectRequest) request;
		int objectId = touchObjectRequest.getObjectId();

		// Touch the in world object
		WWObject object = world.objects[objectId];
		world.touchObject(object, 0, 0, 0, user);

		return user;
	}

	@Override
	public Class getHandledRequestType() {
		return TouchObjectRequest.class;
	}

}
