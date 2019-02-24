package com.gallantrealm.myworld.communication;

import java.io.IOException;

import com.gallantrealm.myworld.model.WWObject;

/**
 * Client request to create an object in the world.
 */
public class CreateObjectRequest extends ClientRequest {
	static final long serialVersionUID = 0;

	WWObject object;

	public CreateObjectRequest() {
	}

	public CreateObjectRequest(WWObject object) {
		this.object = object;
	}

	public WWObject getObject() {
		return object;
	}

	public void send(DataOutputStreamX os) throws IOException {
		os.writeObject(object);
	}

	public void receive(DataInputStreamX is) throws IOException {
		object = (WWObject) is.readObject();
	}

}
