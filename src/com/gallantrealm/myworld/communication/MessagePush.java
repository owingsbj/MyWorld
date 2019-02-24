package com.gallantrealm.myworld.communication;

import java.io.IOException;

/**
 * Server push to send a message to the client (from another user or the server).
 */
public class MessagePush extends ServerPush {
	static final long serialVersionUID = 0;

	private String name;
	private String message;

	public MessagePush() {
	}

	public MessagePush(String name, String message) {
		this.name = name;
		this.message = message;
	}

	public String getName() {
		return name;
	}

	public String getMessage() {
		return message;
	}

	public void send(DataOutputStreamX os) throws IOException {
		os.writeString(name);
		os.writeString(message);
	}

	public void receive(DataInputStreamX is) throws IOException {
		name = is.readString();
		message = is.readString();
	}

}
