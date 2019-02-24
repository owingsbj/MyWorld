package com.gallantrealm.myworld.server.actions;

import com.gallantrealm.myworld.communication.ClientRequest;
import com.gallantrealm.myworld.communication.Connection;
import com.gallantrealm.myworld.communication.UpdateWorldPropertiesRequest;
import com.gallantrealm.myworld.model.WWUser;
import com.gallantrealm.myworld.model.WWWorld;

/**
 * Handles requests to update world properties.
 */
public class UpdateWorldPropertiesAction extends ServerAction {

	@Override
	public Class getHandledRequestType() {
		return UpdateWorldPropertiesRequest.class;
	}

	@Override
	public WWUser doRequest(ClientRequest request, WWUser client, WWWorld world, Connection connection) throws Exception {
		UpdateWorldPropertiesRequest updateWorldPropertiesRequest = (UpdateWorldPropertiesRequest) request;

		WWWorld updatedWorld = updateWorldPropertiesRequest.getWorld();
		world.setGravity(updatedWorld.getGravity());
		world.setSunColor(updatedWorld.getSunColor());
		world.setFogDensity(updatedWorld.getFogDensity());
		world.setAmbientLightIntensity(updatedWorld.getAmbientLightIntensity());
		world.setSkyColor(updatedWorld.getSkyColor());
		world.setSunIntensity(updatedWorld.getSunIntensity());
		world.setUnderglowIntensity(updatedWorld.getUnderglowIntensity());
		world.setSunDirection(updatedWorld.getSunDirection());

		// Update the timestamp in the world to indicate that the world has changed
		long time = world.getWorldTime();
		world.setLastModifyTime(time);

		return client;
	}

}
