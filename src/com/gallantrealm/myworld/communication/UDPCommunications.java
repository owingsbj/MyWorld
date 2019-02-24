package com.gallantrealm.myworld.communication;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UDPCommunications extends Communications {

	private static final int MAX_MESSAGES = 256;
	private static final int MAX_MESSAGE_LENGTH = 1024;

	private static Logger logger = Logger.getLogger("com.gallanrealm.myworld.communications");

	private final Map<Integer, DatagramSocket> serverSockets;

	public UDPCommunications() {
		serverSockets = new HashMap<Integer, DatagramSocket>();
	}

	@Override
	public Connection connect(String worldAddress, int timeout) throws Exception {
		UDPConnection connection = new UDPConnection();

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

		connection.datagramSocket = new DatagramSocket();
		System.out.println("Bound to local port " + connection.datagramSocket.getLocalPort());
		connection.datagramSocket.setSoTimeout(timeout);
		InetAddress inetAddress = InetAddress.getByName(host);
		connection.hostAddress = inetAddress.getHostAddress();
		connection.datagramPacket = new DatagramPacket(connection.receiveBuf, connection.receiveBuf.length, inetAddress, port);

		// Send a message with the local time
		long localTime1 = System.currentTimeMillis();
		String timeString = Long.toString(localTime1);
		connection.datagramPacket.setData(timeString.getBytes());
		connection.datagramSocket.send(connection.datagramPacket);

		// Receive a reply message with the remote time, and server/port to use for further communications
		connection.datagramSocket.receive(connection.datagramPacket);
		timeString = new String(connection.datagramPacket.getData(), 0, connection.datagramPacket.getLength());
		long remoteTime = Long.parseLong(timeString);

		// Calculate the delta time for the connection
		long localTime2 = System.currentTimeMillis();
		long latency = (localTime2 - localTime1) / 2;
		connection.deltaTime = localTime1 - remoteTime + latency;

		System.out.println("Connected, latency = " + latency + " delta time = " + connection.deltaTime);

		return connection;
	}

	@Override
	public Connection acceptConnection(int port) throws Exception {
		DatagramSocket serverSocket = serverSockets.get(port);
		if (serverSocket == null) {
			serverSocket = new DatagramSocket(port);
			serverSockets.put(port, serverSocket);
		}
		DatagramPacket dummyPacket = new DatagramPacket(new byte[MAX_MESSAGE_LENGTH], MAX_MESSAGE_LENGTH);
		serverSocket.receive(dummyPacket); // receive the dummy message
		String timeString = new String(dummyPacket.getData(), 0, dummyPacket.getLength());
		long remoteTime = Long.parseLong(timeString);
		long localTime = System.currentTimeMillis();

		UDPConnection connection = new UDPConnection();
		connection.datagramSocket = new DatagramSocket();
		connection.datagramSocket.setSoTimeout(10000); // requires a request every 5 seconds or so to stay alive
		connection.datagramPacket = new DatagramPacket(connection.receiveBuf, connection.receiveBuf.length, dummyPacket.getAddress(), dummyPacket.getPort());
		timeString = Long.toString(localTime);
		connection.datagramPacket.setData(timeString.getBytes());
		connection.datagramSocket.send(connection.datagramPacket); // send dummy message to give new port to client for further communications
		connection.deltaTime = localTime - remoteTime;
		return connection;
	}

	private class UDPConnection implements Connection {
		public String hostAddress;
		public DatagramSocket datagramSocket;
		public byte[] receiveBuf = new byte[MAX_MESSAGES * MAX_MESSAGE_LENGTH];
		public DatagramPacket datagramPacket; // for last communication sent or received
		long deltaTime;
		ByteArrayOutputStream byteStream;
		DataOutputStreamX sendStream;

		public void disconnect() throws Exception {
			datagramSocket.disconnect();
		}

		public DataOutputStreamX getSendStream(int timeout) throws IOException {
			byteStream = new ByteArrayOutputStream(MAX_MESSAGES * MAX_MESSAGE_LENGTH);
			sendStream = new DataOutputStreamX(byteStream);
			return sendStream;
		}

		public void send(int timeout) throws IOException {
			sendStream.close();
			byte[] sendBuf = byteStream.toByteArray();

			datagramSocket.setSoTimeout(timeout); // although not used for UDP sends
			int sent = 0;
			do {
				if (sendBuf.length - sent > MAX_MESSAGE_LENGTH) {
					datagramPacket.setData(sendBuf, sent, MAX_MESSAGE_LENGTH + 1); // the +1 indicates more on the way
					sent += MAX_MESSAGE_LENGTH;
				} else {
					datagramPacket.setData(sendBuf, sent, sendBuf.length - sent);
					sent += sendBuf.length - sent;
				}
				try {
					Thread.sleep(10);
				} catch (Exception e) {
				}
				;
				logger.log(Level.FINE, "Sending " + datagramPacket.getLength() + " bytes.");
				datagramSocket.send(datagramPacket);
			} while (sent < sendBuf.length);

		}

		public DataInputStreamX receive(int timeout) throws IOException {
			datagramSocket.setSoTimeout(timeout);
			int received = 0;
			do {
				datagramPacket.setData(receiveBuf, received, receiveBuf.length - received);
				datagramSocket.receive(datagramPacket);
				logger.log(Level.FINE, "Received " + datagramPacket.getLength() + " bytes.");
				received += Math.min(datagramPacket.getLength(), MAX_MESSAGE_LENGTH);
			} while (datagramPacket.getLength() > MAX_MESSAGE_LENGTH);
			ByteArrayInputStream byteStream = new ByteArrayInputStream(receiveBuf, 0, received);
			return new DataInputStreamX(byteStream);
		}

		public String getHostAddress() throws Exception {
			return hostAddress;
		}

		public long getDeltaTime() {
			return deltaTime;
		}
	}

	@Override
	public void close(int port) throws IOException {
		// TODO Auto-generated method stub

	}

}
