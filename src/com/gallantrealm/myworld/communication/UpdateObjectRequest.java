package com.gallantrealm.myworld.communication;

import java.io.IOException;

import com.gallantrealm.myworld.model.WWObject;

/**
 * Client request to update the properties of an object. This is used when editing an object's properties by the user.
 */
public class UpdateObjectRequest extends ClientRequest {
	static final long serialVersionUID = 0;

	int objectId;
	WWObject object;

	public UpdateObjectRequest() {
	}

	public UpdateObjectRequest(int objectId, WWObject object) {
		this.objectId = objectId;
		this.object = object;
	}

	public int getObjectId() {
		return objectId;
	}

	public WWObject getObject() {
		return object;
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
