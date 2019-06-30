package com.gallantrealm.myworld.model;

import java.io.IOException;
import java.io.Serializable;
import com.gallantrealm.myworld.FastMath;
import com.gallantrealm.myworld.communication.DataInputStreamX;
import com.gallantrealm.myworld.communication.DataOutputStreamX;
import com.gallantrealm.myworld.communication.Sendable;

/**
 * This class provides a simple fixed vector (starts at the origin). It is used for position and size information.
 */
public class WWVector implements Cloneable, Serializable, Sendable {
	static final long serialVersionUID = 1L;

	public static final float TORADIAN = 0.0174532925f;
	public static final float TODEGREES = 57.29577866f;

	public static final WWVector ZERO_VECTOR = new WWVector(0, 0, 0);

	public float x;
	public float y;
	public float z;

	public WWVector() {
	}

	public WWVector(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public WWVector(WWVector v) {
		this.x = v.x;
		this.y = v.y;
		this.z = v.z;
	}

	@Override
	public final String toString() {
		//String formattedx = String.format("%+.4f", x);
		//String formattedy = String.format("%+.4f", y);
		//String formattedz = String.format("%+.4f", z);
		//return "<" + formattedx + "," + formattedy + "," + formattedz + ">";
		return "<" + x + "," + y + "," + z + ">";
	}

	public final float getX() {
		return x;
	}

	public final float getY() {
		return y;
	}

	public final float getZ() {
		return z;
	}

	/**
	 * Determine the distance between this vector and another, assuming the vectors represent points in threespace.
	 */
	public final float distanceFrom(WWVector v) {
		float dx = v.x - x;
		float dy = v.y - y;
		float dz = v.z - z;
		return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	public final WWVector subtract(WWVector v) {
		x -= v.x;
		y -= v.y;
		z -= v.z;
		return this;
	}

	public final WWVector subtract(float vx, float vy, float vz) {
		x -= vx;
		y -= vy;
		z -= vz;
		return this;
	}

	/**
	 * Cross product another vector with this one.
	 */
	public final WWVector cross(WWVector v) {
		float i = y * v.z - v.y * z;
		float j = x * v.z - v.x * z;
		float k = x * v.y - v.x * y;
		x = i;
		y = j;
		z = k;
		return this;
	}

	public final WWVector cross(float vx, float vy, float vz) {
		float i = y * vz - vy * z;
		float j = x * vz - vx * z;
		float k = x * vy - vx * y;
		x = i;
		y = j;
		z = k;
		return this;
	}

	public final float dot(WWVector v) {
		return x * v.x + y * v.y + z * v.z;
	}

	/**
	 * Get the pan needed to reach the point.
	 */
	public final float getPan() {
		return (float) Math.toDegrees(Math.atan2(x, y));
	}

	/**
	 * Get the tilt needed to reach the point.
	 */
	public final float getTilt() {
		return (float) Math.toDegrees(Math.atan2(z, Math.sqrt(x * x + y * y)));
	}

	/**
	 * Returns the reflection of the vector, given a vector normal to the mirror of reflection.
	 */
	public final WWVector getReflection(WWVector mirror) {
		WWVector mirrorNormal = mirror.clone();
		mirrorNormal.normalize();
		float length = this.length();
		WWVector normal = this.clone();
		normal.normalize();
		WWVector reflection = new WWVector((normal.x + 2 * mirrorNormal.x), (normal.y + 2 * mirrorNormal.y), (normal.z + 2 * mirrorNormal.z));
		reflection.normalize();
		reflection.scale(length);
		return reflection;
	}

	/**
	 * Normalizes this vector.
	 */
	public final WWVector normalize() {
//		if (free) {
//			throw new RuntimeException("Attempting to use an already freed vector");
//		}
		float length = length();
		if (length <= 0.000001) {
			x = 0;
			y = 0;
			z = 0;
			return this;
		}
		x = x / length;
		y = y / length;
		z = z / length;
		return this;
	}

	/**
	 * Returns the distance from the origin of the point represented by the vector.
	 */
	public final float length() {
		return (float) Math.sqrt(x * x + y * y + z * z);
	}

	/**
	 * Add the value of another vector to this vector.
	 */
	public final WWVector add(WWVector vector) {
		x += vector.x;
		y += vector.y;
		z += vector.z;
		return this;
	}

	public final WWVector add(float vx, float vy, float vz) {
		x += vx;
		y += vy;
		z += vz;
		return this;
	}

	/**
	 * Averages another vector into this vector.
	 */
	public final WWVector avg(WWVector vector) {
		x = (x + vector.x) / 2;
		y = (y + vector.y) / 2;
		z = (z + vector.z) / 2;
		return this;
	}

	public final WWVector addRotation(WWVector v) {
		addRotation(v.x, v.y, v.z);
		return this;
	}

//	public final void addRotation(float vattitude, float vbank, float vheading) {
	public final WWVector addRotation(float vheading, float vbank, float vattitude) {

		// Note: heading = z, attitude = x, bank = y

//		float theading = z;
//		float tattitude = x;
//		float tbank = y;
		float theading = x;
		float tattitude = z;
		float tbank = y;

		// convert this to quaternion
		double c1 = Math.cos(Math.toRadians(theading / 2));
		double s1 = Math.sin(Math.toRadians(theading / 2));
		double c2 = Math.cos(Math.toRadians(tattitude / 2));
		double s2 = Math.sin(Math.toRadians(tattitude / 2));
		double c3 = Math.cos(Math.toRadians(tbank / 2));
		double s3 = Math.sin(Math.toRadians(tbank / 2));
		double c1c2 = c1 * c2;
		double s1s2 = s1 * s2;
		double qaw = c1c2 * c3 - s1s2 * s3;
		double qax = c1c2 * s3 + s1s2 * c3;
		double qay = c1 * s2 * c3 + s1 * c2 * s3;
		double qaz = s1 * c2 * c3 - c1 * s2 * s3;

		// convert v to quaternion
		c1 = Math.cos(Math.toRadians(vheading / 2));
		s1 = Math.sin(Math.toRadians(vheading / 2));
		c2 = Math.cos(Math.toRadians(vattitude / 2));
		s2 = Math.sin(Math.toRadians(vattitude / 2));
		c3 = Math.cos(Math.toRadians(vbank / 2));
		s3 = Math.sin(Math.toRadians(vbank / 2));
		c1c2 = c1 * c2;
		s1s2 = s1 * s2;
		double qbw = c1c2 * c3 - s1s2 * s3;
		double qbx = c1c2 * s3 + s1s2 * c3;
		double qby = c1 * s2 * c3 + s1 * c2 * s3;
		double qbz = s1 * c2 * c3 - c1 * s2 * s3;

		// multiply the quaternions (to "add")
		double qcw = qaw * qbw - qax * qbx - qay * qby - qaz * qbz;
		double qcx = qax * qbw + qaw * qbx + qay * qbz - qaz * qby;
		double qcy = qaw * qby - qax * qbz + qay * qbw + qaz * qbx;
		double qcz = qaw * qbz + qax * qby - qay * qbx + qaz * qbw;

//		qcw = qbw;
//		qcx = qbx;
//		qcy = qby;
//		qcz = qbz;

//		// add quaternions
//		double qcw = qaw + qbw;
//		double qcx = qax + qbx;
//		double qcy = qay + qby;
//		double qcz = qaz + qbz;

//		// convert to euler
//		double qcw2 = qcw * qcw;
//		double qcx2 = qcx * qcx;
//		double qcy2 = qcy * qcy;
//		double qcz2 = qcz * qcz;
//		double unitLength = qcw2 + qcx2 + qcy2 + qcz2; // Normalised == 1, otherwise correction divisor.
//		double abcd = qcw * qcx + qcy * qcz;
//		double eps = 1e-7; // TODO: pick from your math lib instead of hardcoding.
//		double pi = 3.14159265358979323846; // TODO: pick from your math lib instead of hardcoding.
//		if (abcd > (0.5 - eps) * unitLength) {
//			theading = (float) Math.toDegrees(2 * Math.atan2(qcy, qcw));
//			tattitude = (float) Math.toDegrees(pi);
//			tbank = 0;
//		} else if (abcd < (-0.5 + eps) * unitLength) {
//			theading = (float) Math.toDegrees(-2 * Math.atan2(qcy, qcw));
//			tattitude = (float) Math.toDegrees(-pi);
//			tbank = 0;
//		} else {
//			double adbc = qcw * qcz - qcx * qcy;
//			double acbd = qcw * qcy - qcx * qcz;
//			theading = (float) Math.toDegrees(Math.atan2(2 * adbc, 1 - 2 * (qcz2 + qcx2)));
//			tattitude = (float) Math.toDegrees(Math.asin(2 * abcd / unitLength));
//			tbank = (float) Math.toDegrees(Math.atan2(2 * acbd, 1 - 2 * (qcy2 + qcx2)));
//		}

		// next try at conversion
		double sqw = qcw * qcw;
		double sqx = qcx * qcx;
		double sqy = qcy * qcy;
		double sqz = qcz * qcz;
		double unit = sqx + sqy + sqz + sqw; // if normalised is one, otherwise is correction factor
		double test = qcx * qcy + qcz * qcw;
		if (test > 0.499 * unit) { // singularity at north pole
			theading = (float) Math.toDegrees(2 * Math.atan2(qcx, qcw));
			tattitude = (float) Math.toDegrees(Math.PI / 2);
			tbank = 0;
		} else if (test < -0.499 * unit) { // singularity at south pole
			theading = (float) Math.toDegrees(-2 * Math.atan2(qcx, qcw));
			tattitude = (float) Math.toDegrees(-Math.PI / 2);
			tbank = (float) Math.toDegrees(0);
		} else {
			theading = (float) Math.toDegrees(Math.atan2(2 * qcy * qcw - 2 * qcx * qcz, sqx - sqy - sqz + sqw));
			tattitude = (float) Math.toDegrees(Math.asin(2 * test / unit));
			tbank = (float) Math.toDegrees(Math.atan2(2 * qcx * qcw - 2 * qcy * qcz, -sqx + sqy - sqz + sqw));
		}

		z = theading;
		x = tattitude;
		y = tbank;
		return this;
	}
	
	public final WWVector rotate(WWVector rotation) {
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

		return this;
	}

	public final WWVector antiRotate(WWVector rotation) {

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

		return this;
	}

	public final WWVector scale(float s) {
		x *= s;
		y *= s;
		z *= s;
		return this;
	}

	public final WWVector scale(float sx, float sy, float sz) {
		x *= sx;
		y *= sy;
		z *= sz;
		return this;
	}
	
	public final WWVector scale(WWVector v) {
		x *= v.x;
		y *= v.y;
		z *= v.z;
		return this;
	}

	public final void set(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public final void setX(float x) {
		this.x = x;
	}

	public final void setY(float y) {
		this.y = y;
	}

	public final void setZ(float z) {
		this.z = z;
	}

	@Override
	public final WWVector clone() {
		return new WWVector(x, y, z);
	}

	public final void copyInto(WWVector v) {
		v.x = x;
		v.y = y;
		v.z = z;
	}

	public final void zero() {
		x = 0;
		y = 0;
		z = 0;
	}

	public final boolean isZero() {
		return x == 0 && y == 0 && z == 0;
	}

	public final boolean isLongerThan(WWVector v) {
		return x * x + y * y + z * z > v.x * v.x + v.y * v.y + v.z * v.z;
	}

	/**
	 * Reduce the length of the vector by the amount specified.
	 */
	public final WWVector reduce(float reduction) {
		float length = length();
		if (length == 0.0f) {
			return this;
		}
		float newLength = FastMath.max(0.0f, length - reduction);
		normalize();
		x = x * newLength;
		y = y * newLength;
		z = z * newLength;
		return this;
	}

	/**
	 * Average-in another vector by the percentage amount. (Using 0.5 as the amount gives an even average.)
	 */
	public final WWVector average(WWVector v, float amount) {
		x = x * (1.0f - amount) + v.x * amount;
		y = y * (1.0f - amount) + v.y * amount;
		z = z * (1.0f - amount) + v.z * amount;
		return this;
	}

	public final WWVector average(WWVector v, WWVector amount) {
		x = x * (1.0f - amount.x) + v.x * amount.x;
		y = y * (1.0f - amount.y) + v.y * amount.y;
		z = z * (1.0f - amount.z) + v.z * amount.z;
		return this;
	}

	public final void send(DataOutputStreamX os) throws IOException {
//		if (free) {
//			throw new RuntimeException("Attempting to use an already freed vector");
//		}
		os.writeFloat(x);
		os.writeFloat(y);
		os.writeFloat(z);
	}

	public final void receive(DataInputStreamX is) throws IOException {
//		if (free) {
//			throw new RuntimeException("Attempting to use an already freed vector");
//		}
		x = is.readFloat();
		y = is.readFloat();
		z = is.readFloat();
	}

}
