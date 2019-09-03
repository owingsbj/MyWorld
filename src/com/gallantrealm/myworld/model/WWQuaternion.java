package com.gallantrealm.myworld.model;

import java.io.IOException;
import java.io.Serializable;
import com.gallantrealm.myworld.communication.DataInputStreamX;
import com.gallantrealm.myworld.communication.DataOutputStreamX;
import com.gallantrealm.myworld.communication.Sendable;

public class WWQuaternion implements Cloneable, Serializable, Sendable {
	private static final long serialVersionUID = 1L;

	float w, x, y, z;

	public WWQuaternion() {
	}

	// create a new object with the given components
	public WWQuaternion(float w, float x, float y, float z) {
		this.w = w;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public WWQuaternion(WWQuaternion q) {
		if (q != null) {
			w = q.w;
			x = q.x;
			y = q.y;
			z = q.z;
		}
	}

	public void set(float w, float x, float y, float z) {
		this.w = w;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public void set(WWQuaternion q) {
		if (q != null) {
			w = q.w;
			x = q.x;
			y = q.y;
			z = q.z;
		}
	}

	// return the quaternion norm
	public float norm() {
		return (float) Math.sqrt(w * w + x * x + y * y + z * z);
	}

	// return the quaternion conjugate
	public WWQuaternion conjugate() {
		return new WWQuaternion(w, -x, -y, -z);
	}

	// return a new Quaternion whose value is (this + b)
	public WWQuaternion plus(WWQuaternion b) {
		WWQuaternion a = this;
		return new WWQuaternion(a.w + b.w, a.x + b.x, a.y + b.y, a.z + b.z);
	}

	public WWQuaternion add(WWQuaternion b) {
		this.w += b.w;
		this.x += b.x;
		this.y += b.y;
		this.z += b.z;
		return this;
	}

	public WWQuaternion multiply(WWQuaternion q) {
		float tw = w * q.w - x * q.x - y * q.y - z * q.z;
		float tx = w * q.x + x * q.w + y * q.z - z * q.y;
		float ty = w * q.y - x * q.z + y * q.w + z * q.x;
		float tz = w * q.z + x * q.y - y * q.x + z * q.w;
		w = tw;
		x = tx;
		y = ty;
		z = tz;
		return this;
	}

	public WWQuaternion invert() {
		float i = w * w + x * x + y * y + z * z;
		w = w / i;
		x = -x / i;
		y = -y / i;
		z = -z / i;
		return this;
	}

	// return a / b
	// we use the definition a * b^-1 (as opposed to b^-1 a)
	public WWQuaternion divide(WWQuaternion q) {
		WWQuaternion qi = new WWQuaternion(q);
		return multiply(qi);
	}

	// return a string representation of the invoking object
	public String toString() {
		return "<" + w + ", " + x + ", " + y + ", " + z + ">";
	}

	@Override
	public void send(DataOutputStreamX os) throws IOException {
		os.writeFloat(w);
		os.writeFloat(x);
		os.writeFloat(y);
		os.writeFloat(z);
	}

	@Override
	public void receive(DataInputStreamX is) throws IOException {
		w = is.readFloat();
		x = is.readFloat();
		y = is.readFloat();
		z = is.readFloat();
	}

	public void toMatrix(float[] mat) {
		float xx = x * x;
		float xy = x * y;
		float xz = x * z;
		float xw = x * w;

		float yy = y * y;
		float yz = y * z;
		float yw = y * w;

		float zz = z * z;
		float zw = z * w;

		mat[0] = 1 - 2 * (yy + zz);
		mat[1] = 2 * (xy - zw);
		mat[2] = 2 * (xz + yw);

		mat[4] = 2 * (xy + zw);
		mat[5] = 1 - 2 * (xx + zz);
		mat[6] = 2 * (yz - xw);

		mat[8] = 2 * (xz - yw);
		mat[9] = 2 * (yz + xw);
		mat[10] = 1 - 2 * (xx + yy);

		mat[3] = mat[7] = mat[11] = mat[12] = mat[13] = mat[14] = 0;
		mat[15] = 1;
	}

	public void rotate(WWVector v) {
		float num12 = x + x;
		float num2 = y + y;
		float num = z + z;
		float num11 = w * num12;
		float num10 = w * num2;
		float num9 = w * num;
		float num8 = x * num12;
		float num7 = x * num2;
		float num6 = x * num;
		float num5 = y * num2;
		float num4 = y * num;
		float num3 = z * num;
		float num15 = ((v.x * ((1f - num5) - num3)) + (v.y * (num7 - num9))) + (v.z * (num6 + num10));
		float num14 = ((v.x * (num7 + num9)) + (v.y * ((1f - num8) - num3))) + (v.z * (num4 - num11));
		float num13 = ((v.x * (num6 - num10)) + (v.y * (num4 + num11))) + (v.z * ((1f - num8) - num5));
		v.x = num15;
		v.y = num14;
		v.z = num13;
	}

	public void antirotate(WWVector v) {

	}

}