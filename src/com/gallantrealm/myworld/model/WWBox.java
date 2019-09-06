package com.gallantrealm.myworld.model;

import java.io.IOException;
import com.gallantrealm.myworld.FastMath;
import com.gallantrealm.myworld.client.renderer.IRenderer;
import com.gallantrealm.myworld.communication.DataInputStreamX;
import com.gallantrealm.myworld.communication.DataOutputStreamX;

/**
 * Represents a box shape. Variants are pyramids, prism shapes.
 */
public class WWBox extends WWSimpleShape {
	static final long serialVersionUID = 1L;

	public boolean noXPenetration;
	public boolean noYPenetration;
	public boolean noZPenetration;

	public WWBox() {
		super();
	}

	public WWBox(float sizeX, float sizeY, float sizeZ) {
		super(sizeX, sizeY, sizeZ);
	}

	@Override
	public void send(DataOutputStreamX os) throws IOException {
		os.writeBoolean(noXPenetration);
		os.writeBoolean(noYPenetration);
		os.writeBoolean(noZPenetration);
		super.send(os);
	}

	@Override
	public void receive(DataInputStreamX is) throws IOException {
		noXPenetration = is.readBoolean();
		noYPenetration = is.readBoolean();
		noZPenetration = is.readBoolean();
		super.receive(is);
	}

	public void createRendering(IRenderer renderer, long worldTime) {
		rendering = renderer.createBoxRendering(this, worldTime);
	}

	@Override
	public void getPenetration(WWVector point, WWVector position, WWVector rotation, long worldTime, WWVector tempPoint, WWVector penetration) {

		// Anti-transform
		tempPoint.x = point.x;
		tempPoint.y = point.y;
		tempPoint.z = point.z;
		antiTransform(tempPoint, position, rotation, worldTime);

		// Get possible penetration in each dimension
		float penetrationX = sizeX / 2.0f - (tempPoint.x < 0 ? -tempPoint.x : tempPoint.x);
		float penetrationY = sizeY / 2.0f - (tempPoint.y < 0 ? -tempPoint.y : tempPoint.y);
		float penetrationZ = sizeZ / 2.0f - (tempPoint.z < 0 ? -tempPoint.z : tempPoint.z);

		// If penetration is not occuring in all dimensions, then the point is not penetrating
		if (penetrationX < 0 || penetrationY < 0 || penetrationZ < 0) {
			penetration.zero();
			return;
		}

		// Doesnt overlap if x,y < hollow point
		if (hollow > 0 && Math.abs(tempPoint.x) < hollow * sizeX / 2.0f && Math.abs(tempPoint.y) < hollow * sizeY / 2.0f) {
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

			if (noXPenetration) {
				penetrationX = 100;
			} else if (noYPenetration) {
				penetrationY = 100;
			} else if (noZPenetration) {
				penetrationZ = 100;
			}

			// Choose the dimension with the least penetration as the side that is penetrated
			if (penetrationX < penetrationY && penetrationX < penetrationZ) { // x
				if (tempPoint.x > 0) {
					penetration.set(-penetrationX, 0, 0);
				} else {
					penetration.set(penetrationX, 0, 0);
				}
			} else if (penetrationY < penetrationX && penetrationY < penetrationZ) { // y
				if (tempPoint.y > 0) {
					penetration.set(0, -penetrationY, 0);
				} else {
					penetration.set(0, penetrationY, 0);
				}
			} else { // z
				if (tempPoint.z > 0) {
					penetration.set(0, 0, -penetrationZ);
				} else {
					penetration.set(0, 0, penetrationZ);
				}
			}

		} else {

			// Get possible inner penetration in each dimension
			float innerPenetrationX = Math.abs(tempPoint.x) - sizeX / 2.0f * hollow;
			float innerPenetrationY = Math.abs(tempPoint.y) - sizeY / 2.0f * hollow;

			// Choose the dimension or inner dimension with the least penetration as the side that is penetrated
			if (innerPenetrationY < 0 && innerPenetrationX < penetrationX && innerPenetrationX < penetrationY && innerPenetrationX < penetrationZ) {
				if (tempPoint.x > 0) {
					penetration.set(innerPenetrationX, 0, 0);
				} else {
					penetration.set(-innerPenetrationX, 0, 0);
				}
			} else if (innerPenetrationX < 0 && innerPenetrationY < penetrationX && innerPenetrationY < penetrationY && innerPenetrationY < penetrationZ) {
				if (tempPoint.y > 0) {
					penetration.set(0, innerPenetrationY, 0);
				} else {
					penetration.set(0, -innerPenetrationY, 0);
				}
			} else if (penetrationX < innerPenetrationX && penetrationX < innerPenetrationY && penetrationX < penetrationY && penetrationX < penetrationZ) {
				if (tempPoint.x > 0) {
					penetration.set(-penetrationX, 0, 0);
				} else {
					penetration.set(penetrationX, 0, 0);
				}
			} else if (penetrationY < innerPenetrationX && penetrationY < innerPenetrationY && penetrationY < penetrationX && penetrationY < penetrationZ) {
				if (tempPoint.y > 0) {
					penetration.set(0, -penetrationY, 0);
				} else {
					penetration.set(0, penetrationY, 0);
				}
			} else { // z
				if (tempPoint.z > 0) {
					penetration.set(0, 0, -penetrationZ);
				} else {
					penetration.set(0, 0, penetrationZ);
				}
			}
		}

		rotate(penetration, rotation, worldTime);
	}

	@Override
	public void getPenetration(WWVector point, float[] positionMatrix, long worldTime, WWVector tempPoint, WWVector penetration) {
		// Anti-transform
		tempPoint.x = point.x;
		tempPoint.y = point.y;
		tempPoint.z = point.z;
		antiTransform(tempPoint, positionMatrix);

		// Get possible penetration in each dimension
		float penetrationX = sizeX / 2.0f - (tempPoint.x < 0 ? -tempPoint.x : tempPoint.x);
		float penetrationY = sizeY / 2.0f - (tempPoint.y < 0 ? -tempPoint.y : tempPoint.y);
		float penetrationZ = sizeZ / 2.0f - (tempPoint.z < 0 ? -tempPoint.z : tempPoint.z);

		// If penetration is not occuring in all dimensions, then the point is not penetrating
		if (penetrationX < 0 || penetrationY < 0 || penetrationZ < 0) {
			penetration.zero();
			return;
		}

		// Doesnt overlap if x,y < hollow point
		if (hollow > 0 && Math.abs(tempPoint.x) < hollow * sizeX / 2.0f && Math.abs(tempPoint.y) < hollow * sizeY / 2.0f) {
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

			if (noXPenetration) {
				penetrationX = 100;
			} else if (noYPenetration) {
				penetrationY = 100;
			} else if (noZPenetration) {
				penetrationZ = 100;
			}

			// Choose the dimension with the least penetration as the side that is penetrated
			if (penetrationX < penetrationY && penetrationX < penetrationZ) { // x
				if (tempPoint.x > 0) {
					penetration.set(-penetrationX, 0, 0);
				} else {
					penetration.set(penetrationX, 0, 0);
				}
			} else if (penetrationY < penetrationX && penetrationY < penetrationZ) { // y
				if (tempPoint.y > 0) {
					penetration.set(0, -penetrationY, 0);
				} else {
					penetration.set(0, penetrationY, 0);
				}
			} else { // z
				if (tempPoint.z > 0) {
					penetration.set(0, 0, -penetrationZ);
				} else {
					penetration.set(0, 0, penetrationZ);
				}
			}

		} else {

			// Get possible inner penetration in each dimension
			float innerPenetrationX = Math.abs(tempPoint.x) - sizeX / 2.0f * hollow;
			float innerPenetrationY = Math.abs(tempPoint.y) - sizeY / 2.0f * hollow;

			// Choose the dimension or inner dimension with the least penetration as the side that is penetrated
			if (innerPenetrationY < 0 && innerPenetrationX < penetrationX && innerPenetrationX < penetrationY && innerPenetrationX < penetrationZ) {
				if (tempPoint.x > 0) {
					penetration.set(innerPenetrationX, 0, 0);
				} else {
					penetration.set(-innerPenetrationX, 0, 0);
				}
			} else if (innerPenetrationX < 0 && innerPenetrationY < penetrationX && innerPenetrationY < penetrationY && innerPenetrationY < penetrationZ) {
				if (tempPoint.y > 0) {
					penetration.set(0, innerPenetrationY, 0);
				} else {
					penetration.set(0, -innerPenetrationY, 0);
				}
			} else if (penetrationX < innerPenetrationX && penetrationX < innerPenetrationY && penetrationX < penetrationY && penetrationX < penetrationZ) {
				if (tempPoint.x > 0) {
					penetration.set(-penetrationX, 0, 0);
				} else {
					penetration.set(penetrationX, 0, 0);
				}
			} else if (penetrationY < innerPenetrationX && penetrationY < innerPenetrationY && penetrationY < penetrationX && penetrationY < penetrationZ) {
				if (tempPoint.y > 0) {
					penetration.set(0, -penetrationY, 0);
				} else {
					penetration.set(0, penetrationY, 0);
				}
			} else { // z
				if (tempPoint.z > 0) {
					penetration.set(0, 0, -penetrationZ);
				} else {
					penetration.set(0, 0, penetrationZ);
				}
			}
		}

		rotate(penetration, positionMatrix);
	}
}
