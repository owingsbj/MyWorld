package com.gallantrealm.myworld.android.renderer.neu;

import com.gallantrealm.myworld.model.WWObject;
import com.gallantrealm.myworld.model.WWPlant;

/**
 * Creates a primitive with four vertical crossed rectangles. These rectangles are considered the four "sides". Given a
 * partially transparent texture, this can represent plants or other symmetrical objects. Either the same texture can be
 * used for all sides, or different textures can be used to give the plant additional complexity.
 */
public class GLPlant extends GLObject {

	public GLPlant(NewAndroidRenderer renderer, WWPlant plant, long worldTime) {
		super(renderer, plant, worldTime);

		// There are eight plant sides that criss-cross.
		for (int i = 0; i < 4; i++) {
			double r = Math.toRadians(i * 45);
			float x1 = (float) (-0.5 * Math.sin(r) * plant.sizeX);
			float y1 = (float) (-0.5 * Math.cos(r) * plant.sizeY);
			float z1 = (float) (-0.5 * plant.sizeZ);
			float x2 = (float) (0.5 * Math.sin(r) * plant.sizeX);
			float y2 = (float) (0.5 * Math.cos(r) * plant.sizeY);
			float z2 = (float) (0.5 * plant.sizeZ);

			GLSurface sideGeometry = new GLSurface(2, 2, false);
			sideGeometry.setVertex(0, 0, x1, z1, y1);
			sideGeometry.setVertex(0, 1, x1, z2, y1);
			sideGeometry.setVertex(1, 1, x2, z2, y2);
			sideGeometry.setVertex(1, 0, x2, z1, y2);
			adjustGeometry(sideGeometry, 1, 1, 1, 0, 0, 0, 0, 0);
			sideGeometry.generateNormals();
			adjustTextureCoords(sideGeometry, WWObject.SIDE_SIDE1 + i);
			setSide(WWObject.SIDE_SIDE1 + i, sideGeometry);

			sideGeometry = new GLSurface(2, 2, false);
			sideGeometry.setVertex(0, 0, x2, z1, y2);
			sideGeometry.setVertex(0, 1, x2, z2, y2);
			sideGeometry.setVertex(1, 1, x1, z2, y1);
			sideGeometry.setVertex(1, 0, x1, z1, y1);
			adjustGeometry(sideGeometry, 1, 1, 1, 0, 0, 0, 0, 0);
			sideGeometry.generateNormals();
			adjustTextureCoords(sideGeometry, WWObject.SIDE_INSIDE1 + i);
			setSide(WWObject.SIDE_INSIDE1 + i, sideGeometry);
		}

	}

}
