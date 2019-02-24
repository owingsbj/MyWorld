package com.gallantrealm.myworld.communication;

import java.io.IOException;

/**
 * Response to the disconnect request. Note that the client may receive pushes from the server for some period until the
 * server cleans up and finally disconnects the server push connection (sending a disconnect push).
 */
public class DisconnectResponse extends ServerResponse {
	static final long serialVersionUID = 0;

	public DisconnectResponse() {
	}

	public void send(DataOutputStreamX os) throws IOException {
	}

	public void receive(DataInputStreamX is) throws IOException {
	}

}
