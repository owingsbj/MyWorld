package com.gallantrealm.myworld.communication;

import java.io.IOException;

/**
 * Response to the start world updates request. Note that this response may occur before, during or after the first
 * server push for world updates.
 */
public class StartWorldUpdatesResponse extends ServerResponse {
	static final long serialVersionUID = 0;

	public StartWorldUpdatesResponse() {
	}

	public void send(DataOutputStreamX os) throws IOException {
	}

	public void receive(DataInputStreamX is) throws IOException {
	}

}
