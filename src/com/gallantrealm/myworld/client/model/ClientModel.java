package com.gallantrealm.myworld.client.model;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import com.amazon.device.ads.AdRegistration;
import com.example.android.trivialdrivesample.util.IabHelper;
import com.example.android.trivialdrivesample.util.IabHelper.OnIabPurchaseFinishedListener;
import com.example.android.trivialdrivesample.util.IabResult;
import com.example.android.trivialdrivesample.util.Purchase;
import com.gallantrealm.myworld.FastMath;
import com.gallantrealm.myworld.android.AdDialog;
import com.gallantrealm.myworld.android.AndroidClientModel;
import com.gallantrealm.myworld.android.BuildConfig;
import com.gallantrealm.myworld.android.GallantActivity;
import com.gallantrealm.myworld.android.MessageDialog;
import com.gallantrealm.myworld.android.PauseAction;
import com.gallantrealm.myworld.android.R;
import com.gallantrealm.myworld.android.ShowWorldActivity;
import com.gallantrealm.myworld.android.TexturePickerDialog;
import com.gallantrealm.myworld.android.themes.DefaultTheme;
import com.gallantrealm.myworld.android.themes.Theme;
import com.gallantrealm.myworld.communication.ClientRequest;
import com.gallantrealm.myworld.communication.Communications;
import com.gallantrealm.myworld.communication.ConnectRequest;
import com.gallantrealm.myworld.communication.ConnectResponse;
import com.gallantrealm.myworld.communication.Connection;
import com.gallantrealm.myworld.communication.CreateObjectRequest;
import com.gallantrealm.myworld.communication.DataInputStreamX;
import com.gallantrealm.myworld.communication.DataOutputStreamX;
import com.gallantrealm.myworld.communication.DeleteObjectRequest;
import com.gallantrealm.myworld.communication.MoveObjectRequest;
import com.gallantrealm.myworld.communication.PauseWorldRequest;
import com.gallantrealm.myworld.communication.ResumeWorldRequest;
import com.gallantrealm.myworld.communication.SendMessageRequest;
import com.gallantrealm.myworld.communication.TCPCommunications;
import com.gallantrealm.myworld.communication.ThrustObjectRequest;
import com.gallantrealm.myworld.communication.TouchObjectRequest;
import com.gallantrealm.myworld.communication.UpdateEntityRequest;
import com.gallantrealm.myworld.communication.UpdateObjectRequest;
import com.gallantrealm.myworld.communication.UpdateWorldPropertiesRequest;
import com.gallantrealm.myworld.model.WWEntity;
import com.gallantrealm.myworld.model.WWObject;
import com.gallantrealm.myworld.model.WWUser;
import com.gallantrealm.myworld.model.WWVector;
import com.gallantrealm.myworld.model.WWWorld;
import com.gallantrealm.myworld.server.MyWorldServer;
import com.zeemote.zc.Controller;
import com.zeemote.zc.DeviceFactory;
import com.zeemote.zc.IDeviceSearch;
import com.zeemote.zc.IProgressMonitor;
import com.zeemote.zc.IStreamConnector;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ConfigurationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.opengl.GLES20;
import android.os.Build;
import android.os.Environment;
import android.os.PowerManager;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.view.InputDevice;
import uk.co.labbookpages.WavFile;

/**
 * This class maintains information shared within the client. Controls and actions in the client act upon the data in this model. Other views, control panels, actions will listen on events from this model and update their state accordingly.
 * This provides a clean separation of model from view, implementing the Document-View design pattern.
 */
public abstract class ClientModel {

	public static AndroidClientModel getClientModel() {
		if (clientModel == null) {
			clientModel = new AndroidClientModel();
		}
		return clientModel;
	}

	public static AndroidClientModel clientModel;

	public class ZeeProgressMonitor implements IProgressMonitor {
		@Override
		public void setMessage(String arg0) {
			System.out.println(arg0);
		}
	}

	public class ReturnValue {
		public int rc;
	}

	// General settings for camera perspectives
	public float initiallyFacingDistance = 6;
	public float behindDistance = 2;
	public float behindTilt = 5;
	public float birdsEyeHeight = 50;
	public float topOfTilt = 15;
	public float wayBehindDistance = 12;
	public float wayBehindTilt = 30;
	public float minCameraDistance = 0.5f;
	public float maxCameraDistance = 250f;

	public boolean limitCameraDistance = true;
	public static final int FULLVERSION = 0;
	public static final int GOOGLE = 1;
	public static final int AMAZON = 2;
	public static final int FREEVERSION = 3;
	public static final int market = GOOGLE;

	private static final float TORADIAN = 0.0174532925f;
	private static final int NSCORES = 24;
	private static final int NAVATARS = 24;
	static final int RC_REQUEST = 10001;
	static final String SKU_FULL_VERSION = "fullversion";
	static final String PAYLOAD = "DontSteal.IWorkedHardToMakeThisApp";
	public static final int MAX_LOG_LINES = 5;

	public Logger logger = Logger.getLogger("com.gallantrealm.myworld.client");
	public Preferences preferences;

	// Private data

	public final ArrayList<ClientModelChangedListener> listeners;
	public WWWorld world;
	public boolean localWorld;
	public String worldAddressField = "";
	public String locationField = "";
	public String userNameField = "";
	public String messageField = "";
	public final Communications communications;
	public Connection requestConnection;
	public UpdatesThread updatesThread;
	public String lastMessageReceived = "";
	public WWObject selectedObject;
	public WWVector selectedPoint;
	public boolean editing;
	public int userId;
	public RequestThread requestThread;
	public boolean addingMember;
	public int lastCreatedObjectId;
	public int viewpoint;
	public boolean stereoscopic;
	public boolean paused;

	// Preferences variables. These are kept in statics just in case they cannot be saved to preferences.
	// Static will allow them to be shared across all instances of client applets in the browser session
	public static boolean localPhysicsThread = true;
	public static float fieldOfView = 90;
	public static int refreshRate = 100;
	public static boolean antialias = false;
	public static String[] logMessages = new String[MAX_LOG_LINES];

	// Camera position information
	public WWObject cameraObject;
	public float xcamera = 0.0f;
	public float ycamera = 0.0f;
	public float zcamera = 10.0f;
	public float cameraPan = 0.0f;
	public float cameraTilt = 5.0f;
	public float cameraLean = 0.0f;
	public float cameraDistance = 1.0f;
	// TODO Have default distance, height, and tilt selectable by the user
	public float cameraPanVelocity = 0.0f;
	public float cameraTiltVelocity = 0.0f;
	public float cameraLeanVelocity = 0.0f;
	public float cameraDistanceVelocity = 0.0f;
	public float cameraSlideX = 0.0f;
	public float cameraSlideY = 0.0f;
	public float cameraSlideZ = 0.0f;
	public float cameraSlideXVelocity = 0.0f;
	public float cameraSlideYVelocity = 0.0f;
	public float cameraSlideZVelocity = 0.0f;

	public float dampedCameraLocationX;
	public float dampedCameraLocationY;
	public float dampedCameraLocationZ;
	public float dampedCameraTilt;
	public float dampedCameraLean;
	public float dampedCameraPan;

	public float cameraDampRate = 0f; // higher moves camera slower

	private AlertListener alertListener;

	static {
		for (int i = 0; i < MAX_LOG_LINES; i++) {
			logMessages[i] = "";
		}
	}

	// Constructor
	public ClientModel() {
		listeners = new ArrayList<ClientModelChangedListener>();
		try {
			preferences = Preferences.userNodeForPackage(this.getClass());
		} catch (SecurityException e) {
		}
		// world = new WWWorld();
		if (preferences != null) {
			initFromPreferences();
		}
		communications = new TCPCommunications();
	}

	public boolean isLocalWorld() {
		return localWorld;
	}

	public Communications getCommunications() {
		return communications;
	}

	protected void initFromPreferences() {
		String fieldOfViewString = preferences.get("FieldOfView", "90.0");
		setFieldOfView(Float.parseFloat(fieldOfViewString));
		String refreshRateString;
		if (Runtime.getRuntime().availableProcessors() > 1) {
			refreshRateString = preferences.get("RefreshRate", "16"); // 60 fps
		} else {
			refreshRateString = preferences.get("RefreshRate", "33"); // 30 fps
		}
		setRefreshRate(Integer.parseInt(refreshRateString));
		String antialiasString = preferences.get("Antialias", "false");
		setAntialias(Boolean.parseBoolean(antialiasString));
		String localPhysicsThreadString = preferences.get("LocalPhysicsThread", "true");
		setLocalPhysicsThread(Boolean.parseBoolean(localPhysicsThreadString));
	}

	public void initializeCameraPosition() {
		xcamera = 0.0f;
		ycamera = 0.0f;
		zcamera = 10.0f;
		cameraPan = 0.0f;
		cameraTilt = 5.0f;
		cameraLean = 0.0f;
		cameraDistance = 1.0f;
		cameraPanVelocity = 0.0f;
		cameraTiltVelocity = 0.0f;
		cameraLeanVelocity = 0.0f;
		cameraDistanceVelocity = 0.0f;
		cameraSlideX = 0.0f;
		cameraSlideY = 0.0f;
		cameraSlideZ = 0.0f;
		cameraSlideXVelocity = 0.0f;
		cameraSlideYVelocity = 0.0f;
		cameraSlideZVelocity = 0.0f;

		dampedCameraLocationX = 0.0f;
		dampedCameraLocationY = 0.0f;
		dampedCameraLocationZ = 0.0f;
	}

	// Listener methods

	public void addClientModelChangedListener(ClientModelChangedListener listener) {
		listeners.add(listener);
	}

	public void removeClientModelChangedListener(ClientModelChangedListener listener) {
		listeners.remove(listener);
	}

	public void fireClientModelChanged(final int changeType) {
		ClientModelChangedEvent event = new ClientModelChangedEvent(changeType);
		Iterator<ClientModelChangedListener> iterator = listeners.iterator();
		while (iterator.hasNext()) {
			ClientModelChangedListener listener = iterator.next();
			listener.clientModelChanged(event);
		}
		if (world != null) {
			world.clientModelChanged(event);
		}
	}

	// Property getters and setters

	public String getLocationField() {
		return locationField;
	}

	public void setLocationField(String locationField) {
		this.locationField = locationField;
		fireClientModelChanged(ClientModelChangedEvent.EVENT_TYPE_NAVIGATOR_FIELD_UPDATED);
	}

	public String getUserNameField() {
		return userNameField;
	}

	public void setUserNameField(String userNameField) {
		this.userNameField = userNameField;
		fireClientModelChanged(ClientModelChangedEvent.EVENT_TYPE_NAVIGATOR_FIELD_UPDATED);
	}

	public void setWorld(WWWorld world) {
		this.world = world;
		testedOpenGL = false; // need to retest if world is changed
		// initializeCameraPosition();
		fireClientModelChanged(ClientModelChangedEvent.EVENT_TYPE_WWMODEL_UPDATED);
	}

	public String getWorldAddressField() {
		return worldAddressField;
	}

	public void setWorldAddressField(String worldAddressField) {
		this.worldAddressField = worldAddressField;
		fireClientModelChanged(ClientModelChangedEvent.EVENT_TYPE_NAVIGATOR_FIELD_UPDATED);
	}

	public final int getRefreshRate() {
		if (((ClientModel) this).isPowerSaver()) {
			return 100;
		} else {
			return 33;
		}
	}

	public void setRefreshRate(int r) {
		refreshRate = r;
		if (preferences != null) {
			preferences.put("RefreshRate", String.valueOf(refreshRate));
		}
		fireClientModelChanged(ClientModelChangedEvent.EVENT_TYPE_REFRESH_RATE_CHANGED);
	}

	public boolean getAntialias() {
		return antialias;
	}

	public void setAntialias(boolean a) {
		antialias = a;
		if (preferences != null) {
			preferences.put("Antialias", String.valueOf(antialias));
		}
		fireClientModelChanged(ClientModelChangedEvent.EVENT_TYPE_ANTIALIAS_CHANGED);
	}

	// Model action methods

	public void setLocalWorld(WWWorld world) {
		this.localWorld = true;
		setWorld(world);
	}

	public void connect() {
		try {

			// Attempt to connect to the new server
			Connection newConnection = communications.connect(getWorldAddressField(), 5000);
			DataOutputStreamX sendStream = newConnection.getSendStream(5000);
			sendStream.writeObject(new ConnectRequest(getUserNameField(), ""));
			newConnection.send(5000);
			DataInputStreamX receiveStream = newConnection.receive(5000);
			ConnectResponse connectResponse = (ConnectResponse) receiveStream.readObject();
			setUserId(connectResponse.getUserId());

			// Disconnect from the old server
			disconnect();
			requestConnection = newConnection;
			requestThread = new RequestThread(this, requestConnection);
			requestThread.start();

			// Create the local (client) copy of the world
			setWorld(new WWWorld(localPhysicsThread, true, null, 25, true));
			world.setDeltaTime(-newConnection.getDeltaTime());

			// Start updates thread
			updatesThread = new UpdatesThread(this);
			updatesThread.start();

			showMessage("Connected to " + getWorldAddressField());
			fireClientModelChanged(ClientModelChangedEvent.EVENT_TYPE_CONNECTED);

		} catch (Exception e) {
			e.printStackTrace();
			showMessage("Failed to connect: " + e.getMessage());
		}
	}

	public boolean isConnected() {
		if (world != null) {
			return true;
		}
		return requestConnection != null;
	}

	public void disconnect() {
		if (requestThread != null) {
			requestThread.interrupt();
			requestThread = null;
		}
		if (requestConnection != null) {
			try {
				requestConnection.disconnect();
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (updatesThread != null) {
				updatesThread.interrupt();
				updatesThread = null;
			}
			requestConnection = null;
		}

		if (isLocalWorld() && world != null) {
			world.pause();
		}

		// world.clear();
		world = null;

		showMessage("disconnected");

		fireClientModelChanged(ClientModelChangedEvent.EVENT_TYPE_DISCONNECTED);
	}

	public String getMessageField() {
		return messageField;
	}

	public void setMessageField(String messageField) {
		this.messageField = messageField;
		fireClientModelChanged(ClientModelChangedEvent.EVENT_TYPE_MESSAGE_FIELD_UPDATED);
	}

	public void showMessage(String message) {
		logger.log(Level.FINER, message);
		lastMessageReceived = message;
		fireClientModelChanged(ClientModelChangedEvent.EVENT_TYPE_MESSAGE_RECEIVED);
	}

	/** Unlike other requests to the server, this will wait until the object is created. */
	public int createObject(WWObject object) {
		if (isLocalWorld()) {
			// TODO
		} else {
			lastCreatedObjectId = 0;
			ClientRequest request = new CreateObjectRequest(object);
			requestThread.queue(request);
			try {
				while (lastCreatedObjectId == 0) {
					Thread.sleep(100);
				}
			} catch (InterruptedException e) {
			}
		}
		return lastCreatedObjectId;
	}

	public void updateObject(int objectId, WWObject object) {
		if (isLocalWorld()) {
			// TODO
		} else {
			if (requestThread != null) {
				ClientRequest request = new UpdateObjectRequest(objectId, object);
				requestThread.queue(request);
			}
		}
	}

	public void updateEntity(int entityId, WWEntity entity) {
		if (isLocalWorld()) {
			// TODO
		} else {
			if (requestThread != null) {
				ClientRequest request = new UpdateEntityRequest(entityId, entity);
				requestThread.queue(request);
			}
		}
	}

	public void updateWorldProperties(WWWorld updatedWorld) {
		if (isLocalWorld()) {
			// TODO
		} else {
			if (requestThread != null) {
				updatedWorld.setLastModifyTime(world.getWorldTime());
				ClientRequest request = new UpdateWorldPropertiesRequest(updatedWorld);
				requestThread.queue(request);
			}
		}
	}

	public void moveObject(int objectId, WWObject object) {
		if (isLocalWorld()) {
			// TODO
		} else {
			if (requestThread != null) {
				object.setLastMoveTime(world.getWorldTime());
				ClientRequest request = new MoveObjectRequest(objectId, object);
				requestThread.queue(request);
			}
		}
	}

	public void thrustObject(int objectId, WWObject object) {
		if (isLocalWorld()) {
			world.thrustObject(objectId, object);
		} else {
			if (requestThread != null) {
				object.setLastMoveTime(world.getWorldTime());
				ClientRequest request = new ThrustObjectRequest(objectId, object);
				requestThread.queue(request);
				// Also thrust local object. This allows the client to appear more responsive. Note that the
				// server will correct any "error" in the client's positioning.
				// getWorld().thrustObject(objectId, object);
				// Note: deactivated because it causes "jerky" motion with latent connections
			}
		}
	}

	public void deleteObject(int objectId) {
		if (isLocalWorld()) {
			world.removeObject(objectId);
		} else {
			if (requestThread != null) {
				ClientRequest request = new DeleteObjectRequest(objectId);
				requestThread.queue(request);
			}
		}
	}

	public String getLastMessageReceived() {
		return lastMessageReceived;
	}

	public void setSelectedObject(WWObject object) {
		selectedObject = object;
		System.out.println("ClientModel.setSelectedObject " + object);
		setCameraObject(object);
		setCameraSlide(0, 0, 0);
		fireClientModelChanged(ClientModelChangedEvent.EVENT_TYPE_OBJECT_SELECTED);
	}

	public WWObject getSelectedObject() {
		return selectedObject;
	}

	public void setSelectedPoint(WWVector point) {
		selectedPoint = point;
		fireClientModelChanged(ClientModelChangedEvent.EVENT_TYPE_POINT_SELECTED);
	}

	public WWVector getSelectedPoint() {
		return selectedPoint;
	}

	public boolean isEditing() {
		return editing;
	}

	public void setEditing(boolean editing) {
		this.editing = editing;
	}

	protected void setUserId(int userId) {
		this.userId = userId;
	}

	public int getUserId() {
		return userId;
	}

	public int getAvatarId() {
		if (world == null) {
			return 0;
		}
		WWUser user = world.getUser(userId);
		if (user == null) {
			return 0;
		}
		return user.getAvatarId();
	}

	public WWObject getAvatar() {
		if (world == null) {
			return null;
		}
		int id = getAvatarId();
		if (id < 0) {
			return null;
		}
		return world.objects[id];
	}

	protected float lastAvatarThrustVelocity = 0.0f;
	protected float lastAvatarTurnVelocity = 0.0f;
	protected float lastAvatarLiftVelocity = 0.0f;
	protected float lastAvatarTiltVelocity = 0.0f;
	protected float lastAvatarLeanVelocity = 0.0f;
	protected float lastAvatarSlideVelocity = 0.0f;

	public float getAvatarThrust() {
		return lastAvatarThrustVelocity;
	}

	public float getAvatarTurn() {
		return lastAvatarTurnVelocity;
	}

	public float getAvatarLift() {
		return lastAvatarLiftVelocity;
	}

	public float getAvatarTilt() {
		return lastAvatarTiltVelocity;
	}

	public float getAvatarLean() {
		return lastAvatarLeanVelocity;
	}

	public float getAvatarSlide() {
		return lastAvatarSlideVelocity;
	}

	/**
	 * This value is used to adjust the camera pan as the avatar is rotated. It is set when the avatar starts to turn, then again set on every camera update.
	 */
	protected float lastAvatarRotationZ = 0.0f;

	public float getLastAvatarRotationZ() {
		return lastAvatarRotationZ;
	}

	public void setLastAvatarRotationZ(float rotationZ) {
		lastAvatarRotationZ = rotationZ;
	}

	public void setViewpoint(int viewpoint) {
		this.viewpoint = viewpoint;
		cameraToViewpoint();
	}

	public int getViewpoint() {
		return viewpoint;
	}

	public void cameraToViewpoint() {
		if (world == null) {
			return;
		}
		if (world.getUser(userId) != null) {
			int avatarId = world.getUser(userId).getAvatarId();
			WWObject avatar = world.objects[avatarId];
			cameraToViewpoint(avatar);
		}
	}

	/**
	 * Make camera see from avatar's vantage
	 */
	public void cameraToViewpoint(WWObject avatar) {
		if (customizeMode) { // ignore viewpoint and place camera in front of avatar
			setCameraObject(avatar);
			setCameraDistance(behindDistance);
			setCameraTilt(behindTilt);
			setCameraPan(180.0f);
		} else {
			if (viewpoint == 0) { // behind avatar
				setCameraObject(avatar);
				setCameraDistance(behindDistance);
				setCameraTilt(behindTilt);
				setCameraPan(0.0f);
			} else if (viewpoint == 1) { // focus object or top of avatar
				WWObject focus = avatar.getDescendant("focus");
				if (focus != null) {
					setCameraObject(focus);
					setCameraDistance(0.0f);
					setCameraPoint(new WWVector(0, 0, 0));
					setCameraTilt(0);
					setCameraPan(0.0f);
				} else {
					setCameraObject(avatar);
					setCameraDistance(0.0f);
					setCameraTilt(topOfTilt);
					setCameraPan(0.0f);
				}
			} else if (viewpoint == 2) { // birds eye
				setCameraObject(avatar);
				setCameraDistance(birdsEyeHeight);
				setCameraTilt(90.0f);
				setCameraPan(0.0f);
			} else if (viewpoint == 3) { // way behind avatar
				setCameraObject(avatar);
				setCameraDistance(wayBehindDistance);
				setCameraTilt(wayBehindTilt);
				setCameraPan(0.0f);
			} else { // behind as default
				setCameraObject(avatar);
				setCameraDistance(behindDistance);
				setCameraTilt(behindTilt);
				setCameraPan(0.0f);
			}
		}
	}

	public void setCameraDampRate(float cameraDampRate) {
		this.cameraDampRate = cameraDampRate;
	}

	/**
	 * Moves an avatar using the thrust velocity and torque velocity specified.
	 */
	public void forceAvatar(float thrustVelocity, float turnVelocity, float liftVelocity, float tiltVelocity, float leanVelocity, float slideVelocity) {
		
		if ((thrustVelocity != 0 || turnVelocity != 0 || liftVelocity != 0 || tiltVelocity != 0 || leanVelocity != 0 || slideVelocity != 0) && selectedObject != getAvatar()) {
			setSelectedObject(getAvatar());
		}

		if (thrustVelocity == lastAvatarThrustVelocity && turnVelocity == lastAvatarTurnVelocity && liftVelocity == lastAvatarLiftVelocity && tiltVelocity == lastAvatarTiltVelocity && leanVelocity == lastAvatarLeanVelocity
				&& slideVelocity == lastAvatarSlideVelocity) {
			return;
		}
		if (world == null) {
			return;
		}
		if (world.getUser(userId) == null) {
			return;
		}
		int avatarId = world.getUser(userId).getAvatarId();
		WWObject avatar = world.objects[avatarId];
		if (avatarId != 0 && avatar != null) {
			cameraToViewpoint();

			// move and turn avatar
			if (isLocalWorld()) {
				avatar.setThrust(new WWVector(slideVelocity * 10, -thrustVelocity * 10, liftVelocity * 10));
				avatar.setThrustVelocity(new WWVector(slideVelocity, -thrustVelocity, liftVelocity));
				avatar.setTorque(new WWVector(tiltVelocity * 100, leanVelocity * 100, turnVelocity * 100));
				avatar.setTorqueVelocity(new WWVector(tiltVelocity, leanVelocity, turnVelocity));
			} else {
				WWObject updatedAvatar = (WWObject) avatar.cloneNoBehavior();
				updatedAvatar.setThrust(new WWVector(slideVelocity * 10, -thrustVelocity * 10, liftVelocity * 10));
				updatedAvatar.setThrustVelocity(new WWVector(slideVelocity, -thrustVelocity, liftVelocity));
				updatedAvatar.setTorque(new WWVector(tiltVelocity * 100, leanVelocity * 100, turnVelocity * 100));
				updatedAvatar.setTorqueVelocity(new WWVector(tiltVelocity, leanVelocity, turnVelocity));
				thrustObject(avatarId, updatedAvatar);
			}

			lastAvatarThrustVelocity = thrustVelocity;
			lastAvatarTurnVelocity = turnVelocity;
			lastAvatarLiftVelocity = liftVelocity;
			lastAvatarTiltVelocity = tiltVelocity;
			lastAvatarLeanVelocity = leanVelocity;
			lastAvatarSlideVelocity = slideVelocity;
		}
	}

	public boolean isAvatarMoving() {
		return (lastAvatarThrustVelocity != 0.0 || lastAvatarTurnVelocity != 0.0);
	}

	public void sendMessage(String message) {
		ClientRequest request = new SendMessageRequest(message);
		requestThread.queue(request);
	}

	public boolean isLocalPhysicsThread() {
		return localPhysicsThread;
	}

	public void setLocalPhysicsThread(boolean l) {
		localPhysicsThread = l;
		if (preferences != null) {
			preferences.put("LocalPhysicsThread", String.valueOf(localPhysicsThread));
		}
	}

	public float getCameraDistance() {
		return cameraDistance;
	}

	public void setCameraDistance(float cameraDistance) {
		// TODO consider making the min and max camera distance a parameter of the world.
		this.cameraDistance = FastMath.min(Math.max(cameraDistance, minCameraDistance), maxCameraDistance);
	}

	public WWObject getCameraObject() {
		return cameraObject;
	}

	public void setCameraObject(WWObject cameraObject) {
		this.cameraObject = cameraObject;
	}

	public float getCameraPan() {
		return cameraPan;
	}

	public void setCameraPan(float cameraPan) {
		this.cameraPan = cameraPan;
	}

	public float getCameraTilt() {
		return cameraTilt;
	}

	public void setCameraTilt(float cameraTilt) {
		this.cameraTilt = cameraTilt;
	}

	public float getCameraLean() {
		return cameraLean;
	}

	public void setCameraLean(float cameraLean) {
		this.cameraLean = cameraLean;
	}

	public void getCameraPoint(WWVector cameraPoint) {
		cameraPoint.x = xcamera;
		cameraPoint.y = ycamera;
		cameraPoint.z = zcamera;
	}

	public void setCameraPoint(WWVector point) {
		xcamera = point.getX();
		ycamera = point.getY();
		zcamera = point.getZ();
	}

	public float getCameraDistanceVelocity() {
		return cameraDistanceVelocity;
	}

	public void setCameraDistanceVelocity(float cameraDistanceVelocity) {
		this.cameraDistanceVelocity = cameraDistanceVelocity;
	}

	public float getCameraPanVelocity() {
		return cameraPanVelocity;
	}

	public void setCameraPanVelocity(float cameraPanVelocity) {
		this.cameraPanVelocity = cameraPanVelocity;
	}

	public float getCameraTiltVelocity() {
		return cameraTiltVelocity;
	}

	public void setCameraTiltVelocity(float cameraTiltVelocity) {
		this.cameraTiltVelocity = cameraTiltVelocity;
	}

	public float getCameraLeanVelocity() {
		return cameraLeanVelocity;
	}

	public void setCameraLeanVelocity(float cameraLeanVelocity) {
		this.cameraLeanVelocity = cameraLeanVelocity;
	}

	public float getCameraSlideX() {
		return cameraSlideX;
	}

	public void setCameraSlideX(float cameraSlideX) {
		this.cameraSlideX = cameraSlideX;
	}

	public float getCameraSlideY() {
		return cameraSlideY;
	}

	public void setCameraSlideY(float cameraSlideY) {
		this.cameraSlideY = cameraSlideY;
	}

	public float getCameraSlideZ() {
		return cameraSlideZ;
	}

	public void setCameraSlideZ(float cameraSlideZ) {
		this.cameraSlideZ = cameraSlideZ;
	}

	public void setCameraSlide(float cameraSlideX, float cameraSlideY, float cameraSlideZ) {
		this.cameraSlideX = cameraSlideX;
		this.cameraSlideY = cameraSlideY;
		this.cameraSlideZ = cameraSlideZ;
	}

	/**
	 * This value is calculated from the camera point, distance, tilt and pan.
	 */
	public WWVector getCameraLocation(long worldTime) {
		float x;
		float y;
		float z;
		if (cameraObject != null) {
			WWVector pos = cameraObject.getAbsolutePosition(worldTime);
			if (cameraObject == getAvatar() || getAvatar().isDescendant(cameraObject)) {
				x = xcamera + pos.x + cameraDistance * FastMath.sin(TORADIAN * cameraPan + cameraObject.getAbsoluteRotation(worldTime).getZ()) * FastMath.cos(TORADIAN * cameraTilt);
				y = ycamera + pos.y + cameraDistance * FastMath.cos(TORADIAN * cameraPan + cameraObject.getAbsoluteRotation(worldTime).getZ()) * FastMath.cos(TORADIAN * cameraTilt);
				z = zcamera + pos.z + FastMath.sin(TORADIAN * cameraTilt) * cameraDistance;
			} else {
				x = xcamera + pos.x + cameraDistance * FastMath.sin(TORADIAN * cameraPan) * FastMath.cos(TORADIAN * cameraTilt);
				y = ycamera + pos.y + cameraDistance * FastMath.cos(TORADIAN * cameraPan) * FastMath.cos(TORADIAN * cameraTilt);
				z = zcamera + pos.z + FastMath.sin(TORADIAN * cameraTilt) * cameraDistance;
			}
		} else {
			x = xcamera + cameraDistance * FastMath.sin(TORADIAN * cameraPan) * FastMath.cos(TORADIAN * cameraTilt);
			y = ycamera + cameraDistance * FastMath.cos(TORADIAN * cameraPan) * FastMath.cos(TORADIAN * cameraTilt);
			z = zcamera + FastMath.sin(TORADIAN * cameraTilt) * cameraDistance;
		}
		return new WWVector(x, y, z);
	}

	public WWVector getDampedCameraLocation() {
		return new WWVector(dampedCameraLocationX, dampedCameraLocationY, dampedCameraLocationZ);
	}

	public void setDampedCameraLocation(float x, float y, float z) {
		dampedCameraLocationX = x;
		dampedCameraLocationY = y;
		dampedCameraLocationZ = z;
	}

	public void setDampedCameraRotation(float x, float y, float z) {
		dampedCameraTilt = x;
		dampedCameraLean = y;
		dampedCameraPan = z;
	}

	public WWVector getDampedCameraRotation() {
		return new WWVector(dampedCameraTilt, dampedCameraLean, dampedCameraPan);
	}

	public float getCameraSlideXVelocity() {
		return cameraSlideXVelocity;
	}

	public void setCameraSlideXVelocity(float cameraSlideXVelocity) {
		this.cameraSlideXVelocity = cameraSlideXVelocity;
	}

	public float getCameraSlideYVelocity() {
		return cameraSlideYVelocity;
	}

	public void setCameraSlideYVelocity(float cameraSlideYVelocity) {
		this.cameraSlideYVelocity = cameraSlideYVelocity;
	}

	public float getCameraSlideZVelocity() {
		return cameraSlideZVelocity;
	}

	public void setCameraSlideZVelocity(float cameraSlideZVelocity) {
		this.cameraSlideZVelocity = cameraSlideZVelocity;
	}

	public float getFieldOfView() {
		return fieldOfView;
	}

	public void setFieldOfView(float f) {
		fieldOfView = f;
		if (preferences != null) {
			preferences.put("FieldOfView", String.valueOf(fieldOfView));
		}
		fireClientModelChanged(ClientModelChangedEvent.EVENT_TYPE_FIELD_OF_VIEW_CHANGED);
	}

	public boolean isAddingMember() {
		return addingMember;
	}

	public void setAddingMember(boolean addingMember) {
		this.addingMember = addingMember;
	}

	public void setLastCreatedObjectId(int lastCreatedObjectId) {
		this.lastCreatedObjectId = lastCreatedObjectId;
	}

	public void touchObject(int objectId) {
		ClientRequest request = new TouchObjectRequest(objectId);
		requestThread.queue(request);
	}

	public void pauseWorld() {
		System.out.println(">ClientModel.pauseWorld");
		paused = true;
		if (isLocalWorld()) {
			world.pause();
		} else {
			ClientRequest request = new PauseWorldRequest();
			requestThread.queue(request);
		}
		System.out.println("<ClientModel.pauseWorld");
	}

	public void resumeWorld() {
		System.out.println(">ClientModel.resumeWorld");
		setViewpoint(getViewpoint());
		paused = false;
		if (isLocalWorld()) {
			world.run();
		} else {
			ClientRequest request = new ResumeWorldRequest();
			requestThread.queue(request);
		}
		System.out.println("<ClientModel.resumeWorld");
	}

	public String getAvatarActionLabel(int action) {
		if (world == null || world.getAvatarActions() == null || action >= world.getAvatarActions().length) {
			return null;
		} else if (world.getAvatarActions()[action] == null) {
			return null;
		}
		return world.getAvatarActions()[action].getName();
	}

	public void startAvatarAction(int actionId, float x, float y) {
		if (world != null && actionId < world.getAvatarActions().length && world.getAvatarActions()[actionId] instanceof PauseAction) {
			world.getAvatarActions()[actionId].start();
		} else {
			if (world == null || actionId >= world.getAvatarActions().length) {
				return;
			} else if (world.getAvatarActions()[actionId] == null) {
				return;
			}
			world.startAvatarAction(actionId, x, y);
			startRepeatAvatarAction(actionId, x, y);
		}
	}

	public void stopAvatarAction(int action) {
		stopRepeatAvatarAction();
		if (world == null || world.getAvatarActions() == null || action >= world.getAvatarActions().length) {
			return;
		} else if (world.getAvatarActions()[action] == null) {
			return;
		}
		world.stopAvatarAction(action);
	}

	private RepeatAvatarActionThread repeatAvatarActionThread;

	private void startRepeatAvatarAction(int action, float x, float y) {
		stopRepeatAvatarAction();
		if (repeatAvatarActionThread == null) {
			repeatAvatarActionThread = new RepeatAvatarActionThread(action, x, y);
			repeatAvatarActionThread.start();
		}
	}

	private void stopRepeatAvatarAction() {
		if (repeatAvatarActionThread != null) {
			synchronized (repeatAvatarActionThread) {
				if (repeatAvatarActionThread != null) {
					repeatAvatarActionThread.safeStop();
					try {
						repeatAvatarActionThread.join();
					} catch (InterruptedException e) {
					}
					repeatAvatarActionThread = null;
				}
			}
		}
	}

	private class RepeatAvatarActionThread extends Thread {
		int action;
		float x, y;
		boolean stop;

		public RepeatAvatarActionThread(int action, float x, float y) {
			setName("RepeatAvatarActionThread");
			this.action = action;
			this.x = x;
			this.y = y;
		}

		@Override
		public void run() {
			try {
				sleep(250);
				while (!stop) {
					if (world.getAvatarActions().length > action) {
						world.getAvatarActions()[action].repeat(x, y);
					}
					sleep(100);
				}
			} catch (Exception e) {
			}
		}

		public void safeStop() {
			stop = true;
		}
	}

	public String getWorldActionLabel(int action) {
		if (world == null || world.getWorldActions() == null || action >= world.getWorldActions().length) {
			return null;
		} else if (world.getWorldActions()[action] == null) {
			return null;
		}
		return world.getWorldActions()[action].getName();
	}

	public void startWorldAction(int action) {
		if (world != null && action < world.getWorldActions().length && world.getWorldActions()[action] instanceof PauseAction) {
			world.getWorldActions()[action].start();
		} else {
			if (world == null || action >= world.getWorldActions().length) {
				return;
			} else if (world.getWorldActions()[action] == null) {
				return;
			}
			// world.getWorldActions()[action].start();
			world.startWorldAction(action, 0, 0);
		}
	}

	public void stopWorldAction(int action) {
		if (world == null || world.getWorldActions() == null || action >= world.getWorldActions().length) {
			return;
		} else if (world.getWorldActions()[action] == null) {
			return;
		}
		// world.getWorldActions()[action].stop();
		world.stopWorldAction(action);
	}

	public int alert(String message, String[] options) {
		return alert(null, message, options, null);
	}

	public int alert(String title, String message, String[] options, String checkinMessage) {
		if (alertListener != null) {
			return alertListener.onAlert(title, message, options, checkinMessage);
		}
		return 0;
	}

	public int alert(String title, String message, String[] options, String leaderboardId, long score, String scoreMsg) {
		if (alertListener != null) {
			return alertListener.onAlert(title, message, options, leaderboardId, score, scoreMsg);
		}
		return 0;
	}

	private MyWorldServer localServer;
	private int nSongPlayers;
	private MediaPlayer dialogSong;
	private int nDialogSongPlayers;
	private String categoryName;
	private String worldName;
	private String worldClassName;
	private String worldParams;
	private String avatarName;
	private boolean playMusic;
	private boolean playSoundEffects;
	private boolean vibration;
	private boolean useSensors;
	private boolean useZeemote;
	private boolean useScreenControl;
	private boolean controlOnLeft;
	public boolean usingMoga;
	private float controlSensitivity = 1.0f;

	public void selectAlert(final String message, final Object[] availableItems, final String[] options, SelectResponseHandler handler) {
		if (alertListener != null) {
			alertListener.onSelectAlert(message, availableItems, options, handler);
		}
	}

	private final int[] scores = new int[NSCORES];
	private final int[] levels = new int[NSCORES];
	private final int[] times = new int[NSCORES];
	private final boolean[] unlocked = new boolean[NSCORES];

	public void inputAlert(final String title, final String message, String initialValue, final String[] options, InputResponseHandler handler) {
		if (alertListener != null) {
			alertListener.onInputAlert(title, message, initialValue, options, handler);
		}
	}

	private final String[] avatarDisplayNames = new String[NAVATARS];
	private String flashMessage;
	private boolean blink;
	private Activity context;
	private boolean alwaysStartAsNew;
	private boolean alwaysStartAsOld;
	private boolean simpleRendering;
	private boolean fullVersion;
	private boolean powerSaver;
	private int playCount;
	private long lastPlayTime;
	private boolean customizeMode;
	private int preferencesVersion;
	public boolean cameraInitiallyFacingAvatar;
	public boolean goggleDogPass;
	private String localFolder;
	private boolean showDebugLogging;
	IabHelper purchaseHelper;
	boolean testedOpenGL;
	boolean supportsOpenGLES20;
	int songId;

	public void loadPreferences(Activity context) {
		this.context = context;

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		preferencesVersion = preferences.getInt("preferencesVersion", 1);
		avatarName = preferences.getString("avatarName", context.getString(R.string.defaultAvatarName));
		worldName = preferences.getString("worldName", context.getString(R.string.defaultWorldName));
		worldClassName = preferences.getString("worldClassName", context.getString(R.string.defaultWorldClassName));
		if (worldClassName.equals("")) {
			worldClassName = worldName;
		}
		playMusic = preferences.getBoolean("playMusic", true);
		playSoundEffects = preferences.getBoolean("playSoundEffects", true);
		vibration = preferences.getBoolean("vibration", true);
		useSensors = preferences.getBoolean("useSensors", false);
		useZeemote = preferences.getBoolean("useZeemote", false);
		useScreenControl = preferences.getBoolean("showScreenControls", true);
		controlOnLeft = preferences.getBoolean("controlOnLeft", true);
		usingMoga = false; // this is determined dynamically now, by querying for a moga controller
		controlSensitivity = preferences.getFloat("controlSensitivity", 0.5f);
		stereoscopic = preferences.getBoolean("stereoscopic", false);
		if (preferencesVersion == 0) {
			simpleRendering = false;
		} else {
			simpleRendering = preferences.getBoolean("simpleRendering", false);
		}
		fullVersion = preferences.getBoolean("fullVersion", "true".equals(context.getString(R.string.fullVersion)));
		powerSaver = preferences.getBoolean("powerSaver", false);
		playCount = preferences.getInt("playCount", 0);
		lastPlayTime = preferences.getLong("lastPlayTime", 0);
		for (int i = 0; i < NSCORES; i++) {
			scores[i] = preferences.getInt("score" + i, 0);
			levels[i] = preferences.getInt("level" + i, 0);
			times[i] = preferences.getInt("time" + i, 0);
			unlocked[i] = preferences.getBoolean("unlocked" + i, false);
		}
		for (int i = 0; i < NAVATARS; i++) {
			avatarDisplayNames[i] = preferences.getString("avatarDisplayName" + i, null);
		}
		localFolder = preferences.getString("localFolder", Environment.getExternalStorageDirectory().toString() + "/DevWorlds");
		showDebugLogging = preferences.getBoolean("showDebugLogging", false);
	}

	public void setContext(Activity context) {
		// if (this.context != null && purchaseObserver != null) {
		// ResponseHandler.unregister(purchaseObserver);
		// purchaseObserver = null;
		// }
		this.context = context;
		if (purchaseHelper == null) {
			try {
				purchaseHelper = new IabHelper(context, context.getString(R.string.googleappkey));
				purchaseHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
					public void onIabSetupFinished(IabResult result) {
					}
				});
			} catch (Exception e) {
				System.out.println("No in-app purchase, the app isn't setup correctly for it: " + e.getMessage());
			}
		}

	}

	public Activity getContext() {
		return context;
	}

	public void savePreferences(Context context) {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putInt("preferencesVersion", 1);
		editor.putString("avatarName", avatarName);
		editor.putString("worldName", worldName);
		editor.putString("worldClassName", worldClassName);
		editor.putBoolean("playMusic", playMusic);
		editor.putBoolean("playSoundEffects", playSoundEffects);
		editor.putBoolean("vibration", vibration);
		editor.putBoolean("useSensors", useSensors);
		editor.putBoolean("useZeemote", useZeemote);
		editor.putBoolean("showScreenControls", useScreenControl);
		editor.putBoolean("controlOnLeft", controlOnLeft);
		editor.putFloat("controlSensitivity", controlSensitivity);
		editor.putBoolean("stereoscopic", stereoscopic);
		editor.putBoolean("simpleRendering", simpleRendering);
		if (fullVersion) {
			editor.putBoolean("fullVersion", fullVersion);
		}
		editor.putBoolean("powerSaver", powerSaver);
		editor.putInt("playCount", playCount);
		editor.putLong("lastPlayTime", lastPlayTime);
		for (int i = 0; i < NSCORES; i++) {
			editor.putInt("score" + i, scores[i]);
			editor.putInt("level" + i, levels[i]);
			editor.putInt("time" + i, times[i]);
			editor.putBoolean("unlocked" + i, unlocked[i]);
		}
		for (int i = 0; i < NAVATARS; i++) {
			editor.putString("avatarDisplayName" + i, avatarDisplayNames[i]);
		}
		editor.putString("localFolder", localFolder);
		editor.putBoolean("showDebugLogging", showDebugLogging);
		editor.commit();
	}

	MediaPlayer songPlayer;
	private Controller zeeController;

	public int getPlayCount() {
		return playCount;
	}

	public void updatePlayCount(Context context) {
		playCount = playCount + 1;
		savePreferences(context);
	}

	public long getLastPlayTime() {
		return lastPlayTime;
	}

	public void updateLastPlayTime(Context context) {
		lastPlayTime = System.currentTimeMillis();
		savePreferences(context);
	}

	private boolean zeeConnected;
	private boolean cancelZee;

	public void playSong(int songId) {
		if (this.songId != songId) {
			if (songPlayer != null) {
				songPlayer.stop();
				songPlayer.release();
				songPlayer = null;
			}
			this.songId = songId;
		}
		if (songPlayer == null) {
			songPlayer = MediaPlayer.create(getContext(), this.songId);
			if (songPlayer == null) {
				return;
			}
			songPlayer.setLooping(true);
			songPlayer.setVolume(1f, 1f);
		}
		nSongPlayers++;
		if (nSongPlayers == 1) {
			if (isPlayMusic()) {
				songPlayer.start();
			}
		}
	}

	public synchronized void stopSong() {
		if (songPlayer != null) {
			nSongPlayers--;
			if (nSongPlayers == 0) {
				songPlayer.stop();
				songPlayer.release();
				songPlayer = null;
			}
		}
	}

	public MyWorldServer getLocalServer() {
		return localServer;
	}

	public void setLocalServer(MyWorldServer server) {
		localServer = server;
	}

	public String getCategoryName() {
		return this.categoryName;
	}

	public void setCategoryName(String categoryName) {
		this.categoryName = categoryName;
		fireClientModelChanged(ClientModelChangedEvent.EVENT_TYPE_SELECTED_CATEGORY_CHANGED);
	}

	public String getWorldName() {
		return this.worldName;
	}

	public void setWorldName(String worldName) {
		this.worldName = worldName;
		this.worldClassName = null;
		fireClientModelChanged(ClientModelChangedEvent.EVENT_TYPE_SELECTED_GAME_CHANGED);
	}

	public void setWorldName(String worldName, String worldClassName) {
		this.worldName = worldName;
		this.worldClassName = worldClassName;
		fireClientModelChanged(ClientModelChangedEvent.EVENT_TYPE_SELECTED_GAME_CHANGED);
	}

	public String getWorldClassName() {
		return this.worldClassName;
	}

	public String getWorldParams() {
		return this.worldParams;
	}

	public void setWorldParams(String params) {
		this.worldParams = params;
	}

	public void setAvatarName(String name) {
		avatarName = name;
		fireClientModelChanged(ClientModelChangedEvent.EVENT_TYPE_SELECTED_AVATAR_CHANGED);
	}

	public String getAvatarName() {
		return avatarName;
	}

	public boolean isPlayMusic() {
		return playMusic && isScreenOn();
	}

	public boolean isScreenOn() {
		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		return pm.isScreenOn();
	}

	public void setPlayMusic(boolean playMusic) {
		this.playMusic = playMusic;
	}

	public boolean isPlaySoundEffects() {
		return playSoundEffects && isScreenOn();
	}

	public void setPlaySoundEffects(boolean playSoundEffects) {
		this.playSoundEffects = playSoundEffects;
	}

	public boolean isVibration() {
		return vibration && isScreenOn();
	}

	public void setVibration(boolean vibration) {
		this.vibration = vibration;
	}
	
	public boolean canUseScreenControl() {
		if (context.getString(R.string.supportsScreenController).equals("true") && context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)) {
			return true;
		} else {
			return false;
		}
	}

	public boolean canUseSensors() {
		SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		if (sensorManager == null) {
			return false;
		}
		List<Sensor> gameSensors = sensorManager.getSensorList(Sensor.TYPE_GAME_ROTATION_VECTOR);
		if (gameSensors != null && gameSensors.size() > 0) {
			return true;
		}
		return false;
	}

	public boolean useSensors() {
		return useSensors && canUseSensors() && !useMoga(context) && !useGamepad(context);
	}

	public void setUseSensors(boolean useSensors) {
		this.useSensors = useSensors;
	}

	public boolean useZeemote() {
		return useZeemote && !useMoga(context) && !useGamepad(context);
	}

	public boolean useGamepad(Context context) {
		int[] deviceIds = InputDevice.getDeviceIds();
		for (int deviceId : deviceIds) {
			InputDevice dev = InputDevice.getDevice(deviceId);
			int sources = dev.getSources();
			// Verify that the device has gamepad buttons, control sticks, or both.
			if (((sources & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) || ((sources & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK)) {
				// This device is a game controller.
				return true;
			}
		}
		return false;
	}

	public boolean useMoga(Context context) {
		if (usingMoga) { // if ever moga was found, use it
			return true;
		}
		if (context instanceof GallantActivity) {
			com.bda.controller.Controller mogaController = ((GallantActivity) context).mogaController;
			if (mogaController == null) {
				return false;
			}
			usingMoga = mogaController.getState(com.bda.controller.Controller.STATE_CONNECTION) == com.bda.controller.Controller.ACTION_CONNECTED;
			return usingMoga;
		} else if (context instanceof ShowWorldActivity) {
			com.bda.controller.Controller mogaController = ((ShowWorldActivity) context).mogaController;
			if (mogaController == null) {
				return false;
			}
			usingMoga = mogaController.getState(com.bda.controller.Controller.STATE_CONNECTION) == com.bda.controller.Controller.ACTION_CONNECTED;
			return usingMoga;
		} else {
			return false;
		}
	}

	public boolean isMogaPocket() {
		if (!useMoga(context)) {
			return false;
		}
		if (context instanceof GallantActivity) {
			com.bda.controller.Controller mogaController = ((GallantActivity) context).mogaController;
			if (mogaController == null) {
				return false;
			} else {
				return mogaController.getState(com.bda.controller.Controller.STATE_CURRENT_PRODUCT_VERSION) == com.bda.controller.Controller.ACTION_VERSION_MOGA;
			}
		} else if (context instanceof ShowWorldActivity) {
			com.bda.controller.Controller mogaController = ((ShowWorldActivity) context).mogaController;
			if (mogaController == null) {
				return false;
			}
			boolean isMogaPocket = mogaController.getState(com.bda.controller.Controller.STATE_CURRENT_PRODUCT_VERSION) == com.bda.controller.Controller.ACTION_VERSION_MOGA;
			return isMogaPocket;
		} else {
			return false;
		}
	}

	public void calibrateSensors() {
		fireClientModelChanged(ClientModelChangedEvent.EVENT_TYPE_CALIBRATE_SENSORS);
	}

	public void setUseZeemote(boolean useZeemote) {
		this.useZeemote = useZeemote;
	}

	public boolean useScreenControl() {
		return useScreenControl && !useMoga(context); // && hasTouchScreen();
	}

	public void setUseScreenControl(boolean useScreenControl) {
		this.useScreenControl = useScreenControl;
	}

	public void setControlOnLeft(boolean controlOnLeft) {
		this.controlOnLeft = controlOnLeft;
	}

	public boolean isControlOnLeft() {
		return this.controlOnLeft;
	}

	public void setControlSensitivity(float sensitivity) {
		this.controlSensitivity = sensitivity;
	}

	public float getControlSensitivity() {
		return this.controlSensitivity;
	}

	public boolean useDPad() {
		return !useScreenControl() && !useSensors() && !useZeemote();
	}

	public void setAlertListener(AlertListener listener) {
		alertListener = listener;
	}

	public void selectColor(String title, int initialColor, SelectColorHandler handler) {
		if (alertListener != null) {
			alertListener.onSelectColor(title, initialColor, handler);
		}
	}

	public void setStereoscopic(boolean stereo) {
		this.stereoscopic = stereo;
	}

	public boolean connectToZeemote(Context context) {
		if (zeeController == null) {
			zeeController = new Controller(1, Controller.TYPE_JS1);
		}
		cancelZee = false;
		try {
			while (!cancelZee) {
				try {
					if (zeeController.isConnected()) {
						return true;
					}
					System.out.println("getting devices");
					IDeviceSearch deviceSearch = DeviceFactory.getDeviceSearch();
					deviceSearch.findDevices(new ZeeProgressMonitor());
					Vector<IStreamConnector> deviceStreams = deviceSearch.getStreamConnectorList();
					System.out.println("connecting to device");
					zeeController.connect(deviceStreams.get(0));
					System.out.println("connected");
					zeeConnected = true;
					if (zeeController.isConnected()) {
						return true;
					}
				} catch (Exception e) {
					System.err.println("Exception accessing zeeMote: " + e.getMessage());
				}
				Thread.sleep(1000);
			}
		} catch (InterruptedException e) {
		}
		return false;
	}

	public void cancelZeemoteConnect() {
		cancelZee = true;
	}

	public Controller getZeeController() {
		return zeeController;
	}

	public void setScore(int game, int score) {
		this.scores[game - 1] = score;
	}

	public int getScore(int game) {
		return this.scores[game - 1];
	}

	public void setLevel(int game, int level) {
		this.levels[game - 1] = level;
	}

	public int getLevel(int game) {
		return this.levels[game - 1];
	}

	public void setTime(int game, int time) {
		this.times[game - 1] = time;
	}

	public int getTime(int game) {
		return this.times[game - 1];
	}

	public void flashMessage(String message, boolean blink) {
		this.flashMessage = message;
		this.blink = blink;
		fireClientModelChanged(ClientModelChangedEvent.EVENT_TYPE_MESSAGE_FLASHED);
	}

	public String getFlashMessage() {
		return flashMessage;
	}

	public boolean isFlashMessageBlink() {
		return blink;
	}

	public void setAlwaysStartAsNew(boolean alwaysStartAsNew) {
		this.alwaysStartAsNew = alwaysStartAsNew;
	}

	public boolean getAlwaysStartAsNew() {
		return alwaysStartAsNew;
	}

	public void setAlwaysStartAsOld(boolean alwaysStartAsOld) {
		this.alwaysStartAsOld = alwaysStartAsOld;
	}

	public boolean getAlwaysStartAsOld() {
		return alwaysStartAsOld;
	}

	public void setSimpleRendering(boolean simpleRendering) {
		this.simpleRendering = simpleRendering;
	}

	public boolean isSimpleRendering() {
		return simpleRendering;
	}

	public boolean isFullVersion() {
		try {
			return fullVersion // actually marked a full version
					|| (!isGoogle() && !isAmazon()) // is not a market that supports in-app purchase
			;
		} catch (Exception e) { // a problem figuring out above
			return true; // free!
		}
	}

	private String themeName = "com.gallantrealm.myworld.android.themes.DefaultTheme";
	private Theme theme = new DefaultTheme();
	Typeface typeface;

	public void buyFullVersion() {
		try {
			purchaseHelper.flagEndAsync();
			purchaseHelper.launchPurchaseFlow(context, SKU_FULL_VERSION, IabHelper.ITEM_TYPE_SUBS, RC_REQUEST, new OnIabPurchaseFinishedListener() {
				public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
					if (purchaseHelper == null) { // shutdown
						return;
					}
					if (result.isFailure()) {
						System.err.println("Purchase Failed: " + result.getMessage());
						// new MessageDialog(context, "Purchase Failed", "There was an error purchasing. Check your internet connection and try again later.", null).show();
						// Note: No need to show an error as Google Play's error dialog is better.
						return;
					}
					setFullVersion(true);
					savePreferences(context);
					new MessageDialog(context, "Purchase Success", "Thanks for purchasing!  Restart the app to enable all features and remove ads.", null).show();
				}
			}, PAYLOAD);
		} catch (Exception e) {
			e.printStackTrace();
			new MessageDialog(context, "Purchase Failed", "There was an error launching Google Play for purchasing.  Please make sure Google Play is installed and working.", null).show();
		}
	}

	public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
		if (purchaseHelper != null) {
			try {
				purchaseHelper.handleActivityResult(requestCode, resultCode, data);
			} catch (Exception e) {
			}
		}
		return false;
	}

	public boolean isGoggleDogPass() {
		return goggleDogPass;
	}

	public void setGoggleDogPass(boolean pass) {
		goggleDogPass = pass;
	}

	public boolean canShowAds() {
		if (isChromebook()) {
			return false;
		}
		return ((isAmazon() || isGoogle()) && !context.getString(R.string.amazonappkey).equals("")) //
				|| (isGoogle() && !context.getString(R.string.admobid).equals(""));
	}

	public void initAds() {
		if (!isFullVersion()) {
			try {
				if (context.getString(R.string.amazonappkey).length() > 0) { // prepare for amazon ads
					System.out.println("Registering for Amazon ads.");
					if (BuildConfig.DEBUG) {
						AdRegistration.enableTesting(true);
						AdRegistration.enableLogging(true);
					}
					AdRegistration.setAppKey(context.getString(R.string.amazonappkey));
					AdRegistration.registerApp(context);
				}
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}

	public boolean isGoogle() {
		return market == GOOGLE;
	}

	public boolean isAmazon() {
		return market == AMAZON;
	}

	public boolean isFree() {
		return market == FREEVERSION;
	}

	public boolean isChromebook() {
		return context.getPackageManager().hasSystemFeature("org.chromium.arc.device_management");
	}

	public boolean isWorldUnlocked(int i) {
		if (isFullVersion()) {
			return true;
		}
		if (isGoggleDogPass()) {
			return true;
		}
		return unlocked[i - 1];
	}

	public void setWorldUnlocked(int i) {
		unlocked[i - 1] = true;
		savePreferences(context);
	}

	/**
	 * Sets only the indicated world unlocked and locks all others (that have zero scores).
	 * 
	 * @param i
	 */
	public void setOnlyWorldUnlocked(int i) {
		for (int j = 0; j < unlocked.length; j++) {
			if (getScore(j + 1) == 0) {
				unlocked[j] = false;
			}
		}
		unlocked[i - 1] = true;
		savePreferences(context);
	}

	public void setAllWorldsUnlocked() {
		for (int j = 0; j < unlocked.length; j++) {
			if (getScore(j + 1) == 0) {
				unlocked[j] = true;
			}
		}
		savePreferences(context);
	}

	public void setFullVersion(boolean fullVersion) {
		this.fullVersion = fullVersion;
		fireClientModelChanged(ClientModelChangedEvent.EVENT_TYPE_FULLVERSION_CHANGED);
	}

	public boolean isPowerSaver() {
		return false; // no longer an option
	}

	public void setPowerSaver(boolean powerSaver) {
		this.powerSaver = powerSaver;
	}

	public void showBannerAds() {
		if (context instanceof ShowWorldActivity && context.getString(R.string.showBannerAds).equals("true")) {
			((ShowWorldActivity) context).showBannerAd();
		}
	}

	public void hideBannerAds() {
		if (context instanceof ShowWorldActivity) {
			((ShowWorldActivity) context).hideBannerAd();
		}
	}

	public void showPopupAd() {
		if (context.getString(R.string.showPopupAds).equals("true") && !isFullVersion() && !useMoga(context)) {
			System.out.println("POPUP AD!!!!!");
			new AdDialog(context).show();
		}
	}

	public TexturePickerDialog lastTexturePickerDialog;

	public boolean isStereoscopic() {
		return stereoscopic;
	}

	public void setThemeName(String themeName) {
		this.themeName = themeName;
		try {
			this.theme = (Theme) this.getClass().getClassLoader().loadClass(themeName).newInstance();
			typeface = null;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String getThemeName() {
		return themeName;
	}

	public Theme getTheme() {
		return theme;
	}

	HashMap<String, Object> properties = new HashMap<String, Object>();

	public Typeface getTypeface(Context context) {
		if (typeface == null) {
			try {
				String font = getTheme().font;
				typeface = Typeface.createFromAsset(context.getAssets(), font);
			} catch (Throwable e) {
				System.err.println("Could not create typeface for app.");
			}
		}
		return typeface;
	}

	public Object getProperty(String propertyName) {
		return properties.get(propertyName);
	}

	/**
	 * Loads a bitmap for editing, such as in the eggworld decorator
	 * 
	 * @param fileName
	 * @return
	 */
	public Bitmap loadBitmap(String fileName) {
		Bitmap bitmap = null;
		try {
			File file = new File(getContext().getFilesDir(), fileName);
			if (!file.exists()) {
				// create a new bitmap, set to white
				bitmap = Bitmap.createBitmap(512, 512, Config.RGB_565);
				for (int i = 0; i < 512; i++) {
					for (int j = 0; j < 512; j++) {
						bitmap.setPixel(i, j, 0xFFFFFFFF);
					}
				}
				saveBitmap(bitmap, fileName);
			}
			InputStream inStream = new BufferedInputStream(new FileInputStream(file), 65536);
			Bitmap tbitmap = BitmapFactory.decodeStream(inStream);
			inStream.close();
			bitmap = tbitmap.copy(tbitmap.getConfig(), true);
			tbitmap.recycle();
			// bitmap = tbitmap;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return bitmap;
	}

	/**
	 * Saves a bitmap to app-local files.
	 * 
	 * @param bitmap
	 * @param fileName
	 */
	public void saveBitmap(Bitmap bitmap, String fileName) {
		try {
			File file = new File(getContext().getFilesDir(), fileName);
			if (file.exists()) {
				file.delete();
			}
			OutputStream outStream = new BufferedOutputStream(new FileOutputStream(file), 65536);
			bitmap.compress(CompressFormat.PNG, 100, outStream);
			outStream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Loads a RAW wave file into a wave table
	 * 
	 * @param fileName
	 * @return
	 */
	public double[] loadWave(String fileName, boolean external) throws Exception {
		System.out.println("Loading wav file: " + fileName);
		File file;
		if (fileName.startsWith("file:")) { // via an external url
			file = new File(fileName.substring(7));
		} else if (fileName.startsWith("/")) { // full path file
			file = new File(fileName);
		} else { // within the application
			if (external) {
				try {
					file = new File(getContext().getExternalFilesDir(null), fileName);
				} catch (Exception e) {
					file = new File(getContext().getFilesDir(), fileName);
				}
			} else {
				file = new File(getContext().getFilesDir(), fileName);
			}
		}
		InputStream is;
		long len;
		if (file.exists()) {
			is = new FileInputStream(file);
			len = file.length();
		} else {
			// if file not found, perhaps it has a built-in replacement, so try assets
			System.out.println("Looking for wav in assets");
			file = null;
			fileName = trimName(fileName);
			is = context.getAssets().open(fileName);
			len = context.getAssets().openFd(fileName).getLength();
		}
		WavFile wavFile = WavFile.openWavFile(is, len);
		int waveLength = Math.min((int) wavFile.getNumFrames(), 1000000);
		double[] wave = new double[waveLength];
		wavFile.readFramesMono(wave, waveLength);
		return wave;
	}

	private String trimName(String sampleName) {
		if (sampleName.lastIndexOf("/") >= 0) {
			return sampleName.substring(sampleName.lastIndexOf("/") + 1);
		} else {
			return sampleName;
		}
	}

	public Object loadObject(String fileName) {
		return loadObject(fileName, false);
	}

	public InputStream loadFile(String fileName, boolean external) {
		InputStream inStream = null;
		if (fileName.startsWith("file:")) { // via an external url
			try {
				File file = new File(fileName.substring(7));
				inStream = new FileInputStream(file);
			} catch (Exception e) {
			}
		} else { // within the application
			if (external && getContext().getExternalFilesDir(null) != null) { // external file
				try {
					File file = new File(getContext().getExternalFilesDir(null), fileName);
					inStream = new FileInputStream(file);
				} catch (Exception e) {
				}
			}
			if (inStream == null) { // internal file
				try {
					File file = new File(getContext().getFilesDir(), fileName);
					inStream = new FileInputStream(file);
				} catch (Exception e) {
				}
			}
			// if file not found, it is a built-in. so try asset
			if (inStream == null) {
				try {
					inStream = context.getAssets().open(fileName.trim());
				} catch (Exception e) {
				}
			}
		}
		return inStream;
	}

	/**
	 * Loads a serializable object from a file.
	 * 
	 * @param fileName
	 * @return
	 */
	public Object loadObject(String fileName, boolean external) {
		Object object = null;
		if (fileName.startsWith("file:")) { // via an external url
			try {
				File file = new File(fileName.substring(7));
				ObjectInputStream inStream = new ObjectInputStream(new FileInputStream(file));
				object = inStream.readObject();
				inStream.close();
			} catch (Exception e) {
			}
		} else { // within the application
			if (external && getContext().getExternalFilesDir(null) != null) { // external file
				try {
					File file = new File(getContext().getExternalFilesDir(null), fileName);
					ObjectInputStream inStream = new ObjectInputStream(new FileInputStream(file));
					object = inStream.readObject();
					inStream.close();
				} catch (Exception e) {
				}
			}
			if (object == null) { // internal file
				try {
					File file = new File(getContext().getFilesDir(), fileName);
					ObjectInputStream inStream = new ObjectInputStream(new FileInputStream(file));
					object = inStream.readObject();
					inStream.close();
				} catch (Exception e) {
				}
			}
			// if file not found, it is a built-in. so try asset
			if (object == null) {
				try {
					InputStream is = context.getAssets().open(fileName.trim());
					ObjectInputStream inStream = new ObjectInputStream(is);
					object = inStream.readObject();
					inStream.close();
				} catch (Exception e) {
				}
			}
		}
		return object;
	}

	public void deleteObject(String fileName) {
		deleteObject(fileName, false);
	}

	public void deleteObject(String fileName, boolean external) {
		try {
			File file;
			if (fileName.startsWith("file:")) { // an external file
				file = new File(fileName.substring(7));
			} else { // within the application
				if (external && getContext().getExternalFilesDir(null) != null) {
					file = new File(getContext().getExternalFilesDir(null), fileName);
				} else {
					file = new File(getContext().getFilesDir(), fileName);
				}
			}
			file.delete();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void saveObject(Object object, String fileName) {
		saveObject(object, fileName, false);
	}

	/**
	 * Saves an object to app-local files. The object needs to be serializable
	 * 
	 * @param bitmap
	 * @param fileName
	 */
	public void saveObject(Object object, String fileName, boolean external) {
		boolean saved = false;
		if (external && getContext().getExternalFilesDir(null) != null) {
			try {
				File file = new File(getContext().getExternalFilesDir(null), fileName);
				if (file.exists()) {
					file.delete();
				}
				ObjectOutputStream outStream = new ObjectOutputStream(new FileOutputStream(file));
				outStream.writeObject(object);
				outStream.close();
				saved = true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (!saved) {
			try {
				File file = new File(getContext().getFilesDir(), fileName);
				if (file.exists()) {
					file.delete();
				}
				ObjectOutputStream outStream = new ObjectOutputStream(new FileOutputStream(file));
				outStream.writeObject(object);
				outStream.close();
				saved = true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Exports an object to sdcard storage.
	 */
	public File exportObject(Object object, String fileName) {
		File file;
		try {
			File appDir = new File(Environment.getExternalStorageDirectory() + "/Android/data/" + context.getApplicationInfo().packageName);
			if (!appDir.exists()) {
				appDir.mkdir();
			}
			file = new File(appDir, fileName);
			if (file.exists()) {
				file.delete();
			}
			ObjectOutputStream outStream = new ObjectOutputStream(new FileOutputStream(file));
			outStream.writeObject(object);
			outStream.close();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return file;
	}

	public boolean isCustomizeMode() {
		return customizeMode;
	}

	public void setCustomizeMode(boolean mode) {
		customizeMode = mode;
	}

	public boolean isCustomizable(int worldNum) {
		if (!isWorldUnlocked(worldNum)) {
			return false;
		}
		int id = 0;
		if (worldNum == 1) {
			id = R.string.world1customizable;
		} else if (worldNum == 2) {
			id = R.string.world2customizable;
		} else if (worldNum == 3) {
			id = R.string.world3customizable;
		} else if (worldNum == 4) {
			id = R.string.world4customizable;
		} else if (worldNum == 5) {
			id = R.string.world5customizable;
		} else if (worldNum == 6) {
			id = R.string.world6customizable;
		} else if (worldNum == 7) {
			id = R.string.world7customizable;
		} else if (worldNum == 8) {
			id = R.string.world8customizable;
		} else if (worldNum == 9) {
			id = R.string.world9customizable;
		} else if (worldNum == 10) {
			id = R.string.world10customizable;
		} else if (worldNum == 11) {
			id = R.string.world11customizable;
		} else if (worldNum == 12) {
			id = R.string.world12customizable;
		} else if (worldNum == 13) {
			id = R.string.world13customizable;
		} else if (worldNum == 14) {
			id = R.string.world14customizable;
		} else if (worldNum == 15) {
			id = R.string.world15customizable;
		} else if (worldNum == 16) {
			id = R.string.world16customizable;
		} else if (worldNum == 17) {
			id = R.string.world17customizable;
		} else if (worldNum == 18) {
			id = R.string.world18customizable;
		} else if (worldNum == 19) {
			id = R.string.world19customizable;
		} else if (worldNum == 20) {
			id = R.string.world20customizable;
		} else if (worldNum == 21) {
			id = R.string.world21customizable;
		} else if (worldNum == 22) {
			id = R.string.world22customizable;
		} else if (worldNum == 23) {
			id = R.string.world23customizable;
		} else if (worldNum == 24) {
			id = R.string.world24customizable;
		} else {
			return false;
		}
		if (context == null) {
			return false;
		}
		return "true".equals(context.getString(id));
	}

	public String getAvatarDisplayName(int avatarNum, String name) {
		if (avatarDisplayNames[avatarNum - 1] != null) {
			return avatarDisplayNames[avatarNum - 1];
		}
		return name;
	}

	public void setAvatarDisplayName(int avatarNum, String displayName) {
		avatarDisplayNames[avatarNum - 1] = displayName;
	}

	public void vibrate(int milliseconds) {
		if (isVibration()) {
			Vibrator vibrator = (Vibrator) context.getSystemService(Activity.VIBRATOR_SERVICE);
			if (vibrator != null) {
				vibrator.vibrate(milliseconds);
			}
		}
	}

	public boolean hasTouchScreen() {
		PackageManager pm = context.getPackageManager();
		if (pm.hasSystemFeature(PackageManager.FEATURE_FAKETOUCH)) {
			return true;
		}
		return false;
	}

	public void setLocalFolder(String localFolder) {
		this.localFolder = localFolder;
		savePreferences(this.context);
	}

	public String getLocalFolder() {
		return localFolder;
	}

	public void setShowDebugLogging(boolean showDebugLogging) {
		this.showDebugLogging = showDebugLogging;
	}

	public boolean isShowDebugLogging() {
		return showDebugLogging;
	}

	public void setProperty(String propertyName, Object value) {
		properties.put(propertyName, value);
	}

	public String getGallantUrl() {
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
			return "https://gallantrealm.com";
		} else {
			return "http://gallantrealm.com";
		}
	}

	public void log(String message) {
		for (int i = 1; i < MAX_LOG_LINES; i++) {
			logMessages[i - 1] = logMessages[i];
		}
		logMessages[MAX_LOG_LINES - 1] = message;
		fireClientModelChanged(ClientModelChangedEvent.EVENT_TYPE_LOG_UPDATED);
	}

	public void clearLog() {
		for (int i = 0; i < MAX_LOG_LINES; i++) {
			logMessages[i] = "";
		}
	}

	// Private methods

}
