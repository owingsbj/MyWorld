package com.gallantrealm.myworld.android.renderer.neu;

import com.gallantrealm.myworld.model.WWBox;
import com.gallantrealm.myworld.model.WWCylinder;
import com.gallantrealm.myworld.model.WWObject;
import com.gallantrealm.myworld.model.WWSimpleShape;
import com.gallantrealm.myworld.model.WWSphere;
import com.gallantrealm.myworld.model.WWTorus;

/**
 * Creates most of the simple primitives. This can draw boxes, cylinders, spheres, and toruses, as well as tapered,
 * sheared, twisted, hollowed, and cutout versions of these, making other shapes such as pyramids, cones, tubes and
 * domes.
 * <p>
 * Like other Primitives, a SimplePrimitive is composed of multiple Shape3D objects, each with an Appearance object.
 * This allows each of the sides of the SimplePrimitive to have a different color and texture. There are several shapes
 * used in all SimplePrimitives:
 * <ul>
 * <li>TOP -- the top of the primitive. For spheres and toruses, there is no top.
 * <li>BOTTOM -- the bottom. For spheres and toruses, there is no bottom.
 * <li>SIDE1, SIDE2, SIDE3, SIDE4 -- the sides of the primitive. For cylinders, spheres and toruses, there is only one
 * side, SIDE1
 * <li>CUT1, CUT2 -- if the primitive is cut (a wedge taken out) this is the shape of each face of the cut
 * <li>INSIDE1, INSIDE2, INSIDE3, INSIDE4 -- if the shape is hollowed, this is the shape of the inside of the hollowed
 * portion. Again, for spheres and toruses, there is only one inner side, INSIDE1.
 */
public class GLSimpleShape extends GLObject {

	public static final int BOX = 0;
	public static final int CYLINDER = 1;
	public static final int SPHERE = 2;
	public static final int TORUS = 3;

	/**
	 * Constructs the PolyPrimitive.
	 * 
	 * @param shapeType
	 *            one of BOX, CYLINDER, SPHERE, TORUS
	 * @param sizeX
	 *            the size of the x dimension
	 * @param sizeY
	 *            the size of the y dimension
	 * @param sizeZ
	 *            the size of the z dimension
	 * @param taperX
	 *            the amount to taper in the x dimension
	 * @param taperY
	 *            the amount to taper in the y dimension
	 * @param shearX
	 *            the amount to shear in the x dimension
	 * @param shearY
	 *            the amount to shear in the y dimension
	 * @param twist
	 *            the amount to twist the shape
	 * @param hollow
	 *            the amount to hollow
	 * @param cutStart
	 *            the starting point of cutout
	 * @param cutEnd
	 *            the ending point of cutout
	 */
	public GLSimpleShape(NewAndroidRenderer renderer, WWSimpleShape object, long worldTime) {
		super(renderer, object, worldTime);

		float sizeX = object.sizeX;
		float sizeY = object.sizeY;
		float sizeZ = object.sizeZ;
		float taperX = object.getTaperX();
		float taperY = object.getTaperY();
		float shearX = object.getShearX();
		float shearY = object.getShearY();
		float twist = object.getTwist();
		float cutStart = object.getCutoutStart();
		float cutEnd = object.getCutoutEnd();
		float hollow = object.getHollow();
		int nCircleVertices = object.getCircleVertices();
		
		// optimize to share geometry
		String geometryMapKey = null;
		if (!object.fixed) {
			geometryMapKey = object.getClass().getSimpleName() + " " + sizeX + " " + sizeY + " " + sizeZ + " " + taperX + " " + taperY + " " + shearX + " " + shearY + " " + twist + " " + cutStart + " " + cutEnd + " " + hollow;
			if (renderer.geometryCache.containsKey(geometryMapKey)) {
				sides = renderer.geometryCache.get(geometryMapKey);
				return;
			}
		}

		// Develop the base polygon. This will be used for the bottom and top, then swept up to create the sides.
		// Also develop the "core". This is the center shape of the object, from which hollowing is centered.
		Point2f[] polygon = null;
		Point2f[] core = null;
		Point2f origin = new Point2f(0.0f, 0.0f); // used alot
		boolean hasEnds = false;
		boolean smoothSides = false;
		if (object instanceof WWBox) {
			if (twist == 0 && cutStart == 0 && cutEnd == 1) { // simple box needing only four cells
				polygon = new Point2f[] { new Point2f(-0.5f, -0.5f), new Point2f(0.5f, -0.5f), new Point2f(0.5f, 0.5f), new Point2f(-0.5f, 0.5f) };
				core = new Point2f[] { origin, origin, origin, origin };
			} else { // more complex box
				polygon = new Point2f[] { new Point2f(-0.5f, -0.5f), new Point2f(-0.25f, -0.5f), new Point2f(0.0f, -0.5f), new Point2f(0.25f, -0.5f), new Point2f(0.5f, -0.5f), new Point2f(0.5f, -0.25f), new Point2f(0.5f, 0.0f), new Point2f(0.5f, 0.25f), new Point2f(0.5f, 0.5f), new Point2f(0.25f, 0.5f), new Point2f(0.0f, 0.5f), new Point2f(-0.25f, 0.5f), new Point2f(-0.5f, 0.5f), new Point2f(-0.5f, 0.25f), new Point2f(-0.5f, 0.0f), new Point2f(-0.5f, -0.25f) };
				core = new Point2f[] { origin, origin, origin, origin, origin, origin, origin, origin, origin, origin, origin, origin, origin, origin, origin, origin };
			}
			hasEnds = true;
			smoothSides = false;
		} else if (object instanceof WWCylinder) {
			polygon = new Point2f[nCircleVertices];
			core = new Point2f[nCircleVertices];
			for (int i = 0; i < nCircleVertices; i++) {
				double r = (double) i / (float) nCircleVertices * 2.0 * Math.PI;
				polygon[i] = new Point2f((float) (Math.sin(-r) / 2.0), (float) (Math.cos(r) / 2.0));
				core[i] = origin;
			}
			hasEnds = true;
			smoothSides = true;
		} else if (object instanceof WWSphere) {
			polygon = new Point2f[nCircleVertices];
			core = new Point2f[nCircleVertices];
			for (int i = 0; i < nCircleVertices; i++) {
				double r = (double) i / (float) nCircleVertices * 2.0 * Math.PI;
				polygon[i] = new Point2f((float) (Math.sin(-r) / 2.0), (float) (Math.cos(r) / 2.0));
				core[i] = origin;
			}
			hasEnds = false;
			smoothSides = true;
		} else if (object instanceof WWTorus) {
			polygon = new Point2f[nCircleVertices];
			core = new Point2f[nCircleVertices];
			for (int i = 0; i < nCircleVertices; i++) {
				double r = (double) i / (float) nCircleVertices * 2.0 * Math.PI;
				polygon[i] = new Point2f((float) (Math.sin(-r) / 2.0), (float) (Math.cos(r) / 2.0));
				core[i] = new Point2f(0.75f * polygon[i].x, 0.75f * polygon[i].y);
			}
			hasEnds = false;
			smoothSides = true;
		}
		int nvertices = polygon.length;

		// Develop the sweepPath and sweepRadius. These are used to adjust the
		// shape as it is swept.
		// Also, set the hollowFactor. This tells how much the hollowing impacts
		// each dimension (x, y, z).
		Point3f[] sweepPath = null;
		Point2f[] sweepRadius = null;
		Point3f[] hollowPath = null;
		if (object instanceof WWBox || object instanceof WWCylinder) {
			if (object.getTwist() == 0.0) {
				sweepPath = new Point3f[] { new Point3f(0.0f, 0.0f, -0.5f), new Point3f(0.0f, 0.0f, 0.5f) };
				sweepRadius = new Point2f[] { new Point2f(1.0f, 1.0f), new Point2f(1.0f, 1.0f) };
			} else {
				sweepPath = new Point3f[nCircleVertices + 1];
				sweepRadius = new Point2f[nCircleVertices + 1];
				for (int i = 0; i < nCircleVertices + 1; i++) {
					float z = (float) i / (float) nCircleVertices;
					sweepPath[i] = new Point3f(0.0f, 0.0f, z - 0.5f);
					sweepRadius[i] = new Point2f(1.0f, 1.0f);
				}
			}
		} else if (object instanceof WWSphere) {
			sweepPath = new Point3f[nCircleVertices + 1];
			sweepRadius = new Point2f[nCircleVertices + 1];
			for (int i = 0; i < nCircleVertices + 1; i++) {
				float r = (float) i / (float) nCircleVertices * (float) Math.PI;
				float z = -(float) Math.cos(r) / 2.0f;
				sweepPath[i] = new Point3f(0.0f, 0.0f, z);
				sweepRadius[i] = new Point2f((float) Math.sin(r), (float) Math.sin(r));
			}
		} else if (object instanceof WWTorus) {
			sweepPath = new Point3f[2 * nCircleVertices + 1];
			for (int i = 0; i < 2 * nCircleVertices + 1; i++) {
				float r = (float) i / (float) nCircleVertices * (float) Math.PI;
				float z = -(float) Math.cos(r) / 2.0f;
				sweepPath[i] = new Point3f(0.0f, 0.0f, z);
			}
			sweepRadius = new Point2f[2 * nCircleVertices + 1];
			for (int i = 0; i < nCircleVertices + 1; i++) {
				float r = (float) i / (float) nCircleVertices * (float) Math.PI;
				float z = -(float) Math.cos(r) / 2.0f;
				sweepRadius[i] = new Point2f(
				//						(float) (0.5 + 0.25*Math.sin(r)), 
				//						(float) (0.5 + 0.25*Math.sin(r)));
						(float) (0.75 + 0.375 * Math.sin(r)), (float) (0.75 + 0.375 * Math.sin(r)));
			}
			for (int i = nCircleVertices + 1; i < 2 * nCircleVertices + 1; i++) {
				float r = (float) (i - nCircleVertices) / (float) nCircleVertices * (float) Math.PI;
				float z = (float) Math.cos(r) / 2.0f;
				sweepRadius[i] = new Point2f(
				//						(float) (0.5 - 0.25*Math.sin(r)), 
				//						(float) (0.5 - 0.25*Math.sin(r)));
						(float) (0.75 - 0.375 * Math.sin(r)), (float) (0.75 - 0.375 * Math.sin(r)));
			}
		}

		// Set up the amount of hollowing to apply to each dimension
		float thollow = Math.max(hollow, 0.00001f); // so a grid is actually formed in all cases
		float hollowx = 0.0f;
		float hollowy = 0.0f;
		float hollowz = 0.0f;
		if (object instanceof WWBox || object instanceof WWCylinder) {
			hollowx = thollow;
			hollowy = thollow;
			hollowz = 1.0f; // always cut through top to bottom;
		} else {
			hollowx = thollow;
			hollowy = thollow;
			hollowz = thollow;
		}

		int firstVertex = (int) (object.getCutoutStart() * polygon.length);
		int lastVertex = (int) (object.getCutoutEnd() * polygon.length - 0.0001);

		// --------------------------
		// Create the base.
		// --------------------------

		if (hasEnds) {
			GLSurface baseGeometry;
			if (polygon.length == 4 && twist == 0 && cutStart == 0 && cutEnd == 1 && hollow == 0) { // simple box
				baseGeometry = new GLSurface(2, 2, false);
				baseGeometry.setVertex(0, 0, sweepPath[0].x + polygon[0].x * sweepRadius[0].x, sweepPath[0].z, sweepPath[0].y + polygon[0].y * sweepRadius[0].y);
				baseGeometry.setVertex(0, 1, sweepPath[0].x + polygon[1].x * sweepRadius[0].x, sweepPath[0].z, sweepPath[0].y + polygon[1].y * sweepRadius[0].y);
				baseGeometry.setVertex(1, 1, sweepPath[0].x + polygon[2].x * sweepRadius[0].x, sweepPath[0].z, sweepPath[0].y + polygon[2].y * sweepRadius[0].y);
				baseGeometry.setVertex(1, 0, sweepPath[0].x + polygon[3].x * sweepRadius[0].x, sweepPath[0].z, sweepPath[0].y + polygon[3].y * sweepRadius[0].y);
			} else {
				baseGeometry = new GLSurface((lastVertex - firstVertex + 2), 2, false);
				for (int v = firstVertex; v <= lastVertex + 1; v++) {
					if (v < polygon.length) {
						baseGeometry.setVertex(v - firstVertex, 0, sweepPath[0].x + polygon[v].x * sweepRadius[0].x * hollowx, sweepPath[0].z, sweepPath[0].y + polygon[v].y * sweepRadius[0].y * hollowy);
						//baseGeometry.setTextureCoordinate(0, i - 1, new float[] { sweepPath[0].x + polygon[v].x * sweepRadius[0].x * hollowx, sweepPath[0].y + polygon[v].y * sweepRadius[0].y * hollowy });
						baseGeometry.setVertex(v - firstVertex, 1, sweepPath[0].x + polygon[v].x * sweepRadius[0].x, sweepPath[0].z, sweepPath[0].y + polygon[v].y * sweepRadius[0].y);
						//baseGeometry.setTextureCoordinate(0, i - 1, new float[] { sweepPath[0].x + polygon[v].x * sweepRadius[0].x, sweepPath[0].y + polygon[v].y * sweepRadius[0].y });
					} else {
						baseGeometry.setVertex(v - firstVertex, 0, sweepPath[0].x + polygon[0].x * sweepRadius[0].x * hollowx, sweepPath[0].z, sweepPath[0].y + polygon[0].y * sweepRadius[0].y * hollowy);
						//baseGeometry.setTextureCoordinate(0, i - 1, new float[] { sweepPath[0].x + polygon[0].x * sweepRadius[0].x * hollowx, sweepPath[0].y + polygon[0].y * sweepRadius[0].y * hollowy });
						baseGeometry.setVertex(v - firstVertex, 1, sweepPath[0].x + polygon[0].x * sweepRadius[0].x, sweepPath[0].z, sweepPath[0].y + polygon[0].y * sweepRadius[0].y);
						//baseGeometry.setTextureCoordinate(0, i - 1, new float[] { sweepPath[0].x + polygon[0].x * sweepRadius[0].x, sweepPath[0].y + polygon[0].y * sweepRadius[0].y });
					}
				}
			}
			adjustGeometry(baseGeometry, sizeX, sizeY, sizeZ, taperX, taperY, shearX, shearY, twist);
			adjustTextureCoords(baseGeometry, WWObject.SIDE_BOTTOM);
			baseGeometry.generateNormals();
			setSide(WWObject.SIDE_BOTTOM, baseGeometry);
		}

		// --------------------------
		// Create the sides
		// --------------------------

		int nsweeps = sweepPath.length;
		if (smoothSides) {

			// For smooth surfaces there is only one side. This is so that normals will be properly
			// created for when the sides are smooth.
			GLSurface sidesGeometry = new GLSurface(lastVertex - firstVertex + 2, nsweeps, true);
			int vertex = 0;
			for (int i = 0; i < nsweeps; i++) {
				for (int v = firstVertex; v <= lastVertex + 1; v++) {
					if (v < polygon.length) {
						sidesGeometry.setVertex(v - firstVertex, i, sweepPath[i].x + polygon[v].x * sweepRadius[i].x, sweepPath[i].z, sweepPath[i].y + polygon[v].y * sweepRadius[i].y);
						//sidesGeometry.setTextureCoordinate(0, vertex - 1, new float[] { -(float) v / nvertices - 0.5f, sweepPath[i].z });
					} else {
						sidesGeometry.setVertex(v - firstVertex, i, sweepPath[i].x + polygon[0].x * sweepRadius[i].x, sweepPath[i].z, sweepPath[i].y + polygon[0].y * sweepRadius[i].y);
						//sidesGeometry.setTextureCoordinate(0, vertex - 1, new float[] { -(float) v / nvertices - 0.5f, sweepPath[i].z });
					}
				}
			}
			adjustGeometry(sidesGeometry, sizeX, sizeY, sizeZ, taperX, taperY, shearX, shearY, twist);
			sidesGeometry.generateNormals(firstVertex == 0 && lastVertex + 1 == polygon.length, false);
			adjustTextureCoords(sidesGeometry, WWObject.SIDE_SIDE1);
			setSide(WWObject.SIDE_SIDE1, sidesGeometry);

		} else {

			// For flat sides, split the vertices up evenly for the number of sides
			// currently assuming 4 sides
			int nsides = 4;
			int verticesPerSide = nvertices / nsides;
			for (int side = 0; side < nsides; side++) {
				int startVertex = Math.max(firstVertex, side * verticesPerSide);
				int endVertex = Math.min(lastVertex, (side + 1) * verticesPerSide - 1);
				if (startVertex <= endVertex) {
					GLSurface sideGeometry = new GLSurface(endVertex - startVertex + 2, nsweeps, false);
					int vertex = 0;
					for (int v = startVertex; v <= endVertex + 1; v++) {
						for (int i = 0; i < nsweeps; i++) {
							if (v < polygon.length) {
								sideGeometry.setVertex(v - startVertex, i, sweepPath[i].x + polygon[v].x * sweepRadius[i].x, sweepPath[i].z, sweepPath[i].y + polygon[v].y * sweepRadius[i].y);
								//sideGeometry.setTextureCoordinate(0, vertex - 1, new float[] { -4.0f * (v - startVertex) / nvertices + 0.5f, sweepPath[i].z });
							} else {
								sideGeometry.setVertex(v - startVertex, i, sweepPath[i].x + polygon[0].x * sweepRadius[i].x, sweepPath[i].z, sweepPath[i].y + polygon[0].y * sweepRadius[i].y);
								//sideGeometry.setTextureCoordinate(0, vertex - 1, new float[] { -4.0f * (v - startVertex) / nvertices + 0.5f, sweepPath[i].z });

							}
						}
					}
					adjustGeometry(sideGeometry, sizeX, sizeY, sizeZ, taperX, taperY, shearX, shearY, twist);
					sideGeometry.generateNormals();
					adjustTextureCoords(sideGeometry, WWObject.SIDE_SIDE1 + side);
					setSide(WWObject.SIDE_SIDE1 + side, sideGeometry);
				}
			}
		}

		// --------------------------
		// Create the insides
		// --------------------------

		if (hollow != 0.0) {

			if (smoothSides) {

				// For smooth surfaces there is only one side. This is so that normals will be properly
				// created for when the sides are smooth.
				GLSurface insidesGeometry = new GLSurface(lastVertex - firstVertex + 2, nsweeps, true);
				int vertex = 0;
				for (int i = 0; i < nsweeps; i++) {
					for (int v = firstVertex; v <= lastVertex + 1; v++) {
						if (v < polygon.length) {
							insidesGeometry.setVertex(lastVertex - v + 1, i, sweepPath[i].x + core[v].x + (polygon[v].x * sweepRadius[i].x - core[v].x) * hollowx, sweepPath[i].z * hollowz, sweepPath[i].y + core[v].y + (polygon[v].y * sweepRadius[i].y - core[v].y) * hollowy);
							//insidesGeometry.setTextureCoordinate(0, vertex - 1, new float[] { (float) v / (float) nvertices - 0.5f, sweepPath[i].z });
						} else {
							insidesGeometry.setVertex(lastVertex - v + 1, i, sweepPath[i].x + core[0].x + (polygon[0].x * sweepRadius[i].x - core[0].x) * hollowx, sweepPath[i].z * hollowz, sweepPath[i].y + core[0].y + (polygon[0].y * sweepRadius[i].y - core[0].y) * hollowy);
							//insidesGeometry.setTextureCoordinate(0, vertex - 1, new float[] { (float) v / (float) nvertices - 0.5f, sweepPath[i].z });
						}
					}
				}
				adjustGeometry(insidesGeometry, sizeX, sizeY, sizeZ, taperX, taperY, shearX, shearY, twist);
				insidesGeometry.generateNormals();
				adjustTextureCoords(insidesGeometry, WWObject.SIDE_INSIDE1);
				setSide(WWObject.SIDE_INSIDE1, insidesGeometry);

			} else {

				// For flat sides, split the vertices up evenly for the number of sides
				// currently assuming 4 sides
				int nsides = 4;
				int verticesPerSide = nvertices / nsides;
				for (int side = 0; side < nsides; side++) {
					int startVertex = Math.max(firstVertex, side * verticesPerSide);
					int endVertex = Math.min(lastVertex, (side + 1) * verticesPerSide - 1);
					if (startVertex <= endVertex) {
						GLSurface insideGeometry = new GLSurface(endVertex - startVertex + 2, nsweeps, false);
						int vertex = 0;
						for (int v = startVertex; v <= endVertex + 1; v++) {
							for (int i = 0; i < nsweeps; i++) {
								if (v < polygon.length) {
									insideGeometry.setVertex(endVertex - v + 1, i, sweepPath[i].x + polygon[v].x * sweepRadius[i].x * hollowx, sweepPath[i].z * hollowz, sweepPath[i].y + polygon[v].y * sweepRadius[i].y * hollowy);
									//insideGeometry.setTextureCoordinate(0, vertex - 1, new float[] { 4.0f * (v - startVertex) / nvertices - 0.5f, sweepPath[i].z });
								} else {
									insideGeometry.setVertex(endVertex - v + 1, i, sweepPath[i].x + polygon[0].x * sweepRadius[i].x * hollowx, sweepPath[i].z * hollowz, sweepPath[i].y + polygon[0].y * sweepRadius[i].y * hollowy);
									//insideGeometry.setTextureCoordinate(0, vertex - 1, new float[] { 4.0f * (v - startVertex) / nvertices - 0.5f, sweepPath[i].z });
								}
							}
						}
						adjustGeometry(insideGeometry, sizeX, sizeY, sizeZ, taperX, taperY, shearX, shearY, twist);
						insideGeometry.generateNormals();
						adjustTextureCoords(insideGeometry, WWObject.SIDE_INSIDE1 + side);
						setSide(WWObject.SIDE_INSIDE1 + side, insideGeometry);
					}
				}
			}

		}

		// --------------------------
		// Create the cutouts
		// --------------------------

		if (cutStart != 0.0 || cutEnd != 1.0) {

			GLSurface cutout1Geometry = new GLSurface(2, nsweeps, false);
			int vertex = 0;
			int v = firstVertex;
			for (int i = 0; i < nsweeps; i++) {
				cutout1Geometry.setVertex(0, i, sweepPath[i].x + core[v].x + (polygon[v].x * sweepRadius[i].x - core[v].x) * hollowx, sweepPath[i].z * hollowz, sweepPath[i].y + core[v].y + (polygon[v].y * sweepRadius[i].y - core[v].y) * hollowy);
				//cutout1Geometry.setTextureCoordinate(0, vertex - 1, new float[] { hollow - 0.5f, sweepPath[i].z });
				cutout1Geometry.setVertex(1, i, sweepPath[i].x + polygon[v].x * sweepRadius[i].x, sweepPath[i].z, sweepPath[i].y + polygon[v].y * sweepRadius[i].y);
				//cutout1Geometry.setTextureCoordinate(0, vertex - 1, new float[] { 0.5f, sweepPath[i].z });
			}
			adjustGeometry(cutout1Geometry, sizeX, sizeY, sizeZ, taperX, taperY, shearX, shearY, twist);
			cutout1Geometry.generateNormals();
			adjustTextureCoords(cutout1Geometry, WWObject.SIDE_CUTOUT1);
			setSide(WWObject.SIDE_CUTOUT1, cutout1Geometry);

			GLSurface cutout2Geometry = new GLSurface(2, nsweeps, false);
			vertex = 0;
			v = lastVertex + 1;
			if (v == polygon.length) {
				v = 0;
			}
			for (int i = 0; i < nsweeps; i++) {
				cutout2Geometry.setVertex(1, i, sweepPath[i].x + core[v].x + (polygon[v].x * sweepRadius[i].x - core[v].x) * hollowx, sweepPath[i].z * hollowz, sweepPath[i].y + core[v].y + (polygon[v].y * sweepRadius[i].y - core[v].y) * hollowy);
				//cutout2Geometry.setTextureCoordinate(0, vertex - 1, new float[] { hollow - 0.5f, sweepPath[i].z });
				cutout2Geometry.setVertex(0, i, sweepPath[i].x + polygon[v].x * sweepRadius[i].x, sweepPath[i].z, sweepPath[i].y + polygon[v].y * sweepRadius[i].y);
				//cutout2Geometry.setTextureCoordinate(0, vertex - 1, new float[] { hollow - 0.5f, sweepPath[i].z });
			}
			adjustGeometry(cutout2Geometry, sizeX, sizeY, sizeZ, taperX, taperY, shearX, shearY, twist);
			cutout2Geometry.generateNormals();
			adjustTextureCoords(cutout2Geometry, WWObject.SIDE_CUTOUT2);
			setSide(WWObject.SIDE_CUTOUT2, cutout2Geometry);
		}

		// --------------------------
		// Create the top
		// --------------------------

		if (hasEnds) {
			GLSurface topGeometry;
			if (polygon.length == 4 && twist == 0 && cutStart == 0 && cutEnd == 1 && hollow == 0) { // simple box
				topGeometry = new GLSurface(2, 2, false);
				topGeometry.setVertex(0, 0, sweepPath[nsweeps - 1].x + polygon[0].x * sweepRadius[nsweeps - 1].x, sweepPath[nsweeps - 1].z, sweepPath[nsweeps - 1].y + polygon[0].y * sweepRadius[nsweeps - 1].y);
				topGeometry.setVertex(1, 0, sweepPath[nsweeps - 1].x + polygon[1].x * sweepRadius[nsweeps - 1].x, sweepPath[nsweeps - 1].z, sweepPath[nsweeps - 1].y + polygon[1].y * sweepRadius[nsweeps - 1].y);
				topGeometry.setVertex(1, 1, sweepPath[nsweeps - 1].x + polygon[2].x * sweepRadius[nsweeps - 1].x, sweepPath[nsweeps - 1].z, sweepPath[nsweeps - 1].y + polygon[2].y * sweepRadius[nsweeps - 1].y);
				topGeometry.setVertex(0, 1, sweepPath[nsweeps - 1].x + polygon[3].x * sweepRadius[nsweeps - 1].x, sweepPath[nsweeps - 1].z, sweepPath[nsweeps - 1].y + polygon[3].y * sweepRadius[nsweeps - 1].y);
			} else {
				topGeometry = new GLSurface(lastVertex - firstVertex + 2, 2, false);
				for (int v = firstVertex; v <= lastVertex + 1; v++) {
					if (v < polygon.length) {
						topGeometry.setVertex(v - firstVertex, 0, sweepPath[nsweeps - 1].x + polygon[v].x * sweepRadius[nsweeps - 1].x, sweepPath[nsweeps - 1].z, sweepPath[nsweeps - 1].y + polygon[v].y * sweepRadius[nsweeps - 1].y);
						//topGeometry.setTextureCoordinate(0, i - 1, new float[] { sweepPath[nsweeps - 1].x + polygon[v].x * sweepRadius[nsweeps - 1].x, sweepPath[nsweeps - 1].y + polygon[v].y * sweepRadius[nsweeps - 1].y });
						topGeometry.setVertex(v - firstVertex, 1, sweepPath[nsweeps - 1].x + polygon[v].x * sweepRadius[nsweeps - 1].x * hollowx, sweepPath[nsweeps - 1].z, sweepPath[nsweeps - 1].y + polygon[v].y * sweepRadius[nsweeps - 1].y * hollowy);
						//topGeometry.setTextureCoordinate(0, i - 1, new float[] { sweepPath[nsweeps - 1].x + polygon[v].x * sweepRadius[nsweeps - 1].x * hollowx, sweepPath[nsweeps - 1].y + polygon[v].y * sweepRadius[nsweeps - 1].y * hollowy });
					} else {
						topGeometry.setVertex(v - firstVertex, 0, sweepPath[nsweeps - 1].x + polygon[0].x * sweepRadius[nsweeps - 1].x, sweepPath[nsweeps - 1].z, sweepPath[nsweeps - 1].y + polygon[0].y * sweepRadius[nsweeps - 1].y);
						//topGeometry.setTextureCoordinate(0, i - 1, new float[] { sweepPath[nsweeps - 1].x + polygon[0].x * sweepRadius[nsweeps - 1].x, sweepPath[nsweeps - 1].y + polygon[0].y * sweepRadius[nsweeps - 1].y });
						topGeometry.setVertex(v - firstVertex, 1, sweepPath[nsweeps - 1].x + polygon[0].x * sweepRadius[nsweeps - 1].x * hollowx, sweepPath[nsweeps - 1].z, sweepPath[nsweeps - 1].y + polygon[0].y * sweepRadius[nsweeps - 1].y * hollowy);
						//topGeometry.setTextureCoordinate(0, i - 1, new float[] { sweepPath[nsweeps - 1].x + polygon[0].x * sweepRadius[nsweeps - 1].x * hollowx, sweepPath[nsweeps - 1].y + polygon[0].y * sweepRadius[nsweeps - 1].y * hollowy });
					}
				}
			}
			adjustGeometry(topGeometry, sizeX, sizeY, sizeZ, taperX, taperY, shearX, shearY, twist);
			topGeometry.generateNormals();
			adjustTextureCoords(topGeometry, WWObject.SIDE_TOP);
			setSide(WWObject.SIDE_TOP, topGeometry);
		}

		// save geometry for optimized sharing
		if (!object.fixed) {
			renderer.geometryCache.put(geometryMapKey, sides);
		}

	}

}
