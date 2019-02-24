package com.gallantrealm.myworld.server.actions;

import com.gallantrealm.myworld.communication.ClientRequest;
import com.gallantrealm.myworld.communication.Connection;
import com.gallantrealm.myworld.communication.UpdateEntityRequest;
import com.gallantrealm.myworld.model.WWEntity;
import com.gallantrealm.myworld.model.WWObject;
import com.gallantrealm.myworld.model.WWUser;
import com.gallantrealm.myworld.model.WWWorld;

/**
 * Handles requests to update the general properties of any entity. This includes the world, users, and objects.
 */
public class UpdateEntityAction extends ServerAction {

	@Override
	public WWUser doRequest(ClientRequest request, WWUser client, WWWorld world, Connection connection) throws Exception {
		UpdateEntityRequest updateEntityRequest = (UpdateEntityRequest) request;
		int entityId = updateEntityRequest.getEntityId();
		WWEntity updatedEntity = updateEntityRequest.getEntity();

		// Find the original entity
		WWEntity originalEntity = null;
		if (updatedEntity instanceof WWWorld) {
			originalEntity = world;
		} else if (updatedEntity instanceof WWUser) {
			originalEntity = world.getUser(entityId);
		} else if (updatedEntity instanceof WWObject) {
			originalEntity = world.objects[entityId];
		}

		// Update the writeable general properties
		originalEntity.setName(updatedEntity.getName());
		originalEntity.setDescription(updatedEntity.getDescription());

		// Time stamp the updated object
		long time = world.getWorldTime();
		originalEntity.setLastModifyTime(time);

		return client;
	}

	@Override
	public Class getHandledRequestType() {
		return UpdateEntityRequest.class;
	}

}
