package com.gallantrealm.myworld.android.renderer.neu;

import com.gallantrealm.myworld.android.renderer.neu.GLSurface;
import com.gallantrealm.myworld.model.WWMesh;
import com.gallantrealm.myworld.model.WWObject;

/**
 * Creates a primitive with a complex surface shape composed from a two dimensional array of rectangles. Three types of
 * meshes are possible: flat, cylindrical, and spherical. A flat mesh can be used for land and other large flat
 * surfaces. Cylindrical is useful for "carved" objects such as statues and limbs of an avatar. Spherical meshes can
 * simulate round objects like the head of an avatar or a model of a planet.
 */
public class GLMesh extends GLObject {

	public static final int BASE_SHAPE_BOX = 0;
	public static final int BASE_SHAPE_CYLINDER = 1;
	public static final int BASE_SHAPE_SPHERE = 2;

	public GLMesh(NewAndroidRenderer renderer, WWMesh mesh, long worldTime) {
		super(renderer, mesh, worldTime);
		if (mesh.getBaseShape() == WWMesh.BASE_SHAPE_BOX) {
			createBoxMesh(mesh.sizeX, mesh.sizeY, mesh.sizeZ, mesh.getCellsX(), mesh.getCellsY(), mesh.getMesh());
		} else if (mesh.getBaseShape() == WWMesh.BASE_SHAPE_CYLINDER) {
			createCylinderMesh(mesh.sizeX, mesh.sizeY, mesh.sizeZ, mesh.getCellsX(), mesh.getCellsY(), mesh.getMesh());
		} else if (mesh.getBaseShape() == WWMesh.BASE_SHAPE_SPHERE) {
			createSphereMesh(mesh.sizeX, mesh.sizeY, mesh.sizeZ, mesh.getCellsX(), mesh.getCellsY(), mesh.getMesh());
		}
	}

	private void createBoxMesh(float sizeX, float sizeY, float sizeZ, int cellsX, int cellsY, float[][] mesh) {

		// Some common values used in calculations
		float sizeXPerCell = sizeX / cellsX;
		float sizeYPerCell = sizeY / cellsY;

		// Create the top. This is the actual mesh.
		GLSurface topGeometry = new GLSurface(cellsX + 1, cellsY + 1, false);
		for (int cy = 0; cy <= cellsY; cy++) {
			for (int cx = 0; cx <= cellsX; cx++) {
				topGeometry.setVertex(cx, cy, cx * sizeXPerCell - sizeX / 2, sizeZ * mesh[cx][cy] - sizeZ / 2, cy * sizeYPerCell - sizeY / 2);
				//topGeometry.setTextureCoordinate(0, i, new float[] { cx / (float) cellsX - 0.5f, cy / (float) cellsY - 0.5f });
			}
		}
		topGeometry.generateNormals();
		adjustTextureCoords(topGeometry, WWObject.SIDE_TOP);
		setSide(WWObject.SIDE_TOP, topGeometry);

		// Create each of the sides. These "edge" the sides of the mesh

		// - side1 (front)
		GLSurface side1Geometry = new GLSurface(cellsX + 1, 2, false);
		for (int cx = 0; cx <= cellsX; cx++) {
			side1Geometry.setVertex(cellsX - cx, 0, cx * sizeXPerCell - sizeX / 2, -sizeZ / 2, sizeY / 2);
			//side1Geometry.setTextureCoordinate(0, i, new float[] { cx / (float) cellsX - 0.5f, -0.5f });
			side1Geometry.setVertex(cellsX - cx, 1, cx * sizeXPerCell - sizeX / 2, sizeZ * mesh[cx][cellsY] - sizeZ / 2, sizeY / 2);
			//side1Geometry.setTextureCoordinate(0, i, new float[] { (cx) / (float) cellsX - 0.5f, mesh[cx][cellsY] - 0.5f });
		}
		side1Geometry.generateNormals();
		adjustTextureCoords(topGeometry, WWObject.SIDE_SIDE1);
		setSide(WWObject.SIDE_SIDE1, side1Geometry);

		// - side2 (right)
		GLSurface side2Geometry = new GLSurface(cellsY + 1, 2, false);
		for (int cy = 0; cy <= cellsY; cy++) {
			side2Geometry.setVertex(cy, 0, sizeX / 2, -sizeZ / 2, cy * sizeYPerCell - sizeY / 2);
			//side2Geometry.setTextureCoordinate(0, i, new float[] { (cellsY - cy) / (float) cellsY - 0.5f, -0.5f });
			side2Geometry.setVertex(cy, 1, sizeX / 2, sizeZ * mesh[cellsX][cy] - sizeZ / 2, cy * sizeYPerCell - sizeY / 2);
			//side2Geometry.setTextureCoordinate(0, i, new float[] { (cellsY - cy) / (float) cellsY - 0.5f, mesh[cellsX][cy] - 0.5f });
		}
		side2Geometry.generateNormals();
		adjustTextureCoords(topGeometry, WWObject.SIDE_SIDE2);
		setSide(WWObject.SIDE_SIDE2, side2Geometry);

		// - side3 (back)
		GLSurface side3Geometry = new GLSurface(cellsX + 1, 2, false);
		for (int cx = 0; cx <= cellsX; cx++) {
			side3Geometry.setVertex(cx, 0, cx * sizeXPerCell - sizeX / 2, -sizeZ / 2, -sizeY / 2);
			//side3Geometry.setTextureCoordinate(0, i, new float[] { (cellsX - cx) / (float) cellsX - 0.5f, -0.5f });
			side3Geometry.setVertex(cx, 1, (cx) * sizeXPerCell - sizeX / 2, sizeZ * mesh[cx][0] - sizeZ / 2, -sizeY / 2);
			//side3Geometry.setTextureCoordinate(0, i, new float[] { (cellsX - cx) / (float) cellsX - 0.5f, mesh[cx][0] - 0.5f });
		}
		side3Geometry.generateNormals();
		adjustTextureCoords(topGeometry, WWObject.SIDE_SIDE3);
		setSide(WWObject.SIDE_SIDE3, side3Geometry);

		// - side4 (left)
		GLSurface side4Geometry = new GLSurface(cellsY + 1, 2, false);
		for (int cy = 0; cy <= cellsY; cy++) {
			side4Geometry.setVertex(cellsY - cy, 0, -sizeX / 2, -sizeZ / 2, cy * sizeYPerCell - sizeY / 2);
			//side4Geometry.setTextureCoordinate(0, i, new float[] { cy / (float) cellsY - 0.5f, -0.5f });
			side4Geometry.setVertex(cellsY - cy, 1, -sizeX / 2, sizeZ * mesh[0][cy] - sizeZ / 2, cy * sizeYPerCell - sizeY / 2);
			//side4Geometry.setTextureCoordinate(0, i, new float[] { cy / (float) cellsY - 0.5f, mesh[0][cy] - 0.5f });
		}
		side4Geometry.generateNormals();
		adjustTextureCoords(topGeometry, WWObject.SIDE_SIDE4);
		setSide(WWObject.SIDE_SIDE4, side4Geometry);

		// Create the bottom. This is simply a rectangle to close the shape
		GLSurface bottomGeometry = new GLSurface(2, 2, false);
		bottomGeometry.setVertex(0, 0, -sizeX / 2, -sizeZ / 2, -sizeY / 2);
		//bottomGeometry.setTextureCoordinate(0, i, new float[] { -0.5f, -0.5f });
		bottomGeometry.setVertex(0, 1, sizeX / 2, -sizeZ / 2, -sizeY / 2);
		//bottomGeometry.setTextureCoordinate(0, i, new float[] { 0.5f, -0.5f });
		bottomGeometry.setVertex(1, 0, -sizeX / 2, -sizeZ / 2, sizeY / 2);
		//bottomGeometry.setTextureCoordinate(0, i, new float[] { 0.5f, 0.5f });
		bottomGeometry.setVertex(1, 1, sizeX / 2, -sizeZ / 2, sizeY / 2);
		//bottomGeometry.setTextureCoordinate(0, i, new float[] { -0.5f, 0.5f });

		bottomGeometry.generateNormals();
		adjustTextureCoords(topGeometry, WWObject.SIDE_BOTTOM);
		setSide(WWObject.SIDE_BOTTOM, bottomGeometry);

	}

	/**
	 * Useful in cases where a point on the mesh has changed value (such as during terraforming)
	 */
	public void updateRendering() {

		WWMesh meshObject = (WWMesh) getObject();
		float[][] mesh = meshObject.getMesh();
		float sizeX = getObject().sizeX;
		float sizeY = getObject().sizeY;
		float sizeZ = getObject().sizeZ;
		int cellsX = meshObject.getCellsX();
		int cellsY = meshObject.getCellsY();
		float sizeXPerCell = sizeX / cellsX;
		float sizeYPerCell = sizeY / cellsY;

		GLSurface topGeometry = getSide(WWObject.SIDE_TOP);

		// Create the top. This is the actual mesh.
		for (int cy = 0; cy <= cellsY; cy++) {
			for (int cx = 0; cx <= cellsX; cx++) {
				topGeometry.setVertex(cx, cy, cx * sizeXPerCell - sizeX / 2, sizeZ * mesh[cx][cy] - sizeZ / 2, cy * sizeYPerCell - sizeY / 2);
				//topGeometry.setTextureCoordinate(0, i, new float[] { cx / (float) cellsX - 0.5f, cy / (float) cellsY - 0.5f });
			}
		}
		topGeometry.generateNormals();
		topGeometry.needsBufferBinding = true;

	}

	private void createCylinderMesh(float sizeX, float sizeY, float sizeZ, int cellsX, int cellsY, float[][] mesh) {

//		int i;
//		float x;
//		float y;
//		float p;
//
//		// Setup normal generator (used later)
//		NormalGenerator ng = new NormalGenerator();
//		GeometryInfo gi;
//
//		// Create the top.
//		Shape3D topShape = new Shape3D();
//		GLSurface topGeometry = new TriangleFanArray(cellsX + 2, GLSurface.COORDINATES | GLSurface.NORMALS | GLSurface.TEXTURE_COORDINATE_2, new int[] { cellsX + 2 });
//		i = 0;
//		topGeometry.setVertex(i, new float[] { 0, sizeZ / 2, 0 });
//		topGeometry.setTextureCoordinate(0, i, new float[] { 0.0f, 0.0f });
//		for (int cx = 0; cx <= cellsX; cx++) {
//			x = (float) Math.sin(2 * Math.PI * cx / cellsX);
//			y = (float) Math.cos(2 * Math.PI * cx / cellsX);
//			p = mesh[cx][cellsY];
//			topGeometry.setVertex(i, new float[] { sizeX * x / 2 * p, sizeZ / 2, sizeY * y / 2 * p });
//			topGeometry.setTextureCoordinate(0, i, new float[] { x / 2 * p, y / 2 * p });
//			i++;
//		}
//		gi = new GeometryInfo(topGeometry);
//		ng.generateNormals(gi);
//		topGeometry = gi.getGLSurface();
//		topShape.addGeometry(topGeometry);
//		primitive.addChild(topShape);
//		primitive.setShape(TOP, topShape);
//
//		// Create the sides. This is the actual mesh.
//		Shape3D sideShape = new Shape3D();
//		GLSurface sideGeometry = new GLSurface(4 * cellsX * cellsY, GLSurface.COORDINATES | GLSurface.NORMALS | GLSurface.TEXTURE_COORDINATE_2);
//		i = 0;
//		for (int cy = 0; cy < cellsY; cy++) {
//			for (int cx = 0; cx < cellsX; cx++) {
//				x = (float) Math.sin(2 * Math.PI * cx / cellsX);
//				y = (float) Math.cos(2 * Math.PI * cx / cellsX);
//				p = mesh[cx][cy];
//				sideGeometry.setVertex(i, new float[] { sizeX * x / 2 * p, sizeZ * cy / cellsY - sizeZ / 2, sizeY * y / 2 * p });
//				sideGeometry.setTextureCoordinate(0, i, new float[] { cx / (float) cellsX - 0.5f, cy / (float) cellsY - 0.5f });
//				i++;
//				x = (float) Math.sin(2 * Math.PI * (cx + 1) / cellsX);
//				y = (float) Math.cos(2 * Math.PI * (cx + 1) / cellsX);
//				p = mesh[(cx + 1) % cellsX][cy];
//				sideGeometry.setVertex(i, new float[] { sizeX * x / 2 * p, sizeZ * cy / cellsY - sizeZ / 2, sizeY * y / 2 * p });
//				sideGeometry.setTextureCoordinate(0, i, new float[] { (cx + 1) / (float) cellsX - 0.5f, cy / (float) cellsY - 0.5f });
//				i++;
//				x = (float) Math.sin(2 * Math.PI * (cx + 1) / cellsX);
//				y = (float) Math.cos(2 * Math.PI * (cx + 1) / cellsX);
//				p = mesh[(cx + 1) % cellsX][cy + 1];
//				sideGeometry.setVertex(i, new float[] { sizeX * x / 2 * p, sizeZ * (cy + 1) / cellsY - sizeZ / 2, sizeY * y / 2 * p });
//				sideGeometry.setTextureCoordinate(0, i, new float[] { (cx + 1) / (float) cellsX - 0.5f, (cy + 1) / (float) cellsY - 0.5f });
//				i++;
//				x = (float) Math.sin(2 * Math.PI * cx / cellsX);
//				y = (float) Math.cos(2 * Math.PI * cx / cellsX);
//				p = mesh[cx][cy + 1];
//				sideGeometry.setVertex(i, new float[] { sizeX * x / 2 * p, sizeZ * (cy + 1) / cellsY - sizeZ / 2, sizeY * y / 2 * p });
//				sideGeometry.setTextureCoordinate(0, i, new float[] { cx / (float) cellsX - 0.5f, (cy + 1) / (float) cellsY - 0.5f });
//				i++;
//			}
//		}
//		gi = new GeometryInfo(sideGeometry);
//		ng.generateNormals(gi);
//		sideGeometry = gi.getGLSurface();
//		sideShape.addGeometry(sideGeometry);
//		primitive.addChild(sideShape);
//		primitive.setShape(SIDE1, sideShape);
//
//		// Create the bottom.
//		Shape3D bottomShape = new Shape3D();
//		GLSurface bottomGeometry = new TriangleFanArray(cellsX + 2, GLSurface.COORDINATES | GLSurface.NORMALS | GLSurface.TEXTURE_COORDINATE_2, new int[] { cellsX + 2 });
//		i = 0;
//		bottomGeometry.setVertex(i, new float[] { 0, -sizeZ / 2, 0 });
//		bottomGeometry.setTextureCoordinate(0, i, new float[] { 0.0f, 0.0f });
//		for (int cx = cellsX; cx >= 0; cx--) {
//			x = (float) Math.sin(2 * Math.PI * cx / cellsX);
//			y = (float) Math.cos(2 * Math.PI * cx / cellsX);
//			p = mesh[cx][0];
//			bottomGeometry.setVertex(i, new float[] { sizeX * x / 2 * p, -sizeZ / 2, sizeY * y / 2 * p });
//			bottomGeometry.setTextureCoordinate(0, i, new float[] { x / 2 * p, y / 2 * p });
//			i++;
//		}
//		gi = new GeometryInfo(bottomGeometry);
//		ng.generateNormals(gi);
//		bottomGeometry = gi.getGLSurface();
//		bottomShape.addGeometry(bottomGeometry);
//		primitive.addChild(bottomShape);
//		primitive.setShape(BOTTOM, bottomShape);

	}

	private void createSphereMesh(float sizeX, float sizeY, float sizeZ, int cellsX, int cellsY, float[][] mesh) {

//		int i;
//		float x;
//		float y;
//		float p;
//
//		// Setup normal generator (used later)
//		NormalGenerator ng = new NormalGenerator();
//		GeometryInfo gi;
//
//		// Create the sides. This is the only surface, and is the actual mesh.
//		Shape3D sideShape = new Shape3D();
//		GLSurface sideGeometry = new GLSurface(4 * cellsX * cellsY, GLSurface.COORDINATES | GLSurface.NORMALS | GLSurface.TEXTURE_COORDINATE_2);
//		i = 0;
//		for (int cy = 0; cy < cellsY; cy++) {
//			for (int cx = 0; cx < cellsX; cx++) {
//				float r = (float) cy / (float) cellsY * (float) Math.PI;
//				float z = -(float) Math.cos(r) / 2.0f;
//				x = (float) Math.sin(2 * Math.PI * cx / cellsX) * (float) Math.sin(r);
//				y = (float) Math.cos(2 * Math.PI * cx / cellsX) * (float) Math.sin(r);
//				p = mesh[cx][cy];
//				sideGeometry.setVertex(i, new float[] { sizeX * x / 2 * p, sizeZ * z * p, sizeY * y / 2 * p });
//				sideGeometry.setTextureCoordinate(0, i, new float[] { cx / (float) cellsX - 0.5f, cy / (float) cellsY - 0.5f });
//				i++;
//				x = (float) Math.sin(2 * Math.PI * (cx + 1) / cellsX) * (float) Math.sin(r);
//				y = (float) Math.cos(2 * Math.PI * (cx + 1) / cellsX) * (float) Math.sin(r);
//				p = mesh[(cx + 1) % cellsX][cy];
//				sideGeometry.setVertex(i, new float[] { sizeX * x / 2 * p, sizeZ * z * p, sizeY * y / 2 * p });
//				sideGeometry.setTextureCoordinate(0, i, new float[] { (cx + 1) / (float) cellsX - 0.5f, cy / (float) cellsY - 0.5f });
//				i++;
//				r = (float) (cy + 1) / (float) cellsY * (float) Math.PI;
//				z = -(float) Math.cos(r) / 2.0f;
//				x = (float) Math.sin(2 * Math.PI * (cx + 1) / cellsX) * (float) Math.sin(r);
//				y = (float) Math.cos(2 * Math.PI * (cx + 1) / cellsX) * (float) Math.sin(r);
//				p = mesh[(cx + 1) % cellsX][cy + 1];
//				sideGeometry.setVertex(i, new float[] { sizeX * x / 2 * p, sizeZ * z * p, sizeY * y / 2 * p });
//				sideGeometry.setTextureCoordinate(0, i, new float[] { (cx + 1) / (float) cellsX - 0.5f, (cy + 1) / (float) cellsY - 0.5f });
//				i++;
//				x = (float) Math.sin(2 * Math.PI * cx / cellsX) * (float) Math.sin(r);
//				y = (float) Math.cos(2 * Math.PI * cx / cellsX) * (float) Math.sin(r);
//				p = mesh[cx][cy + 1];
//				sideGeometry.setVertex(i, new float[] { sizeX * x / 2 * p, sizeZ * z * p, sizeY * y / 2 * p });
//				sideGeometry.setTextureCoordinate(0, i, new float[] { cx / (float) cellsX - 0.5f, (cy + 1) / (float) cellsY - 0.5f });
//				i++;
//			}
//		}
//		gi = new GeometryInfo(sideGeometry);
//		ng.generateNormals(gi);
//		sideGeometry = gi.getGLSurface();
//		sideShape.addGeometry(sideGeometry);
//		primitive.addChild(sideShape);
//		primitive.setShape(SIDE1, sideShape);

	}

}
