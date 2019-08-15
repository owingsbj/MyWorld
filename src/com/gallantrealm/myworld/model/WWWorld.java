package com.gallantrealm.myworld.model;

import java.io.IOException;
import java.nio.IntBuffer;
import java.util.ArrayList;
import com.gallantrealm.myworld.FastMath;
import com.gallantrealm.myworld.android.AndroidClientModel;
import com.gallantrealm.myworld.android.renderer.AndroidRenderer;
import com.gallantrealm.myworld.client.model.ClientModel;
import com.gallantrealm.myworld.client.model.ClientModelChangedEvent;
import com.gallantrealm.myworld.client.model.ClientModelChangedListener;
import com.gallantrealm.myworld.client.renderer.IRenderable;
import com.gallantrealm.myworld.client.renderer.IRenderer;
import com.gallantrealm.myworld.client.renderer.IRendering;
import com.gallantrealm.myworld.communication.DataInputStreamX;
import com.gallantrealm.myworld.communication.DataOutputStreamX;
import com.gallantrealm.myworld.model.behavior.BehaviorThread;
import com.gallantrealm.myworld.model.persistence.SaveWorldThread;
import com.gallantrealm.myworld.model.physics.NewPhysicsThread;
import com.gallantrealm.myworld.model.physics.ObjectCollision;
import com.gallantrealm.myworld.model.physics.PhysicsThread;
import android.opengl.GLES20;

/**
 * This is the anchor for all objects representing the world.
 * <p>
 * Aspects of this world are shared as needed between the server and client, but not everything -- some information is private to the client or server. For this reason, customized send/receive logic is used rather than using default Java
 * serialization. Serialization is still used however, to preserve the state of the world to a more permanent store.
 */
public class WWWorld extends WWEntity implements IRenderable, ClientModelChangedListener {
	static final long serialVersionUID = 1L;

	public static final int MAX_USERS = 100;
	public static final int MAX_OBJECTS = 10000;

	public WWUser[] users;
	public WWObject[] objects;
	public int lastObjectIndex = 1;

	// World properties
	float gravity = 9.8f;
	float fogDensity = 0.0f;
	float ambientLightIntensity = 0.5f;
	int skyColor = 0xC0C0FF; // light bluehue
	float sunIntensity = 1.0f;
	float underglowIntensity = 0.25f;
	float sunDirectionX = 0.5f;
	float sunDirectionY = 0.5f;
	float sunDirectionZ = 1.0f;
	int sunColor = 0xFFFFFF; // pure white
	public float renderingThreshold = 1000.0f; // Note: not currently working right on Android
	int nextGroup;
	public WWAction[] actions;
	boolean persistent; // if true the world is saved by a background thread
	boolean confrontMode;  // the world's avatar turns automatically on camera pan

	// Game related properties
	int level;
	int score = -1;
	String status = "";

	// Transient data
	public final boolean createPhysicsThread;
	public final boolean createBehaviorThread;
	public final String saveWorldFileName;
	public final int iterationTime;
	public transient PhysicsThread physicsThread;
	public transient BehaviorThread behaviorThread;
	public transient SaveWorldThread saveWorldThread;
	public transient boolean onClient;
	public transient boolean rendered;
	public transient ArrayList<String> preloadedTextures;
	public transient ArrayList<String> preloadedSounds;

	/**
	 * The current time in the world. When the world is created the time starts at zero. It is then updated whenever the physics thread is running.
	 */
	long worldTime;

	/** The time difference between the server and the client. */
	transient long serverTimeDelta;

	// Transient values related to rendering
	public transient IRendering rendering;
	transient long lastRenderingTime;

	public WWWorld() {
		this(true, true, null, 15, false);
	}

	/**
	 * Constructor. This is used both for client and server versions of the world. A world should never be created by a behavior.
	 * 
	 * @param createPhysicsThread
	 *            if true, a physics thread will be created to perform physical interactions between objects. This is required on the server, and is optional on the client.
	 * @param createBehaviorThread
	 *            if true, a behavior thread is created. This always true on for server. If made true on the client, behaviors will be downloaded for all objects and performed on the client simultaneously with the server. This can provide
	 *            more realistic behavior.
	 * @param iterationTime
	 *            the time between iterations of physics simulations.
	 */
	public WWWorld(boolean createPhysicsThread, boolean createBehaviorThread, String saveWorldFileName, int iterationTime, boolean onClient) {
		world = this;
		users = new WWUser[MAX_USERS];
		objects = new WWObject[MAX_OBJECTS];
		this.createPhysicsThread = createPhysicsThread;
		this.createBehaviorThread = createBehaviorThread;
		this.saveWorldFileName = saveWorldFileName;
		this.iterationTime = iterationTime;
		this.onClient = onClient;
		if (onClient) {
			ClientModel.getClientModel().setLocalWorld(this);
		} else {
			ClientModel.getClientModel().setWorld(this);
		}
		// if (createPhysicsThread) {
		// physicsThread = new PhysicsThread(this, iterationTime);
		// physicsThread.start();
		// }
		// if (createBehaviorThread) {
		// behaviorThread = new BehaviorThread();
		// behaviorThread.start();
		// }
	}

	boolean isOnClient() {
		return onClient;
	}

	/**
	 * Invoked after the world has been restored from a saved state. (It is not displayed yet.)
	 */
	public void restored() {
	}

	/**
	 * The (local) world is now being displayed.
	 */
	public void displayed() {
	}

	/**
	 * Set the delta time between the server and the client. This is used to synchronize clocks between the client and the server, providing more accurate world time. On the server or local world the delta time is always zero.
	 */
	public void setDeltaTime(long deltaTime) {
		serverTimeDelta = deltaTime;
	}

	/**
	 * Returns the current time in the world. This value takes into account any difference in time clocks between the client and server and attempts to keep all clients synchronized. This value should be used for all time calculations
	 * involving physics movement.
	 */
	@Override
	public long getWorldTime() {
		return worldTime + serverTimeDelta;
	}

	// private transient long actualTimeAtLastUpdate;

	/**
	 * Returns the current time in the world, at exactly the point when the call is made to this method. This is used for rendering only and should not be used in any calculations involving physics movement.
	 * 
	 * @return
	 */
	public long getRenderingWorldTime() {
		// if (running) {
		// return worldTime + serverTimeDelta + System.currentTimeMillis() - actualTimeAtLastUpdate;
		// } else {
		return worldTime + serverTimeDelta;
		// }
	}

	/**
	 * Invoked by the physics thread to update the time
	 */
	public void updateWorldTime(long millis) {
		worldTime += millis;
		// actualTimeAtLastUpdate = System.currentTimeMillis();
	}

	/**
	 * Returns the number of millisecond between iterations of the physics engine
	 */
	public long getPhysicsIterationTime() {
		return iterationTime;
	}

	/**
	 * Note: Do NOT call this method from client-side logic as the id's of the objects on the client and server will be out-of-sync.
	 */
	public int addObject(WWObject object) {
		// TODO add a test to make sure this is called from server-side logic only
		if (object.getCreateTime() == 0) {
			long time = getWorldTime();
			object.setCreateTime(time);
			object.setLastModifyTime(time);
			object.setLastMoveTime(time);
		}
		for (int i = 1; i < objects.length; i++) {
			if (objects[i] == null || objects[i].isDeleted()) {
				object.setWorld(this);
				objects[i] = object;
				object.id = i;
				if (i > lastObjectIndex) {
					lastObjectIndex = i;
				}
				// if the object has children, add the children as well
				WWObject[] childObjects = object.getChildren();
				for (int j = 0; j < childObjects.length; j++) {
					addObject(childObjects[j]);
					childObjects[j].parentId = object.id;
				}
				return i;
			}
		}
		return -1;
	}

	public void removeObject(int id) {
		// TODO add a test to make sure this is called from server-side logic only
		if (id <= 0) {
			throw new IllegalArgumentException("Invalid object id: " + id);
		}
		WWObject object = objects[id];
		if (object != null) {

			// if the object has children, remove the children as well
			WWObject[] children = object.getChildren();
			for (int i = 0; i < children.length; i++) {
				removeObject(children[i].id);
			}

			// if the object is a member of a collection, remove it from the collection
			if (object.parentId != 0) {
				WWObject parent = getObject(object.parentId);
				if (parent != null) {
					parent.removeChild(object);
					parent.setLastModifyTime(getWorldTime());
				}
				object.parentId = 0;
			}

			object.setDeleted();
			object.setLastModifyTime(getWorldTime());
			object.id = 0;
			object.dropRendering();
		}
	}

	public final WWObject[] getObjects() {
		return objects;
	}

	public int addUser(WWUser user) {
		// TODO add a test to make sure this is called from server-side logic only
		for (int i = 0; i < users.length; i++) {
			if (users[i] == null || users[i].isDeleted()) {
				user.setWorld(this);
				users[i] = user;
				return i;
			}
		}
		return -1;
	}

	public void removeUser(int id) {
		// TODO add a test to make sure this is called from server-side logic only
		users[id].setDeleted();
		users[id].setLastModifyTime(getWorldTime());
	}

	public WWUser[] getUsers() {
		return users;
	}

	public WWUser getUser(String userName) {
		for (int i = 0; i < users.length; i++) {
			WWUser user = users[i];
			if (user != null && user.getName() != null && user.getName().equalsIgnoreCase(userName)) {
				return user;
			}
		}
		return null;
	}

	public WWUser getUser(int id) {
		return users[id];
	}

	public int getUserId(WWUser user) {
		for (int i = 0; i < users.length; i++) {
			if (users[i] == user) {
				return i;
			}
		}
		return -1;
	}

	public final WWObject getObject(int id) {
		if (id < 0) {
			throw new IllegalArgumentException("Invalid object id: " + id);
		}
		return objects[id];
	}

	public int getObjectId(WWObject object) {
		for (int i = 0; i < objects.length; i++) {
			if (objects[i] == object) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Updates most properties of an object (which is probably not ideal, but easy to do).
	 * 
	 * @param id
	 *            the identifier of the object in world to be replaced
	 * @param newObject
	 *            the new object which will replace the in world object
	 */
	public void updateObject(int id, WWObject newObject) {
		if (id < 0) {
			return;
		}
		WWObject oldObject = getObject(id);
		oldObject.dropRendering();
		oldObject.copyFrom(newObject);
	}

	/**
	 * Move an object. The newObject describes the new location and rotation of the object. If the object is physical, the velocity and angular momentum are also updated from the newobject.
	 */
	public void moveObject(int id, WWObject newObject) {
		if (id < 0) {
			return;
		}
		WWObject oldObject = getObject(id);
		if (oldObject != null) {
			// synchronized (oldObject) {
			long time = newObject.getLastMoveTime();
			WWVector newThrust = newObject.getThrust();
			WWVector newThrustVelocity = newObject.getThrustVelocity();
			WWVector newTorque = newObject.getTorque();
			WWVector newTorqueVelocity = newObject.getTorqueVelocity();
			WWVector newPosition = newObject.getPosition(time);
			WWVector newRotation = newObject.getRotation(time);
			WWVector newVelocity = newObject.getVelocity();
			WWVector newAMomentum = newObject.getAMomentum();
			oldObject.setThrust(newThrust);
			oldObject.setThrustVelocity(newThrustVelocity);
			oldObject.setTorque(newTorque);
			oldObject.setTorqueVelocity(newTorqueVelocity);
			oldObject.setOrientation(newPosition, newRotation, newVelocity, newAMomentum, time);
			// }
		}
	}

	/**
	 * Thrust an object. The newObject describes the new thrust, thrust velocity, torque and torque velocity.
	 */
	public void thrustObject(int id, WWObject newObject) {
		WWObject oldObject = getObject(id);
		if (oldObject != null) {
			// synchronized (oldObject) {
			long time = newObject.getLastMoveTime();
			WWVector newThrust = newObject.getThrust();
			WWVector newThrustVelocity = newObject.getThrustVelocity();
			WWVector newTorque = newObject.getTorque();
			WWVector newTorqueVelocity = newObject.getTorqueVelocity();
			WWVector newPosition = oldObject.getPosition(time);
			WWVector newRotation = oldObject.getRotation(time);
			WWVector newVelocity = oldObject.getVelocity();
			WWVector newAMomentum = oldObject.getAMomentum();
			oldObject.setThrust(newThrust);
			oldObject.setThrustVelocity(newThrustVelocity);
			oldObject.setTorque(newTorque);
			oldObject.setTorqueVelocity(newTorqueVelocity);
			oldObject.setOrientation(newPosition, newRotation, newVelocity, newAMomentum, time);
			// }
		}
	}

	/**
	 * Clears the world model, including destroying the rendering, preparing it for reuse, such as for connecting to a different server.
	 */
	public void clear() {
		// Destroy renderings of all objects
		for (int i = 0; i < objects.length; i++) {
			if (objects[i] != null) {
				objects[i].dropRendering();
			}
		}

		// Clear object and user arrays
		objects = new WWObject[MAX_OBJECTS];
		users = new WWUser[MAX_USERS];
	}

	@Override
	public void send(DataOutputStreamX os) throws IOException {
		os.writeFloat(gravity);
		os.writeFloat(fogDensity);
		os.writeFloat(ambientLightIntensity);
		os.writeInt(skyColor);
		os.writeFloat(sunIntensity);
		os.writeFloat(underglowIntensity);
		os.writeFloat(sunDirectionX);
		os.writeFloat(sunDirectionY);
		os.writeFloat(sunDirectionZ);
		os.writeInt(sunColor);
		os.writeFloat(renderingThreshold);
		super.send(os);
	}

	@Override
	public void receive(DataInputStreamX is) throws IOException {
		gravity = is.readFloat();
		fogDensity = is.readFloat();
		ambientLightIntensity = is.readFloat();
		skyColor = is.readInt();
		sunIntensity = is.readFloat();
		underglowIntensity = is.readFloat();
		sunDirectionX = is.readFloat();
		sunDirectionY = is.readFloat();
		sunDirectionZ = is.readFloat();
		sunColor = is.readInt();
		renderingThreshold = is.readFloat();
		super.receive(is);
	}

	public void preloadTexture(String textureName) {
		if (preloadedTextures == null) {
			preloadedTextures = new ArrayList<String>();
		}
		preloadedTextures.add(textureName);
	}
	
	public void preloadSound(String soundName) {
		if (preloadedSounds == null) {
			preloadedSounds = new ArrayList<String>();
		}
		preloadedSounds.add(soundName);
	}

	@Override
	public void createRendering(IRenderer renderer, long worldTime) {
		rendering = renderer.createWorldRendering(this, worldTime);
		if (playingSongname != null) {
			if (rendering != null && rendering.getRenderer() != null && rendering.getRenderer().getSoundGenerator() != null) {
				System.out.println("Resuming playing of song " + playingSongname);
				rendering.getRenderer().getSoundGenerator().playSong(playingSongname, playingSongVolume);
			} else {
				System.out.println("Could not resume playing of song " + playingSongname + " because renderer not available.");
			}
		}
		if (preloadedTextures != null) {
			for (int i = 0; i < preloadedTextures.size(); i++) {
				((AndroidRenderer) renderer).getTexture(preloadedTextures.get(i), false);
			}
		}
		if (preloadedSounds != null) {
			for (int i = 0; i < preloadedSounds.size(); i++) {
				((AndroidRenderer)renderer).getSoundGenerator().loadSound(preloadedSounds.get(i));
			}
		}
	}

	@Override
	public IRendering getRendering() {
		return rendering;
	}

	@Override
	public void updateRendering() {
		if (rendering != null) {
			rendering.updateRendering();
		}
	}

	@Override
	public void dropRendering() {
		if (rendering != null) {
			// rendering.destroy();
			rendering = null;
		}
	}

	public float getGravity() {
		return gravity;
	}

	public void setGravity(float gravity) {
		this.gravity = gravity;
	}

	/**
	 * Override this method to provide more exotic gravitational fields!
	 */
	public WWVector getGravityForce(float x, float y, float z) {
		return new WWVector(0, 0, -gravity);
	}

	public WWColor getSunColor() {
		return new WWColor(sunColor);
	}

	public void setSunColor(WWColor sunColor) {
		this.sunColor = sunColor.getRGB();
	}

	public float getFogDensity() {
		return fogDensity;
	}

	public void setFogDensity(float fogDensity) {
		this.fogDensity = fogDensity;
	}

	/**
	 * Overridden to not clone the user and objects lists.
	 */
	@Override
	public Object clone() {
		WWWorld clone = (WWWorld) super.clone();
		clone.users = null;
		clone.objects = null;
		return clone;
	}

	public long getLastRenderingTime() {
		return lastRenderingTime;
	}

	public void setLastRenderingTime(long time) {
		lastRenderingTime = time;
	}

	public float getAmbientLightIntensity() {
		return ambientLightIntensity;
	}

	public void setAmbientLightIntensity(float ambientLightIntensity) {
		this.ambientLightIntensity = ambientLightIntensity;
	}

	public WWColor getSkyColor() {
		return new WWColor(skyColor);
	}

	public void setSkyColor(WWColor skyColor) {
		this.skyColor = skyColor.getRGB();
	}

	public float getSunIntensity() {
		return sunIntensity;
	}

	public void setSunIntensity(float sunIntensity) {
		this.sunIntensity = sunIntensity;
	}

	public float getUnderglowIntensity() {
		return underglowIntensity;
	}

	public void setUnderglowIntensity(float underglowIntensity) {
		this.underglowIntensity = underglowIntensity;
	}

	public float getSunDirectionX() {
		return sunDirectionX;
	}

	public void setSunDirectionX(float sunDirectionX) {
		this.sunDirectionX = sunDirectionX;
	}

	public float getSunDirectionY() {
		return sunDirectionY;
	}

	public void setSunDirectionY(float sunDirectionY) {
		this.sunDirectionY = sunDirectionY;
	}

	public float getSunDirectionZ() {
		return sunDirectionZ;
	}

	public void setSunDirectionZ(float sunDirectionZ) {
		this.sunDirectionZ = sunDirectionZ;
	}

	public WWVector getSunDirection() {
		return new WWVector(sunDirectionX, sunDirectionY, sunDirectionZ);
	}

	public void setSunDirection(WWVector direction) {
		sunDirectionX = direction.getX();
		sunDirectionY = direction.getY();
		sunDirectionZ = direction.getZ();
	}

	public void setRendered(boolean rendered) {
		this.rendered = rendered;

	}

	public boolean getRendered() {
		return rendered;
	}

	class TouchParams {
		public WWObject object;
		public int surface;
		public float x;
		public float y;
	}

	public void pressObject(WWObject object, int surface, float x, float y, WWUser user) {
		TouchParams params = new TouchParams();
		params.object = object;
		params.surface = surface;
		params.x = x;
		params.y = y;
		if (behaviorThread != null) {
			behaviorThread.queue("press", object, user, params);
		} else {
			object.invokeBehavior("press", user, params);
		}
	}

	public void dragObject(WWObject object, int surface, float x, float y, WWUser user) {
		TouchParams params = new TouchParams();
		params.object = object;
		params.surface = surface;
		params.x = x;
		params.y = y;
		if (behaviorThread != null) {
			behaviorThread.queue("drag", object, user, params);
		} else {
			object.invokeBehavior("drag", user, params);
		}
	}

	public void releaseObject(WWObject object, int surface, float x, float y, WWUser user) {
		TouchParams params = new TouchParams();
		params.object = object;
		params.surface = surface;
		params.x = x;
		params.y = y;
		if (behaviorThread != null) {
			behaviorThread.queue("release", object, user, params);
		} else {
			object.invokeBehavior("release", user, params);
		}
	}

	public void touchObject(WWObject object, int surface, float x, float y, WWUser user) {
		TouchParams params = new TouchParams();
		params.object = object;
		params.surface = surface;
		params.x = x;
		params.y = y;
		if (behaviorThread != null) {
			behaviorThread.queue("touch", object, user, params);
		} else {
			object.invokeBehavior("touch", user, params);
		}
	}

	/**
	 * Called when a swipe that causes an object move completes
	 * 
	 * @param object
	 */
	public void doneMovingObject(WWObject object) {

	}

	/**
	 * Called when two objects first hit each other
	 * 
	 * @param object
	 * @param collidingObject
	 * @param overlapVector
	 */
	public void collideObject(ObjectCollision collision) {
		if (behaviorThread != null) {
			behaviorThread.queue("collide", collision.firstObject, collision.secondObject, collision.overlapVector);
			behaviorThread.queue("collide", collision.secondObject, collision.firstObject, collision.overlapVector);
		} else {
			collision.firstObject.invokeBehavior("collide", collision.secondObject, collision.overlapVector);
			collision.secondObject.invokeBehavior("collide", collision.firstObject, collision.overlapVector);
		}
		long time = getWorldTime();
		if (collision.firstObject.getImpactSound() != null) {
			WWVector position;
			if (collision.firstObject.extent > collision.secondObject.extent) {
				position = collision.secondObject.getPosition(time);
			} else {
				position = collision.firstObject.getPosition(time);
			}
			WWVector relativeVelocity = collision.firstObject.getVelocity();
			WWVector secondVelocity = collision.secondObject.getVelocity();
			relativeVelocity.subtract(secondVelocity);
			WWVector collisionVector = collision.overlapVector.clone().normalize();
			relativeVelocity.scale(collisionVector);
			float volume = FastMath.min(1.0f, relativeVelocity.length() * 0.025f);
			if (volume > 0.1f && rendering != null) {
				rendering.getRenderer().getSoundGenerator().playSound(collision.firstObject.getImpactSound(), 1, position, volume, 1.0f);
			}
		}
		if (collision.secondObject.getImpactSound() != null) {
			WWVector position;
			if (collision.firstObject.extent > collision.secondObject.extent) {
				position = collision.secondObject.getPosition(time);
			} else {
				position = collision.firstObject.getPosition(time);
			}
			WWVector relativeVelocity = collision.firstObject.getVelocity();
			WWVector secondVelocity = collision.secondObject.getVelocity();
			relativeVelocity.subtract(secondVelocity);
			WWVector collisionVector = collision.overlapVector.clone().normalize();
			relativeVelocity.scale(collisionVector);
			float volume = FastMath.min(1.0f, relativeVelocity.length() * 0.025f);
			if (volume > 0.1f && rendering != null) {
				rendering.getRenderer().getSoundGenerator().playSound(collision.secondObject.getImpactSound(), 1, position, volume, 1.0f);
			}
		}
	}

	public void slideObject(ObjectCollision collision) {
		if (behaviorThread != null) {
			behaviorThread.queue("slide", collision.firstObject, collision.secondObject, collision.overlapVector);
			behaviorThread.queue("slide", collision.secondObject, collision.firstObject, collision.overlapVector);
		} else {
			collision.firstObject.invokeBehavior("slide", collision.secondObject, collision.overlapVector);
			collision.secondObject.invokeBehavior("slide", collision.firstObject, collision.overlapVector);
		}
		long time = getWorldTime();
		if (collision.firstObject.getSlidingSound() != null) {
			WWVector position;
			if (collision.firstObject.extent > collision.secondObject.extent) {
				position = collision.secondObject.getPosition(time);
			} else {
				position = collision.firstObject.getPosition(time);
			}
			WWVector relativeVelocity = collision.firstObject.getVelocity();
			WWVector secondVelocity = collision.secondObject.getVelocity();
			relativeVelocity.subtract(secondVelocity);
			if (collision.firstObject.solid) {
				WWVector perpendicular = collision.firstObject.getPosition(time);
				WWVector secondPosition = collision.secondObject.getPosition(time);
				perpendicular.subtract(secondPosition);
				perpendicular.normalize();
				relativeVelocity.cross(perpendicular);
			}
			float volume = FastMath.min(1.0f, relativeVelocity.length() * 0.1f);
			if (rendering != null) {
				if (collision.firstSlidingStreamId > 0) {
					collision.firstSlidingStreamId = rendering.getRenderer().getSoundGenerator().adjustPlayingSound(collision.firstSlidingStreamId, position, volume, 1);
				} else {
					collision.firstSlidingStreamId = rendering.getRenderer().getSoundGenerator().startPlayingSound(collision.firstObject.getSlidingSound(), 0, position, 0, 1); // no volume for first collision
				}
			}
		}
		if (collision.secondObject.getSlidingSound() != null) {
			WWVector position;
			if (collision.firstObject.extent > collision.secondObject.extent) {
				position = collision.secondObject.getPosition(time);
			} else {
				position = collision.firstObject.getPosition(time);
			}
			WWVector relativeVelocity = collision.firstObject.getVelocity();
			WWVector secondVelocity = collision.secondObject.getVelocity();
			relativeVelocity.subtract(secondVelocity);
			if (collision.secondObject.solid) {
				WWVector perpendicular = collision.firstObject.getPosition(time);
				WWVector secondPosition = collision.secondObject.getPosition(time);
				perpendicular.subtract(secondPosition);
				perpendicular.normalize();
				relativeVelocity.cross(perpendicular);
			}
			float volume = FastMath.min(1.0f, relativeVelocity.length() * 0.1f);
			if (rendering != null) {
				if (collision.secondSlidingStreamId > 0) {
					collision.secondSlidingStreamId = rendering.getRenderer().getSoundGenerator().adjustPlayingSound(collision.secondSlidingStreamId, position, volume, 1);
				} else {
					collision.secondSlidingStreamId = rendering.getRenderer().getSoundGenerator().startPlayingSound(collision.secondObject.getSlidingSound(), 0, position, 0, 1); // no volume for first collision
				}
			}
		}
	}

	public void stopSlidingObject(ObjectCollision collision) {
		if (behaviorThread != null) {
			behaviorThread.queue("stopSlide", collision.firstObject, collision.secondObject, null);
			behaviorThread.queue("stopSlide", collision.secondObject, collision.firstObject, null);
		} else {
			collision.firstObject.invokeBehavior("stopSlide", collision.secondObject, null);
			collision.secondObject.invokeBehavior("stopSlide", collision.firstObject, null);
		}
		if (collision.firstSlidingStreamId > 0 && rendering != null) {
			rendering.getRenderer().getSoundGenerator().stopPlayingSound(collision.firstSlidingStreamId);
		}
		if (collision.secondSlidingStreamId > 0 && rendering != null) {
			rendering.getRenderer().getSoundGenerator().stopPlayingSound(collision.secondSlidingStreamId);
		}
	}

	/** Sends a message to users with avatars "close" to the sending user's avatar. */
	public void sendMessage(WWUser sendingUser, WWObject sendingAvatar, String message, double distance) {
		long time = getWorldTime();
		if (sendingAvatar != null) {
			WWUser[] users = getUsers();
			for (int i = 0; i < users.length; i++) {
				WWUser receivingUser = users[i];
				if (receivingUser != null && !receivingUser.equals(sendingUser) && receivingUser.isConnected()) {
					WWObject receivingAvatar = getObject(receivingUser.getAvatarId());
					if (receivingAvatar != null & receivingAvatar.getPosition(time).distanceFrom(sendingAvatar.getPosition(time)) < distance) {
						if (sendingUser != null) {
							receivingUser.queueMessage(sendingUser.getName(), message);
						} else {
							receivingUser.queueMessage(sendingAvatar.getName(), message);
						}
					}
				}
			}
		}
	}
	
	public void playSound(String soundName, float volume) {
		if (getRendering() != null) {
			rendering.getRenderer().getSoundGenerator().playSound(soundName, 1, null, volume, 1.0f);
		}
	}

	public void playSound(String soundName, float volume, float pitch) {
		if (getRendering() != null) {
			rendering.getRenderer().getSoundGenerator().playSound(soundName, 1, null, volume, pitch);
		}
	}

	public int startSound(String soundName, float volume, float pitch) {
		if (getRendering() != null) {
			return rendering.getRenderer().getSoundGenerator().startPlayingSound(soundName, 1, null, volume, pitch);
		}
		return 0;
	}

	public int adjustSound(int soundId, float volume, float pitch) {
		if (getRendering() != null) {
			return rendering.getRenderer().getSoundGenerator().adjustPlayingSound(soundId, null, volume, pitch);
		}
		return 0;
	}

	public void stopSound(int soundId) {
		if (getRendering() != null) {
			rendering.getRenderer().getSoundGenerator().stopPlayingSound(soundId);
		}
	}

	public void adjustSound(String soundName, float volume, float pitch) {
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		run();
	}

	transient public boolean running;

	public boolean isRunning() {
		return running;
	}

	public void pause() {
		System.out.println(">WWWorld.pause");
		if (running) {
			stopAllSounds();
			if (physicsThread != null) {
				physicsThread.safeStop = true;
				physicsThread = null;
			}
			if (behaviorThread != null) {
				behaviorThread.safeStop = true;
				behaviorThread = null;
			}
			if (saveWorldThread != null) {
				saveWorldThread.safeStop = true;
				saveWorldThread = null;
			}
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
			}
			running = false;
		}
		System.out.println("<WWWorld.pause");
	}

	public void run() {
		System.out.println(">WWWorld.run");
		if (!running) {
			if (createPhysicsThread && physicsThread == null) {
				physicsThread = makePhysicsThread();
				physicsThread.start();
			}
			if (createBehaviorThread && behaviorThread == null) {
				behaviorThread = new BehaviorThread(this);
				behaviorThread.start();
			}
			if (persistent && saveWorldFileName != null && saveWorldThread == null) {
				saveWorldThread = new SaveWorldThread(world, saveWorldFileName);
				saveWorldThread.start();
			}
			restartSounds();
			running = true;
		}
		System.out.println("<WWWorld.run");
	}

	public void performPhysicsIteration(long timeIncrement) {
		if (physicsThread != null) {
			physicsThread.performIteration(timeIncrement);
		}
	}

	/**
	 * @deprecated Use get/setActions
	 */
	public WWAction[] getWorldActions() {
		if (actions != null) {
			return actions;
		}
		return new WWAction[0];
	}

	public WWAction[] getActions() {
		return actions;
	}

	public void setActions(WWAction[] actions) {
		this.actions = actions;
	}

	public void startWorldAction(int i, float x, float y) {
		if (behaviorThread != null) {
			ActionParams params = new ActionParams();
			params.i = i;
			params.x = x;
			params.y = y;
			WWUser user = getUsers()[AndroidClientModel.getClientModel().getUserId()];
			behaviorThread.queue("startWorldAction", null, user, params);
		}
	}

	public void stopWorldAction(int i) {
		if (behaviorThread != null) {
			ActionParams params = new ActionParams();
			params.i = i;
			WWUser user = getUsers()[AndroidClientModel.getClientModel().getUserId()];
			behaviorThread.queue("stopWorldAction", null, user, params);
		}
	}

	/**
	 * @deprecated Use WWObject.getActions
	 */
	public WWAction[] getAvatarActions() {
		if (AndroidClientModel.getClientModel().getSelectedObject() != null) {
			if (AndroidClientModel.getClientModel().getSelectedObject().getActions() != null) {
				return AndroidClientModel.getClientModel().getSelectedObject().getActions();
			}
		}
		if (AndroidClientModel.getClientModel().getAvatar() != null) {
			return AndroidClientModel.getClientModel().getAvatar().getActions();
		}
		return new WWAction[0];
	}

	public void startAvatarAction(int i, float x, float y) {
		if (behaviorThread != null) {
			ActionParams params = new ActionParams();
			params.i = i;
			params.x = x;
			params.y = y;
			WWObject object = AndroidClientModel.getClientModel().getSelectedObject();
			if (object == null) {
				object = AndroidClientModel.getClientModel().getAvatar();
			}
			WWUser user = getUsers()[AndroidClientModel.getClientModel().getUserId()];
			behaviorThread.queue("startAvatarAction", null /* object */, user, params);
			// Note: currently behavior thread expects null for object of action
		}
	}

	public void stopAvatarAction(int i) {
		if (behaviorThread != null) {
			ActionParams params = new ActionParams();
			params.i = i;
			WWObject object = AndroidClientModel.getClientModel().getSelectedObject();
			if (object == null) {
				object = AndroidClientModel.getClientModel().getAvatar();
			}
			WWUser user = getUsers()[AndroidClientModel.getClientModel().getUserId()];
			behaviorThread.queue("stopAvatarAction", null /* object */, user, params);
			// Note: currently behavior thread expects null for object of action
		}
	}

	public class ActionParams {
		int i;
		float x;
		float y;
	}

	/**
	 * Invoked by BehaviorThread to launch world actions
	 */
	public final void invokeAction(String command, WWEntity agent, ActionParams params) {
		try {
			if (command.equals("startWorldAction")) {
				getWorldActions()[params.i].start(params.x, params.y);
			} else if (command.equals("stopWorldAction")) {
				getWorldActions()[params.i].stop();
			} else if (command.equals("startAvatarAction")) {
				getAvatarActions()[params.i].start(params.x, params.y);
			} else if (command.equals("stopAvatarAction")) {
				getAvatarActions()[params.i].stop();
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public float[] getMoveXTurn() {
		return new float[] { -90, -60, -30, 0, 30, 60, 90 };
	}

	public float[] getMoveXLean() {
		return new float[] { -15, -5, 0, 5, 15 };
	}

	public float[] getMoveXSlide() {
		return new float[] { -8, -4, -2, 0, 2, 4, 8 };
	}

	public float[] getMoveYTilt() {
		return new float[] { 30, 10, 0, -5, -10 };
	}

	public float[] getMoveYThrust() {
		return new float[] { -4, -2, 0, 4, 8 };
	}

	public float[] getMoveYLift() {
		return new float[] { -8, -4, -2, 0, 2, 4, 8 };
	}

	public static final int MOVE_TYPE_NONE = 0;
	public static final int MOVE_TYPE_TURN = 1;
	public static final int MOVE_TYPE_THRUST = 2;
	public static final int MOVE_TYPE_LEAN = 3;
	public static final int MOVE_TYPE_TILT = 4;
	public static final int MOVE_TYPE_THRUSTX = 5;
	public static final int MOVE_TYPE_LIFT = 6;
	public static final int MOVE_TYPE_SLIDE = 7;

	public int getMoveXType() {
		return MOVE_TYPE_TURN;
	}

	public int getMoveYType() {
		return MOVE_TYPE_THRUST;
	}

	/**
	 * If true, the controller (either the joystick/button or accelerometer) is needed.
	 * 
	 * @return
	 */
	public boolean usesController() {
		return true;
	}

	/**
	 * If true, the accelerometer is the prefered way to control. This can be true even if the usesController returns false, in which case if the user doesn't have an accelerometer or chooses not to use it, no alternative controller will be
	 * provided for the equivalent behavior.
	 * 
	 * @return
	 */
	public boolean usesAccelerometer() {
		return false;
	}

	public int getSensorXType() {
		return MOVE_TYPE_NONE;
	}

	public int getSensorYType() {
		return MOVE_TYPE_NONE;
	}

	public final int getLevel() {
		return level;
	}

	public final void setLevel(int level) {
		this.level = level;
		this.setLastModifyTime(getWorldTime());
	}

	public final int getScore() {
		return score;
	}

	public final void setScore(int score) {
		this.score = score;
		this.setLastModifyTime(getWorldTime());
	}

	public final String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
		this.setLastModifyTime(getWorldTime());
	}

	public float getRenderingThreshold() {
		return renderingThreshold;
	}

	public void setRenderingThreshold(float threshold) {
		renderingThreshold = threshold;
	}

	public void stopAllSounds() {
		if (this.rendering != null && this.rendering.getRenderer() != null && this.rendering.getRenderer().getSoundGenerator() != null) {
			for (int i = 0; i < objects.length; i++) {
				WWObject object = objects[i];
				if (object != null && object.soundStreamId != 0) {
					object.setSound(object.getSound(), 0, 0);
					this.rendering.getRenderer().getSoundGenerator().stopPlayingSound(object.soundStreamId);
					object.soundStreamId = 0;
				}
			}
			this.rendering.getRenderer().getSoundGenerator().reset();
		}
		if (playingSongname != null) {
			String t = playingSongname;
			stopPlayingSong();
			playingSongname = t;
		}
	}

	public void restartSounds() {
		System.out.println(">WWWorld.restartSounds");
		for (int i = 0; i < objects.length; i++) {
			WWObject object = objects[i];
			if (object != null) {
				object.updateSound();
			}
		}
		if (playingSongname != null) {
			if (rendering != null && rendering.getRenderer() != null && rendering.getRenderer().getSoundGenerator() != null) {
				System.out.println("Resuming playing of song " + playingSongname);
				rendering.getRenderer().getSoundGenerator().playSong(playingSongname, playingSongVolume);
			} else {
				System.out.println("Could not resume playing of song " + playingSongname + " because renderer not available.");
			}
		}
		System.out.println("<WWWorld.restartSounds");
	}

	public PhysicsThread makePhysicsThread() {
		return new NewPhysicsThread(this, iterationTime, 2);
	}

	/**
	 * If true, the camera movements will be dampened to give the user a feeling for the movement of the avatar. For fast moving avatars this should be false to prevent "jerking" of the camera.
	 * 
	 * @return
	 */
	public boolean dampenCamera() {
		return true;
	}

	/**
	 * Override to take action on the controller. This is both sensor and joy button controllers.
	 */
	public boolean controller(float deltaX, float deltaY) {
		return false;
	}

	/**
	 * Override to take action on the sensor controller.
	 */
	public boolean sensorController(float deltaX, float deltaY) {
		return false;
	}

	String playingSongname;
	float playingSongVolume;

	public void playSong(String songname, float volume) {
		if (rendering != null && rendering.getRenderer() != null && rendering.getRenderer().getSoundGenerator() != null) {
			rendering.getRenderer().getSoundGenerator().playSong(songname, volume);
		}
		playingSongname = songname;
		playingSongVolume = volume;
		System.out.println("Playing song " + songname);
	}

	public void stopPlayingSong() {
		if (rendering != null && rendering.getRenderer() != null && rendering.getRenderer().getSoundGenerator() != null) {
			rendering.getRenderer().getSoundGenerator().stopPlayingSong();
		}
		System.out.println("Stopped playing song " + playingSongname);
		playingSongname = null;
	}

	/**
	 * If true, the client will allow the user to change the camera position.
	 * 
	 * @return
	 */
	public boolean allowCameraPositioning() {
		return true;
	}

	public boolean allowPicking() {
		return false;
	}

	/**
	 * If true, the client will allow the user to move objects around by dragging.
	 * 
	 * @return
	 */
	public boolean allowObjectMoving() {
		return false;
	}

	/**
	 * Point on screen touched. This is used in BuildIt as the actual point on the surface of textures isn't yet working.
	 * 
	 * @param x
	 * @param y
	 */
	public void touch(float x, float y) {

	}

	public int getNextGroup() {
		nextGroup += 1;
		return nextGroup;
	}

	/**
	 * Implemented by worlds in order to save state.
	 */
	public void save() {
	}

	/**
	 * Implemented by worlds that provide editing to indicate that the world needs saving on quit.
	 * 
	 * @return
	 */
	public boolean needsSaving() {
		return false;
	}

	/**
	 * Indicates that after a save a quit can immediately occur. Default returns true. Return false if the save requires other prompting.
	 * 
	 * @return
	 */
	public boolean supportsSaveAndQuit() {
		return true;
	}

	/**
	 * Implemented so that worlds can take client-specific actions. Note that these actions should be phantom in nature because they will not be reflected in the shared server world.
	 */
	@Override
	public void clientModelChanged(ClientModelChangedEvent event) {
	}

	/**
	 * Override to return true if it is known that the entire drawing area is always filled with objects (so no background ever shows).
	 */
	public boolean drawsFully() {
		return false;
	}

	/**
	 * Override to return false if the world is not yet ready for OpenGL ES20.
	 */
	public boolean supportsOpenGLES20() {
		return true;
	}

	public void showBannerAds() {
		AndroidClientModel.getClientModel().showBannerAds();
	}

	public void hideBannerAds() {
		AndroidClientModel.getClientModel().hideBannerAds();
	}

	private boolean determinedShadowSupport;
	private boolean shadowSupport;

	/**
	 * Override to return false if the world does not want/like shadows. Default chooses shadows if the hardware can support them (a deep enough depth buffer)
	 * 
	 * @return
	 */
	public boolean supportsShadows() {
		if (!determinedShadowSupport) {
			IntBuffer depthBufferBits = IntBuffer.allocate(1);
			GLES20.glGetIntegerv(GLES20.GL_DEPTH_BITS, depthBufferBits);
			System.out.println("DEPTH BUFFER BITS = " + depthBufferBits.get(0));
			shadowSupport = depthBufferBits.get(0) >= 16;
			determinedShadowSupport = true;
		}
		return shadowSupport;
	}

	public void setPersistent(boolean save) {
		persistent = save;
	}

	public boolean isPersistent() {
		return persistent;
	}
	
	public boolean isConfrontMode() {
		return confrontMode;
	}
	
	public void setConfrontMode(boolean confrontMode) {
		this.confrontMode = confrontMode;
	}
}
