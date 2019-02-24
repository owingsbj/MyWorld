package com.gallantrealm.myworld.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import com.gallantrealm.myworld.Globals;
import com.gallantrealm.myworld.communication.Communications;
import com.gallantrealm.myworld.communication.TCPCommunications;
import com.gallantrealm.myworld.model.WWColor;
import com.gallantrealm.myworld.model.WWCylinder;
import com.gallantrealm.myworld.model.WWSimpleShape;
import com.gallantrealm.myworld.model.WWSphere;
import com.gallantrealm.myworld.model.WWUser;
import com.gallantrealm.myworld.model.WWVector;
import com.gallantrealm.myworld.model.WWWorld;
import com.gallantrealm.myworld.server.exits.WorldCreator;

/**
 * This is the main class of the MyWorld server. A console window is provided, but this program can run headless as
 * well, in which case information will be printed to System.out.
 */
public class MyWorldServer {

	private static Logger logger = Logger.getLogger("MyWorld.server");

	/**
	 * @param args
	 *            no arguments are understood
	 */
	public static void main(String[] args) {

		String worldFileName = "MyWorld.dat";
		int port = 8880;
		int clientLimit = 10;
		boolean reset = false;
		boolean create = false;
		String worldName = null;

		// Set up main log handler
		Logger parentLogger = Logger.getLogger("MyWorld");
		parentLogger.setUseParentHandlers(false);
		parentLogger.addHandler(new Handler() {
			@Override
			public void close() throws SecurityException {
			}

			@Override
			public void flush() {
			}

			@Override
			public void publish(LogRecord record) {
				System.out.println(record.getLevel() + " " + record.getMessage());
			}
		});

		// Parse options
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if (arg.startsWith("-")) {
				String option = arg.substring(1);
				if (option.equals("port")) {
					String portString = option.substring(4);
					port = Integer.decode(portString);
				} else if (option.equals("clientLimit")) {
					String clientLimitString = args[++i];
					clientLimit = Integer.decode(clientLimitString);
				} else if (option.equals("reset")) {
					reset = true;
				} else if (option.startsWith("create")) {
					reset = true;
					create = true;
					worldName = args[++i];
				} else if (option.startsWith("trace")) {
					parentLogger.setLevel(Level.FINE);
				}
			} else {
				worldFileName = arg;
			}
		}

		// Create or restore the world
		WWWorld world;
		try {
			File worldFile = new File(worldFileName);
			if (worldFile.exists() && !reset) {
				FileInputStream worldInputStream = new FileInputStream(worldFileName);
				ObjectInputStream worldObjectStream = new ObjectInputStream(worldInputStream);
				try {
					world = (WWWorld) worldObjectStream.readObject();
				} catch (Exception e) {
					e.printStackTrace();
					System.out.println("The world's state could not be restored.  Use the backup copy.  Delete MyWorld.dat and rename MyWorld.bak to MyWorld.dat.");
					System.exit(-1);
					return;
				}
				worldObjectStream.close();
			} else {
				if (create) {
					world = createWorld(worldName);
				} else {
					world = createDefaultWorld();
				}
			}

			Communications communications = new TCPCommunications();

			// Start serving the world
			MyWorldServer server = new MyWorldServer(world, communications, port, clientLimit);
			server.startServer(false);

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

	}

	/**
	 * Create the default world.
	 */
	public static WWWorld createDefaultWorld() {

		// create a world, add ground, some objects, and one "egg" user
		WWWorld world = new WWWorld(true, true, null, 25, false);
		world.setGravity(9.8f); // earth gravity

		// the ground
		WWSimpleShape ground = new WWCylinder(); // for the ground
		ground.setColor(WWSimpleShape.SIDE_TOP, new WWColor(0x008000)); // green, like grass
		ground.setPosition(new WWVector(0, 0, -50.5f));
		ground.setSize(new WWVector(1000, 1000, 100));
		world.addObject(ground);

		// user
		WWUser user = new WWUser();
		user.setName("guest");
		world.addUser(user);
		WWSimpleShape avatar1 = new WWSphere();
		avatar1.setName("guest");
		avatar1.setSize(new WWVector(1, 1, 1));
		avatar1.setTaperX(0.25f);
		avatar1.setTaperY(0.25f);
		avatar1.setTextureURL(WWSimpleShape.SIDE_ALL, "media/guest.png");
		avatar1.setPhysical(true); // so "happy" will hug the ground
		avatar1.setDensity(0.1f);
		avatar1.setFreedomRotateX(false);
		avatar1.setFreedomRotateY(false);
		int avatar1id = world.addObject(avatar1);
		user.setAvatarId(avatar1id);

		// a "super" user
		WWUser god = new WWUser();
		god.setName("god");
		god.setAvatarId(-1); // this super user has no avatar
		world.addUser(god);

		return world;
	}

	/**
	 * Creates a world programmatically from a Java class.
	 */
	public static WWWorld createWorld(String worldName) {
		try {
			Class worldCreatorClass = MyWorldServer.class.getClassLoader().loadClass(worldName);
			WorldCreator worldCreator = (WorldCreator) worldCreatorClass.newInstance();
			WWWorld world = worldCreator.createWorld();
			return world;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	static final int CLIENT_LIMIT = 10;
	static final int SERVER_PORT = 8880;

	RouterThread routerThread;
	ArrayList<ClientInfo> clientInfos;
	WWWorld world;
	String worldFileName;
	Communications communications;
	int port;
	int clientLimit;

	public MyWorldServer(WWWorld world, Communications communications, int port, int clientLimit) {
		logger.log(Level.INFO, Globals.getName() + " server, version " + Globals.getVersion() + "." + Globals.getRelease());

		this.world = world;
		this.communications = communications;
		this.port = port;
		this.clientLimit = clientLimit;

		// Create the worker thread arrays
		clientInfos = new ArrayList<ClientInfo>();
	}

	/**
	 * Starts the server's physics thread, which iterates through the objects of the server, and applies physics as
	 * needed. Starts the server's router thread, which will listen for clients and start worker threads as needed.
	 */
	public synchronized void startServer(boolean saveWorld) {
		if (routerThread == null) {
			logger.log(Level.INFO, "Starting router thread");
			routerThread = new RouterThread(communications, port, clientLimit, world);
			routerThread.start();
		}
	}

	public synchronized void stopServer() {
		if (routerThread != null) {
			logger.log(Level.INFO, "Stopping router thread");
			routerThread.interrupt();
			routerThread = null;
		}
	}

}
