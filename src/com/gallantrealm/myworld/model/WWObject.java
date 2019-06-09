package com.gallantrealm.myworld.model;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import com.gallantrealm.myworld.FastMath;
import com.gallantrealm.myworld.client.renderer.IRenderable;
import com.gallantrealm.myworld.client.renderer.IRendering;
import com.gallantrealm.myworld.client.renderer.IVideoTextureRenderer;
import com.gallantrealm.myworld.communication.DataInputStreamX;
import com.gallantrealm.myworld.communication.DataOutputStreamX;
import com.gallantrealm.myworld.communication.Sendable;

/**
 * This is the base class for all objects in the world. Note that not all fields of this class are shared with clients
 * -- only those that are needed for rendering the client. An editor for the object may expose other fields as well via
 * editing, and fields are available for object behaviors (which run on the server).
 */
public abstract class WWObject extends WWEntity implements IRenderable, Serializable, Cloneable, Sendable {
	static final long serialVersionUID = 1L;

	static final int[] DEFAULT_CHILDREN = new int[] {};

	public static final float TORADIAN = 0.0174532925f;
	public static final float TODEGREES = 57.29577866f;

	int id;

	// Positioning properties
	public long lastMoveTime;
	float positionX;
	float positionY;
	float positionZ;
	float rotationX; // degrees
	float rotationY; // degrees
	float rotationZ; // degrees

	// Grouping properties
	public int parentId;
	boolean gluedToParent = true;
	int[] childrenIds;

	// Physics properties
	public boolean physical;
	public boolean phantom;
	public float velocityX;
	public float velocityY;
	public float velocityZ;
	public float aMomentumX;
	public float aMomentumY;
	public float aMomentumZ;
	public float density = 0.0f;
	public boolean solid = true;
	public float elasticity;
	public float friction = 0.9f;
	public boolean freedomMoveX = true;
	public boolean freedomMoveY = true;
	public boolean freedomMoveZ = true;
	public boolean freedomRotateX = true;
	public boolean freedomRotateY = true;
	public boolean freedomRotateZ = true;
	public float thrustX;
	public float thrustY;
	public float thrustZ;
	public float thrustVelocityX;
	public float thrustVelocityY;
	public float thrustVelocityZ;
	public float torqueX;
	public float torqueY;
	public float torqueZ;
	public float torqueVelocityX;
	public float torqueVelocityY;
	public float torqueVelocityZ;

	public boolean pickable = false; // indicates that the object can be focused on
	public boolean penetratable = false; // indicates that camera is okay to penetrate into the object
	public boolean monolithic = false; // indicates that all surfaces of the object are similar textured, so all surfaces can be rendered together
	public boolean fixed; // indicates that the object never, ever moves
	public int group; // indicates this object is similar texture to other objects such that it can be rendered together

	public String sound;
	public float soundVolume;
	public float soundPitch;
	public transient int soundStreamId;
	public String impactSound;
	public String slidingSound;

	public float sizeX = 1.0f;
	public float sizeY = 1.0f;
	public float sizeZ = 1.0f;
	public float extent = 1.732f;
	public float extentx = 1.732f;
	public float extenty = 1.732f;
	public float extentz = 1.732f;
	public SideAttributes[] sideAttributes = new SideAttributes[NSIDES];

	// Behavior properties
	public BehaviorAttributes[] behaviors;

	// Transient (rendering related) values
	// Note: the Java3d rendering has a primitive within a transform group within a branch group.
	public transient IRendering rendering;
	public boolean renderit;
	public transient long lastRenderingTime;

	public boolean alwaysRender;
	
	public boolean shadowless;

	transient WWVector startPosition;
	transient WWVector startRotation;
	transient WWVector stopPosition;
	transient WWVector stopRotation;

	transient WWVector[] edgePoints;
	transient Object primitive;
	transient IVideoTextureRenderer videoTextureRenderer;

	//public transient boolean visible; // indicates that the object is visible to the viewer (local world only)

	public WWObject() {
		sideAttributes = new SideAttributes[NSIDES];
		for (int i = 0; i < NSIDES; i++) {
			sideAttributes[i] = SideAttributes.getDefaultSideAttributes();
		}
	}

	public WWObject(float sizeX, float sizeY, float sizeZ) {
		this();
		this.sizeX = sizeX;
		this.sizeY = sizeY;
		this.sizeZ = sizeZ;
		calculateExtents();
	}

	/**
	 * This method streams the information that needs to be sent for an update or the object's state on the client.
	 * <p>
	 * When overriding this method in subclasses, be sure to invoke the parent method LAST. Also, implement the receive
	 * method to match, or communication will be corrupted.
	 */
	@Override
	public void send(DataOutputStreamX os) throws IOException {
		os.writeInt(id);
		os.writeFloat(sizeX);
		os.writeFloat(sizeY);
		os.writeFloat(sizeZ);
		os.writeFloat(extent);
		os.writeFloat(extentx);
		os.writeFloat(extenty);
		os.writeFloat(extentz);
		os.writeKnownObjectArray(sideAttributes);
		os.writeInt(parentId);
		os.writeBoolean(gluedToParent);
		os.writeIntArray(childrenIds);
		os.writeBoolean(physical);
		os.writeBoolean(phantom);
		os.writeFloat(density);
		os.writeFloat(elasticity);
		os.writeFloat(friction);
		os.writeBoolean(solid);
		os.writeBoolean(freedomMoveX);
		os.writeBoolean(freedomMoveY);
		os.writeBoolean(freedomMoveZ);
		os.writeBoolean(freedomRotateX);
		os.writeBoolean(freedomRotateY);
		os.writeBoolean(freedomRotateZ);
		os.writeBoolean(pickable);
		os.writeBoolean(penetratable);
		os.writeBoolean(monolithic);
		os.writeBoolean(fixed);
		os.writeInt(group);
		os.writeString(sound);
		os.writeFloat(soundVolume);
		os.writeFloat(soundPitch);
		os.writeString(impactSound);
		os.writeString(slidingSound);
		os.writeBoolean(alwaysRender);
//		os.writeKnownObjectArray(behaviors);
		// TODO improve efficiency of behavior streaming
		sendPositionPrivate(os);
		super.send(os);
	}

	public void sendPosition(DataOutputStreamX os) throws IOException {
		sendPositionPrivate(os);
		super.send(os);
	}

	final void sendPositionPrivate(DataOutputStreamX os) throws IOException {
		os.writeFloat(positionX);
		os.writeFloat(positionY);
		os.writeFloat(positionZ);
		os.writeFloat(rotationX);
		os.writeFloat(rotationY);
		os.writeFloat(rotationZ);
		os.writeFloat(velocityX);
		os.writeFloat(velocityY);
		os.writeFloat(velocityZ);
		os.writeFloat(aMomentumX);
		os.writeFloat(aMomentumY);
		os.writeFloat(aMomentumZ);
		os.writeFloat(thrustX);
		os.writeFloat(thrustY);
		os.writeFloat(thrustZ);
		os.writeFloat(thrustVelocityX);
		os.writeFloat(thrustVelocityY);
		os.writeFloat(thrustVelocityZ);
		os.writeFloat(torqueX);
		os.writeFloat(torqueY);
		os.writeFloat(torqueZ);
		os.writeFloat(torqueVelocityX);
		os.writeFloat(torqueVelocityY);
		os.writeFloat(torqueVelocityZ);
		os.writeKnownObject(stopPosition);
		os.writeKnownObject(stopRotation);
		os.writeLong(lastMoveTime);
	}

	/**
	 * This method receives streamed information sent from a server for an update to the object.
	 * <p>
	 * When overriding this method in subclasses, be sure to invoke the parent method LAST.
	 * 
	 * @throws ClassNotFoundException
	 */
	@Override
	public void receive(DataInputStreamX is) throws IOException {
		id = is.readInt();
		sizeX = is.readFloat();
		sizeY = is.readFloat();
		sizeZ = is.readFloat();
		extent = is.readFloat();
		extentx = is.readFloat();
		extenty = is.readFloat();
		extentz = is.readFloat();
		sideAttributes = (SideAttributes[]) is.readKnownObjectArray(SideAttributes.class);
		parentId = is.readInt();
		gluedToParent = is.readBoolean();
		childrenIds = is.readIntArray();
		physical = is.readBoolean();
		phantom = is.readBoolean();
		density = is.readFloat();
		elasticity = is.readFloat();
		friction = is.readFloat();
		solid = is.readBoolean();
		freedomMoveX = is.readBoolean();
		freedomMoveY = is.readBoolean();
		freedomMoveZ = is.readBoolean();
		freedomRotateX = is.readBoolean();
		freedomRotateY = is.readBoolean();
		freedomRotateZ = is.readBoolean();
		pickable = is.readBoolean();
		penetratable = is.readBoolean();
		monolithic = is.readBoolean();
		fixed = is.readBoolean();
		group = is.readInt();
		sound = is.readString();
		soundVolume = is.readFloat();
		soundPitch = is.readFloat();
		impactSound = is.readString();
		slidingSound = is.readString();
		alwaysRender = is.readBoolean();
//		behaviors = (BehaviorAttributes[]) is.readKnownObjectArray(BehaviorAttributes.class);
//		if (behaviors != null) {
//			// Need to set owner in behavior objects, since they were (re)created on receiving
//			for (int i = 0; i < behaviors.length; i++) {
//				behaviors[i].behavior.owner = this;
//			}
//		}
		receivePositionPrivate(is);
		super.receive(is);
	}

	public void receivePosition(DataInputStreamX is) throws IOException, ClassNotFoundException {
		receivePositionPrivate(is);
		super.receive(is);
	}

	final void receivePositionPrivate(DataInputStreamX is) throws IOException {
		positionX = is.readFloat();
		positionY = is.readFloat();
		positionZ = is.readFloat();
		rotationX = is.readFloat();
		rotationY = is.readFloat();
		rotationZ = is.readFloat();
		velocityX = is.readFloat();
		velocityY = is.readFloat();
		velocityZ = is.readFloat();
		aMomentumX = is.readFloat();
		aMomentumY = is.readFloat();
		aMomentumZ = is.readFloat();
		thrustX = is.readFloat();
		thrustY = is.readFloat();
		thrustZ = is.readFloat();
		thrustVelocityX = is.readFloat();
		thrustVelocityY = is.readFloat();
		thrustVelocityZ = is.readFloat();
		torqueX = is.readFloat();
		torqueY = is.readFloat();
		torqueZ = is.readFloat();
		torqueVelocityX = is.readFloat();
		torqueVelocityY = is.readFloat();
		torqueVelocityZ = is.readFloat();
		stopPosition = (WWVector) is.readKnownObject(WWVector.class);
		stopRotation = (WWVector) is.readKnownObject(WWVector.class);
		lastMoveTime = is.readLong();
	}

	public final int getId() {
		return id;
	}

	/** Overridden to set world on behaviors (if any) */
	@Override
	public void setWorld(WWWorld world) {
		super.setWorld(world);
		if (behaviors != null) {
			for (int i = 0; i < behaviors.length; i++) {
				behaviors[i].behavior.owner = this;
			}
		}
	}

	public final long getLastMoveTime() {
		return lastMoveTime;
	}

	// TODO make this private or atleast protected (set internally)
	public void setLastMoveTime(long lastMoveTime) {
		this.lastMoveTime = lastMoveTime;
	}

	public final WWVector getRotation(long worldTime) {
		WWVector rotation = new WWVector();
		getRotation(rotation, worldTime);
		return rotation;
	}

	/**
	 * Returns the rotation including contribution from parents
	 */
	public final WWVector getAbsoluteRotation(long worldTime) {
		WWVector rotation = new WWVector();
		getRotation(rotation, worldTime);
		int p = parentId;
		if (p != 0) {
			WWObject parent = world.objects[p];
			WWVector parentRotation = parent.getAbsoluteRotation(worldTime);
			rotation.add(parentRotation);
		}
		return rotation;
	}

	/**
	 * Returns the animation rotation including contribution from parents
	 */
	public final WWVector getAbsoluteAnimatedRotation(long worldTime) {
		WWVector rotation = new WWVector();
		getAnimatedRotation(rotation, worldTime);
		int p = parentId;
		if (p != 0) {
			WWObject parent = world.objects[p];
			WWVector parentRotation = parent.getAbsoluteAnimatedRotation(worldTime);
			rotation.add(parentRotation);
		}
		return rotation;
	}

	public final WWVector getRotation() {
		return getRotation(getWorldTime());
	}

	public final void getRotation(WWVector rotation, long worldTime) {
		if (fixed) {
			rotation.x = rotationX;
			rotation.y = rotationY;
			rotation.z = rotationZ;
		} else {
			float deltaTime = (worldTime - lastMoveTime) / 1000.0f;
			rotation.x = rotationX + deltaTime * aMomentumX;
			rotation.y = rotationY + deltaTime * aMomentumY;
			rotation.z = rotationZ + deltaTime * aMomentumZ;
			// Apply start and stop rotation limits if any
			if (startRotation != null) {
				if (rotationX > startRotation.x && rotation.x < startRotation.x) {
					rotation.x = startRotation.x;
				} else if (rotationX < startRotation.x && rotation.x > startRotation.x) {
					rotation.x = startRotation.x;
				}
				if (rotationY > startRotation.y && rotation.y < startRotation.y) {
					rotation.y = startRotation.y;
				} else if (rotationY < startRotation.y && rotation.y > startRotation.y) {
					rotation.y = startRotation.y;
				}
				if (rotationZ > startRotation.z && rotation.z < startRotation.z) {
					rotation.z = startRotation.z;
				} else if (rotationZ < startRotation.z && rotation.z > startRotation.z) {
					rotation.z = startRotation.z;
				}
			}
			if (stopRotation != null) {
				if (rotationX > stopRotation.x && rotation.x < stopRotation.x) {
					rotation.x = stopRotation.x;
				} else if (rotationX < stopRotation.x && rotation.x > stopRotation.x) {
					rotation.x = stopRotation.x;
				}
				if (rotationY > stopRotation.y && rotation.y < stopRotation.y) {
					rotation.y = stopRotation.y;
				} else if (rotationY < stopRotation.y && rotation.y > stopRotation.y) {
					rotation.y = stopRotation.y;
				}
				if (rotationZ > stopRotation.z && rotation.z < stopRotation.z) {
					rotation.z = stopRotation.z;
				} else if (rotationZ < stopRotation.z && rotation.z > stopRotation.z) {
					rotation.z = stopRotation.z;
				}
			}
		}
	}

	public final void getAnimatedRotation(WWVector rotation, long worldTime) {
		getRotation(rotation, worldTime);
		processRotationAnimators(this, rotation, worldTime);
	}

	/** Invoke animators for rotation for this object and its parent(s). */
	final void processRotationAnimators(WWObject object, WWVector rotation, long worldTime) {
		if (behaviors != null) {
			for (int i = 0; i < behaviors.length; i++) {
				WWBehavior behavior = behaviors[i].behavior;
				if (behavior instanceof WWAnimation) {
					WWAnimation animation = (WWAnimation) behavior;
					animation.getAnimatedRotation(object, rotation, worldTime);
				}
			}
		}
		if (parentId != 0) {
			world.objects[parentId].processRotationAnimators(object, rotation, worldTime);
		}
	}

	public final boolean isAtRotationBounds() {
		long worldTime = getWorldTime();
		float deltaTime = (worldTime - lastMoveTime) / 1000.0f;
		float newRotationX = rotationX + deltaTime * aMomentumX;
		float newRotationY = rotationY + deltaTime * aMomentumY;
		float newRotationZ = rotationZ + deltaTime * aMomentumZ;
		if (startRotation != null) {
			if (rotationX > startRotation.x && newRotationX < startRotation.x) {
				return true;
			} else if (rotationX < startRotation.x && newRotationX > startRotation.x) {
				return true;
			}
			if (rotationY > startRotation.y && newRotationY < startRotation.y) {
				return true;
			} else if (rotationY < startRotation.y && newRotationY > startRotation.y) {
				return true;
			}
			if (rotationZ > startRotation.z && newRotationZ < startRotation.z) {
				return true;
			} else if (rotationZ < startRotation.z && newRotationZ > startRotation.z) {
				return true;
			}
		}
		if (stopRotation != null) {
			if (rotationX > stopRotation.x && newRotationX < stopRotation.x) {
				return true;
			} else if (rotationX < stopRotation.x && newRotationX > stopRotation.x) {
				return true;
			}
			if (rotationY > stopRotation.y && newRotationY < stopRotation.y) {
				return true;
			} else if (rotationY < stopRotation.y && newRotationY > stopRotation.y) {
				return true;
			}
			if (rotationZ > stopRotation.z && newRotationZ < stopRotation.z) {
				return true;
			} else if (rotationZ < stopRotation.z && newRotationZ > stopRotation.z) {
				return true;
			}
		}
		return false;
	}

	public final void setRotation(WWVector rotation) {
		setOrientation(getPosition(getWorldTime()), rotation, null, null, getWorldTime());
	}

	public final void setRotation(float x, float y, float z) {
		setOrientation(getPosition(getWorldTime()), new WWVector(x, y, z), null, null, getWorldTime());
	}
	
	public final void setRotation(float[] rots) {
		setRotation(rots[0], rots[1], rots[2]);
	}

	public final WWVector getAbsolutePosition(long worldTime) {
		WWVector position = new WWVector();
		getAbsolutePosition(position, worldTime);
		return position;
	}

	public final WWVector getPosition(long worldTime) {
		WWVector position = new WWVector();
		getPosition(position, worldTime);
		return position;
	}

	public final WWVector getPosition() {
		return getPosition(getWorldTime());
	}

	public final void getPosition(WWVector position, long worldTime) {
		if (fixed) {
			position.x = positionX;
			position.y = positionY;
			position.z = positionZ;
		} else {
			// Determine position based on delta time since it was set and the velocity and acceleration
			float deltaTime = (worldTime - lastMoveTime) / 1000.0f;
			position.x = positionX + deltaTime * velocityX;
			position.y = positionY + deltaTime * velocityY;
			position.z = positionZ + deltaTime * velocityZ;
			// Apply start and stop position limits if any
			if (startPosition != null) {
				if (positionX > startPosition.x && position.x < startPosition.x) {
					position.x = startPosition.x;
				} else if (positionX < startPosition.x && position.x > startPosition.x) {
					position.x = startPosition.x;
				}
				if (positionY > startPosition.y && position.y < startPosition.y) {
					position.y = startPosition.y;
				} else if (positionY < startPosition.y && position.y > startPosition.y) {
					position.y = startPosition.y;
				}
				if (positionZ > startPosition.z && position.z < startPosition.z) {
					position.z = startPosition.z;
				} else if (positionZ < startPosition.z && position.z > startPosition.z) {
					position.z = startPosition.z;
				}
			}
			if (stopPosition != null) {
				if (positionX > stopPosition.x && position.x < stopPosition.x) {
					position.x = stopPosition.x;
				} else if (positionX < stopPosition.x && position.x > stopPosition.x) {
					position.x = stopPosition.x;
				}
				if (positionY > stopPosition.y && position.y < stopPosition.y) {
					position.y = stopPosition.y;
				} else if (positionY < stopPosition.y && position.y > stopPosition.y) {
					position.y = stopPosition.y;
				}
				if (positionZ > stopPosition.z && position.z < stopPosition.z) {
					position.z = stopPosition.z;
				} else if (positionZ < stopPosition.z && position.z > stopPosition.z) {
					position.z = stopPosition.z;
				}
			}
		}
	}

	transient long lastGetAbsolutePositionTime = -1;
	transient float lastGetAbsolutePositionX, lastGetAbsolutePositionY, lastGetAbsolutePositionZ;

	/**
	 * Returns the position in real space, adding in parent position
	 * 
	 * @param position
	 * @param worldTime
	 */
	public final void getAbsolutePosition(WWVector position, long worldTime) {
		if (lastGetAbsolutePositionTime == worldTime) {
			position.x = lastGetAbsolutePositionX;
			position.y = lastGetAbsolutePositionY;
			position.z = lastGetAbsolutePositionZ;
		} else {
			getPosition(position, worldTime);
			// if this is a member of a collection, adjust position according to parent's position/rotation
			if (parentId != 0) {
				WWObject parent = world.objects[parentId];
				WWVector parentPosition = new WWVector();
				WWVector parentRotation = new WWVector();
				parent.getAbsolutePosition(parentPosition, worldTime);
				parent.getRotation(parentRotation, worldTime);
				//parent.antiTransform(position, parent.getPosition(lastMoveTime), parent.getRotation(lastMoveTime));
				parent.transform(position, parentPosition, parentRotation, worldTime);
			}
			lastGetAbsolutePositionX = position.x;
			lastGetAbsolutePositionY = position.y;
			lastGetAbsolutePositionZ = position.z;
			lastGetAbsolutePositionTime = worldTime;
		}
	}

	/**
	 * Returns the animated position in real space, adding in parent position
	 * 
	 * @param position
	 * @param worldTime
	 */
	public final void getAbsoluteAnimatedPosition(WWVector position, long worldTime) {
		getAnimatedPosition(position, worldTime);
		// if this is a member of a collection, adjust position according to parent's position/rotation
		if (parentId != 0) {
			WWObject parent = world.objects[parentId];
			WWVector parentPosition = new WWVector();
			WWVector parentRotation = new WWVector();
			parent.getAbsoluteAnimatedPosition(parentPosition, worldTime);
			parent.getAnimatedRotation(parentRotation, worldTime);
			//parent.antiTransform(position, parent.getPosition(lastMoveTime), parent.getRotation(lastMoveTime));
			parent.transform(position, parentPosition, parentRotation, worldTime);
		}
	}

	public final void getAnimatedPosition(WWVector position, long worldTime) {
		getPosition(position, worldTime);
		processPositionAnimators(this, position, worldTime);
	}

	/** Invoke animators for position for this object and its parent(s). */
	final void processPositionAnimators(WWObject object, WWVector position, long worldTime) {
		if (behaviors != null) {
			for (int i = 0; i < behaviors.length; i++) {
				WWBehavior behavior = behaviors[i].behavior;
				if (behavior instanceof WWAnimation) {
					WWAnimation animation = (WWAnimation) behavior;
					animation.getAnimatedPosition(object, position, worldTime);
				}
			}
		}
		if (parentId != 0) {
			world.objects[parentId].processPositionAnimators(object, position, worldTime);
		}
	}

	public final void setPosition(WWVector position) {
		setOrientation(position, getRotation(getWorldTime()), null, null, getWorldTime());
	}

	public final void setPosition(float x, float y, float z) {
		setOrientation(new WWVector(x, y, z), getRotation(getWorldTime()), null, null, getWorldTime());
	}
	
	public final void setPosition(float[] pos) {
		setPosition(pos[0], pos[1], pos[2]);
	}

	public final void setStartPosition(WWVector v) {
		startPosition = v.clone();
	}

	public final WWVector getStartPosition() {
		return startPosition.clone();
	}

	public final void setStartRotation(WWVector v) {
		startRotation = v.clone();
	}

	public final WWVector getStartRotation() {
		return startRotation.clone();
	}

	public final void setStopPosition(WWVector v) {
		stopPosition = v.clone();
	}

	public final WWVector getStopPosition() {
		return stopPosition.clone();
	}

	public final void setStopRotation(WWVector v) {
		stopRotation = v.clone();
	}

	public final WWVector getStopRotation() {
		return stopRotation.clone();
	}

	public final boolean isPhysical() {
		return physical;
	}

	/**
	 * A physical object is influenced by gravity and by the interaction of other objects. (An object does NOT need to
	 * be physical to influence other objects, however. By making an object physical, it will use time on the server and
	 * client to simulate the interactions with other objects.)
	 */
	public final void setPhysical(boolean physical) {
		this.physical = physical;
	}

	public final WWVector getVelocity() {
		return new WWVector(velocityX, velocityY, velocityZ);
	}

	public final void getVelocity(WWVector velocity) {
		velocity.x = velocityX;
		velocity.y = velocityY;
		velocity.z = velocityZ;
	}

	public final void setVelocity(WWVector velocity) {
		setOrientation(getPosition(getWorldTime()), getRotation(getWorldTime()), velocity, null, getWorldTime());
	}

	public final float getVelocityLength() {
		return (float) Math.sqrt(velocityX * velocityX + velocityY * velocityY + velocityZ * velocityZ);
	}

	public final WWVector getAMomentum() {
		return new WWVector(aMomentumX, aMomentumY, aMomentumZ);
	}

	public final void getAMomentum(WWVector aMomentum) {
		aMomentum.x = aMomentumX;
		aMomentum.y = aMomentumY;
		aMomentum.z = aMomentumZ;
	}

	public final void setAMomentum(WWVector aMomentum) {
		setOrientation(getPosition(getWorldTime()), getRotation(getWorldTime()), null, aMomentum, getWorldTime());
	}

	public final float getAMomentumLength() {
		return (float) Math.sqrt(aMomentumX * aMomentumX + aMomentumY * aMomentumY + aMomentumZ * aMomentumZ);
	}

	public final WWVector getThrust() {
		return new WWVector(thrustX, thrustY, thrustZ);
	}

	public final void setThrust(WWVector thrust) {
		thrustX = thrust.x;
		thrustY = thrust.y;
		thrustZ = thrust.z;
	}

	public final WWVector getThrustVelocity() {
		return new WWVector(thrustVelocityX, thrustVelocityY, thrustVelocityZ);
	}

	public final void setThrustVelocity(WWVector thrustVelocity) {
		thrustVelocityX = thrustVelocity.x;
		thrustVelocityY = thrustVelocity.y;
		thrustVelocityZ = thrustVelocity.z;
	}

	public final WWVector getTorque() {
		return new WWVector(torqueX, torqueY, torqueZ);
	}

	public final void setTorque(WWVector torque) {
		torqueX = torque.x;
		torqueY = torque.y;
		torqueZ = torque.z;
	}

	public final WWVector getTorqueVelocity() {
		return new WWVector(torqueVelocityX, torqueVelocityY, torqueVelocityZ);
	}

	public final void setTorqueVelocity(WWVector torqueVelocity) {
		torqueVelocityX = torqueVelocity.x;
		torqueVelocityY = torqueVelocity.y;
		torqueVelocityZ = torqueVelocity.z;
	}

	/**
	 * Use this method to set all position, rotation, and momentum values in one call. This avoid undo overhead from
	 * detection of updated objects. Note that individual setPosition, setRotation, setVelocity, and setAMomentum calls
	 * will internally call this method.
	 */
	public final void setOrientation(WWVector newPosition, WWVector newRotation, WWVector newVelocity, WWVector newAMomentum, long newMoveTime) {

		/*
		 * ignoring glue behavior from child to parent // If this is a child and the child is glued to the parent,
		 * orient the parent // instead. It will move all of its children accordingly. if (parentId != 0 &&
		 * gluedToParent && world != null) { WWVector deltaPosition = newPosition.clone();
		 * deltaPosition.subtract(getPosition(lastMoveTime)); WWVector deltaVelocity = newVelocity.clone();
		 * deltaVelocity.subtract(getVelocity()); WWVector deltaRotation = newRotation.clone();
		 * deltaRotation.subtract(getRotation(lastMoveTime)); WWVector deltaAMomentum = newAMomentum.clone();
		 * deltaAMomentum.subtract(getAMomentum()); WWObject parent = world.objects[parentId]; WWVector
		 * newParentPosition = parent.getPosition(lastMoveTime); newParentPosition.add(deltaPosition); WWVector
		 * newParentVelocity = parent.getVelocity(); // TODO rotate/position parent relative to child delta rotate
		 * parent.setOrientation(newParentPosition, newRotation, newParentVelocity, newAMomentum, newMoveTime); // TODO
		 * determine if some dynamic properties should be ignored for glued children return; }
		 */

//		WWVector[] savedChildPositions = null;
//		if (childrenIds != null && world != null) {
//
//			// Remember the position of the contained objects relative to the old
//			// parent's position. The child positions will be updated later after the parent
//			// has been moved
//			savedChildPositions = new WWVector[childrenIds.length];
//			for (int i = 0; i < childrenIds.length; i++) {
//				WWObject child = world.objects[childrenIds[i]];
//				if (child != null) {
//					// adjust the child position based on the rotation of the parent
//					WWVector childPosition = child.getPosition(lastMoveTime);
//					antiTransform(childPosition, getPosition(lastMoveTime), getRotation(lastMoveTime), lastMoveTime);
//					savedChildPositions[i] = childPosition;
//				}
//			}
//
//			// Rotate the contained objects
//			WWVector deltaRotation = newRotation.clone();
//			deltaRotation.subtract(getRotation(lastMoveTime));
//			for (int i = 0; i < childrenIds.length; i++) {
//				WWObject child = world.objects[childrenIds[i]];
//				if (child != null) {
//					// adjust the child rotation to match the amount that the parent will rotate
//					WWVector newChildRotation = child.getRotation(lastMoveTime);
//					newChildRotation.add(deltaRotation);
//					child.rotationX = newChildRotation.x;
//					child.rotationY = newChildRotation.y;
//					child.rotationZ = newChildRotation.z;
//					child.lastMoveTime = newMoveTime;
//				}
//			}
//
//		}

		this.positionX = newPosition.x;
		this.positionY = newPosition.y;
		this.positionZ = newPosition.z;
		this.lastGetAbsolutePositionTime = -1;
		this.rotationX = newRotation.x;
		this.rotationY = newRotation.y;
		this.rotationZ = newRotation.z;
		if (newVelocity != null) {
			this.velocityX = newVelocity.x;
			this.velocityY = newVelocity.y;
			this.velocityZ = newVelocity.z;
		}
		if (newAMomentum != null) {
			this.aMomentumX = newAMomentum.x;
			this.aMomentumY = newAMomentum.y;
			this.aMomentumZ = newAMomentum.z;
		}
		this.lastMoveTime = newMoveTime;

//		if (childrenIds != null && world != null) {
//			// Update the position of the contained objects relative to the new
//			// parent's position.
//			for (int i = 0; i < childrenIds.length; i++) {
//				WWObject child = world.objects[childrenIds[i]];
//				if (child != null) {
//					// adjust the child position based on the rotation of the parent
//					WWVector childPosition = savedChildPositions[i];
//					transform(childPosition, getPosition(newMoveTime), getRotation(newMoveTime), newMoveTime);
//					child.positionX = childPosition.x;
//					child.positionY = childPosition.y;
//					child.positionZ = childPosition.z;
//					child.lastMoveTime = newMoveTime;
//				}
//			}
//		}

		// Since extents for fixed objects are tuned to current rotation, recalculate them
		if (fixed && !phantom) {
			calculateExtents();
		}
	}

	public final float getDensity() {
		return density;
	}

	public final void setDensity(float density) {
		this.density = density;
	}

	public final float getElasticity() {
		return elasticity;
	}

	public final void setElasticity(float elasticity) {
		this.elasticity = elasticity;
	}

	public final float getFriction() {
		return friction;
	}

	public final void setFriction(float friction) {
		this.friction = friction;
	}

	public final boolean isSolid() {
		return solid;
	}

	public final void setSolid(boolean solid) {
		this.solid = solid;
	}

	public final boolean isFreedomMoveX() {
		return freedomMoveX;
	}

	public final void setFreedomMoveX(boolean freedomMoveX) {
		this.freedomMoveX = freedomMoveX;
	}

	public final boolean isFreedomMoveY() {
		return freedomMoveY;
	}

	public final void setFreedomMoveY(boolean freedomMoveY) {
		this.freedomMoveY = freedomMoveY;
	}

	public final boolean isFreedomMoveZ() {
		return freedomMoveZ;
	}

	public final void setFreedomMoveZ(boolean freedomMoveZ) {
		this.freedomMoveZ = freedomMoveZ;
	}
	
	public final void setFreedomMove(boolean[] freedoms) {
		this.freedomMoveX = freedoms[0];
		this.freedomMoveX = freedoms[1];
		this.freedomMoveX = freedoms[2];
	}

	public final boolean isFreedomRotateX() {
		return freedomRotateX;
	}

	public final void setFreedomRotateX(boolean freedomRotateX) {
		this.freedomRotateX = freedomRotateX;
	}

	public final boolean isFreedomRotateY() {
		return freedomRotateY;
	}

	public final void setFreedomRotateY(boolean freedomRotateY) {
		this.freedomRotateY = freedomRotateY;
	}

	public final boolean isFreedomRotateZ() {
		return freedomRotateZ;
	}

	public final void setFreedomRotateZ(boolean freedomRotateZ) {
		this.freedomRotateZ = freedomRotateZ;
	}
	
	public final void setFreedomRotate(boolean[] freedoms) {
		this.freedomRotateX = freedoms[0];
		this.freedomRotateY = freedoms[1];
		this.freedomRotateZ = freedoms[2];
	}

	/**
	 * If true, the object can be picked (usually by clicking on it).
	 */
	public final boolean isPickable() {
		return pickable;
	}

	public final void setPickable(boolean pickable) {
		this.pickable = pickable;
	}

	/**
	 * If true, the camera is allowed to penetrate the object. Defaults to true.
	 */
	public final boolean isPenetratable() {
		return penetratable;
	}

	public final void setPenetratable(boolean penetratable) {
		this.penetratable = penetratable;
	}

	/**
	 * If true, the object is considered to have similar properties for all surfaces. This can speed up drawing of an
	 * object by allowing the drawing of all surface to be combined into a single call to the graphics engine.
	 */
	public final boolean isMonolithic() {
		return monolithic;
	}

	public final void setMonolithic(boolean monolithic) {
		this.monolithic = monolithic;
	}

	/**
	 * A sound that is always played by the object.
	 */
	public final String getSound() {
		return sound;
	}

	public final void setSound(String sound, float volume, float pitch) {
		this.sound = sound;
		this.soundVolume = volume;
		this.soundPitch = pitch;
		updateSound();
	}

	final void updateSound() {
		if (sound != null && soundVolume > 0) {
			WWVector absolutePosition = new WWVector();
			getAbsolutePosition(absolutePosition, world.getWorldTime());
			if (soundStreamId == 0) {
				soundStreamId = world.rendering.getRenderer().getSoundGenerator().startPlayingSound(getSound(), 2, absolutePosition, soundVolume, soundPitch);
			} else {
				soundStreamId = world.rendering.getRenderer().getSoundGenerator().adjustPlayingSound(soundStreamId, absolutePosition, soundVolume, soundPitch);
			}
		} else {
			if (soundStreamId != 0) {
				world.rendering.getRenderer().getSoundGenerator().stopPlayingSound(soundStreamId);
				soundStreamId = 0;
			}
		}
	}

	/**
	 * A sound that is played when something hits the object.
	 */
	public final String getImpactSound() {
		return impactSound;
	}

	public final void setImpactSound(String impactSound) {
		this.impactSound = impactSound;
	}

	/**
	 * A sound that is played when something moves across/through the object.
	 */
	public final String getSlidingSound() {
		return slidingSound;
	}

	public final void setSlidingSound(String slidingSound) {
		this.slidingSound = slidingSound;
	}

	/**
	 * Returns true if the object is in motion in any way. For WWObject, this means moving or rotating. If the object is
	 * a member of a collection and the collection is dynamic, the object is also dynamic. Subclasses can add additional
	 * property tests that indicate the object is dynamically changing in some way.
	 */
	public final boolean isDynamic() {
		if (sideAttributes != null) {
			for (int side = 0; side < NSIDES; side++) {
				if (sideAttributes[side] != null) {
					float textureVelocityX = sideAttributes[side].textureVelocityX;
					float textureVelocityY = sideAttributes[side].textureVelocityY;
					float textureAMomentum = sideAttributes[side].textureAMomentum;
					if (textureVelocityX != 0 || textureVelocityY != 0 || textureAMomentum != 0) {
						return true;
					}
				}
			}
		}
		boolean dynamic = velocityX != 0.0 || velocityY != 0.0 || velocityZ != 0.0 || aMomentumX != 0.0 || aMomentumY != 0.0 || aMomentumZ != 0.0;
		if (!dynamic && world != null && parentId != 0) {
			WWObject parent = world.objects[parentId];
			dynamic = parent.isDynamic();
		}
		return dynamic;
	}

	public final long getLastRenderingTime() {
		return lastRenderingTime;
	}

	@Override
	public final IRendering getRendering() {
		return rendering;
	}

	@Override
	public void updateRendering() {
		if (rendering != null) {
			rendering.updateRendering();
		}
	}
	
	public final WWVector getSize() {
		return new WWVector(sizeX, sizeY, sizeZ);
	}

	/**
	 * Determines if this object overlaps the give object. If so, return a vector pointing directly to the location of
	 * the overlap. The length of the vector depends on the depth of penetration of the object. This vector can be used
	 * to apply physical properties to the object as a result of the two objects making contact.
	 * <p>
	 * For situations where the object is contained within another object, the vector's direction is taken from the
	 * velocity vector of the object. This is somewhat arbitrary, but hints to the physics thread as to the direction in
	 * which to move the objects such that they are not overlapping.
	 * <p>
	 * Note: the world time is passed into this method to improve accuracy. The physics simulation depends on applying
	 * all physics for all objects in the world at exact points in time.
	 */
	public final WWVector getOverlapVector(WWObject object, long worldTime) {

		WWVector position = getPosition(worldTime);
		WWVector rotation = getRotation(worldTime);
		WWVector objectPosition = object.getPosition(worldTime);
		WWVector objectRotation = object.getRotation(worldTime);
		WWVector tempPoint = new WWVector();
		WWVector tempPoint2 = new WWVector();
		WWVector overlapPoint = new WWVector();
		WWVector overlapVector = new WWVector();

		getOverlap(object, position, rotation, objectPosition, objectRotation, worldTime, tempPoint, tempPoint2, overlapPoint, overlapVector);

		return overlapVector;

	}

	private transient WWVector[] lastTransformedEdgePoints;
	private transient WWVector lastOverlapPosition;
	private transient WWVector lastOverlapRotation;

	public final void getOverlap(WWObject object, WWVector position, WWVector rotation, WWVector objectPosition, WWVector objectRotation, long worldTime, WWVector tempPoint, WWVector tempPoint2, WWVector overlapPoint, WWVector overlapVector) {
		// To simplify overlap testing, check several key points. These are the eight corners, twelve
		// half edge points, and six center side points of the box. Test each of these. 

		// First, transform the edge points by current rotation and position.  These points are cached just in case
		// they are needed on the next overlap
		WWVector[] transformedEdgePoints;
		int edgePointsLength;
		if (parentId == 0 && lastTransformedEdgePoints != null && (fixed || (lastOverlapPosition.equals(position) && lastOverlapRotation.equals(rotation)))) {
			transformedEdgePoints = lastTransformedEdgePoints;
			edgePointsLength = edgePoints.length;
		} else {
			WWVector[] constEdgePoints = getEdgePoints();
			edgePointsLength = constEdgePoints.length;
			transformedEdgePoints = lastTransformedEdgePoints;
			if (transformedEdgePoints == null) {
				transformedEdgePoints = new WWVector[edgePointsLength];
			}
			for (int i = 0; i < edgePointsLength; i++) {
				WWVector edgePoint = transformedEdgePoints[i];
				if (edgePoint == null) {
					edgePoint = constEdgePoints[i].clone();
				} else {
					constEdgePoints[i].copyInto(edgePoint);
				}
				transform(edgePoint, position, rotation, worldTime);
				transformedEdgePoints[i] = edgePoint;
			}
			lastTransformedEdgePoints = transformedEdgePoints;
			lastOverlapPosition = position;
			lastOverlapRotation = rotation;
		}

		overlapVector.zero();
		for (int i = 0; i < edgePointsLength; i++) {
			WWVector edgePoint = transformedEdgePoints[i];
			// first test point with extents to avoid a costlier penetration calculation
			if (object.extentx + objectPosition.x > edgePoint.x && -object.extentx + objectPosition.x < edgePoint.x && //
					object.extenty + objectPosition.y > edgePoint.y && -object.extenty + objectPosition.y < edgePoint.y && //
					object.extentz + objectPosition.z > edgePoint.z && -object.extentz + objectPosition.z < edgePoint.z) {
				object.getPenetration(edgePoint, objectPosition, objectRotation, worldTime, tempPoint, tempPoint2);
				if (tempPoint2.isLongerThan(overlapVector)) {
					tempPoint2.copyInto(overlapVector);
					transformedEdgePoints[i].copyInto(overlapPoint);
					return; // less accurate but faster
				}
			}
		}

		// Note: If objects still penetrate walls, the only thing to do is to speed up the physics thread and iterate more often.
		// I've tried just about everything else.  Keep in mind that with enough speed, any object can quantum-jump!
	}

	/**
	 * Returns a vector giving the amount of penetration of a point within the object, or null if the point does not
	 * penetrate.
	 */
	public abstract void getPenetration(WWVector point, WWVector position, WWVector rotation, long worldTime, WWVector tempPoint, WWVector penetrationVector);

	/**
	 * Returns an array of points describing the edges of the object
	 */
	protected WWVector[] getEdgePoints() {
		if (edgePoints == null) {
			float sx2 = sizeX / 2.0f;
			float sy2 = sizeY / 2.0f;
			float sz2 = sizeZ / 2.0f;
			edgePoints = new WWVector[] {
					// - six center side points, starting with base, then front (for speed)
					new WWVector(0, 0, -sz2), new WWVector(0, -sy2, 0), new WWVector(sx2, 0, 0), new WWVector(-sx2, 0, 0), new WWVector(0, sy2, 0), new WWVector(0, 0, sz2),
					// - eight corners
					new WWVector(sx2, sy2, sz2), new WWVector(-sx2, sy2, sz2), new WWVector(sx2, -sy2, sz2), new WWVector(-sx2, -sy2, sz2), new WWVector(sx2, sy2, -sz2), new WWVector(-sx2, sy2, -sz2), new WWVector(sx2, -sy2, -sz2), new WWVector(-sx2, -sy2, -sz2),
					// - twelve half edge points
					new WWVector(sx2, sy2, 0), new WWVector(-sx2, sy2, 0), new WWVector(sx2, -sy2, 0), new WWVector(-sx2, -sy2, 0), new WWVector(sx2, 0, sz2), new WWVector(-sx2, 0, sz2), new WWVector(sx2, 0, -sz2), new WWVector(-sx2, 0, -sz2), new WWVector(0, sy2, sz2), new WWVector(0, -sy2, sz2), new WWVector(0, sy2, -sz2), new WWVector(0, -sy2, -sz2) };
		}
		return edgePoints;
	}

	/**
	 * Rotate a point according to the object's rotation.
	 */
	public final WWVector rotate(WWVector point, WWVector rotation, long worldTime) {

		// if this is a member of a collection, adjust rotation according to parent's rotation
//		if (parentId != 0) {
//			WWObject parent = world.objects[parentId];
//			parent.rotate(point, parent.getRotation(worldTime), worldTime);
//		}

		float x = point.x;
		float y = point.y;
		float z = point.z;
		float r;
		float theta;
		float newTheta;

		// Rotate around x axis
		if (rotation.x != 0.0) {
			r = (float) Math.sqrt(y * y + z * z);
			theta = FastMath.atan2(y, z);
			newTheta = theta + TORADIAN * rotation.x;
			y = r * FastMath.sin(newTheta);
			z = r * FastMath.cos(newTheta);
		}

		// Rotate around y axis
		if (rotation.y != 0.0) {
			r = (float) Math.sqrt(x * x + z * z);
			theta = FastMath.atan2(x, z);
			newTheta = theta + TORADIAN * -rotation.y;
			x = r * FastMath.sin(newTheta);
			z = r * FastMath.cos(newTheta);
		}

		// Rotate around z axis
		if (rotation.z != 0.0) {
			r = (float) Math.sqrt(x * x + y * y);
			theta = FastMath.atan2(x, y);
			newTheta = theta + TORADIAN * rotation.z;
			x = r * FastMath.sin(newTheta);
			y = r * FastMath.cos(newTheta);
		}

		point.x = x;
		point.y = y;
		point.z = z;
		return point;
	}

	/**
	 * Anti-rotate a point, removing the object's rotation. Note that this is only useful to perform on velocity/force
	 * vectors.
	 */
	public final WWVector antiRotate(WWVector point, WWVector rotation, long worldTime) {

		float x = point.x;
		float y = point.y;
		float z = point.z;
		float r;
		float theta;
		float newTheta;

		// Anti-rotate around z axis
		if (rotation.z != 0.0f) {
			r = (float) Math.sqrt(x * x + y * y);
			theta = FastMath.atan2(x, y);
			newTheta = theta - TORADIAN * rotation.z;
			x = r * FastMath.sin(newTheta);
			y = r * FastMath.cos(newTheta);
		}

		// Anti-rotate around y axis
		if (rotation.y != 0.0f) {
			r = (float) Math.sqrt(x * x + z * z);
			theta = FastMath.atan2(x, z);
			newTheta = theta - TORADIAN * -rotation.y;
			x = r * FastMath.sin(newTheta);
			z = r * FastMath.cos(newTheta);
		}

		// Anti-rotate around x axis
		if (rotation.x != 0.0f) {
			r = (float) Math.sqrt(y * y + z * z);
			theta = FastMath.atan2(y, z);
			newTheta = theta - TORADIAN * rotation.x;
			y = r * FastMath.sin(newTheta);
			z = r * FastMath.cos(newTheta);
		}

		point.x = x;
		point.y = y;
		point.z = z;

		// if this is a member of a collection, adjust rotation according to parent's rotation
//		if (parentId != 0) {
//			WWObject parent = world.objects[parentId];
//			parent.antiRotate(point, parent.getRotation(worldTime), worldTime);
//		}

		return point;
	}

	/**
	 * Transform a point according to the object's rotation and position at the given time.
	 */
	public final WWVector transform(WWVector point, WWVector position, WWVector rotation, long worldTime) {

		// if this is a member of a collection, translate according to parent first
//		if (parentId != 0) {
//			WWObject parent = world.objects[parentId];
//			parent.transform(point, parent.getPosition(worldTime), parent.getRotation(worldTime), worldTime);
//		}

		float x = point.x;
		float y = point.y;
		float z = point.z;
		float r;
		float theta;
		float newTheta;

		// Rotate around x axis
		if (rotation.x != 0.0f) {
			r = (float) Math.sqrt(y * y + z * z);
			theta = FastMath.atan2(y, z);
			newTheta = theta + TORADIAN * rotation.x;
			y = r * FastMath.sin(newTheta);
			z = r * FastMath.cos(newTheta);
		}

		// Rotate around y axis
		if (rotation.y != 0.0f) {
			r = (float) Math.sqrt(x * x + z * z);
			theta = FastMath.atan2(x, z);
			newTheta = theta + TORADIAN * -rotation.y;
			x = r * FastMath.sin(newTheta);
			z = r * FastMath.cos(newTheta);
		}

		// Rotate around z axis
		if (rotation.z != 0.0f) {
			r = (float) Math.sqrt(x * x + y * y);
			theta = FastMath.atan2(x, y);
			newTheta = theta + TORADIAN * rotation.z;
			x = r * FastMath.sin(newTheta);
			y = r * FastMath.cos(newTheta);
		}

		// Translate
		x = x + position.x;
		y = y + position.y;
		z = z + position.z;

		point.x = x;
		point.y = y;
		point.z = z;

		return point;
	}

	/**
	 * Anti-transform a point, removing the object's position and rotation.
	 */
	public final WWVector antiTransform(WWVector point, WWVector position, WWVector rotation, long time) {

		float r;
		float theta;
		float newTheta;

		// Anti-translate
		float x = point.x - position.x;
		float y = point.y - position.y;
		float z = point.z - position.z;

		// Anti-rotate around z axis
		if (rotation.z != 0.0f) {
			r = (float) Math.sqrt(x * x + y * y);
			theta = FastMath.atan2(x, y);
			newTheta = theta - TORADIAN * rotation.z;
			x = r * FastMath.sin(newTheta);
			y = r * FastMath.cos(newTheta);
		}

		// Anti-rotate around y axis
		if (rotation.y != 0.0f) {
			r = (float) Math.sqrt(x * x + z * z);
			theta = FastMath.atan2(x, z);
			newTheta = theta - TORADIAN * -rotation.y;
			x = r * FastMath.sin(newTheta);
			z = r * FastMath.cos(newTheta);
		}

		// Anti-rotate around x axis
		if (rotation.x != 0.0f) {
			r = (float) Math.sqrt(y * y + z * z);
			theta = FastMath.atan2(y, z);
			newTheta = theta - TORADIAN * rotation.x;
			y = r * FastMath.sin(newTheta);
			z = r * FastMath.cos(newTheta);
		}

		point.x = x;
		point.y = y;
		point.z = z;

		// if this is a member of a collection, antitranslate according to parent
//		if (parentId != 0) {
//			WWObject parent = world.objects[parentId];
//			parent.antiTransform(point, parent.getPosition(time), parent.getRotation(time), time);
//		}

		return point;
	}

	@Override
	public Object clone() {
		WWObject clone = (WWObject) super.clone();
		if (world != null) {
			long currenttime = getWorldTime();
			clone.setLastModifyTime(currenttime);
			// manually clone position and rotation as these change due to modified time
			clone.setPosition(getPosition(currenttime));
			clone.setRotation(getRotation(currenttime));
		}
		if (behaviors != null) {
			// Need to clone behaviors and set owner in cloned behaviors to the cloned owner
			clone.behaviors = behaviors.clone();
			for (int i = 0; i < behaviors.length; i++) {
				clone.behaviors[i] = (BehaviorAttributes) behaviors[i].clone();
				clone.behaviors[i].behavior.owner = clone;
			}
		}
		return clone;
	}

	/**
	 * Same as clone, but behaviors aren't cloned.
	 * 
	 * @return
	 */
	public Object cloneNoBehavior() {
		WWObject clone = (WWObject) super.clone();
		if (world != null) {
			long currenttime = getWorldTime();
			clone.setLastModifyTime(currenttime);
			// manually clone position and rotation as these change due to modified time
			clone.setPosition(getPosition(currenttime));
			clone.setRotation(getRotation(currenttime));
		}
		clone.behaviors = null;
		return clone;
	}

	public void copyFrom(WWObject newObject) {
		//this.id = newObject.id;
		this.lastMoveTime = newObject.lastMoveTime;
		this.positionX = newObject.positionX;
		this.positionY = newObject.positionY;
		this.positionZ = newObject.positionZ;
		this.rotationX = newObject.rotationX;
		this.rotationY = newObject.rotationY;
		this.rotationZ = newObject.rotationZ;

		// Grouping properties
		this.parentId = newObject.parentId;
		this.gluedToParent = newObject.gluedToParent;
		this.childrenIds = newObject.childrenIds;

		// Physics properties
		this.physical = newObject.physical;
		this.phantom = newObject.phantom;
		this.velocityX = newObject.velocityX;
		this.velocityY = newObject.velocityY;
		this.velocityZ = newObject.velocityZ;
		this.aMomentumX = newObject.aMomentumX;
		this.aMomentumY = newObject.aMomentumY;
		this.aMomentumZ = newObject.aMomentumZ;
		this.density = newObject.density;
		this.solid = newObject.solid;
		this.elasticity = newObject.elasticity;
		this.friction = newObject.friction;
		this.freedomMoveX = newObject.freedomMoveX;
		this.freedomMoveY = newObject.freedomMoveY;
		this.freedomMoveZ = newObject.freedomMoveZ;
		this.freedomRotateX = newObject.freedomRotateX;
		this.freedomRotateY = newObject.freedomRotateY;
		this.freedomRotateZ = newObject.freedomRotateZ;
		this.thrustX = newObject.thrustX;
		this.thrustY = newObject.thrustY;
		this.thrustZ = newObject.thrustZ;
		this.thrustVelocityX = newObject.thrustVelocityX;
		this.thrustVelocityY = newObject.thrustVelocityY;
		this.thrustVelocityZ = newObject.thrustVelocityZ;
		this.torqueX = newObject.torqueX;
		this.torqueY = newObject.torqueY;
		this.torqueZ = newObject.torqueZ;
		this.torqueVelocityX = newObject.torqueVelocityX;
		this.torqueVelocityY = newObject.torqueVelocityY;
		this.torqueVelocityZ = newObject.torqueVelocityZ;

		this.pickable = newObject.pickable;
		this.penetratable = newObject.penetratable;

		this.sound = newObject.sound;
		this.impactSound = newObject.impactSound;
		this.slidingSound = newObject.slidingSound;

		//this.behaviors = newObject.behaviors;

		//this.rendering = newObject.rendering;
		//this.lastRenderingTime = newObject.lastRenderingTime;

		this.stopPosition = newObject.stopPosition;
		this.stopRotation = newObject.stopRotation;

	}

	public final int getParent() {
		return parentId;
	}

	public final void setParent(int parentId) {
		this.parentId = parentId;
	}

	public final void setParent(WWObject parent) {
		if (parent == null) {
			this.parentId = 0;
		} else {
			this.parentId = parent.getId();
		}
	}

	public final boolean isChildOf(WWObject parent) {
		int tempId = parentId;
		while (tempId != 0) {
			if (parent.id == tempId) {
				return true;
			}
			tempId = world.objects[tempId].parentId;
		}
		return false;
	}

	public final void setPhantom(boolean phantom) {
		this.phantom = phantom;
	}

	/**
	 * Indicates that the object is completely ghost-like, not partaking in physics of any kind.
	 */
	public final boolean isPhantom() {
		return phantom;
	}

	public final boolean isGluedToParent() {
		return gluedToParent;
	}

	public final void setGluedToParent(boolean gluedToParent) {
		this.gluedToParent = gluedToParent;
	}

	public final int[] getChildren() {
		if (childrenIds == null) {
			return DEFAULT_CHILDREN;
		}
		return childrenIds;
	}

	public final void setChildren(int[] childrenIds) {
		this.childrenIds = childrenIds;
	}

	public final int getChildId(int i) {
		return childrenIds[i];
	}

	public final WWObject getChild(String name) {
		for (int i = 0; i < childrenIds.length; i++) {
			int childId = childrenIds[i];
			WWObject childObject = world.objects[childId];
			if (name.equals(childObject.getName())) {
				return childObject;
			}
		}
		return null;
	}

	public final void addChild(int childId) {
		int[] newChildrenIds;
		if (childrenIds == null) {
			newChildrenIds = new int[1];
		} else {
			newChildrenIds = new int[childrenIds.length + 1];
			for (int i = 0; i < childrenIds.length; i++) {
				newChildrenIds[i] = childrenIds[i];
			}
		}
		newChildrenIds[newChildrenIds.length - 1] = childId;
		childrenIds = newChildrenIds;
	}

	public final void removeChild(int childId) {
		if (childrenIds != null) {
			for (int i = 0; i < childrenIds.length; i++) {
				if (childrenIds[i] == childId) {
					int[] newChildrenIds = new int[childrenIds.length - 1];
					for (int j = 0; j < i; j++) {
						newChildrenIds[j] = childrenIds[j];
					}
					for (int j = i; j < childrenIds.length - 1; j++) {
						newChildrenIds[j] = childrenIds[j + 1];
					}
					childrenIds = newChildrenIds;
					return;
				}
			}
		}
	}

	public final int addBehavior(final byte[] behaviorClassBinary) {
		BehaviorAttributes behaviorAttributes = new BehaviorAttributes(behaviorClassBinary);
		return addBehavior(behaviorAttributes);
	}

	public final int addBehavior(final String behaviorClassName) {
		BehaviorAttributes behaviorAttributes = new BehaviorAttributes(behaviorClassName);
		return addBehavior(behaviorAttributes);
	}

	public final int addBehavior(final WWBehavior behavior) {
		behavior.owner = this;
		BehaviorAttributes behaviorAttributes = new BehaviorAttributes(behavior);
		return addBehavior(behaviorAttributes);
	}

	private final int addBehavior(BehaviorAttributes behaviorAttributes) {
		BehaviorAttributes[] newBehaviors;
		if (behaviors == null) {
			newBehaviors = new BehaviorAttributes[1];
		} else {
			newBehaviors = new BehaviorAttributes[behaviors.length + 1];
			for (int i = 0; i < behaviors.length; i++) {
				newBehaviors[i] = behaviors[i];
			}
		}
		behaviorAttributes.instantiateBehavior();
		newBehaviors[newBehaviors.length - 1] = behaviorAttributes;
		behaviors = newBehaviors;
		return behaviors.length - 1;
	}

	public final void removeBehavior(int behaviorIndex) {
		BehaviorAttributes[] newBehaviors = new BehaviorAttributes[behaviors.length - 1];
		for (int i = 0; i < behaviorIndex; i++) {
			newBehaviors[i] = behaviors[i];
		}
		for (int i = behaviorIndex + 1; i < behaviors.length; i++) {
			newBehaviors[i - 1] = behaviors[i];
		}
		behaviors = newBehaviors;
	}

	public final int getBehaviorCount() {
		if (behaviors == null) {
			return 0;
		}
		return behaviors.length;
	}

	public final WWBehavior getBehavior(int behaviorIndex) {
		return behaviors[behaviorIndex].behavior;
	}

	public final WWBehavior getBehavior(Class behaviorClass) {
		if (behaviors == null) {
			return null;
		}
		for (int i = 0; i < behaviors.length; i++) {
			WWBehavior behavior = behaviors[i].behavior;
			if (behavior.getClass() == behaviorClass) {
				return behavior;
			}
		}
		return null;
	}

	public final WWBehavior[] getBehaviors() {
		if (behaviors == null) {
			return new WWBehavior[0];
		}
		WWBehavior[] b = new WWBehavior[behaviors.length];
		for (int i = 0; i < behaviors.length; i++) {
			b[i] = behaviors[i].behavior;
		}
		return b;
	}

	/**
	 * Invoked by BehaviorThread to launch behaviors events on the object. If the object has a parent and the behaviors
	 * do not override, the parent object will also be invoked for the behavior event.
	 */
	final void invokeBehavior(String command, WWEntity agent, Object params) {
		try {
			boolean overrideParent = false;
			if (behaviors != null) {
				if (command.equals("touch")) {
					for (int i = 0; i < behaviors.length; i++) {
						behaviors[i].behavior.world = world;
						WWWorld.TouchParams tparams = (WWWorld.TouchParams) params;
						overrideParent |= behaviors[i].behavior.touchEvent(this, agent, tparams.surface, tparams.x, tparams.y);
					}
				} else if (command.equals("press")) {
					for (int i = 0; i < behaviors.length; i++) {
						behaviors[i].behavior.world = world;
						WWWorld.TouchParams tparams = (WWWorld.TouchParams) params;
						overrideParent |= behaviors[i].behavior.pressEvent(this, agent, tparams.surface, tparams.x, tparams.y);
					}
				} else if (command.equals("drag")) {
					for (int i = 0; i < behaviors.length; i++) {
						behaviors[i].behavior.world = world;
						WWWorld.TouchParams tparams = (WWWorld.TouchParams) params;
						overrideParent |= behaviors[i].behavior.dragEvent(this, agent, tparams.surface, tparams.x, tparams.y);
					}
				} else if (command.equals("release")) {
					for (int i = 0; i < behaviors.length; i++) {
						behaviors[i].behavior.world = world;
						WWWorld.TouchParams tparams = (WWWorld.TouchParams) params;
						overrideParent |= behaviors[i].behavior.releaseEvent(this, agent, tparams.surface, tparams.x, tparams.y);
					}
				} else if (command.equals("collide")) {
					for (int i = 0; i < behaviors.length; i++) {
						behaviors[i].behavior.world = world;
						overrideParent |= behaviors[i].behavior.collideEvent(this, (WWObject) agent, (WWVector) params);
					}
				} else if (command.equals("slide")) {
					for (int i = 0; i < behaviors.length; i++) {
						behaviors[i].behavior.world = world;
						overrideParent |= behaviors[i].behavior.slideEvent(this, (WWObject) agent, (WWVector) params);
					}
				} else if (command.equals("stopSlide")) {
					for (int i = 0; i < behaviors.length; i++) {
						behaviors[i].behavior.world = world;
						overrideParent |= behaviors[i].behavior.stopSlideEvent(this, (WWObject) agent, (WWVector) params);
					}
				} else if (command.equals("timer")) {
					for (int i = 0; i < behaviors.length; i++) {
						behaviors[i].behavior.world = world;
						overrideParent |= behaviors[i].behavior.timerEvent(this);
					}
				}
			}
			if (!overrideParent && parentId != 0 && world != null) {
				world.objects[parentId].invokeBehavior(command, agent, params);
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	@Override
	public final void dropRendering() {
		if (rendering != null) {
//			rendering.destroy();
		}
		rendering = null;
	}

//	public final float getSizeX() {
//		return sizeX;
//	}
//
//	public final float getSizeY() {
//		return sizeY;
//	}
//
//	public final float getSizeZ() {
//		return sizeZ;
//	}

	public final void setSize(WWVector v) {
		sizeX = v.x;
		sizeY = v.y;
		sizeZ = v.z;
		calculateExtents();
		edgePoints = null;
	}

	public final void setSize(float x, float y, float z) {
		sizeX = x;
		sizeY = y;
		sizeZ = z;
		calculateExtents();
		edgePoints = null;
	}

	public void setSize(float[] dims) {
		setSize(dims[0], dims[1], dims[2]);
	}

	/**
	 * Overridable is subclasses to make smarter extent calculations, avoiding more complicated math when determining
	 * overlaps.
	 */
	protected void calculateExtents() {
		extent = (float) Math.sqrt(sizeX * sizeX + sizeY * sizeY + sizeZ * sizeZ) / 2.0f;
		extentx = extent;
		extenty = extent;
		extentz = extent;
		if (fixed && parentId == 0) {
			if (rotationX == 0 && rotationY == 0) {
				extentz = sizeZ / 2.0f;
			}
			if (rotationX == 0 && rotationZ == 0) {
				extenty = sizeY / 2.0f;
			}
			if (rotationY == 0 && rotationZ == 0) {
				extentx = sizeX / 2.0f;
			}
		}
	}

	/**
	 * Note: this is not intended for use by behaviors. It is provided for import/export.
	 */
	public final SideAttributes[] getSideAttributesArray() {
		return sideAttributes;
	}

	/**
	 * Note: this is not intended for use by behaviors. It is provided for import/export.
	 */
	public final void setSideAttributesArray(SideAttributes[] sideAttributes) {
		this.sideAttributes = sideAttributes;
	}

	private final SideAttributes getEditableSideAttributes(int side) {
		if (side == SIDE_ALL) {
			if (sideAttributes[SIDE_ALL].isDefault) {
				sideAttributes[SIDE_ALL] = new SideAttributes();
				for (int i = SIDE_ALL + 1; i < NSIDES; i++) {
					if (sideAttributes[i].isDefault) {
						sideAttributes[i] = sideAttributes[SIDE_ALL];
					}
				}
			}
		} else {
			if (sideAttributes[side].isDefault || sideAttributes[side] == sideAttributes[SIDE_ALL]) {
				sideAttributes[side] = sideAttributes[SIDE_ALL].copy();
				sideAttributes[side].isDefault = false;
				monolithic = false;
			}
		}
		return sideAttributes[side];
	}

	public final WWColor getColor(int side) {
		return new WWColor(sideAttributes[side].red, sideAttributes[side].green, sideAttributes[side].blue);
	}

	public final float getRedColor(int side) {
		return sideAttributes[side].red;
	}

	public final float getGreenColor(int side) {
		return sideAttributes[side].green;
	}

	public final float getBlueColor(int side) {
		return sideAttributes[side].blue;
	}

	public final void setColor(int side, WWColor color) {
		getEditableSideAttributes(side).red = color.getRed();
		getEditableSideAttributes(side).green = color.getGreen();
		getEditableSideAttributes(side).blue = color.getBlue();
	}
	
	public final void setColor(int color) {
		setColor(SIDE_ALL, new WWColor(color));
	}
	
	public final void setColorTop(int color) {
		setColor(SIDE_TOP, new WWColor(color));
	}

	public final void setColorBottom(int color) {
		setColor(SIDE_BOTTOM, new WWColor(color));
	}

	public final void setColorSide1(int color) {
		setColor(SIDE_SIDE1, new WWColor(color));
	}

	public final void setColorSide2(int color) {
		setColor(SIDE_SIDE2, new WWColor(color));
	}

	public final void setColorSide3(int color) {
		setColor(SIDE_SIDE3, new WWColor(color));
	}

	public final void setColorSide4(int color) {
		setColor(SIDE_SIDE4, new WWColor(color));
	}

	public final void setColorInsideTop(int color) {
		setColor(SIDE_INSIDE_TOP, new WWColor(color));
	}

	public final void setColorInsideBottom(int color) {
		setColor(SIDE_INSIDE_BOTTOM, new WWColor(color));
	}

	public final void setColorInside1(int color) {
		setColor(SIDE_INSIDE1, new WWColor(color));
	}

	public final void setColorInside2(int color) {
		setColor(SIDE_INSIDE2, new WWColor(color));
	}

	public final void setColorInside3(int color) {
		setColor(SIDE_INSIDE3, new WWColor(color));
	}

	public final void setColorInside4(int color) {
		setColor(SIDE_INSIDE4, new WWColor(color));
	}

	public final void setColorCutout1(int color) {
		setColor(SIDE_CUTOUT1, new WWColor(color));
	}

	public final void setColorCutout2(int color) {
		setColor(SIDE_CUTOUT2, new WWColor(color));
	}

	public final String getTextureURL(int side) {
		return sideAttributes[side].textureURL;
	}

	public final void setTextureURL(int side, String textureURL) {
		getEditableSideAttributes(side).textureURL = textureURL;
	}

	public final float getTextureRotation(int side, long worldTime) {
		float deltaTime = (worldTime - lastMoveTime) / 1000.0f;
		return sideAttributes[side].textureRotation + deltaTime * sideAttributes[side].textureAMomentum;
	}

	public final void setTextureRotation(int side, float rotation) {
		getEditableSideAttributes(side).textureRotation = rotation;
	}

	public final float getTextureAMomentum(int side) {
		return sideAttributes[side].textureAMomentum;
	}

	public final void setTextureAMomentum(int side, float aMomentum) {
		getEditableSideAttributes(side).textureAMomentum = aMomentum;
	}

	public final float getTextureOffsetX(int side, long worldTime) {
		float deltaTime = (worldTime - lastMoveTime) / 1000.0f;
		return sideAttributes[side].textureOffsetX + deltaTime * sideAttributes[side].textureVelocityX;
	}

	public final void setTextureOffsetX(int side, float offsetX) {
		getEditableSideAttributes(side).textureOffsetX = offsetX;
	}

	public final float getTextureOffsetY(int side, long worldTime) {
		float deltaTime = (worldTime - lastMoveTime) / 1000.0f;
		return sideAttributes[side].textureOffsetY + deltaTime * sideAttributes[side].textureVelocityY;
	}

	public final void setTextureOffsetY(int side, float offsetY) {
		getEditableSideAttributes(side).textureOffsetY = offsetY;
	}

	public final float getTextureScaleX(int side) {
		return sideAttributes[side].textureScaleX;
	}

	public final void setTextureScaleX(int side, float scaleX) {
		getEditableSideAttributes(side).textureScaleX = scaleX;
	}

	public final float getTextureScaleY(int side) {
		return sideAttributes[side].textureScaleY;
	}

	public final void setTextureScaleY(int side, float scaleY) {
		getEditableSideAttributes(side).textureScaleY = scaleY;
	}

	public final void setTextureScale(int side, float scaleX, float scaleY) {
		getEditableSideAttributes(side).textureScaleX = scaleX;
		getEditableSideAttributes(side).textureScaleY = scaleY;
	}

	public final void setTexture(int side, String textureUrl, float scaleX, float scaleY) {
		setTextureURL(side, textureUrl);
		setTextureScale(side, scaleX, scaleY);
	}

	public final void setTexture(int side, String textureUrl, float scaleX, float scaleY, float rotation) {
		setTextureURL(side, textureUrl);
		setTextureScale(side, scaleX, scaleY);
		setTextureRotation(side, rotation);
	}
	
	public final void setTexture(int side, Object[] textureAttributes) {
		if (textureAttributes.length == 1) {
			setTexture(side, (String)textureAttributes[0], 1.0f, 1.0f, 0.0f);
		} else if (textureAttributes.length == 2) {
			setTexture(side, (String)textureAttributes[0], ((Double)textureAttributes[1]).floatValue(), 1.0f, 0.0f);
		} else if (textureAttributes.length == 3) {
			setTexture(side, (String)textureAttributes[0], ((Double)textureAttributes[1]).floatValue(), ((Double)textureAttributes[2]).floatValue(), 0.0f);
		} else if (textureAttributes.length == 4) {
			setTexture(side, (String)textureAttributes[0], ((Double)textureAttributes[1]).floatValue(), ((Double)textureAttributes[2]).floatValue(), ((Double)textureAttributes[3]).floatValue());
		}
	}
	
	public final void setTexture(Object[] textureAttributes) {
		setTexture(SIDE_ALL, textureAttributes);
	}

	public final void setTextureTop(Object[] textureAttributes) {
		setTexture(SIDE_TOP, textureAttributes);
	}

	public final void setTextureBottom(Object[] textureAttributes) {
		setTexture(SIDE_BOTTOM, textureAttributes);
	}

	public final void setTextureSide1(Object[] textureAttributes) {
		setTexture(SIDE_SIDE1, textureAttributes);
	}

	public final void setTextureSide2(Object[] textureAttributes) {
		setTexture(SIDE_SIDE2, textureAttributes);
	}

	public final void setTextureSide3(Object[] textureAttributes) {
		setTexture(SIDE_SIDE3, textureAttributes);
	}

	public final void setTextureSide4(Object[] textureAttributes) {
		setTexture(SIDE_SIDE4, textureAttributes);
	}

	public final void setTextureInsideTop(Object[] textureAttributes) {
		setTexture(SIDE_INSIDE_TOP, textureAttributes);
	}

	public final void setTextureInsideBottom(Object[] textureAttributes) {
		setTexture(SIDE_INSIDE_BOTTOM, textureAttributes);
	}

	public final void setTextureInside1(Object[] textureAttributes) {
		setTexture(SIDE_INSIDE1, textureAttributes);
	}

	public final void setTextureInside2(Object[] textureAttributes) {
		setTexture(SIDE_INSIDE2, textureAttributes);
	}

	public final void setTextureInside3(Object[] textureAttributes) {
		setTexture(SIDE_INSIDE3, textureAttributes);
	}

	public final void setTextureInside4(Object[] textureAttributes) {
		setTexture(SIDE_INSIDE4, textureAttributes);
	}

	public final void setTextureCutout1(Object[] textureAttributes) {
		setTexture(SIDE_CUTOUT1, textureAttributes);
	}

	public final void setTextureCutout2(Object[] textureAttributes) {
		setTexture(SIDE_CUTOUT2, textureAttributes);
	}

	public final float getTextureVelocityX(int side) {
		return sideAttributes[side].textureVelocityX;
	}

	public final void setTextureVelocityX(int side, float velocityX) {
		getEditableSideAttributes(side).textureVelocityX = velocityX;
	}

	public final float getTextureVelocityY(int side) {
		return sideAttributes[side].textureVelocityY;
	}

	public final void setTextureVelocityY(int side, float velocityY) {
		getEditableSideAttributes(side).textureVelocityY = velocityY;
	}

	public final long getTextureRefreshInterval(int side) {
		return sideAttributes[side].textureRefreshInterval;
	}

	public final void setTextureRefreshInterval(int side, long millis) {
		getEditableSideAttributes(side).textureRefreshInterval = millis;
	}

	public final float getTransparency(int side) {
		return sideAttributes[side].transparency;
	}

	public final void setTransparency(int side, float transparency) {
		getEditableSideAttributes(side).transparency = transparency;
	}
	
	public final void setTransparency(float transparency) {
		setTransparency(SIDE_ALL, transparency);
	}

	public final void setTransparencyTop(float transparency) {
		setTransparency(SIDE_TOP, transparency);
	}

	public final void setTransparencyBottom(float transparency) {
		setTransparency(SIDE_BOTTOM, transparency);
	}

	public final void setTransparencySide1(float transparency) {
		setTransparency(SIDE_SIDE1, transparency);
	}

	public final void setTransparencySide2(float transparency) {
		setTransparency(SIDE_SIDE2, transparency);
	}

	public final void setTransparencySide3(float transparency) {
		setTransparency(SIDE_SIDE3, transparency);
	}

	public final void setTransparencySide4(float transparency) {
		setTransparency(SIDE_SIDE4, transparency);
	}

	public final void setTransparencyInsideTop(float transparency) {
		setTransparency(SIDE_INSIDE_TOP, transparency);
	}

	public final void setTransparencyInsideBottom(float transparency) {
		setTransparency(SIDE_INSIDE_BOTTOM, transparency);
	}

	public final void setTransparencyInside1(float transparency) {
		setTransparency(SIDE_INSIDE1, transparency);
	}

	public final void setTransparencyInside2(float transparency) {
		setTransparency(SIDE_INSIDE2, transparency);
	}

	public final void setTransparencyInside3(float transparency) {
		setTransparency(SIDE_INSIDE3, transparency);
	}

	public final void setTransparencyInside4(float transparency) {
		setTransparency(SIDE_INSIDE4, transparency);
	}

	public final void setTransparencyCutout1(float transparency) {
		setTransparency(SIDE_CUTOUT1, transparency);
	}

	public final void setTransparencyCutout2(float transparency) {
		setTransparency(SIDE_CUTOUT2, transparency);
	}

	public final float getShininess(int side) {
		return sideAttributes[side].shininess;
	}

	public final void setShininess(int side, float shininess) {
		getEditableSideAttributes(side).shininess = shininess;
	}

	public final void setShininess(float shininess) {
		getEditableSideAttributes(SIDE_ALL).shininess = shininess;
	}

	public final void setShininessTop(float shininess) {
		getEditableSideAttributes(SIDE_TOP).shininess = shininess;
	}

	public final void setShininessBottom(float shininess) {
		getEditableSideAttributes(SIDE_BOTTOM).shininess = shininess;
	}

	public final void setShininessSide1(float shininess) {
		getEditableSideAttributes(SIDE_SIDE1).shininess = shininess;
	}

	public final void setShininessSide2(float shininess) {
		getEditableSideAttributes(SIDE_SIDE2).shininess = shininess;
	}

	public final void setShininessSide3(float shininess) {
		getEditableSideAttributes(SIDE_SIDE3).shininess = shininess;
	}

	public final void setShininessSide4(float shininess) {
		getEditableSideAttributes(SIDE_SIDE4).shininess = shininess;
	}

	public final void setShininessInsideTop(float shininess) {
		getEditableSideAttributes(SIDE_INSIDE_TOP).shininess = shininess;
	}

	public final void setShininessInsideBottom(float shininess) {
		getEditableSideAttributes(SIDE_INSIDE_BOTTOM).shininess = shininess;
	}

	public final void setShininessInside1(float shininess) {
		getEditableSideAttributes(SIDE_INSIDE1).shininess = shininess;
	}

	public final void setShininessInside2(float shininess) {
		getEditableSideAttributes(SIDE_INSIDE2).shininess = shininess;
	}

	public final void setShininessInside3(float shininess) {
		getEditableSideAttributes(SIDE_INSIDE3).shininess = shininess;
	}

	public final void setShininessInside4(float shininess) {
		getEditableSideAttributes(SIDE_INSIDE4).shininess = shininess;
	}

	public final void setShininessCutout1(float shininess) {
		getEditableSideAttributes(SIDE_CUTOUT1).shininess = shininess;
	}

	public final void setShininessCutout2(float shininess) {
		getEditableSideAttributes(SIDE_CUTOUT2).shininess = shininess;
	}

	public final boolean isFullBright(int side) {
		return sideAttributes[side].fullBright;
	}

	public final void setFullBright(int side, boolean fullBright) {
		getEditableSideAttributes(side).fullBright = fullBright;
	}
	
	public final void setFullBright(boolean fullBright) {
		setFullBright(SIDE_ALL, fullBright);
	}

	public final void setFullBrightTop(boolean fullBright) {
		setFullBright(SIDE_TOP, fullBright);
	}

	public final void setFullBrightBottom(boolean fullBright) {
		setFullBright(SIDE_BOTTOM, fullBright);
	}

	public final void setFullBrightSide1(boolean fullBright) {
		setFullBright(SIDE_SIDE1, fullBright);
	}

	public final void setFullBrightSide2(boolean fullBright) {
		setFullBright(SIDE_SIDE2, fullBright);
	}

	public final void setFullBrightSide3(boolean fullBright) {
		setFullBright(SIDE_SIDE3, fullBright);
	}

	public final void setFullBrightSide4(boolean fullBright) {
		setFullBright(SIDE_SIDE4, fullBright);
	}

	public final void setFullBrightInsideTop(boolean fullBright) {
		setFullBright(SIDE_INSIDE_TOP, fullBright);
	}

	public final void setFullBrightInsideBottom(boolean fullBright) {
		setFullBright(SIDE_INSIDE_BOTTOM, fullBright);
	}

	public final void setFullBrightInside1(boolean fullBright) {
		setFullBright(SIDE_INSIDE1, fullBright);
	}

	public final void setFullBrightInside2(boolean fullBright) {
		setFullBright(SIDE_INSIDE2, fullBright);
	}

	public final void setFullBrightInside3(boolean fullBright) {
		setFullBright(SIDE_INSIDE3, fullBright);
	}

	public final void setFullBrightInside4(boolean fullBright) {
		setFullBright(SIDE_INSIDE4, fullBright);
	}

	public final void setFullBrightCutout1(boolean fullBright) {
		setFullBright(SIDE_CUTOUT1, fullBright);
	}

	public final void setFullBrightCutout2(boolean fullBright) {
		setFullBright(SIDE_CUTOUT2, fullBright);
	}

	public final boolean getTextureAlphaTest(int side) {
		return sideAttributes[side].alphaTest;
	}

	public final void setTextureAlphaTest(int side, boolean alphaTest) {
		getEditableSideAttributes(side).alphaTest = alphaTest;
	}

	public final boolean isVideoTexture() {
		return (videoTextureRenderer != null);
	}

	public final boolean isPlaying() {
		return videoTextureRenderer.isPlaying();
	}

	public final void play() {
		videoTextureRenderer.play();
	}

	public final void pause() {
		videoTextureRenderer.pause();
	}

	public final void stop() {
		videoTextureRenderer.stop();
	}

	@Override
	public ArrayList<String> getActions() {
		ArrayList<String> actions = new ArrayList<String>();
		actions.add("new");
		actions.add("edit");
		if (isVideoTexture()) {
			if (isPlaying()) {
				actions.add("pause");
			} else {
				actions.add("play");
			}
			actions.add("stop");
		}
		actions.add("copy");
		actions.add("paste");
		actions.add("import");
		actions.add("export");
		actions.add("delete");
		return actions;
	}

	public final void setFixed(boolean fixed) {
		this.fixed = fixed;
		calculateExtents();
	}

	public final boolean isFixed() {
		return fixed;
	}

	public final void setGroup(int group) {
		this.group = group;
	}

	public final int getGroup() {
		return group;
	}

}