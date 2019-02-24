package com.gallantrealm.myworld.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.ProtectionDomain;

import com.gallantrealm.myworld.communication.DataInputStreamX;
import com.gallantrealm.myworld.communication.DataOutputStreamX;
import com.gallantrealm.myworld.communication.Sendable;

/**
 * This class contains the attributes associated with a single instance of a behavior within a world object.
 */
public final class BehaviorAttributes implements Serializable, Cloneable, Sendable {
	static final long serialVersionUID = 1L;

	/** The bytecode for the behavior classes. This is shared across all behaviors of the same class. */
	private byte[] behaviorClassBinary;
	private String behaviorClassName;

	/** An instance of the behavior for this object. Transient so it is never serialized. */
	public WWBehavior behavior;

	public BehaviorAttributes() {
	}

	public BehaviorAttributes(byte[] behaviorClassBinary) {
		this.behaviorClassBinary = behaviorClassBinary;
	}

	public BehaviorAttributes(String behaviorClassName) {
		this.behaviorClassName = behaviorClassName;
	}

	public BehaviorAttributes(WWBehavior behavior) {
		this.behavior = behavior;
	}

	/** Override of default read behavior to instantiate the behavior object. */
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		instantiateBehavior();
	}

	/**
	 * Overridden to instantiate a new behavior object rather than clone the existing behavior object.
	 */
	@Override
	public Object clone() {
		try {
			BehaviorAttributes clone = (BehaviorAttributes) super.clone();
			if (behaviorClassBinary != null || behaviorClassName != null) {
				clone.behavior = null;
				clone.instantiateBehavior();
			} else {
				clone.behavior = (WWBehavior) behavior.clone();
			}
			return clone;
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}

	/** Instantiates the behavior object from the behavior class's binary. */
	public void instantiateBehavior() {
		if (behavior == null) {
			try {
				Class behaviorClass = null;
				if (behaviorClassBinary != null) {
					// Need to create a custom classloader here as defineClass is protected.
					ClassLoader classLoader = new URLClassLoader(new URL[] {}) {
						@Override
						public Class findClass(String name) throws ClassNotFoundException {
							Class c = defineClass(null, behaviorClassBinary, 0, behaviorClassBinary.length, (ProtectionDomain) null);
							// Todo: add protection domain to sandbox the behavior
							return c;
						}
					};
					behaviorClass = classLoader.loadClass("dummy");
				} else if (behaviorClassName != null) {
					behaviorClass = this.getClass().getClassLoader().loadClass(behaviorClassName);
				}
				if (behaviorClass != null) {
					behavior = (WWBehavior) behaviorClass.newInstance();
				}
			} catch (ClassNotFoundException e) {
				// doesn't happen (I hope)
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void send(DataOutputStreamX os) throws IOException {
		// TODO Improve sending (doesn't work for UDP)
		os.writeByteArray(behaviorClassBinary);
		os.writeString(behaviorClassName);
	}

	public void receive(DataInputStreamX is) throws IOException {
		behaviorClassBinary = is.readByteArray();
		behaviorClassName = is.readString();
		instantiateBehavior();
	}

}
