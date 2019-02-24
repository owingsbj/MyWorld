package com.gallantrealm.myworld.server.exits;

import com.gallantrealm.myworld.model.WWWorld;

/**
 * This interface can be implemented by classes that construct worlds. The name of the class can be passed to the server
 * using the -create option.
 */
public interface WorldCreator {
	public WWWorld createWorld();
}
