package com.gallantrealm.myworld.client.model;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.gallantrealm.myworld.communication.ConnectRequest;
import com.gallantrealm.myworld.communication.ConnectResponse;
import com.gallantrealm.myworld.communication.Connection;
import com.gallantrealm.myworld.communication.DataInputStreamX;
import com.gallantrealm.myworld.communication.DataOutputStreamX;
import com.gallantrealm.myworld.communication.DisconnectPush;
import com.gallantrealm.myworld.communication.KeepAlivePush;
import com.gallantrealm.myworld.communication.MessagePush;
import com.gallantrealm.myworld.communication.StartWorldUpdatesRequest;
import com.gallantrealm.myworld.communication.WorldUpdatePush;
import com.gallantrealm.myworld.model.WWObject;
import com.gallantrealm.myworld.model.WWUser;
import com.gallantrealm.myworld.model.WWWorld;

/**
 * This thread processes updates from the server for changes to the world model.
 */
public class UpdatesThread extends Thread {

	private final Logger logger = Logger.getLogger("com.gallantrealm.myworld.client");
	private final ClientModel clientModel;
	private final WWWorld world;

	public UpdatesThread(ClientModel clientModel) {
		setName("UpdatesThread");
		setDaemon(true);
		this.clientModel = clientModel;
		this.world = clientModel.world;
	}

	@Override
	public void run() {
		try {
			Connection connection = clientModel.getCommunications().connect(clientModel.getWorldAddressField(), 10000);

			DataOutputStreamX sendStream = connection.getSendStream(10000);
			sendStream.writeObject(new ConnectRequest(clientModel.getUserNameField(), ""));
			connection.send(10000);

			DataInputStreamX receiveStream = connection.receive(10000);
			ConnectResponse connectResponse = (ConnectResponse) receiveStream.readObject();

			sendStream = connection.getSendStream(10000);
			sendStream.writeObject(new StartWorldUpdatesRequest(clientModel.world.getRenderingThreshold()));
			connection.send(10000);

			int nUpdatesReceived = 0;
			boolean connected = true;
			while (connected) {
				receiveStream = connection.receive(10000);
				Object object = receiveStream.readObject();
				logger.log(Level.FINER, object.toString());
				if (object instanceof WorldUpdatePush) {
					WorldUpdatePush updatePush = (WorldUpdatePush) object;
					receiveWorldUpdate(updatePush, receiveStream);
					clientModel.fireClientModelChanged(ClientModelChangedEvent.EVENT_TYPE_WWMODEL_UPDATED);
				} else if (object instanceof MessagePush) {
					MessagePush messagePush = (MessagePush) object;
					clientModel.showMessage(messagePush.getName() + ": " + messagePush.getMessage());
				} else if (object instanceof DisconnectPush) {
					connected = false;
				} else if (object instanceof KeepAlivePush) {
					// nothing to do
				}
				nUpdatesReceived++;
				// DebugConsole.display("Updates received: ", nUpdatesReceived);
			}
			connection.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
			// TODO fire model event?
		}
	}

	private void receiveWorldUpdate(WorldUpdatePush updatePush, DataInputStreamX receiveStream) {
		try {
			int updateType = updatePush.getUpdateType();
			int id = updatePush.getEntityId();
			if (updateType == WorldUpdatePush.UPDATE_TYPE_WORLD_PROPERTIES) {
				world.receive(receiveStream);
			} else if (updateType == WorldUpdatePush.UPDATE_TYPE_OBJECT_CREATE) {
				WWObject receivedObject = (WWObject) updatePush.getEntity();
				world.updateObject(id, receivedObject);
			} else if (updateType == WorldUpdatePush.UPDATE_TYPE_OBJECT_DELETE) {
				world.removeObject(id);
			} else if (updateType == WorldUpdatePush.UPDATE_TYPE_OBJECT_MODIFY) {
				WWObject receivedObject = (WWObject) updatePush.getEntity();
				// TODO choose a more efficient update
				world.updateObject(id, receivedObject);
			} else if (updateType == WorldUpdatePush.UPDATE_TYPE_OBJECT_MOVE) {
				WWObject receivedObject = (WWObject) updatePush.getEntity();
				// TODO might need to optimize as much info sent over
				world.moveObject(id, receivedObject);
			} else if (updateType == WorldUpdatePush.UPDATE_TYPE_USER_CREATE) {
				WWUser receivedUser = (WWUser) updatePush.getEntity();
				// TODO will new users be placed at the correct index?!
				world.addUser(receivedUser);
			} else if (updateType == WorldUpdatePush.UPDATE_TYPE_USER_MODIFY) {
				WWUser receivedUser = (WWUser) updatePush.getEntity();
				WWUser user = world.getUser(id);
				user.setAvatarId(receivedUser.getAvatarId());
				user.setDescription(receivedUser.getDescription());
				user.setName(receivedUser.getName());
				user.setLastModifyTime(receivedUser.getLastModifyTime());
			} else if (updateType == WorldUpdatePush.UPDATE_TYPE_USER_DELETE) {
				WWUser receivedUser = (WWUser) updatePush.getEntity();
				WWUser user = world.getUser(id);
				user.setDeleted();
				user.setLastModifyTime(receivedUser.getLastModifyTime());
			}
		} catch (Exception e) { // errors here won't cause the communications to fail but indicate a logic problem
			e.printStackTrace();
		}
	}

}
