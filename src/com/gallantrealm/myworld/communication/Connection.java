package com.gallantrealm.myworld.communication;

import java.io.IOException;

/**
 * Interface for a connection to a server.
 */
public interface Connection {
	public abstract void disconnect() throws Exception;

	public abstract DataOutputStreamX getSendStream(int timeout) throws IOException;

	public abstract void send(int timeout) throws IOException;

	public abstract DataInputStreamX receive(int timeout) throws IOException;

	public abstract String getHostAddress() throws Exception;

	public abstract long getDeltaTime();
}
