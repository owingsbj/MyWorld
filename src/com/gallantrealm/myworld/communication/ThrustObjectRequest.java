package com.gallantrealm.myworld.communication;

import java.io.IOException;

import com.gallantrealm.myworld.model.WWObject;

/**
 * Client request to push an object in the world. This is normally used to move the avatar.
 */
public class ThrustObjectRequest extends ClientRequest {
	static final long serialVersionUID = 0;

	int objectId;
	WWObject object;

	public ThrustObjectRequest() {
	}

	public ThrustObjectRequest(int objectId, WWObject object) {
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
