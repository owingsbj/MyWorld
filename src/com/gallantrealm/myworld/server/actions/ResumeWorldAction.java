package com.gallantrealm.myworld.server.actions;

import com.gallantrealm.myworld.communication.ClientRequest;
import com.gallantrealm.myworld.communication.Connection;
import com.gallantrealm.myworld.communication.ResumeWorldRequest;
import com.gallantrealm.myworld.model.WWUser;
import com.gallantrealm.myworld.model.WWWorld;

public class ResumeWorldAction extends ServerAction {

	@Override
	public Class getHandledRequestType() {
		return ResumeWorldRequest.class;
	}

	@Override
	public WWUser doRequest(ClientRequest request, WWUser client, WWWorld world, Connection connection) throws Exception {

		world.run();

		// This action requires no response

		return client;
	}

}
