package com.gallantrealm.myworld.communication;

import java.io.IOException;

import com.gallantrealm.myworld.model.WWWorld;

/**
 * Client request to updates the properties of the world.
 */
public class UpdateWorldPropertiesRequest extends ClientRequest {
	static final long serialVersionUID = 0;

	private WWWorld world;

	public UpdateWorldPropertiesRequest() {
	}

	public UpdateWorldPropertiesRequest(WWWorld world) {
		this.world = world;
	}

	public WWWorld getWorld() {
		return world;
	}

	public void send(DataOutputStreamX os) throws IOException {
		os.writeObject(world);
	}

	public void receive(DataInputStreamX is) throws IOException {
		world = (WWWorld) is.readObject();
	}

}
