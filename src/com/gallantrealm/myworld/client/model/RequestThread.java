package com.gallantrealm.myworld.client.model;

import java.util.ArrayList;

import com.gallantrealm.myworld.communication.ClientRequest;
import com.gallantrealm.myworld.communication.Connection;
import com.gallantrealm.myworld.communication.CreateObjectRequest;
import com.gallantrealm.myworld.communication.CreateObjectResponse;
import com.gallantrealm.myworld.communication.DataInputStreamX;
import com.gallantrealm.myworld.communication.DataOutputStreamX;
import com.gallantrealm.myworld.communication.DisconnectRequest;
import com.gallantrealm.myworld.communication.KeepAliveRequest;

/**
 * This class provides a secondary thread with queue for requests to the server, to allow the client UI to continue
 * while requests are sent to the server for processing. Features of the queue allow for updates to existing requests.
 */
public class RequestThread extends Thread {

	ClientModel clientModel;
	Connection requestConnection;
	ArrayList<ClientRequest> requestQueue;
	Object queueSemaphore;

	public RequestThread(ClientModel clientModel, Connection requestConnection) {
		super("RequestThread");
		this.clientModel = clientModel;
		this.requestConnection = requestConnection;
		requestQueue = new ArrayList<ClientRequest>();
		queueSemaphore = new Object();
	}

	public void queue(ClientRequest request) {
		synchronized (queueSemaphore) {
			requestQueue.add(request);
			queueSemaphore.notify();
		}
	}

	@Override
	public void run() {
		int nRequestsSent = 0;
		ClientRequest request = null;
		do {
			try {
				synchronized (queueSemaphore) {
					if (requestQueue.isEmpty()) {
						queueSemaphore.wait(5000);
					}
					if (requestQueue.size() > 0) {
						request = requestQueue.remove(0);
					} else {
						request = new KeepAliveRequest();
					}
				}
				DataOutputStreamX sendStream = requestConnection.getSendStream(5000);
				sendStream.writeObject(request);
				requestConnection.send(5000);
				if (request instanceof CreateObjectRequest) {
					DataInputStreamX receiveStream = requestConnection.receive(5000);
					CreateObjectResponse response = (CreateObjectResponse) receiveStream.readObject();
					clientModel.setLastCreatedObjectId(response.getObjectId());
				}
				nRequestsSent++;
				// DebugConsole.display("Requests sent: ", nRequestsSent);
			} catch (InterruptedException e) {
				return;
			} catch (Exception e) {
				e.printStackTrace();
				clientModel.showMessage("There is a problem communicating with the server.");
				return;
				// TODO restore request connection (unless interrupted exception)
			}
		} while (!(request instanceof DisconnectRequest));
	}

}
