package com.gallantrealm.myworld.communication;

import java.io.IOException;

/**
 * Client request to connect to the server.
 */
public class ConnectRequest extends ClientRequest {
	static final long serialVersionUID = 0;

	String userid;
	String credentials;
	long currentClientTime;

	public ConnectRequest() {
	}

	public ConnectRequest(String userid, String credentials) {
		this.userid = userid;
		this.credentials = credentials;
		this.currentClientTime = System.currentTimeMillis();
	}

	public String getCredentials() {
		return credentials;
	}

	public String getUserid() {
		return userid;
	}

	public long getCurrentClientTime() {
		return currentClientTime;
	}

	public void send(DataOutputStreamX os) throws IOException {
		os.writeString(userid);
		os.writeString(credentials);
		os.writeLong(currentClientTime);
	}

	public void receive(DataInputStreamX is) throws IOException {
		userid = is.readString();
		credentials = is.readString();
		currentClientTime = is.readLong();
	}

}
