package com.gallantrealm.myworld.communication;

import java.io.IOException;

import com.gallantrealm.myworld.model.WWEntity;

/**
 * A client request to update the general properties of any entity (the world, a user, an object). This currently is
 * only name and description.
 */
public class UpdateEntityRequest extends ClientRequest {
	static final long serialVersionUID = 0;

	int entityId;
	WWEntity entity;

	public UpdateEntityRequest() {
	}

	public UpdateEntityRequest(int entityId, WWEntity entity) {
		this.entityId = entityId;
		this.entity = entity;
	}

	public int getEntityId() {
		return entityId;
	}

	public WWEntity getEntity() {
		return entity;
	}

	public void send(DataOutputStreamX os) throws IOException {
		os.writeInt(entityId);
		os.writeObject(entity);
	}

	public void receive(DataInputStreamX is) throws IOException {
		entityId = is.readInt();
		entity = (WWEntity) is.readObject();
	}

}
