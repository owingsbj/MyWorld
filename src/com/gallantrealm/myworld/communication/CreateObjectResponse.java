package com.gallantrealm.myworld.communication;

import java.io.IOException;

/**
 * Response to the create object request, providing the id of the object created.
 */
public class CreateObjectResponse extends ServerResponse {
	static final long serialVersionUID = 0;

	int objectId;

	public CreateObjectResponse() {
	}

	public CreateObjectResponse(int objectId) {
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
