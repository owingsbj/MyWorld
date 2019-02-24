package com.gallantrealm.myworld.model;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

import com.gallantrealm.myworld.communication.Connection;
import com.gallantrealm.myworld.communication.DataInputStreamX;
import com.gallantrealm.myworld.communication.DataOutputStreamX;
import com.gallantrealm.myworld.communication.MessagePush;
import com.gallantrealm.myworld.communication.Sendable;

/**
 * This class keeps information about a user of the world. This information persists regardless of if the user is
 * connected to the server or not. (The connected property indicates if the user is connected.)
 * <p>
 * A special object, known as the avatar is used to represent the user in the world. Typically, the client establishes a
 * camera view relative to the avatar in order to view the world as the avatar moves.
 */
public class WWUser extends WWEntity implements Serializable, Cloneable, Sendable {
	static final long serialVersionUID = 1L;

	private transient boolean connected;
	private int avatarId;

	class QueuedMessage {
		String name;;
		String message;

		QueuedMessage(String name, String message) {
			this.name = name;
			this.message = message;
		}

		public String getName() {
			return name;
		}

		public String getMessage() {
			return message;
		}
	}

	private transient ArrayList<QueuedMessage> messageQueue;

	public WWUser() {
	}

	public boolean isConnected() {
		return connected;
	}

	public synchronized void connect() {
		connected = true;
		setLastModifyTime(getWorldTime());
	}

	public synchronized void disconnect() {
		connected = false;
		setLastModifyTime(getWorldTime());
	}

	public int getAvatarId() {
		return avatarId;
	}

	public void setAvatarId(int avatarId) {
		this.avatarId = avatarId;
		setLastModifyTime(getWorldTime());
	}

	@Override
	public void send(DataOutputStreamX os) throws IOException {
		os.writeBoolean(connected);
		os.writeInt(avatarId);
		super.send(os);
	}

	@Override
	public void receive(DataInputStreamX is) throws IOException {
		connected = is.readBoolean();
		avatarId = is.readInt();
		super.receive(is);
	}

	public void queueMessage(String name, String message) {
		if (messageQueue == null) {
			messageQueue = new ArrayList<QueuedMessage>();
		}
		QueuedMessage queuedMessage = new QueuedMessage(name, message);
		synchronized (messageQueue) {
			messageQueue.add(queuedMessage);
		}
	}

	public boolean hasQueueMessages() {
		if (messageQueue == null) {
			return false;
		}
		return !messageQueue.isEmpty();
	}

	public ArrayList<QueuedMessage> getMessageQueue() {
		return messageQueue;
	}

	public void sendQueuedMessages(Connection connection, int timeout) throws IOException {
		while (hasQueueMessages()) {
			ArrayList<QueuedMessage> messageQueue = getMessageQueue();
			synchronized (messageQueue) {
				QueuedMessage queuedMessage = messageQueue.remove(0);
				DataOutputStreamX sendStream = connection.getSendStream(timeout);
				sendStream.writeObject(new MessagePush(queuedMessage.getName(), queuedMessage.getMessage()));
				//connection.send(timeout);
			}
		}
	}

}
