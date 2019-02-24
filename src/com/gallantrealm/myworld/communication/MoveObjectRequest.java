package com.gallantrealm.myworld.communication;

import java.io.IOException;

import com.gallantrealm.myworld.model.WWObject;

/**
 * Client request to move an object in the world.
 */
public class MoveObjectRequest extends ClientRequest {
	static final long serialVersionUID = 0;

	int objectId;
	WWObject object;

	public MoveObjectRequest() {
	}

	public MoveObjectRequest(int objectId, WWObject object) {
		this.objectId = objectId;
		this.object = object;
	}

	public int getObjectId() {
		return objectId;
	}

	public WWObject getObject() {
		return object;
	}

	public boolean hasResponse() {
		return false;
	}

	public void send(DataOutputStreamX os) throws IOException {
		os.writeInt(objectId);
		os.writeObject(object);
	}

	public void receive(DataInputStreamX is) throws IOException {
		objectId = is.readInt();
		object = (WWObject) is.readObject();
	}

}
