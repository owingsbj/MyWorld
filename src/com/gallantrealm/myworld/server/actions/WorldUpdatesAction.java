package com.gallantrealm.myworld.server.actions;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.gallantrealm.myworld.communication.ClientRequest;
import com.gallantrealm.myworld.communication.Connection;
import com.gallantrealm.myworld.communication.DataOutputStreamX;
import com.gallantrealm.myworld.communication.DisconnectPush;
import com.gallantrealm.myworld.communication.KeepAlivePush;
import com.gallantrealm.myworld.communication.StartWorldUpdatesRequest;
import com.gallantrealm.myworld.communication.WorldUpdatePush;
import com.gallantrealm.myworld.model.WWObject;
import com.gallantrealm.myworld.model.WWUser;
import com.gallantrealm.myworld.model.WWVector;
import com.gallantrealm.myworld.model.WWWorld;

/**
 * This action is special, entering into a loop where periodically updates of the world are sent to the client. The
 * client work thread does not return from this action. Instead, another action (on a different thread) for the same
 * client is used to cancel the updating.
 */
public class WorldUpdatesAction extends ServerAction {

	private final Logger logger = Logger.getLogger("com.gallantrealm.myworld.server");

	@Override
	public WWUser doRequest(ClientRequest request, WWUser user, WWWorld world, Connection connection) throws Exception {
		StartWorldUpdatesRequest startUpdatesRequest = (StartWorldUpdatesRequest) request;

		long lastUpdateTime = 0;
		long[] objectsLastUpdateTime = new long[WWWorld.MAX_OBJECTS];
		while (user != null && user.isConnected()) {
			long updateTime = world.getWorldTime();
			sendUpdatesToClient(world, user, connection, lastUpdateTime, objectsLastUpdateTime, startUpdatesRequest.getRenderingThreshold());
			if (user.hasQueueMessages()) {
				sendQueuedMessages(user, connection);
			}
			logger.log(Level.FINER, "sent update at: " + lastUpdateTime);
			lastUpdateTime = updateTime;
			long currentTime = world.getWorldTime();
			Thread.sleep(Math.max(0, 250 - (currentTime - updateTime))); // try to update four times a second
		}
		DataOutputStreamX sendStream = connection.getSendStream(TIMEOUT);
		DisconnectPush disconnectPush = new DisconnectPush();
		sendStream.writeObject(disconnectPush);
		connection.send(TIMEOUT);
		return user;
	}

	@Override
	public Class getHandledRequestType() {
		return StartWorldUpdatesRequest.class;
	}

	/**
	 * Send an world update push to the client, but only if something has changed. Also, avoid sending updates for
	 * objects outside of this user's range of "view".
	 */
	public void sendUpdatesToClient(WWWorld world, WWUser thisUser, Connection connection, long lastUpdateTime, long[] objectsLastUpdateTime, double renderingThreshold) throws Exception {
		boolean sentMessage = false;

		// Send world properties if they've changed
		if (lastUpdateTime < world.getLastModifyTime()) {
			DataOutputStreamX sendStream = connection.getSendStream(TIMEOUT);
			sendStream.writeObject(new WorldUpdatePush(WorldUpdatePush.UPDATE_TYPE_WORLD_PROPERTIES, 0, null));
			world.send(sendStream);
			//connection.send(TIMEOUT);
			sentMessage = true;
		}

		// Send info about users (done first, so avatar relationships will be discovered on client)
		WWUser[] users = world.getUsers();
		for (int i = 0; i < users.length; i++) {
			WWUser user = users[i];
			if (user != null) {
				if (user.isDeleted()) {
					if (lastUpdateTime <= user.getLastModifyTime()) {
						DataOutputStreamX sendStream = connection.getSendStream(TIMEOUT);
						sendStream.writeObject(new WorldUpdatePush(WorldUpdatePush.UPDATE_TYPE_USER_DELETE, i, null));
						//connection.send(TIMEOUT);
						sentMessage = true;
					}
				} else if (lastUpdateTime <= user.getCreateTime()) {
					DataOutputStreamX sendStream = connection.getSendStream(TIMEOUT);
					sendStream.writeObject(new WorldUpdatePush(WorldUpdatePush.UPDATE_TYPE_USER_CREATE, i, user));
					//connection.send(TIMEOUT);
					sentMessage = true;
				} else if (lastUpdateTime <= user.getLastModifyTime()) {
					DataOutputStreamX sendStream = connection.getSendStream(TIMEOUT);
					sendStream.writeObject(new WorldUpdatePush(WorldUpdatePush.UPDATE_TYPE_USER_MODIFY, i, user));
					//connection.send(TIMEOUT);
					sentMessage = true;
				}
			}
		}

		// Send info on objects
		long time = world.getWorldTime();
		WWVector avatarPosition;
		int avatarId = thisUser.getAvatarId();
		if (avatarId >= 0) {
			WWObject avatar = world.objects[thisUser.getAvatarId()];
			if (avatar != null) {
				avatarPosition = avatar.getPosition(time);
			} else {
				avatarPosition = new WWVector(0, 0, 0);
			}
		} else {
			avatarPosition = new WWVector(0, 0, 0);
		}
		WWObject[] objects = world.getObjects();
		int lastObjectIndex = world.lastObjectIndex;
		WWVector objectPosition = new WWVector();
		for (int i = 0; i < lastObjectIndex; i++) {
			WWObject object = objects[i];
			if (object != null) {

				// If the object is within rendering threshold (considering size), send any updates for it
				object.getPosition(objectPosition, time);
				if (avatarPosition.distanceFrom(objectPosition) / object.extent <= renderingThreshold) {
					long lastObjectUpdateTime = objectsLastUpdateTime[i];
					long objectCreateTime = object.getCreateTime();
					long lastObjectModifyTime = object.getLastModifyTime();
					long lastObjectMoveTime = object.getLastMoveTime();
					if (object.deleted) {
						if (lastObjectUpdateTime < lastObjectModifyTime) {
							DataOutputStreamX sendStream = connection.getSendStream(TIMEOUT);
							sendStream.writeObject(new WorldUpdatePush(WorldUpdatePush.UPDATE_TYPE_OBJECT_DELETE, i, null));
							//connection.send(TIMEOUT);
							objectsLastUpdateTime[i] = lastObjectModifyTime;
							sentMessage = true;
						}
					} else if (lastObjectUpdateTime < objectCreateTime) {
						DataOutputStreamX sendStream = connection.getSendStream(TIMEOUT);
						sendStream.writeObject(new WorldUpdatePush(WorldUpdatePush.UPDATE_TYPE_OBJECT_CREATE, i, object));
						//connection.send(TIMEOUT);
						objectsLastUpdateTime[i] = objectCreateTime;
						sentMessage = true;
					} else if (lastObjectUpdateTime < lastObjectModifyTime) {
						DataOutputStreamX sendStream = connection.getSendStream(TIMEOUT);
						sendStream.writeObject(new WorldUpdatePush(WorldUpdatePush.UPDATE_TYPE_OBJECT_MODIFY, i, object));
						//connection.send(TIMEOUT);
						sentMessage = true;
						objectsLastUpdateTime[i] = lastObjectModifyTime;
					} else if (lastObjectUpdateTime < lastObjectMoveTime) {
						DataOutputStreamX sendStream = connection.getSendStream(TIMEOUT);
						sendStream.writeObject(new WorldUpdatePush(WorldUpdatePush.UPDATE_TYPE_OBJECT_MOVE, i, object));
						//connection.send(TIMEOUT);
						sentMessage = true;
						objectsLastUpdateTime[i] = lastObjectMoveTime;
					}
				} else {
					// TODO figure out how to delete on client without disregarding update times
				} // if object outside of threshold

			} // if object != null
		} // for all objects

		// If no message was sent, send one to keep the connection alive
		if (!sentMessage) {
			DataOutputStreamX sendStream = connection.getSendStream(TIMEOUT);
			sendStream.writeObject(new KeepAlivePush());
		}
		connection.send(TIMEOUT);
	}

	public void sendQueuedMessages(WWUser user, Connection connection) throws Exception {
		if (user.hasQueueMessages()) {
			user.sendQueuedMessages(connection, TIMEOUT);
		}
	}

}
