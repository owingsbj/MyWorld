package com.gallantrealm.myworld.communication;

import java.io.IOException;

/**
 * An abstract class providing the interface for the communications mechanism.
 */
public abstract class Communications {

	/**
	 * Connect to a server.
	 * 
	 * @param worldAddress
	 * @return
	 * @throws Exception
	 */
	public abstract Connection connect(String worldAddress, int timeout) throws Exception;

	/**
	 * Accept connection from a client.
	 * 
	 * @param port
	 * @return
	 * @throws Exception
	 */
	public abstract Connection acceptConnection(int port) throws Exception;

	public abstract void close(int port) throws IOException;
}
