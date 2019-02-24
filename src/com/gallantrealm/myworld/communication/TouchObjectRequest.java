package com.gallantrealm.myworld.communication;

import java.io.IOException;

/**
 * Client request to touch an object in the world. The action that the object takes when touched depends on the
 * behavior(s) of the object.
 */
public class TouchObjectRequest extends ClientRequest {
	static final long serialVersionUID = 0;

	int objectId;

	public TouchObjectRequest() {
	}

	public TouchObjectRequest(int objectId) {
		this.objectId = objectId;
	}

	public int getObjectId() {
		return objectId;
	}

	public void send(DataOutputStreamX os) throws IOException {
		os.writeInt(objectId);
	}

	public void receive(DataInputStreamX is) throws IOException {
		objectId = is.readInt();
	}

}
