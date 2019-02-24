package com.gallantrealm.myworld.server;

import java.util.ArrayList;

import com.gallantrealm.myworld.model.WWUser;

/**
 * A class to keep server-side information about a client that is actively connected. This is opposed to general client
 * information that is kept in the world (in WWClient).
 */
public class ClientInfo {

	WWUser client;
	ArrayList<RequestThread> clientRequestThreads;

	boolean disconnecting;

	public ClientInfo(WWUser client) {
		this.client = client;
		clientRequestThreads = new ArrayList<RequestThread>();
	}

	public void addClientRequestThread(RequestThread thread) {
		clientRequestThreads.add(thread);
	}

	public void removeClientRequestThread(RequestThread thread) {
		clientRequestThreads.remove(thread);
	}

	public boolean isDisconnecting() {
		return disconnecting;
	}

	public void setDisconnecting(boolean disconnecting) {
		this.disconnecting = disconnecting;
	}

}
