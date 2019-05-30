package com.gallantrealm.myworld.model;

import java.io.IOException;
import java.io.Serializable;

import com.gallantrealm.myworld.FastMath;
import com.gallantrealm.myworld.communication.DataInputStreamX;
import com.gallantrealm.myworld.communication.DataOutputStreamX;
import com.gallantrealm.myworld.communication.Sendable;

/**
 * A simple shape is a primitive that forms the simpler shapes: box, cylinder, sphere, torus, and variants of these due
 * to tapering, shearing, twist, cutout, and hollowing.
 */
public abstract class WWSimpleShape extends WWObject implements Serializable, Cloneable, Sendable {
	static final long serialVersionUID = 1L;

	public float taperX;
	public float taperY;
	public float shearX;
	public float shearY;
	public float hollow;
	public float cutoutStart;
	public float cutoutEnd = 1.0f;
	public float twist;
	public int circleVertices;

	protected WWSimpleShape() {
	}

	protected WWSimpleShape(float sizeX, float sizeY, float sizeZ) {
		super(sizeX, sizeY, sizeZ);
	}

	@Override
	public void send(DataOutputStreamX os) throws IOException {
		os.writeFloat(shearX);
		os.writeFloat(shearY);
		os.writeFloat(taperX);
		os.writeFloat(taperY);
		os.writeFloat(hollow);
		os.writeFloat(cutoutStart);
		os.writeFloat(cutoutEnd);
		os.writeFloat(twist);
		os.writeInt(circleVertices);
		super.send(os);
	}

	@Override
	public void receive(DataInputStreamX is) throws IOException {
		shearX = is.readFloat();
		shearY = is.readFloat();
		taperX = is.readFloat();
		taperY = is.readFloat();
		hollow = is.readFloat();
		cutoutStart = is.readFloat();
		cutoutEnd = is.readFloat();
		twist = is.readFloat();
		circleVertices = is.readInt();
		super.receive(is);
	}

	public final float getShearX() {
		return shearX;
	}

	public void setShearX(float shearX) {
		this.shearX = shearX;
	}

	public final float getShearY() {
		return shearY;
	}

	public void setShearY(float shearY) {
		this.shearY = shearY;
	}
	
	public final void setShear(float[] dims) {
		this.shearX = dims[0];
		this.shearY = dims[1];
	}

	public final float getTaperX() {
		return taperX;
	}

	public void setTaperX(float taperX) {
		this.taperX = taperX;
	}

	public final float getTaperY() {
		return taperY;
	}

	public void setTaperY(float taperY) {
		this.taperY = taperY;
	}

	public final void setTaper(float taperX, float taperY) {
		this.taperX = taperX;
		this.taperY = taperY;
	}

	public final float getHollow() {
		return hollow;
	}

	public void setHollow(float hollow) {
		this.hollow = hollow;
	}

	public final float getCutoutStart() {
		return cutoutStart;
	}

	public final float getCutoutEnd() {
		return cutoutEnd;
	}

	public void setCutoutStart(float cutoutStart) {
		this.cutoutStart = Math.round(FastMath.max(FastMath.min(cutoutStart, 1.0f), 0.0f) * 8.0) / 8.0f;
	}

	public void setCutoutEnd(float cutoutEnd) {
		this.cutoutEnd = Math.round(FastMath.max(FastMath.min(cutoutEnd, 1.0f), 0.0f) * 8.0) / 8.0f;
	}

	public final void setCutout(float start, float end) {
		setCutoutStart(start);
		setCutoutEnd(end);
	}
	
	public final void setCutout(float[] dims) {
		setCutout(dims[0], dims[1]);
	}

	public final float getTwist() {
		return twist;
	}

	public final void setTwist(float twist) {
		this.twist = twist;
	}

	public final int getCircleVertices() {
		return (circleVertices > 0) ? circleVertices : 16;
	}

	public final void setCircleVertices(int vertices) {
		circleVertices = vertices;
	}

}
