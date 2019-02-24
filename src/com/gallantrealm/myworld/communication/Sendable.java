package com.gallantrealm.myworld.communication;

import java.io.IOException;

/**
 * Classes that implement this interface can be sent/received.
 */
public interface Sendable {
	public void send(DataOutputStreamX os) throws IOException;

	public void receive(DataInputStreamX is) throws IOException;
}
