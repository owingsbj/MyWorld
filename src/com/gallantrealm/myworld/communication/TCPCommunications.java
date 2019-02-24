package com.gallantrealm.myworld.communication;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;

public class TCPCommunications extends Communications {

	private final Map<Integer, ServerSocket> serverSockets;

	public TCPCommunications() {
		serverSockets = new HashMap<Integer, ServerSocket>();
	}

	@Override
	public Connection connect(String worldAddress, int timeout) throws Exception {
		TCPConnection connection = new TCPConnection();

		// Parse world address into host and port
		String host;
		int port;
		if (worldAddress == "") {
			host = "localhost";
			port = 8880;
		} else if (worldAddress.contains(":")) {
			int portOffset = worldAddress.indexOf(":");
			host = worldAddress.substring(0, portOffset);
			port = Integer.decode(worldAddress.substring(portOffset + 1)).intValue();
		} else {
			host = worldAddress;
			port = 8880;
		}

		connection.socket = new Socket();
		SocketAddress socketAddress = new InetSocketAddress(host, port);
		connection.socket.connect(socketAddress, timeout);

		// Send a message with the local time
		long localTime1 = System.currentTimeMillis();
		new DataOutputStream(connection.socket.getOutputStream()).writeLong(localTime1);
		connection.socket.getOutputStream().flush();

		// Receive a reply message with the remote time
		long remoteTime = new DataInputStream(connection.socket.getInputStream()).readLong();

		// Calculate the delta time for the connection
		long localTime2 = System.currentTimeMillis();
		long latency = (localTime2 - localTime1) / 2;
		connection.deltaTime = localTime1 - remoteTime + latency;

		return connection;
	}

	@Override
	public Connection acceptConnection(int port) throws Exception {
		ServerSocket serverSocket = serverSockets.get(port);
		if (serverSocket == null) {
			serverSocket = new ServerSocket(port);
			serverSockets.put(port, serverSocket);
		}
		Socket socket = serverSocket.accept();
		TCPConnection connection = new TCPConnection();
		connection.socket = socket;

		// Receive a message with remote time
		long remoteTime = new DataInputStream(connection.socket.getInputStream()).readLong();

		// Send a message with the local time
		long localTime1 = System.currentTimeMillis();
		new DataOutputStream(connection.socket.getOutputStream()).writeLong(localTime1);
		connection.socket.getOutputStream().flush();

		// Calculate the delta time for the connection
		long localTime2 = System.currentTimeMillis();
		long latency = (localTime2 - localTime1) / 2;
		connection.deltaTime = localTime1 - remoteTime + latency;

		return connection;
	}

	@Override
	public void close(int port) throws IOException {
		ServerSocket serverSocket = serverSockets.get(port);
		serverSocket.close();
	}

	private class TCPConnection implements Connection {
		public Socket socket;
		public DataOutputStreamX socketOutputStream;
		public DataInputStreamX socketInputStream;
		long deltaTime;

		public void disconnect() throws Exception {
			socket.close();
		}

		public DataOutputStreamX getSendStream(int timeout) throws IOException {
			socket.setSoTimeout(timeout);
			if (socketOutputStream == null) {
				socketOutputStream = new DataOutputStreamX(socket.getOutputStream());
			}
			return socketOutputStream;
		}

		public void send(int timeout) throws IOException {
			socket.setSoTimeout(timeout);
			socketOutputStream.flush();
		}

		public DataInputStreamX receive(int timeout) throws IOException {
			socket.setSoTimeout(timeout);
			if (socketInputStream == null) {
				socketInputStream = new DataInputStreamX(socket.getInputStream());
			}
			return socketInputStream;
		}

		public String getHostAddress() throws Exception {
			return socket.getInetAddress().getHostAddress();
		}

		public long getDeltaTime() {
			return deltaTime;
		}

	}

}
