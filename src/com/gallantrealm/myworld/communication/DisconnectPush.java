package com.gallantrealm.myworld.communication;

import java.io.IOException;

/**
 * Server push to notify the client that the server is disconnecting the client. This will be the last push to the
 * client after which the server will close the connection with the client.
 */
public class DisconnectPush extends ServerPush {
	static final long serialVersionUID = 0;

	public DisconnectPush() {
	}

	public void send(DataOutputStreamX os) throws IOException {
	}

	public void receive(DataInputStreamX is) throws IOException {
	}

}
