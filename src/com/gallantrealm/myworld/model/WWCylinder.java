package com.gallantrealm.myworld.model;

import java.io.IOException;
import java.io.Serializable;

import com.gallantrealm.myworld.FastMath;
import com.gallantrealm.myworld.client.renderer.IRenderer;
import com.gallantrealm.myworld.communication.DataInputStreamX;
import com.gallantrealm.myworld.communication.DataOutputStreamX;

/**
 * Represents a cylinder shape. Variants are cones, helixes.
 */
public class WWCylinder extends WWSimpleShape implements Serializable, Cloneable {
	static final long serialVersionUID = 1L;

	static final int NCIRCLE_VERTS = 16;

	public boolean noTopPenetration;
	public boolean noBottomPenetration;
	public boolean noInsidePenetration;
	public boolean noOutsidePenetration;

	public WWCylinder() {
		super();
	}

	public WWCylinder(float sizeX, float sizeY, float sizeZ) {
		super(sizeX, sizeY, sizeZ);
	}

	@Override
	public void send(DataOutputStreamX os) throws IOException {
		os.writeBoolean(noTopPenetration);
		os.writeBoolean(noBottomPenetration);
		os.writeBoolean(noInsidePenetration);
		os.writeBoolean(noOutsidePenetration);
		super.send(os);
	}

	@Override
	public void receive(DataInputStreamX is) throws IOException {
		noTopPenetration = is.readBoolean();
		noBottomPenetration = is.readBoolean();
		noInsidePenetration = is.readBoolean();
		noOutsidePenetration = is.readBoolean();
		super.receive(is);
	}

	@Override
	public void createRendering(IRenderer renderer, long worldTime) {
		rendering = renderer.createCylinderRendering(this, worldTime);
	}

	/**
	 * Returns an array of points describing the edges of the cylinder
	 */
	@Override
	protected WWVector[] getEdgePoints() {
		if (edgePoints == null) {
			float sx2 = sizeX / 2.0f;
			float sy2 = sizeY / 2.0f;
			float sz2 = sizeZ / 2.0f;
			edgePoints = new WWVector[] {
					// - six center side points, starting with base, then front (for speed)
					new WWVector(0, 0, -sz2), new WWVector(0, -sy2, 0), new WWVector(sx2, 0, 0), new WWVector(-sx2, 0, 0), new WWVector(0, sy2, 0), new WWVector(0, 0, sz2),
					// - eight corners
					new WWVector(0.7f * sx2, 0.7f * sy2, sz2), new WWVector(-0.7f * sx2, 0.7f * sy2, sz2), new WWVector(0.7f * sx2, -0.7f * sy2, sz2), new WWVector(-0.7f * sx2, -0.7f * sy2, sz2), new WWVector(0.7f * sx2, 0.7f * sy2, -sz2), new WWVector(-0.7f * sx2, 0.7f * sy2, -sz2), new WWVector(0.7f * sx2, -0.7f * sy2, -sz2), new WWVector(-0.7f * sx2, -0.7f * sy2, -sz2),
					// - twelve half edge points
					new WWVector(0.7f * sx2, 0.7f * sy2, 0), new WWVector(-0.7f * sx2, 0.7f * sy2, 0), new WWVector(0.7f * sx2, -0.7f * sy2, 0), new WWVector(-0.7f * sx2, -0.7f * sy2, 0), new WWVector(sx2, 0, sz2), new WWVector(-sx2, 0, sz2), new WWVector(sx2, 0, -sz2), new WWVector(-sx2, 0, -sz2), new WWVector(0, sy2, sz2), new WWVector(0, -sy2, sz2), new WWVector(0, sy2, -sz2), new WWVector(0, -sy2, -sz2) };
		}
		return edgePoints;
	}

	/**
	 * Returns a vector giving the amount of penetration of a point within the object, or null if the point does not
	 * penetrate.
	 */
	@Override
	public void getPenetration(WWVector point, WWVector position, WWVector rotation, long worldTime, WWVector tempPoint, WWVector penetration) {

		// Anti-transform
		tempPoint.x = point.x;
		tempPoint.y = point.y;
		tempPoint.z = point.z;
		antiTransform(tempPoint, position, rotation, worldTime);

		// Scale the point down to unit scale, just to make it easier
		tempPoint.scale(1.0f / sizeX, 1.0f / sizeY, 1.0f / sizeZ);

		// Determine the radius distance. This is the distance to the center of the cylinder
		float radiusDistanceSquared = tempPoint.x * tempPoint.x + tempPoint.y * tempPoint.y;

		// Doesn't overlap if x,y is > 0.5 from center or z is > 0.5 from center
		if (radiusDistanceSquared >= 0.25 || tempPoint.z >= 0.5 || tempPoint.z <= -0.5) {
			penetration.zero();
			return;
		}

		float radiusDistance = (float) Math.sqrt(radiusDistanceSquared);

		// Doesnt overlap if x,y is < hollow point
		if (radiusDistance < hollow / 2) {
			penetration.zero();
			return;
		}

		// Doesnt overlap if x,y is in cutout area
		if (cutoutStart != 0 || cutoutEnd != 1) {
			float theta = FastMath.atan2(tempPoint.x, tempPoint.y);
			float cutPoint = (1.0f - TODEGREES * theta / 360.0f) % 1.0f;
			if (cutPoint < cutoutStart || cutPoint > cutoutEnd) {
				penetration.zero();
				return;
			}
		}

		// Choose either the z (top/bottom) or x,y (sides) as the closest side.
		float insidePenetration = noInsidePenetration ? 100 : radiusDistance - hollow / 2;
		float outsidePenetration = noOutsidePenetration ? 100 : 0.5f - radiusDistance;
		float topPenetration = noTopPenetration ? 100 : 0.5f - tempPoint.z;
		float bottomPenetration = noBottomPenetration ? 100 : tempPoint.z + 0.5f;
		if (FastMath.min(insidePenetration, outsidePenetration) * FastMath.min(sizeX, sizeY) < FastMath.min(topPenetration, bottomPenetration) * sizeZ) { // side

			// Choose inside or outside
			if (insidePenetration < outsidePenetration) { // inside
				penetration.set(tempPoint.x, tempPoint.y, 0);
				penetration.scale(insidePenetration);
				penetration.scale(FastMath.min(sizeX, sizeY)); // alternate scaling that doesn't distort penetration
			} else { // outside
//			penetration.set(-tempPoint.x * (0.5 - tempPoint.length()), -tempPoint.y * (0.5 - tempPoint.length()), 0);
				penetration.set(tempPoint.x, tempPoint.y, 0);
				penetration.scale(-outsidePenetration);
				penetration.scale(FastMath.min(sizeX, sizeY)); // alternate scaling that doesn't distort penetration
			}
		} else { // top/bottom
			if (topPenetration < bottomPenetration) { // top
				penetration.set(0.0f, 0.0f, -topPenetration);
			} else { // bottom
				penetration.set(0.0f, 0.0f, bottomPenetration);
			}
			penetration.scale(sizeZ); // alternate scaling that doesn't distort penetration
		}
//		penetration.scale(getSizeX(), getSizeY(), getSizeZ());   // the original scaling, distorted penetration vector
		rotate(penetration, rotation, worldTime);
	}
}
