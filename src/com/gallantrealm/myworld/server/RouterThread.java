package com.gallantrealm.myworld.server;

import com.gallantrealm.myworld.communication.Communications;
import com.gallantrealm.myworld.communication.Connection;
import com.gallantrealm.myworld.model.WWWorld;

/**
 * This thread waits for new clients to connect to the world. It then checks for availability and if there are
 * available resources it will begin bidirectional communication with the client. If there are not enough resources, it
 * will atleast return a meaningful response to the client to let it know the situation.
 */
public class RouterThread extends Thread {

	int serverPort;
	int clientLimit;
	ThreadGroup requestThreadGroup;
	WWWorld world;
	Communications communications;

	public RouterThread(Communications communications, int serverPort, int clientLimit, WWWorld world) {
		super("RouterThread");
		this.communications = communications;
		this.serverPort = serverPort;
		this.clientLimit = clientLimit;
		this.world = world;
		requestThreadGroup = new ThreadGroup("ClientRequestThreads");
		setPriority(10);
	}

	@Override
	public void run() {
		try {
			while (true) {
				Connection clientRequestConnection = communications.acceptConnection(serverPort);
				if (requestThreadGroup.activeCount() >= clientLimit) {
					try {
						clientRequestConnection.disconnect();
						// TODO Tell client that it cannot be connected
						//ObjectOutputStream responseStream = new ObjectOutputStream(clientRequestSocket.getOutputStream());
						//ServerResponse response = new ConnectResponse(false, "Too many clients are using the server.", 0, 0, 0);
						//responseStream.writeObject(response);
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					RequestThread clientRequestThread = new RequestThread(requestThreadGroup, world, clientRequestConnection);
					clientRequestThread.start();
				}
			}
		} catch (InterruptedException e) {
			return;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void interrupt() {
		try {
			communications.close(serverPort);
		} catch (Exception e) {
		}
		requestThreadGroup.interrupt();
		super.interrupt();
	}

}
