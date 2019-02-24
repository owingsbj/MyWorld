package com.gallantrealm.myworld.communication;

import java.io.IOException;

/**
 * Client request to send a message.
 */
public class SendMessageRequest extends ClientRequest {
	static final long serialVersionUID = 0;

	String message;

	public SendMessageRequest() {
	}

	public SendMessageRequest(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}

	public void send(DataOutputStreamX os) throws IOException {
		os.writeString(message);
	}

	public void receive(DataInputStreamX is) throws IOException {
		message = is.readString();
	}

}
