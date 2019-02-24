package com.gallantrealm.myworld.communication;

import java.io.IOException;

import com.gallantrealm.myworld.model.WWEntity;

/**
 * Server push to provides updates of the world's state. Note that not all of the world is sent to the client,
 * but only a portion of the world local to the avatar of the user.
 */
public class WorldUpdatePush extends ServerPush {
	static final long serialVersionUID = 0;

	public static final int UPDATE_TYPE_END = 0; // means no more object updates following
	public static final int UPDATE_TYPE_OBJECT_CREATE = 1;
	public static final int UPDATE_TYPE_OBJECT_MODIFY = 2;
	public static final int UPDATE_TYPE_OBJECT_MOVE = 3;
	public static final int UPDATE_TYPE_OBJECT_DELETE = 4;
	public static final int UPDATE_TYPE_USER_CREATE = 5;
	public static final int UPDATE_TYPE_USER_MODIFY = 6;
	public static final int UPDATE_TYPE_USER_DELETE = 7;
	public static final int UPDATE_TYPE_WORLD_PROPERTIES = 8;

	private int updateType;
	private int entityId;
	private WWEntity entity;

	public WorldUpdatePush() {
	}

	public WorldUpdatePush(int updateType, int entityId, WWEntity entity) {
		this.updateType = updateType;
		this.entityId = entityId;
		this.entity = entity;
	}

	public int getUpdateType() {
		return updateType;
	}

	public int getEntityId() {
		return entityId;
	}

	public WWEntity getEntity() {
		return entity;
	}

	public void send(DataOutputStreamX os) throws IOException {
		os.writeInt(updateType);
		os.writeInt(entityId);
		os.writeObject(entity);
	}

	public void receive(DataInputStreamX is) throws IOException {
		updateType = is.readInt();
		entityId = is.readInt();
		entity = (WWEntity) is.readObject();
	}

}
