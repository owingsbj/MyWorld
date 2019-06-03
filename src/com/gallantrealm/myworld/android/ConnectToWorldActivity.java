package com.gallantrealm.myworld.android;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.gallantrealm.myworld.FastMath;
import com.gallantrealm.myworld.client.model.ClientModelChangedEvent;
import com.gallantrealm.myworld.client.model.ClientModelChangedListener;
import com.gallantrealm.myworld.communication.Communications;
import com.gallantrealm.myworld.communication.TCPCommunications;
import com.gallantrealm.myworld.model.WWBox;
import com.gallantrealm.myworld.model.WWColor;
import com.gallantrealm.myworld.model.WWCylinder;
import com.gallantrealm.myworld.model.WWMesh;
import com.gallantrealm.myworld.model.WWSimpleShape;
import com.gallantrealm.myworld.model.WWSphere;
import com.gallantrealm.myworld.model.WWTorus;
import com.gallantrealm.myworld.model.WWTranslucency;
import com.gallantrealm.myworld.model.WWUser;
import com.gallantrealm.myworld.model.WWVector;
import com.gallantrealm.myworld.model.WWWorld;
import com.gallantrealm.myworld.server.MyWorldServer;

public class ConnectToWorldActivity extends Activity {

	private AndroidClientModel clientModel;
	EditText urlText;
	Button connectButton;
	TextView messagesText;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		System.out.println(">ConnectToWorldActivity.onCreate");
		super.onCreate(savedInstanceState);

		// Set up main log handler
		Logger parentLogger = Logger.getLogger("myworld");
		try {
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
		} catch (Exception e) {
			// can happen due to security
		}

		setContentView(R.layout.connect_to_world);

		urlText = (EditText) findViewById(R.id.urlText);
		connectButton = (Button) findViewById(R.id.connectButton);
		messagesText = (TextView) findViewById(R.id.messagesText);
		connectButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				onConnectButtonClicked();
			}
		});

		clientModel = AndroidClientModel.getClientModel();
		clientModel.setLocalPhysicsThread(false); // TODO set to true when no local server
		clientModel.addClientModelChangedListener(new ClientModelChangedListener() {
			public void clientModelChanged(ClientModelChangedEvent event) {
				if (event.getEventType() == ClientModelChangedEvent.EVENT_TYPE_MESSAGE_RECEIVED) {
					//thread restrictions in android.. need to switch threads.    messagesText.setText(clientModel.getLastMessageReceived());
				} else if (event.getEventType() == ClientModelChangedEvent.EVENT_TYPE_CONNECTED) {
					Intent intent = new Intent(ConnectToWorldActivity.this, ShowWorldActivity.class);
					startActivity(intent);
				}
			}
		});

		System.out.println("<ConnectToWorldActivity.onCreate");
	}

	public void onConnectButtonClicked() {
		messagesText.setText("");
		clientModel.setUserNameField("jack"); //"guest");
		clientModel.setWorldAddressField(urlText.getText().toString());

		WWWorld world = null;
		if (clientModel.getWorldAddressField().startsWith("localhost")) {
			world = startupTheWorld();
		}
		clientModel.connect();
	}

	public WWWorld startupTheWorld() {
		System.out.println(">ConnectToWorldActivity.startupWorld");
		String worldFileName = getFileStreamPath("MyWorld.dat").getAbsolutePath();
		int port = 8880;
		int clientLimit = 10;
		boolean reset = true; //false;

		// Create or restore the world
		WWWorld world = null;
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
				}
				worldObjectStream.close();
			} else {
				world = createDemoWorld();
			}

			Communications communications = new TCPCommunications();

			// Start serving the world
			if (clientModel.getLocalServer() != null) {
				clientModel.getLocalServer().stopServer();
			}
			MyWorldServer server = new MyWorldServer(world, communications, port, clientLimit);
			clientModel.setLocalServer(server);
			server.startServer(true);

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		System.out.println("<ConnectToWorldActivity.startupWorld");
		return world;
	}

	/**
	 * Create the default world.
	 */
	public WWWorld createWorld() {

		// create a world, add ground, some objects, and one "egg" user
		WWWorld world = new WWWorld(true, true, null, 25, false);
		world.setGravity(9.8f); // earth gravity

		// the ground
		WWSimpleShape ground = new WWCylinder(); // for the ground
		ground.setColor(WWSimpleShape.SIDE_TOP, new WWColor(0x008000)); // green, like grass
		ground.setColor(WWSimpleShape.SIDE_BOTTOM, new WWColor(0x008000)); // green, like grass
		ground.setColor(WWSimpleShape.SIDE_SIDE1, new WWColor(0x008000)); // green, like grass
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
		avatar1.setTextureURL(WWSimpleShape.SIDE_ALL, "guest");
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

	public WWWorld createDemoWorld() {

		// The basic world (like earth)
		WWWorld world = new WWWorld();
		world.setGravity(9.8f); // earth gravity
		world.setFogDensity(0.3f);
		long time = world.getWorldTime();
		world.setCreateTime(time);
		world.setLastModifyTime(time);

		// the ground
		WWMesh ground = new WWMesh(); // for the ground
		ground.setName("ground");
		ground.setPickable(false);
		ground.setPenetratable(false);
		ground.setImpactSound("grass");
		ground.setSlidingSound("movingGrass");
		ground.setColor(WWSimpleShape.SIDE_TOP, new WWColor(0x00E000)); // green, like grass
		ground.setColor(WWSimpleShape.SIDE_ALL, new WWColor(0x404000)); // all others brown
		ground.setSize(new WWVector(1000, 1000, 250));
		ground.setPosition(new WWVector(0, 0, -10.0f));
		int meshSize = 100;
		ground.setMeshSize(meshSize, meshSize);
		// - roughen the general terrain
		for (int i = 0; i <= meshSize; i++) {
			for (int j = 0; j <= meshSize; j++) {
				ground.setMeshPoint(i, j, 0.5f + FastMath.random() * 0.02f);
			}
		}
		// - add a few peaks
		for (int i = 0; i < 50; i++) {
			int x = (int) (meshSize * FastMath.random());
			int y = (int) (meshSize * FastMath.random());
			float z = FastMath.random() * 0.0005f;
			int baseSize = (int) (FastMath.random() * 20) + 5;
			for (int cx = x - baseSize; cx < x + baseSize; cx++) {
				for (int cy = y - baseSize; cy < y + baseSize; cy++) {
					if (cx >= 0 && cx <= meshSize && cy >= 0 && cy <= meshSize) {
						float d = (float)Math.sqrt((cx - x) * (cx - x) + (cy - y) * (cy - y));
						if (d < baseSize) {
							ground.setMeshPoint(cx, cy, ground.getMeshPoint(cx, cy) + (baseSize - d) * (baseSize - d) * z);
						}
					}
				}
			}
		}
		// - add a few gorges
		for (int i = 0; i < 100; i++) {
			int x = (int) (meshSize * FastMath.random());
			int y = (int) (meshSize * FastMath.random());
			float z = -FastMath.random() * 0.00001f;
			int baseSize = (int) (FastMath.random() * 20) + 5;
			for (int cx = x - baseSize; cx < x + baseSize; cx++) {
				for (int cy = y - baseSize; cy < y + baseSize; cy++) {
					if (cx >= 0 && cx <= meshSize && cy >= 0 && cy <= meshSize) {
						float d = (float)Math.sqrt((cx - x) * (cx - x) + (cy - y) * (cy - y));
						if (d < baseSize) {
							ground.setMeshPoint(cx, cy, ground.getMeshPoint(cx, cy) + baseSize * (baseSize - d) * z);
						}
					}
				}
			}
		}

		ground.setTextureURL(WWSimpleShape.SIDE_TOP, "grass");
		ground.setTextureScaleX(WWSimpleShape.SIDE_TOP, 0.001f);
		ground.setTextureScaleY(WWSimpleShape.SIDE_TOP, 0.001f);
		world.addObject(ground);

		// the water
		WWTranslucency water = new WWTranslucency();
		water.setName("water");
		water.setPickable(false);
		water.setInsideLayerDensity(0.25f);
		water.setPosition(new WWVector(0, 0, -25));
		water.setSize(new WWVector(1000, 1000, 50));
		water.setSolid(false);
		water.setDensity(1.0f);
		water.setFriction(0.1f);
		water.setImpactSound("water");
		water.setSlidingSound("movingWater");
		water.setInsideColor(0x202040);
		water.setInsideTransparency(0.7f);
		water.setColor(WWSimpleShape.SIDE_ALL, new WWColor(0x8080F0));
		water.setColor(WWSimpleShape.SIDE_TOP, new WWColor(0xA0A0F0));
		water.setTransparency(WWSimpleShape.SIDE_TOP, 0.1f);
		water.setTextureURL(WWSimpleShape.SIDE_TOP, "water");
		water.setTextureScaleX(WWSimpleShape.SIDE_TOP, 0.001f);
		water.setTextureScaleY(WWSimpleShape.SIDE_TOP, 0.001f);
		water.setTextureVelocityX(WWSimpleShape.SIDE_TOP, 0.0001f);
		water.setColor(WWSimpleShape.SIDE_INSIDE_TOP, new WWColor(0xF0F0F0));
		water.setTransparency(WWSimpleShape.SIDE_INSIDE_TOP, 0.50f);
		water.setTextureURL(WWSimpleShape.SIDE_INSIDE_TOP, "water");
		water.setTextureScaleX(WWSimpleShape.SIDE_INSIDE_TOP, 0.001f);
		water.setTextureScaleY(WWSimpleShape.SIDE_INSIDE_TOP, 0.001f);
		water.setTextureVelocityX(WWSimpleShape.SIDE_INSIDE_TOP, 0.0001f);
		water.setColor(WWSimpleShape.SIDE_CUTOUT1, new WWColor(0x6060C0));
		water.setTransparency(WWSimpleShape.SIDE_CUTOUT1, 0.35f);
		world.addObject(water);

		// the sky
		WWSimpleShape sky = new WWSphere();
		sky.setName("sky");
		sky.setPickable(false);
		sky.setPosition(new WWVector(0, 0, -800));
		sky.setSize(new WWVector(2000, 5000, 5000));
		sky.setCutoutStart(0.5f); // half dome
		sky.setSolid(false); // for now.. otherwise physical objects pushed out of world
		sky.setFriction(0); // for now.. otherwise physical objects slowed down in hollowed area
		sky.setRotation(new WWVector(0, 90, 0));
		sky.setHollow(0.99f);
		sky.setTextureURL(WWSimpleShape.SIDE_INSIDE1, "sky");
		sky.setTextureScaleX(WWSimpleShape.SIDE_INSIDE1, 0.25f);
		sky.setTextureScaleY(WWSimpleShape.SIDE_INSIDE1, 0.25f);
		//sky.setColor(WWSimpleShape.SIDE_ALL, new WWColor(0xc0c0f0));
		sky.setTextureVelocityY(WWSimpleShape.SIDE_INSIDE1, 0.0005f);
		// for future
		//sky.setTextureURL(Side.INSIDE1, "http://www.moonglow.net/latest");
		//sky.setTextureRefreshInterval(Side.INSIDE1,"60000");
		// TODO fog color should match average color of sky
		sky.setFullBright(WWSimpleShape.SIDE_INSIDE1, true); // bright sky
		sky.setColor(WWSimpleShape.SIDE_INSIDE1, new WWColor(0xd0d0ff));
		sky.setTextureURL(WWSimpleShape.SIDE_SIDE1, "sky");
		sky.setTransparency(WWSimpleShape.SIDE_SIDE1, 0.25f);
		world.addObject(sky);

		// air (to provide friction and balloon floating)
		WWSimpleShape air = new WWSphere();
		air.setName("air");
		air.setPickable(false);
		air.setPosition(new WWVector(0, 0, -100));
		air.setSize(new WWVector(500, 5000, 5000));
		air.setTransparency(WWSimpleShape.SIDE_ALL, 1.0f);
		air.setCutoutStart(0.5f); // half dome
		air.setSolid(false);
		air.setRotation(new WWVector(0, 90, 0));
		air.setFriction(0.01f);
		air.setDensity(0.0001f);
		world.addObject(air);

		// generate the random objects
		for (int i = 0; i < 250; i++) {
			int type = (int) (5.0 * FastMath.random());
			WWSimpleShape object;
			if (type < 1) {
				object = new WWBox();
			} else if (type < 2) {
				object = new WWCylinder();
			} else if (type < 3) {
				object = new WWSphere();
			} else { // float the torus, as it is fun!
				object = new WWTorus();
			}
			object.setPenetratable(false);
			object.setImpactSound("wood");
			object.setPosition(new WWVector(FastMath.random() * 200.0f - 100.0f, FastMath.random() * 200.0f - 100.0f, FastMath.random() * 2.0f - 1.0f));
			float overallSize = FastMath.random() * 0.5f + 0.8f;
			overallSize = overallSize * overallSize;
			overallSize = overallSize * overallSize;
			overallSize = overallSize * overallSize;
			overallSize = overallSize * overallSize;
			object.setSize(new WWVector(FastMath.random() * overallSize, FastMath.random() * overallSize, FastMath.random() * overallSize));
			object.setRotation(new WWVector(FastMath.random() * 360.0f, FastMath.random() * 360.0f, FastMath.random() * 360.0f));
			if (FastMath.random() < 0.1) {
				object.setAMomentum(new WWVector(FastMath.random() * 180.0f - 90.0f, FastMath.random() * 180.0f - 90.0f, FastMath.random() * 180.0f - 90.0f));
			}
			object.setColor(WWSimpleShape.SIDE_ALL, new WWColor((int) (FastMath.random() * 0xffffff)));
			if (FastMath.random() > 0.25) {
				// Choose a random texture
				float texture = FastMath.random() * 5.0f;
				if (texture < 1.0) {
					object.setTextureURL(WWSimpleShape.SIDE_ALL, "concrete");
				} else if (texture < 2.0) {
					object.setTextureURL(WWSimpleShape.SIDE_ALL, "wood");
				} else if (texture < 3.0) {
					object.setTextureURL(WWSimpleShape.SIDE_ALL, "stucco");
				} else if (texture < 4.0) {
					object.setTextureURL(WWSimpleShape.SIDE_ALL, "grass");
				} else {
					object.setTextureURL(WWSimpleShape.SIDE_ALL, "ivy");
				}
				object.setTextureScaleX(WWSimpleShape.SIDE_ALL, 10.0f / object.sizeX);
				object.setTextureScaleY(WWSimpleShape.SIDE_ALL, 10.0f / object.sizeY);
			}
			world.addObject(object);
		}

		// first user
		WWUser user = new WWUser();
		user.setName("jack");
		world.addUser(user);
		WWSimpleShape avatar1 = new WWSphere();
		avatar1.setName("jack");
		avatar1.setDescription("jack's avatar -- a simple egg");
		avatar1.setSize(new WWVector(1, 1, 1));
		avatar1.setTaperX(0.25f);
		avatar1.setTaperY(0.25f);
		avatar1.setTextureURL(WWSimpleShape.SIDE_ALL, "jack");
		avatar1.setPhysical(true);
		avatar1.setDensity(0.1f);
		avatar1.setFreedomMoveZ(true);
		avatar1.setFreedomRotateX(false);
		avatar1.setFreedomRotateY(false);
		avatar1.setFreedomRotateZ(false);
		avatar1.setPosition(new WWVector(0, 0, 25));
		int avatar1id = world.addObject(avatar1);
		user.setAvatarId(avatar1id);

		// second user
		user = new WWUser();
		user.setName("jill");
		world.addUser(user);
		WWSimpleShape avatar2 = new WWSphere();
		avatar2.setName("jill");
		avatar2.setDescription("jill's avatar -- a simple egg");
		avatar2.setSize(new WWVector(1, 1, 1));
		avatar2.setPosition(new WWVector(4, 4, 25));
		avatar2.setTaperX(0.25f);
		avatar2.setTaperY(0.25f);
		avatar2.setTextureURL(WWSimpleShape.SIDE_ALL, "jill");
		avatar2.setPhysical(true);
		avatar2.setDensity(0.1f);
		avatar2.setFreedomRotateX(false);
		avatar2.setFreedomRotateY(false);
		avatar2.setFreedomRotateZ(false);
		int avatar2id = world.addObject(avatar2);
		user.setAvatarId(avatar2id);

		return world;
	}
}
