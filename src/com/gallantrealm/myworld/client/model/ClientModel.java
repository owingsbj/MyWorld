package com.gallantrealm.myworld.client.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import com.gallantrealm.myworld.FastMath;
import com.gallantrealm.myworld.android.AndroidClientModel;
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

/**
 * This class maintains information shared within the client. Controls and actions in the client act upon the data in this model. Other views, control panels, actions will listen on events from this model and update their state accordingly.
 * This provides a clean separation of model from view, implementing the Document-View design pattern.
 */
public abstract class ClientModel {

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

	private static final float TORADIAN = 0.0174532925f;

	public Logger logger = Logger.getLogger("com.gallantrealm.myworld.client");
	public Preferences preferences;

	// Private data

	public final ArrayList<ClientModelChangedListener> listeners;
	public WWWorld world;
	public WWWorld localWorld;
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
		return localWorld != null;
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

	private final WWWorld getWorld() {
		return world;
	}

	public void setWorld(WWWorld world) {
		this.world = world;
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
		if (((AndroidClientModel) this).isPowerSaver()) {
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

	public void setLocalWorld(WWWorld localWorld) {
		this.localWorld = localWorld;
		setWorld(localWorld);
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
		if (localWorld != null) {
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
		if (localWorld != null) {
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
		if (localWorld != null) {
			// TODO
		} else {
			if (requestThread != null) {
				ClientRequest request = new UpdateObjectRequest(objectId, object);
				requestThread.queue(request);
			}
		}
	}

	public void updateEntity(int entityId, WWEntity entity) {
		if (localWorld != null) {
			// TODO
		} else {
			if (requestThread != null) {
				ClientRequest request = new UpdateEntityRequest(entityId, entity);
				requestThread.queue(request);
			}
		}
	}

	public void updateWorldProperties(WWWorld updatedWorld) {
		if (localWorld != null) {
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
		if (localWorld != null) {
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
		if (localWorld != null) {
			localWorld.thrustObject(objectId, object);
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
		if (localWorld != null) {
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
		if (viewpoint == 0) { // behind avatar
			setCameraObject(avatar);
			setCameraDistance(behindDistance);
			setCameraTilt(behindTilt);
			setCameraPan(0.0f);
		} else if (viewpoint == 1) { // top or head of avatar
			// TODO consider making head object name adjustable
			WWObject head = avatar.getDescendant("head");
			if (head != null) {
				setCameraObject(head);
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

	public void setCameraDampRate(float cameraDampRate) {
		this.cameraDampRate = cameraDampRate;
	}

	public void forceAvatar(float thrustVelocity, float turnVelocity, float liftVelocity, float tiltVelocity, float leanVelocity) {
		forceAvatar(thrustVelocity, turnVelocity, liftVelocity, tiltVelocity, leanVelocity, 0);
	}

	/**
	 * Moves an avatar using the thrust velocity and torque velocity specified.
	 */
	public void forceAvatar(float thrustVelocity, float turnVelocity, float liftVelocity, float tiltVelocity, float leanVelocity, float slideVelocity) {

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
			if (localWorld != null) {
				avatar.setThrust(new WWVector(slideVelocity, -thrustVelocity, liftVelocity));
				avatar.setThrustVelocity(new WWVector(slideVelocity, -thrustVelocity, liftVelocity));
				avatar.setTorque(new WWVector(tiltVelocity, leanVelocity, turnVelocity));
				avatar.setTorqueVelocity(new WWVector(tiltVelocity, leanVelocity, turnVelocity));
			} else {
				WWObject updatedAvatar = (WWObject) avatar.cloneNoBehavior();
				updatedAvatar.setThrust(new WWVector(slideVelocity, -thrustVelocity, liftVelocity));
				updatedAvatar.setThrustVelocity(new WWVector(slideVelocity, -thrustVelocity, liftVelocity));
				updatedAvatar.setTorque(new WWVector(tiltVelocity, leanVelocity, turnVelocity));
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
		System.out.println("ClientModel.setCameraObject " + cameraObject);
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
		if (selectedObject == getAvatar()) {
			x = xcamera + cameraDistance * FastMath.sin(TORADIAN * cameraPan + selectedObject.getRotation(worldTime).getZ()) * FastMath.cos(TORADIAN * cameraTilt);
			y = ycamera + cameraDistance * FastMath.cos(TORADIAN * cameraPan + selectedObject.getRotation(worldTime).getZ()) * FastMath.cos(TORADIAN * cameraTilt);
		} else {
			x = xcamera + cameraDistance * FastMath.sin(TORADIAN * cameraPan) * FastMath.cos(TORADIAN * cameraTilt);
			y = ycamera + cameraDistance * FastMath.cos(TORADIAN * cameraPan) * FastMath.cos(TORADIAN * cameraTilt);
		}
		float z = zcamera + FastMath.sin(TORADIAN * cameraTilt) * cameraDistance;
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
			getWorld().pause();
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
			getWorld().run();
		} else {
			ClientRequest request = new ResumeWorldRequest();
			requestThread.queue(request);
		}
		System.out.println("<ClientModel.resumeWorld");
	}

	public void calibrateSensors() {
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
		if (world == null || actionId >= world.getAvatarActions().length) {
			return;
		} else if (world.getAvatarActions()[actionId] == null) {
			return;
		}
		world.startAvatarAction(actionId, x, y);
		startRepeatAvatarAction(actionId, x, y);
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
		if (world == null || action >= world.getWorldActions().length) {
			return;
		} else if (world.getWorldActions()[action] == null) {
			return;
		}
		// world.getWorldActions()[action].start();
		world.startWorldAction(action, 0, 0);
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

	public void selectAlert(final String message, final Class[] availableItems, final String[] options, SelectResponseHandler handler) {
		if (alertListener != null) {
			alertListener.onSelectAlert(message, availableItems, options, handler);
		}
	}

	public void inputAlert(final String title, final String message, String initialValue, final String[] options, InputResponseHandler handler) {
		if (alertListener != null) {
			alertListener.onInputAlert(title, message, initialValue, options, handler);
		}
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

	public boolean isStereoscopic() {
		return stereoscopic;
	}

	HashMap<String, Object> properties = new HashMap<String, Object>();

	public Object getProperty(String propertyName) {
		return properties.get(propertyName);
	}

	public void setProperty(String propertyName, Object value) {
		properties.put(propertyName, value);
	}

	// Private methods

}
