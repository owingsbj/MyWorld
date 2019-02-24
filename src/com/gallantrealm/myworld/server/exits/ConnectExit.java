package com.gallantrealm.myworld.server.exits;

import com.gallantrealm.myworld.model.WWUser;
import com.gallantrealm.myworld.model.WWWorld;

/**
 * This exit is to allow for authentication of the user. The information provided by the client can be used to identify
 * the user definition and authenticate if the client is a valid user of the system.
 * <p>
 * Implementations can change this exit to provide any type of authentication necessary. The authentication process can
 * even create new users if that makes sense.
 * <p>
 * The default implementation checks for user name matching the name in the definitions. If it does, that user is used.
 */
public class ConnectExit {

	/**
	 * Obtain the WWUser given the user name and credentials.
	 * 
	 * @param world
	 *            the world being connected to
	 * @param userName
	 *            the name of the user
	 * @param credentials
	 *            the credentials of the user (usually a password or identity of the user on another authentication
	 *            server)
	 * @param message
	 *            an array of one element where a message can be returned. This message will be sent to the client.
	 * @return
	 */
	public static WWUser getAuthenticatedUser(WWWorld world, String userName, String credentials, String[] message) {
		if (userName.equals("")) {
			message[0] = "Missing user name";
			return null;
		}
		WWUser user = world.getUser(userName);
		if (user == null) {
			message[0] = "User " + userName + " does not exist in the world.";
		}
		return user;
	}

}
