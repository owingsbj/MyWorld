package com.gallantrealm.myworld.model;

import java.io.IOException;

import com.gallantrealm.myworld.client.renderer.IRenderer;
import com.gallantrealm.myworld.communication.DataInputStreamX;
import com.gallantrealm.myworld.communication.DataOutputStreamX;

/**
 * A mesh is a shape with an uneven surface which is formed using a grid of values. A mesh can be a box, where only the
 * top is uneven, a cylinder, where the sides are uneven, or a sphere, where the entire surface is uneven.
 */
public class WWMesh extends WWObject {
	static final long serialVersionUID = 1L;

	public static final int BASE_SHAPE_BOX = 0;
	public static final int BASE_SHAPE_CYLINDER = 1;
	public static final int BASE_SHAPE_SPHERE = 2;

	int baseShape;
	public int cellsX = 16;
	public int cellsY = 16;
	private float[][] mesh = new float[cellsX + 1][cellsY + 1];

	public WWMesh() {
		for (int cy = 0; cy <= cellsY; cy++) {
			for (int cx = 0; cx <= cellsX; cx++) {
				mesh[cx][cy] = ((float) Math.random()) * 0.2f + 0.5f; // some randomness so it is obviously a mesh
			}
		}
	}

	public WWMesh(float sizeX, float sizeY, float sizeZ) {
		super(sizeX, sizeY, sizeZ);
		for (int cy = 0; cy <= cellsY; cy++) {
			for (int cx = 0; cx <= cellsX; cx++) {
				mesh[cx][cy] = ((float) Math.random()) * 0.2f + 0.5f; // some randomness so it is obviously a mesh
			}
		}
	}

	@Override
	public Object clone() {
		WWMesh clone = (WWMesh) super.clone();
		clone.mesh = mesh.clone();
		return clone;
	}

	public final int getBaseShape() {
		return baseShape;
	}

	public final void setBaseShape(int baseShape) {
		this.baseShape = baseShape;
	}

	public final int getCellsX() {
		return cellsX;
	}

	public final int getCellsY() {
		return cellsY;
	}

	public final float[][] getMesh() {
		if (mesh == null) {
			mesh = new float[cellsX + 1][cellsY + 1];
			for (int cy = 0; cy <= cellsY; cy++) {
				for (int cx = 0; cx <= cellsX; cx++) {
					mesh[cx][cy] = ((float) Math.random()) * 0.2f + 0.5f; // some randomness so it is obviously a mesh
				}
			}
		}
		return mesh;
	}

	public final void setMeshSize(int cellsX, int cellsY) {
		int oldCellsX = this.cellsX;
		int oldCellsY = this.cellsY;
		float[][] oldmesh = mesh;
		mesh = new float[cellsX + 1][cellsY + 1];
		for (int cy = 0; cy <= cellsY; cy++) {
			for (int cx = 0; cx <= cellsX; cx++) {
				mesh[cx][cy] = 0.5f; // TODO derive from old mesh
			}
		}
		this.cellsX = cellsX;
		this.cellsY = cellsY;
	}

	public final float getMeshPoint(int cx, int cy) {
		return mesh[cx][cy];
	}

	public final void setMeshPoint(int cx, int cy, float value) {
		if (value > 1.0f) {
			value = 1.0f;
		} else if (value < 0.0f) {
			value = 0.0f;
		}
		mesh[cx][cy] = value;
	}

	public final void raiseMeshPoint(int cx, int cy, float value) {
		if (value > mesh[cx][cy]) {
			setMeshPoint(cx, cy, value);
		}
	}

	public final void lowerMeshPoint(int cx, int cy, float value) {
		if (value < mesh[cx][cy]) {
			setMeshPoint(cx, cy, value);
		}
	}

	@Override
	public void send(DataOutputStreamX os) throws IOException {
		os.writeInt(baseShape);
		os.writeInt(cellsX);
		os.writeInt(cellsY);
		for (int cy = 0; cy <= cellsY; cy++) {
			for (int cx = 0; cx <= cellsX; cx++) {
				os.writeFloat(mesh[cx][cy]);
			}
		}
		super.send(os);
	}

	@Override
	public void receive(DataInputStreamX is) throws IOException {
		baseShape = is.readInt();
		cellsX = is.readInt();
		cellsY = is.readInt();
		mesh = new float[cellsX + 1][cellsY + 1];
		for (int cy = 0; cy <= cellsY; cy++) {
			for (int cx = 0; cx <= cellsX; cx++) {
				mesh[cx][cy] = is.readFloat();
			}
		}
		super.receive(is);
	}

	@Override
	public void createRendering(IRenderer renderer, long worldTime) {
		rendering = renderer.createMeshRendering(this, worldTime);
	}

	@Override
	public void getPenetration(WWVector point, WWVector position, WWVector rotation, long worldTime, WWVector tempPoint, WWVector penetration) {

		// Anti-transform
		tempPoint.x = point.x;
		tempPoint.y = point.y;
		tempPoint.z = point.z;
		antiTransform(tempPoint, position, rotation, worldTime);

		float absTempPointX = tempPoint.x > 0 ? tempPoint.x : -tempPoint.x;
		float absTempPointY = tempPoint.y > 0 ? tempPoint.y : -tempPoint.y;
		float absTempPointZ = tempPoint.z > 0 ? tempPoint.z : -tempPoint.z;

		// Get possible penetration in each dimension
		float penetrationX = sizeX / 2.0f - absTempPointX;
		float penetrationY = sizeY / 2.0f - absTempPointY;
		float penetrationZ;
		if (tempPoint.z > -sizeZ / 2.1f) { // very close to the bottom, so mesh can be very low
			getMeshPenetration(tempPoint, penetration);
			penetrationZ = penetration.z;
		} else {
			penetrationZ = sizeZ / 2.0f - absTempPointZ;
		}

		// If penetration is not occuring in all dimensions, then the point is not penetrating
		if (penetrationX < 0 || penetrationY < 0 || penetrationZ < 0) {
			penetration.zero();
			return;
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
			if (tempPoint.z > -sizeZ / 2.1f) { // to be very low so mesh points can be near 0
				penetration.scale(-1);
			} else {
				penetration.set(0, 0, penetrationZ);
			}
		}

		rotate(penetration, rotation, worldTime);
	}

	/**
	 * Determines the penetration of a point into the mesh. This assumes the point is already antitransformed.
	 */
	private final void getMeshPenetration(WWVector point, WWVector penetration) {
		float px = point.x;
		float py = point.y;
		float pz = point.z;

		// Transform the point to the left, back, lower
		px += sizeX / 2;
		py += sizeY / 2;
		pz += sizeZ / 2;

		// Scale the point down by the object size to make the calculations easier
		px *= 1 / sizeX;
		py *= 1 / sizeY;
		pz *= 1 / sizeZ;

		// Determine the cell that the point is within
		int cellX = (int) (px * cellsX);
		int cellY = (int) (py * cellsY);

		// If not within any cell, then return (zero penetration)
		if (cellX < 0 || cellY < 0 || cellX >= cellsX || cellY >= cellsY) {
			penetration.zero();
			return;
		}

		// Determine the offset within the cell
		float offsetX = px * cellsX - cellX;
		float offsetY = py * cellsY - cellY;

		// Using the four edges of the cell, calculate the penetration
		// See http://mathinsight.org/distance_point_plane
		// Note: Take into account the fact that rendering is using triangle strips. The quads in the mesh
		// are actually specifying triangles.
		float p1 = mesh[cellX][cellY];
		float p2 = mesh[cellX + 1][cellY];
		float p3 = mesh[cellX][cellY + 1];
		float p4 = mesh[cellX + 1][cellY + 1];

		// 1 - determine the normal (penetration == normal after this)
		if (offsetX + offsetY < 1) {
			penetration.set(sizeX / cellsX, 0, sizeZ * (p2 - p1));
			penetration.cross(0, sizeY / cellsY, sizeZ * (p3 - p1));
		} else {
			penetration.set(sizeX / cellsX, 0, sizeZ * (p4 - p3));
			penetration.cross(0, sizeY / cellsY, sizeZ * (p4 - p2));
		}
		penetration.normalize();
		penetration.y *= -1;
		float A = penetration.x;
		float B = penetration.y;
		float C = penetration.z;

		// 2 - determine "D" using p2
		float D = -A * sizeX * (cellX + 1) / cellsX - B * sizeY * cellY / cellsY - C * sizeZ * p2;

		// 3 - determine the distance (note that we wanted it to be signed so no abs)
		float distance = (A * sizeX * px + B * sizeY * py + C * sizeZ * pz + D) / (float) Math.sqrt(A * A + B * B + C * C);

		penetration.scale(-distance);

		// Note: no need to scale the point back up since sizes were multipled above (so as not to distort penetration vector)
	}

	/**
	 * Determines the value of the mesh at a particular point. This can be used to position objects so they reside on
	 * top of the mesh.
	 * 
	 * @param point
	 * @return
	 */
	public final float getMeshValue(WWVector point) {
		// Anti-transform
		WWVector tempPoint = point.clone();
		antiTransform(tempPoint, getPosition(), getRotation(), getWorldTime());
		tempPoint.z = 0;

		float px = tempPoint.x;
		float py = tempPoint.y;
		float pz = tempPoint.z;

		// Transform the point to the left, back, lower
		px += sizeX / 2;
		py += sizeY / 2;
		pz += sizeZ / 2;

		// Scale the point down by the object size to make the calculations easier
		px *= 1 / sizeX;
		py *= 1 / sizeY;
		pz *= 1 / sizeZ;

		// Determine the cell that the point is within
		int cellX = (int) (px * cellsX);
		int cellY = (int) (py * cellsY);

		// If not within any cell, then return zero
		if (cellX < 0 || cellY < 0 || cellX >= cellsX || cellY >= cellsY) {
			return 0;
		}

		// Determine the offset within the cell
		float offsetX = px * cellsX - cellX;
		float offsetY = py * cellsY - cellY;

		// Using the four edges of the cell, calculate the penetration
		// See http://mathinsight.org/distance_point_plane
		// Note: Take into account the fact that rendering is using triangle strips. The quads in the mesh
		// are actually specifying triangles.
		float p1 = mesh[cellX][cellY];
		float p2 = mesh[cellX + 1][cellY];
		float p3 = mesh[cellX][cellY + 1];
		float p4 = mesh[cellX + 1][cellY + 1];

		float value;
		if (offsetX + offsetY < 1) { // the triangle using p3, p2, p1
			float zx = p1 * (1.0f - offsetX) + p2 * offsetX;
			value = zx * (1.0f - offsetY) + p3 * offsetY;
		} else { // the triangle using p4, p3, p2
			float zx = p3 * (1.0f - offsetX) + p4 * offsetX;
			value = p2 * (1.0f - offsetY) + zx * offsetY;
		}

		value = (value - 0.5f) * sizeZ;

		return value;
	}

	/**
	 * Determines the cell (returned in x and y of the vector) at a particular point.
	 * 
	 * @param point
	 * @return
	 */
	public final WWVector getMeshCell(WWVector point) {
		float px = point.x;
		float py = point.y;
		float pz = point.z;

		// Transform the point to the left, back, lower
		px += sizeX / 2;
		py += sizeY / 2;
		pz += sizeZ / 2;

		// Scale the point down by the object size to make the calculations easier
		px *= 1 / sizeX;
		py *= 1 / sizeY;
		pz *= 1 / sizeZ;

		// Determine the cell that the point is within
		int cellX = (int) (px * cellsX);
		int cellY = (int) (py * cellsY);

		return new WWVector(cellX, cellY, 0);
	}

}
