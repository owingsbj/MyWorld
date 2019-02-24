package com.gallantrealm.myworld.communication;

import java.io.IOException;

/**
 * This is a request that the client sends to the server every 5 seconds or so. If the server doesn't receive on within
 * 10 seconds, it will close connection with the client.
 */
public class KeepAliveRequest extends ClientRequest {

	public KeepAliveRequest() {
	}

	public void receive(DataInputStreamX is) throws IOException {
	}

	public void send(DataOutputStreamX os) throws IOException {
	}

}
