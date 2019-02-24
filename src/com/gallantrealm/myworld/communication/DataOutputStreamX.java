package com.gallantrealm.myworld.communication;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * An extension of DataOutputStream to better handle some of the custom data types that are used in the world model.
 */
public class DataOutputStreamX extends DataOutputStream {

	public DataOutputStreamX(OutputStream out) {
		super(out);
	}

	public void writeString(String string) throws IOException {
		boolean isNull = (string == null);
		writeBoolean(isNull);
		if (!isNull) {
			writeUTF(string);
		}
	}

	public void writeByteArray(byte[] array) throws IOException {
		boolean isNull = (array == null);
		writeBoolean(isNull);
		if (!isNull) {
			writeInt(array.length);
			write(array);
		}
	}

	public void writeIntArray(int[] array) throws IOException {
		boolean isNull = (array == null);
		writeBoolean(isNull);
		if (!isNull) {
			writeInt(array.length);
			for (int i = 0; i < array.length; i++) {
				writeInt(array[i]);
			}
		}
	}

	public void writeKnownObject(Sendable object) throws IOException {
		boolean isNull = (object == null);
		writeBoolean(isNull);
		if (!isNull) {
			object.send(this);
		}
	}

	public void writeObject(Sendable object) throws IOException {
		boolean isNull = (object == null);
		writeBoolean(isNull);
		if (!isNull) {
			writeUTF(object.getClass().getName());
			object.send(this);
		}
	}

	public void writeKnownObjectArray(Sendable[] array) throws IOException {
		boolean isNull = (array == null);
		writeBoolean(isNull);
		if (!isNull) {
			writeInt(array.length);
			for (int i = 0; i < array.length; i++) {
				writeKnownObject(array[i]);
			}
		}
	}

}
