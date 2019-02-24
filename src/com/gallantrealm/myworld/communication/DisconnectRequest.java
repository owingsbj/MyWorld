package com.gallantrealm.myworld.communication;

import java.io.IOException;

/**
 * Sent by the client when it wishes to disconnect from the server. This can be either when logging off or when
 * navigating to a world on a different server.
 */
public class DisconnectRequest extends ClientRequest {
	static final long serialVersionUID = 0;

	public DisconnectRequest() {
	}

	public void send(DataOutputStreamX os) throws IOException {
	}

	public void receive(DataInputStreamX is) throws IOException {
	}

}
