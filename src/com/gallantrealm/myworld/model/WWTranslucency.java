package com.gallantrealm.myworld.model;

import java.io.IOException;
import com.gallantrealm.myworld.client.renderer.IRenderer;
import com.gallantrealm.myworld.communication.DataInputStreamX;
import com.gallantrealm.myworld.communication.DataOutputStreamX;

/**
 * A translucency is a primitive shape that allows light to pass through but is more diffused the thicker the
 * translucency. This effect is created using layers of parallel planes within the translucency. The texture of the
 * translucency is repeated on each layer, and should be semi-transparent.
 */
public class WWTranslucency extends WWObject {
	static final long serialVersionUID = 1L;

	private float insideLayerDensity = 1;
	private float insideTransparency = 0.9f;
	private int insideColor = 0xffffff;

	public WWTranslucency() {
		setTransparency(SIDE_ALL, 0.9f);
		setFullBright(SIDE_INSIDE1, true);
		setTransparency(SIDE_INSIDE1, 0.9f);
		setFullBright(SIDE_INSIDE2, true);
		setTransparency(SIDE_INSIDE2, 0.9f);
		setFullBright(SIDE_INSIDE3, true);
		setTransparency(SIDE_INSIDE3, 0.9f);
	}

	public float getInsideLayerDensity() {
		return insideLayerDensity;
	}

	public void setInsideLayerDensity(float density) {
		insideLayerDensity = density;
	}

	@Override
	public void send(DataOutputStreamX os) throws IOException {
		super.send(os);
		os.writeFloat(insideLayerDensity);
		os.writeFloat(insideTransparency);
		os.writeInt(insideColor);
	}

	@Override
	public void receive(DataInputStreamX is) throws IOException {
		super.receive(is);
		insideLayerDensity = is.readFloat();
		insideTransparency = is.readFloat();
		insideColor = is.readInt();
	}

	public void createRendering(IRenderer renderer, long worldTime) {
		rendering = renderer.createTranslucencyRendering(this, worldTime);
	}

	public int getInsideColor() {
		return insideColor;
	}

	public void setInsideColor(int insideColor) {
		this.insideColor = insideColor;
	}

	public float getInsideTransparency() {
		return insideTransparency;
	}

	public void setInsideTransparency(float insideTransparency) {
		this.insideTransparency = insideTransparency;
	}

	@Override
	public void getPenetration(WWVector point, float[] positionMatrix, long worldTime, WWVector tempPoint, WWVector penetrationVector) {

		// Anti-transform
		tempPoint = point.clone();
		antiTransform(tempPoint, positionMatrix);

		// Get possible penetration in each dimension
		float penetrationX = sizeX / 2.0f - Math.abs(tempPoint.x);
		float penetrationY = sizeY / 2.0f - Math.abs(tempPoint.y);
		float penetrationZ = sizeZ / 2.0f - Math.abs(tempPoint.z);

		// If penetration is not occuring in all dimensions, then the point is not penetrating
		if (penetrationX < 0 || penetrationY < 0 || penetrationZ < 0) {
			penetrationVector.zero();
			return;
		}

		// Choose the dimension with the least penetration as the side that is penetrated
		if (penetrationX < penetrationY && penetrationX < penetrationZ) { // x
			if (tempPoint.x > 0) {
				penetrationVector.set(-penetrationX, 0, 0);
			} else {
				penetrationVector.set(penetrationX, 0, 0);
			}
		} else if (penetrationY < penetrationX && penetrationY < penetrationZ) { // y
			if (tempPoint.y > 0) {
				penetrationVector.set(0, -penetrationY, 0);
			} else {
				penetrationVector.set(0, penetrationY, 0);
			}
		} else { // z
			if (tempPoint.z > 0) {
				penetrationVector.set(0, 0, -penetrationZ);
			} else {
				penetrationVector.set(0, 0, penetrationZ);
			}
		}

		rotate(penetrationVector, positionMatrix);
	}

}
