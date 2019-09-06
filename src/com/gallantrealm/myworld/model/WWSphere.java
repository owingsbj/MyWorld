package com.gallantrealm.myworld.model;

import com.gallantrealm.myworld.FastMath;
import com.gallantrealm.myworld.client.renderer.IRenderer;

/**
 * Represents a spherical shape. Variants allow for egg shapes, bowls, waterdrop shapes.
 */
public class WWSphere extends WWSimpleShape {
	static final long serialVersionUID = 1L;

	static final int NCIRCLE_VERTS = 16;

	public WWSphere() {
		super();
	}

	public WWSphere(float sizeX, float sizeY, float sizeZ) {
		super(sizeX, sizeY, sizeZ);
	}

	@Override
	public void createRendering(IRenderer renderer, long worldTime) {
		rendering = renderer.createSphereRendering(this, worldTime);
	}

	/**
	 * Returns an array of points describing the edges of the sphere
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
					new WWVector(0.5f * sx2, 0.5f * sy2, 0.5f * sz2), new WWVector(-0.5f * sx2, 0.5f * sy2, 0.5f * sz2), new WWVector(0.5f * sx2, -0.5f * sy2, 0.5f * sz2), new WWVector(-0.5f * sx2, -0.5f * sy2, 0.5f * sz2), new WWVector(0.5f * sx2, 0.5f * sy2, -0.5f * sz2), new WWVector(-0.5f * sx2, 0.5f * sy2, -0.5f * sz2), new WWVector(0.5f * sx2, -0.5f * sy2, -0.5f * sz2), new WWVector(-0.5f * sx2, -0.5f * sy2, -0.5f * sz2),
					// - twelve half edge points
					new WWVector(0.7f * sx2, 0.7f * sy2, 0), new WWVector(-0.7f * sx2, 0.7f * sy2, 0), new WWVector(0.7f * sx2, -0.7f * sy2, 0), new WWVector(-0.7f * sx2, -0.7f * sy2, 0), new WWVector(0.7f * sx2, 0, 0.7f * sz2), new WWVector(-0.7f * sx2, 0, 0.7f * sz2), new WWVector(0.7f * sx2, 0, -0.7f * sz2), new WWVector(-0.7f * sx2, 0, -0.7f * sz2), new WWVector(0, 0.7f * sy2, 0.7f * sz2), new WWVector(0, -0.7f * sy2, 0.7f * sz2), new WWVector(0, 0.7f * sy2, -0.7f * sz2), new WWVector(0, -0.7f * sy2, -0.7f * sz2) };
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

		// Doesnt overlap if normalized point is > 0.5 from center of sphere
		float tempPointLength = tempPoint.length();
		if (tempPointLength > 0.5) {
			penetration.zero();
			return;
		}

		// Doesn't overlap if normalized point is < 0.5*hollow (if hollowed)
		if (tempPointLength < 0.5 * hollow) {
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

		if (hollow == 0) {
			// The penetration is the difference in the length from 1.0, pointing away from the sphere center,
			// and readjusted for the sphere's size
			tempPoint.copyInto(penetration);
			penetration.scale(tempPointLength - 0.5f);
//			penetration.scale(getSizeX(), getSizeY(), getSizeZ());
			penetration.scale(Math.min(Math.min(sizeX, sizeY), sizeZ)); // alternate scaling that doesn't distort penetration 
			rotate(penetration, rotation, worldTime);
		} else {
			float insidePenetrationLength = tempPointLength - 0.5f * hollow;
			float outsidePenetrationLength = 0.5f - tempPointLength;
			if (insidePenetrationLength < outsidePenetrationLength) {
				// The penetration is the difference in the length from 1.0, pointing away from the sphere center,
				// and readjusted for the sphere's size
				tempPoint.copyInto(penetration);
				penetration.scale(insidePenetrationLength);
				penetration.scale(Math.min(Math.min(sizeX, sizeY), sizeZ)); // alternate scaling that doesn't distort penetration 
				rotate(penetration, rotation, worldTime);
			} else {
				// The penetration is the difference in the length from 1.0, pointing away from the sphere center,
				// and readjusted for the sphere's size
				tempPoint.copyInto(penetration);
				penetration.scale(-outsidePenetrationLength);
				penetration.scale(Math.min(Math.min(sizeX, sizeY), sizeZ)); // alternate scaling that doesn't distort penetration 
				rotate(penetration, rotation, worldTime);
			}
		}

	}

	@Override
	public void getPenetration(WWVector point, float[] positionMatrix, long worldTime, WWVector tempPoint, WWVector penetration) {
		// Anti-transform
		tempPoint.x = point.x;
		tempPoint.y = point.y;
		tempPoint.z = point.z;
		antiTransform(tempPoint, positionMatrix);

		// Scale the point down to unit scale, just to make it easier
		tempPoint.scale(1.0f / sizeX, 1.0f / sizeY, 1.0f / sizeZ);

		// Doesnt overlap if normalized point is > 0.5 from center of sphere
		float tempPointLength = tempPoint.length();
		if (tempPointLength > 0.5) {
			penetration.zero();
			return;
		}

		// Doesn't overlap if normalized point is < 0.5*hollow (if hollowed)
		if (tempPointLength < 0.5 * hollow) {
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

		if (hollow == 0) {
			// The penetration is the difference in the length from 1.0, pointing away from the sphere center,
			// and readjusted for the sphere's size
			tempPoint.copyInto(penetration);
			penetration.scale(tempPointLength - 0.5f);
//			penetration.scale(getSizeX(), getSizeY(), getSizeZ());
			penetration.scale(Math.min(Math.min(sizeX, sizeY), sizeZ)); // alternate scaling that doesn't distort penetration 
		} else {
			float insidePenetrationLength = tempPointLength - 0.5f * hollow;
			float outsidePenetrationLength = 0.5f - tempPointLength;
			if (insidePenetrationLength < outsidePenetrationLength) {
				// The penetration is the difference in the length from 1.0, pointing away from the sphere center,
				// and readjusted for the sphere's size
				tempPoint.copyInto(penetration);
				penetration.scale(insidePenetrationLength);
				penetration.scale(Math.min(Math.min(sizeX, sizeY), sizeZ)); // alternate scaling that doesn't distort penetration 
			} else {
				// The penetration is the difference in the length from 1.0, pointing away from the sphere center,
				// and readjusted for the sphere's size
				tempPoint.copyInto(penetration);
				penetration.scale(-outsidePenetrationLength);
				penetration.scale(Math.min(Math.min(sizeX, sizeY), sizeZ)); // alternate scaling that doesn't distort penetration 
			}
		}

		rotate(penetration, positionMatrix);
	}

}
