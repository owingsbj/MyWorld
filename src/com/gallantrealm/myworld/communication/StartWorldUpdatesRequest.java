package com.gallantrealm.myworld.communication;

import java.io.IOException;

/**
 * This client request tells the server to start pushing updates to the client. The server will then start pushing
 * updates to the client (on a separate port) until the client sends a request to disconnect or the client connection
 * dies.
 */
public class StartWorldUpdatesRequest extends ClientRequest {
	static final long serialVersionUID = 0;

	private double renderingThreshold;

	// TODO possibly send last server update time, to avoid resending entire model upon reconnecting.  This would require
	// that the timestamp data on objects be saved somewhere though

	public StartWorldUpdatesRequest() {
	}

	public StartWorldUpdatesRequest(double renderingThreshold) {
		this.renderingThreshold = renderingThreshold;
	}

	public double getRenderingThreshold() {
		return renderingThreshold;
	}

	public void send(DataOutputStreamX os) throws IOException {
		os.writeDouble(renderingThreshold);
	}

	public void receive(DataInputStreamX is) throws IOException {
		renderingThreshold = is.readDouble();
	}

}
