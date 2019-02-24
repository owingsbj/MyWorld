package com.gallantrealm.myworld.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * The physical properties of objects. This is contained in a separate class as most of the time an object uses default
 * physical properties. A static final instance, DEFAULT_PHYSICS, is shared by objects that have default physical
 * properties.
 */
public class Physics implements Cloneable, Serializable {
	private static final long serialVersionUID = 1L;

	public static final Physics DEFAULT_PHYSICS = new Physics();
	public boolean physical;
	public float velocityX;
	public float velocityY;
	public float velocityZ;
	public float aMomentumX;
	public float aMomentumY;
	public float aMomentumZ;
	public float density = 0.0f;
	public boolean solid = true;
	public float elasticity;
	public float friction = 1.0f;
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

	public Physics() {
	}

	public void send(ObjectOutputStream os) throws IOException {
		os.writeBoolean(physical);
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
	}

	public void sendPosition(ObjectOutputStream os) throws IOException {
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
	}

	public void receive(ObjectInputStream is) throws IOException, ClassNotFoundException {
		physical = is.readBoolean();
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
	}

	public void receivePosition(ObjectInputStream is) throws IOException {
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
	}

	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}

}
