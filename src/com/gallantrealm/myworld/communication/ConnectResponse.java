package com.gallantrealm.myworld.communication;

import java.io.IOException;

/**
 * This response is sent when a client first connects to the server. The client may not be able to be supported on the
 * server, due to load, but in any case this response is returned.
 */
public class ConnectResponse extends ServerResponse {
	static final long serialVersionUID = 0;
	private boolean connected;
	private String message;
	private int userId;
	private long currentWorldTime;
	private long timeDifference;

	public ConnectResponse() {
	}

	public ConnectResponse(boolean connected, String message, int userId, long currentWorldTime, long timeDifference) {
		this.connected = connected;
		this.message = message;
		this.userId = userId;
		this.currentWorldTime = currentWorldTime;
		this.timeDifference = timeDifference;
	}

	public boolean isConnected() {
		return connected;
	}

	public String getMessage() {
		return message;
	}

	public int getUserId() {
		return userId;
	}

	public long getCurrentWorldTime() {
		return currentWorldTime;
	}

	public long getTimeDifference() {
		return timeDifference;
	}

	public String toString() {
		return getClass().getName() + "[connected: " + isConnected() + " message: " + message + " userId: " + userId + " worldTime: " + currentWorldTime + "]";
	}

	public void send(DataOutputStreamX os) throws IOException {
		os.writeBoolean(connected);
		os.writeString(message);
		os.writeInt(userId);
		os.writeLong(currentWorldTime);
		os.writeLong(timeDifference);
	}

	public void receive(DataInputStreamX is) throws IOException {
		connected = is.readBoolean();
		message = is.readString();
		userId = is.readInt();
		currentWorldTime = is.readLong();
		timeDifference = is.readLong();
	}

}
