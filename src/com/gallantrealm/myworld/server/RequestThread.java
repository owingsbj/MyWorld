package com.gallantrealm.myworld.server;

import java.net.SocketException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.gallantrealm.myworld.communication.ClientRequest;
import com.gallantrealm.myworld.communication.Connection;
import com.gallantrealm.myworld.communication.DataInputStreamX;
import com.gallantrealm.myworld.communication.DisconnectRequest;
import com.gallantrealm.myworld.model.WWUser;
import com.gallantrealm.myworld.model.WWWorld;
import com.gallantrealm.myworld.server.actions.ConnectAction;
import com.gallantrealm.myworld.server.actions.CreateObjectAction;
import com.gallantrealm.myworld.server.actions.DeleteObjectAction;
import com.gallantrealm.myworld.server.actions.DisconnectAction;
import com.gallantrealm.myworld.server.actions.MoveObjectAction;
import com.gallantrealm.myworld.server.actions.PauseWorldAction;
import com.gallantrealm.myworld.server.actions.ResumeWorldAction;
import com.gallantrealm.myworld.server.actions.SendMessageAction;
import com.gallantrealm.myworld.server.actions.ServerAction;
import com.gallantrealm.myworld.server.actions.ThrustObjectAction;
import com.gallantrealm.myworld.server.actions.TouchObjectAction;
import com.gallantrealm.myworld.server.actions.UpdateEntityAction;
import com.gallantrealm.myworld.server.actions.UpdateObjectAction;
import com.gallantrealm.myworld.server.actions.UpdateWorldPropertiesAction;
import com.gallantrealm.myworld.server.actions.WorldUpdatesAction;

/**
 * This thread processes requests from a single client.
 */
public class RequestThread extends Thread {

	/** Maps requests to the actions that support them. */
	private static HashMap<Class, ServerAction> supportedActions;

	private static void addSupportedAction(ServerAction serverAction) {
		supportedActions.put(serverAction.getHandledRequestType(), serverAction);
	}

	static {
		supportedActions = new HashMap<Class, ServerAction>();
		addSupportedAction(new ConnectAction());
		addSupportedAction(new UpdateEntityAction());
		addSupportedAction(new WorldUpdatesAction());
		addSupportedAction(new DisconnectAction());
		addSupportedAction(new CreateObjectAction());
		addSupportedAction(new UpdateObjectAction());
		addSupportedAction(new MoveObjectAction());
		addSupportedAction(new ThrustObjectAction());
		addSupportedAction(new DeleteObjectAction());
		addSupportedAction(new SendMessageAction());
		addSupportedAction(new UpdateWorldPropertiesAction());
		addSupportedAction(new TouchObjectAction());
		addSupportedAction(new PauseWorldAction());
		addSupportedAction(new ResumeWorldAction());
	}

	private final Logger logger = Logger.getLogger("com.gallantrealm.myworld.server");
	private final WWWorld world;
	private final Connection clientConnection;

	public RequestThread(ThreadGroup group, WWWorld world, Connection clientConnection) throws Exception {
		super(group, "RequestThread " + clientConnection.getHostAddress());
		this.world = world;
		this.clientConnection = clientConnection;
		setDaemon(true);
	}

	@Override
	public void run() {
		WWUser user = null;
		try {

			// Loop handling requests, until a disconnect request is received or a receive exception
			ClientRequest request;
			do {
				DataInputStreamX receiveStream = clientConnection.receive(10000);
				request = (ClientRequest) receiveStream.readObject();
				if (logger.isLoggable(Level.FINE)) {
					logger.log(Level.FINE, request.toString());
				}
				ServerAction action = supportedActions.get(request.getClass());
				if (action != null) {
					try {
						user = action.doRequest(request, user, world, clientConnection);
					} catch (InterruptedException e) {
						break;
					} catch (Exception e) { // handle bad actions without dropping connection
						e.printStackTrace();
					}
				}
			} while (!(request instanceof DisconnectRequest));

		} catch (SocketException e) {
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (user != null) {
			user.disconnect();
		}
	}

	@Override
	public void interrupt() {
		try {
			clientConnection.disconnect();
		} catch (Exception e) {
		}
		super.interrupt();
	}

}
