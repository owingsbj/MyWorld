package com.gallantrealm.myworld.communication;

import java.io.IOException;

/**
 * Response to the update object request. Note that the server may send the updated state of the object to the client at
 * any point in time, before, during or after the response, using a server push.
 */
public class UpdateObjectResponse extends ServerResponse {
	static final long serialVersionUID = 0;

	public UpdateObjectResponse() {
	}

	public void send(DataOutputStreamX os) throws IOException {
	}

	public void receive(DataInputStreamX is) throws IOException {
	}

}
