package com.gallantrealm.myworld.model;

import com.gallantrealm.myworld.FastMath;
import com.gallantrealm.myworld.client.renderer.IRenderer;

/**
 * Represents a torus (tire) shape. Variants allow for pipes and other ring shapes.
 */
public class WWTorus extends WWSimpleShape {
	static final long serialVersionUID = 1L;

	public WWTorus() {
		super();
	}

	public WWTorus(float sizeX, float sizeY, float sizeZ) {
		super(sizeX, sizeY, sizeZ);
	}

	public void createRendering(IRenderer renderer, long worldTime) {
		rendering = renderer.createTorusRendering(this, worldTime);
	}

	/**
	 * Returns an array of points describing the edges of the torus
	 */
	@Override
	protected WWVector[] getEdgePoints() {
		if (edgePoints == null) {
			// TODO this is copied from cylinder. tune for torus
			float sx2 = sizeX / 2.0f;
			float sy2 = sizeY / 2.0f;
			float sz2 = sizeZ / 2.0f;
			edgePoints = new WWVector[] {
					// - six center side points
					new WWVector(sx2, 0, 0), new WWVector(-sx2, 0, 0), new WWVector(0, sy2, 0), new WWVector(0, -sy2, 0), new WWVector(0, 0, sz2), new WWVector(0, 0, -sz2),
					// - eight corners
					new WWVector(0.7f * sx2, 0.7f * sy2, sz2), new WWVector(-0.7f * sx2, 0.7f * sy2, sz2), new WWVector(0.7f * sx2, -0.7f * sy2, sz2), new WWVector(-0.7f * sx2, -0.7f * sy2, sz2), new WWVector(0.7f * sx2, 0.7f * sy2, -sz2), new WWVector(-0.7f * sx2, 0.7f * sy2, -sz2), new WWVector(0.7f * sx2, -0.7f * sy2, -sz2), new WWVector(-0.7f * sx2, -0.7f * sy2, -sz2),
					// - twelve half edge points
					new WWVector(0.7f * sx2, 0.7f * sy2, 0), new WWVector(-0.7f * sx2, 0.7f * sy2, 0), new WWVector(0.7f * sx2, -0.7f * sy2, 0), new WWVector(-0.7f * sx2, -0.7f * sy2, 0), new WWVector(sx2, 0, sz2), new WWVector(-sx2, 0, sz2), new WWVector(sx2, 0, -sz2), new WWVector(-sx2, 0, -sz2), new WWVector(0, sy2, sz2), new WWVector(0, -sy2, sz2), new WWVector(0, sy2, -sz2), new WWVector(0, -sy2, -sz2) };
		}
		return edgePoints;
	}

	@Override
	public void getPenetration(WWVector point, float[] positionMatrix, long worldTime, WWVector tempPoint, WWVector penetration) {

		// Anti-transform
		tempPoint = point.clone();
		antiTransform(tempPoint, positionMatrix);

		// Scale the point down to unit scale, just to make it easier
		tempPoint.scale(1.0f / sizeX, 1.0f / sizeY, 1.0f / sizeZ);

		tempPoint.scale(1.0f, 1.0f, 0.1875f / 0.5f); // adjust for z ring thickness difference

		// Doesnt overlap if x,y is in cutout area
		if (cutoutStart != 0 || cutoutEnd != 1) {
			float theta = FastMath.atan2(tempPoint.x, tempPoint.y);
			float cutPoint = (1.0f - TODEGREES * theta / 360.0f) % 1.0f;
			if (cutPoint < cutoutStart || cutPoint > cutoutEnd) {
				penetration.zero();
				return;
			}
		}

		// Determine the radius distance. This is the distance to the center of the torus
		float radiusDistance = (float)Math.sqrt(tempPoint.x * tempPoint.x + tempPoint.y * tempPoint.y);

		// Doesnt overlap if x,y is > 0.5 from center or z is > ringThickness from center
//		if (radiusDistance > 0.5 || Math.abs(tempPoint.getZ()) > ringThickness / 2) {
//			return null;
//		}

		// Doesnt overlap if x,y is < 0.5-ringThickness from center (the hole)
//		if (radiusDistance < 0.5 - ringThickness / 2) {
//			return null;
//		}

		// Determine the nearest point on the inner 'ring' of the torus.
		WWVector ringPoint = new WWVector(tempPoint.x / radiusDistance * 0.375f, tempPoint.y / radiusDistance * 0.375f, 0.0f);

		// Determine the distance from the inner ring to the point.  If > ringthickness then the point does not intersect.
		// Adjust for the fact that the torus is stretched in the z dimension
//		WWVector tempPointAdjusted = tempPoint.clone();
//		tempPointAdjusted.z /= 2.75;
//		float ringDistance = ringPoint.distanceFrom(tempPointAdjusted);
//		if (ringDistance > ringThickness / 2) {
//			return null;
//		}

		// Determine the penetration vector by determining the difference between a vector that is 0.25 length and the vector
		// that connects the ring core point to the transformed point.
		WWVector ringCoreDistance = new WWVector(tempPoint.x - ringPoint.x, tempPoint.y - ringPoint.y, tempPoint.z - ringPoint.z);
		WWVector ringCoreThicknessVector = ringCoreDistance.clone();
		ringCoreThicknessVector.normalize();
		ringCoreThicknessVector.scale(0.1875f); // ring thickness (unadjusted in z dimension)
		if (ringCoreDistance.length() >= ringCoreThicknessVector.length()) {
			penetration.zero();
			return;
		}
		ringCoreDistance.copyInto(penetration);
		penetration.subtract(ringCoreThicknessVector);

//		penetration.scale(1.0, 1.0, 0.5 / 0.1875); // unadjust for z ring thickness difference
//		penetration.scale(getSizeX(), getSizeY(), getSizeZ());	
		penetration.scale(Math.min(Math.min(sizeX, sizeY), sizeZ)); // alternate scaling that doesn't distort penetration 
		rotate(penetration, positionMatrix);
	}
}
