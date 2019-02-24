package com.gallantrealm.myworld.communication;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;

/**
 * Subclass of DataInputStream to better handle data types used in the world model.
 */
public class DataInputStreamX extends DataInputStream {

	public DataInputStreamX(InputStream in) {
		super(in);
	}

	public String readString() throws IOException {
		boolean isNull = readBoolean();
		if (isNull) {
			return null;
		}
		String string = readUTF();
		return string;
	}

	public byte[] readByteArray() throws IOException {
		boolean isNull = readBoolean();
		if (isNull) {
			return null;
		}
		byte[] array = new byte[readInt()];
		for (int i = 0; i < array.length; i++) {
			array[i] = readByte();
		}
		return array;
	}

	public int[] readIntArray() throws IOException {
		boolean isNull = readBoolean();
		if (isNull) {
			return null;
		}
		int[] array = new int[readInt()];
		for (int i = 0; i < array.length; i++) {
			array[i] = readInt();
		}
		return array;
	}

	public Sendable readKnownObject(Class objectClass) throws IOException {
		boolean isNull = readBoolean();
		if (isNull) {
			return null;
		}
		Sendable object;
		try {
			object = (Sendable) objectClass.newInstance();
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
		object.receive(this);
		return object;
	}

	public Sendable readObject() throws IOException {
		boolean isNull = readBoolean();
		if (isNull) {
			return null;
		}
		Sendable object;
		try {
			String objectClassName = readUTF();
			Class objectClass = Class.forName(objectClassName);
			object = (Sendable) objectClass.newInstance();
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
		object.receive(this);
		return object;
	}

	public Sendable[] readKnownObjectArray(Class objectClass) throws IOException {
		boolean isNull = readBoolean();
		if (isNull) {
			return null;
		}
		int length = readInt();
		Sendable[] array = (Sendable[]) Array.newInstance(objectClass, length);
		for (int i = 0; i < array.length; i++) {
			array[i] = readKnownObject(objectClass);
		}
		return array;
	}

}
