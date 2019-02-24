package com.gallantrealm.myworld.communication;

import java.io.IOException;

/**
 * This message is sent from servers to clients every so often to let the client's know that communication is still
 * okay.
 */
public class KeepAlivePush extends ServerPush {

	public KeepAlivePush() {
	}

	public void receive(DataInputStreamX is) throws IOException {
	}

	public void send(DataOutputStreamX os) throws IOException {
	}

}
