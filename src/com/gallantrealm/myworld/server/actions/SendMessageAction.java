package com.gallantrealm.myworld.server.actions;

import com.gallantrealm.myworld.communication.ClientRequest;
import com.gallantrealm.myworld.communication.Connection;
import com.gallantrealm.myworld.communication.SendMessageRequest;
import com.gallantrealm.myworld.model.WWObject;
import com.gallantrealm.myworld.model.WWUser;
import com.gallantrealm.myworld.model.WWWorld;

/**
 * Handles requests to send messages from a client avatar to all other avatars within receiving range.
 */
public class SendMessageAction extends ServerAction {

	@Override
	public WWUser doRequest(ClientRequest request, WWUser sendingUser, WWWorld world, Connection connection) throws Exception {
		SendMessageRequest sendMessageRequest = (SendMessageRequest) request;
		String message = sendMessageRequest.getMessage();

		// Queue the message up to all "close" connected users (transient info)
		WWObject sendingAvatar = world.objects[sendingUser.getAvatarId()];
		world.sendMessage(sendingUser, sendingAvatar, message, 10.0);

		return sendingUser;
	}

	@Override
	public Class getHandledRequestType() {
		return SendMessageRequest.class;
	}

}
