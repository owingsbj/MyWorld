package com.gallantrealm.myworld.communication;

import java.io.IOException;

/**
 * Client request to delete an object in the world.
 */
public class DeleteObjectRequest extends ClientRequest {
	static final long serialVersionUID = 0;

	int objectId;

	public DeleteObjectRequest() {
	}

	public DeleteObjectRequest(int objectId) {
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
