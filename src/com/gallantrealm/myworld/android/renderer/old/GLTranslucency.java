package com.gallantrealm.myworld.android.renderer.old;

import android.opengl.GLES10;

import com.gallantrealm.myworld.android.AndroidClientModel;
import com.gallantrealm.myworld.android.renderer.AndroidRenderer;
import com.gallantrealm.myworld.model.SideAttributes;
import com.gallantrealm.myworld.model.WWObject;
import com.gallantrealm.myworld.model.WWTranslucency;
import com.gallantrealm.myworld.model.WWVector;

/**
 * Creates a primitive with evenly spaced layers in the x, y, and z dimensions. These layers, when given partial
 * transparency, form the illusion of a translucent substance. This is useful for water or other nearly clear objects,
 * to give the illusion of depth. A translucency should be clipped by other objects to provide the desired effect.
 */
public final class GLTranslucency extends GLObject {

//	Shape3D sideTranslucencyShape;
//	QuadArray sideTranslucencyGeometry;
//	Appearance sideTranslucencyAppearance;
	int sideLayers;
//	Shape3D topTranslucencyShape;
//	QuadArray topTranslucencyGeometry;
//	Appearance topTranslucencyAppearance;
	int topLayers;
	float sizeX;
	float sizeY;
	float sizeZ;
	float layerDensity;
	float layerTransparency;
	int layerColor;

	public GLTranslucency(AndroidRenderer renderer, WWObject object, long worldTime) {
		super(renderer, object, worldTime);
		this.sizeX = object.sizeX;
		this.sizeY = object.sizeY;
		this.sizeZ = object.sizeZ;
		this.layerDensity = ((WWTranslucency) object).getInsideLayerDensity();
		this.layerTransparency = ((WWTranslucency) object).getInsideTransparency();
		this.layerColor = ((WWTranslucency) object).getInsideColor();

		sideLayers = (int) (layerDensity * sizeY);
		topLayers = (int) (layerDensity * sizeZ);

		// Create top (use a 10x10 matrix for better fog look on large transparencies like water
		GLSurface topGeometry = new GLSurface(11, 11, false);
		for (int y = 0; y < 11; y++) {
			for (int x = 0; x < 11; x++) {
				topGeometry.setVertex(x, y, x * sizeX / 10 - sizeX / 2, sizeZ / 2, y * sizeY / 10 - sizeY / 2);
			}
		}
		topGeometry.generateNormals();
		this.setSide(WWObject.SIDE_TOP, topGeometry);

		// Create the inside top, no normals
		GLSurface insideTopGeometry = new GLSurface(11, 11, false);
		for (int y = 0; y < 11; y++) {
			for (int x = 0; x < 11; x++) {
				insideTopGeometry.setVertex(10 - x, y, x * sizeX / 10 - sizeX / 2, sizeZ / 2, y * sizeY / 10 - sizeY / 2);
			}
		}
		this.setSide(WWObject.SIDE_INSIDE_TOP, insideTopGeometry);

		// Create the inside sides (inside1-inside4), no normals
		GLSurface inside1Geometry = new GLSurface(2, 2, false);
		inside1Geometry.setVertex(0, 0, -sizeX / 2, -sizeZ / 2, sizeY / 2);
		inside1Geometry.setVertex(1, 0, sizeX / 2, -sizeZ / 2, sizeY / 2);
		inside1Geometry.setVertex(0, 1, -sizeX / 2, sizeZ / 2, sizeY / 2);
		inside1Geometry.setVertex(1, 1, sizeX / 2, sizeZ / 2, sizeY / 2);
		this.setSide(WWObject.SIDE_INSIDE1, inside1Geometry);
		GLSurface inside2Geometry = new GLSurface(2, 2, false);
		inside2Geometry.setVertex(0, 0, -sizeX / 2, -sizeZ / 2, -sizeY / 2);
		inside2Geometry.setVertex(1, 0, -sizeX / 2, -sizeZ / 2, sizeY / 2);
		inside2Geometry.setVertex(0, 1, -sizeX / 2, sizeZ / 2, -sizeY / 2);
		inside2Geometry.setVertex(1, 1, -sizeX / 2, sizeZ / 2, sizeY / 2);
		this.setSide(WWObject.SIDE_INSIDE2, inside2Geometry);
		GLSurface inside3Geometry = new GLSurface(2, 2, false);
		inside3Geometry.setVertex(0, 0, sizeX / 2, -sizeZ / 2, -sizeY / 2);
		inside3Geometry.setVertex(1, 0, -sizeX / 2, -sizeZ / 2, -sizeY / 2);
		inside3Geometry.setVertex(0, 1, sizeX / 2, sizeZ / 2, -sizeY / 2);
		inside3Geometry.setVertex(1, 1, -sizeX / 2, sizeZ / 2, -sizeY / 2);
		this.setSide(WWObject.SIDE_INSIDE3, inside3Geometry);
		GLSurface inside4Geometry = new GLSurface(2, 2, false);
		inside4Geometry.setVertex(0, 0, sizeX / 2, -sizeZ / 2, sizeY / 2);
		inside4Geometry.setVertex(1, 0, sizeX / 2, -sizeZ / 2, -sizeY / 2);
		inside4Geometry.setVertex(0, 1, sizeX / 2, sizeZ / 2, sizeY / 2);
		inside4Geometry.setVertex(1, 1, sizeX / 2, sizeZ / 2, -sizeY / 2);
		this.setSide(WWObject.SIDE_INSIDE4, inside4Geometry);

		// Create an inside shape to box the camera.  This shape is only shown when the object is clipped by the camera frustrum.
		// It provides a (crude) illusion of the translucency even when the translucency is penetrated. Mapped to CUTOUT1
		GLSurface insideXGeometry = new GLSurface(5, 2, false);
		float space = 1.5f * AndroidRenderer.CLOSENESS;
		insideXGeometry.setVertex(0, 0, -space, -sizeZ / 2, -space);
		insideXGeometry.setVertex(1, 0, -space, -sizeZ / 2, space);
		insideXGeometry.setVertex(2, 0, space, -sizeZ / 2, space);
		insideXGeometry.setVertex(3, 0, space, -sizeZ / 2, -space);
		insideXGeometry.setVertex(4, 0, -space, -sizeZ / 2, -space);
		insideXGeometry.setVertex(0, 1, -space, sizeZ / 2, -space);
		insideXGeometry.setVertex(1, 1, -space, sizeZ / 2, space);
		insideXGeometry.setVertex(2, 1, space, sizeZ / 2, space);
		insideXGeometry.setVertex(3, 1, space, sizeZ / 2, -space);
		insideXGeometry.setVertex(4, 1, -space, sizeZ / 2, -space);
		this.setSide(WWObject.SIDE_CUTOUT1, insideXGeometry);

		// 

//		// Create side1 (front)
//		Shape3D side1Shape = new Shape3D();
//		GLSurface side1Geometry = new QuadArray(4, GLSurface.COORDINATES | GLSurface.NORMALS | GLSurface.TEXTURE_COORDINATE_2);
//		i = 0;
//		side1Geometry.setCoordinate(i, new float[] { -sizeX / 2, -sizeZ / 2, sizeY / 2 });
//		side1Geometry.setTextureCoordinate(0, i, new float[] { -0.5f, -0.5f });
//		i++;
//		side1Geometry.setCoordinate(i, new float[] { sizeX / 2, -sizeZ / 2, sizeY / 2 });
//		side1Geometry.setTextureCoordinate(0, i, new float[] { 0.5f, -0.5f });
//		i++;
//		side1Geometry.setCoordinate(i, new float[] { sizeX / 2, sizeZ / 2, sizeY / 2 });
//		side1Geometry.setTextureCoordinate(0, i, new float[] { 0.5f, 0.5f });
//		i++;
//		side1Geometry.setCoordinate(i, new float[] { -sizeX / 2, sizeZ / 2, sizeY / 2 });
//		side1Geometry.setTextureCoordinate(0, i, new float[] { -0.5f, 0.5f });
//		i++;
//		gi = new GeometryInfo(side1Geometry);
//		ng.generateNormals(gi);
//		side1Geometry = gi.getGLSurface();
//		side1Shape.addGeometry(side1Geometry);
//		primitive.addChild(side1Shape);
//		primitive.setShape(SIDE1, side1Shape);
//
//		// Create side2 (right)
//		Shape3D side2Shape = new Shape3D();
//		GLSurface side2Geometry = new QuadArray(4, GLSurface.COORDINATES | GLSurface.NORMALS | GLSurface.TEXTURE_COORDINATE_2);
//		i = 0;
//		side2Geometry.setCoordinate(i, new float[] { sizeX / 2, -sizeZ / 2, sizeY / 2 });
//		side2Geometry.setTextureCoordinate(0, i, new float[] { -0.5f, -0.5f });
//		i++;
//		side2Geometry.setCoordinate(i, new float[] { sizeX / 2, -sizeZ / 2, -sizeY / 2 });
//		side2Geometry.setTextureCoordinate(0, i, new float[] { 0.5f, -0.5f });
//		i++;
//		side2Geometry.setCoordinate(i, new float[] { sizeX / 2, sizeZ / 2, -sizeY / 2 });
//		side2Geometry.setTextureCoordinate(0, i, new float[] { 0.5f, 0.5f });
//		i++;
//		side2Geometry.setCoordinate(i, new float[] { sizeX / 2, sizeZ / 2, sizeY / 2 });
//		side2Geometry.setTextureCoordinate(0, i, new float[] { -0.5f, 0.5f });
//		i++;
//		gi = new GeometryInfo(side2Geometry);
//		ng.generateNormals(gi);
//		side2Geometry = gi.getGLSurface();
//		side2Shape.addGeometry(side2Geometry);
//		primitive.addChild(side2Shape);
//		primitive.setShape(SIDE2, side2Shape);
//
//		// Create side3 (back)
//		Shape3D side3Shape = new Shape3D();
//		GLSurface side3Geometry = new QuadArray(4, GLSurface.COORDINATES | GLSurface.NORMALS | GLSurface.TEXTURE_COORDINATE_2);
//		i = 0;
//		side3Geometry.setCoordinate(i, new float[] { sizeX / 2, -sizeZ / 2, -sizeY / 2 });
//		side3Geometry.setTextureCoordinate(0, i, new float[] { -0.5f, -0.5f });
//		i++;
//		side3Geometry.setCoordinate(i, new float[] { -sizeX / 2, -sizeZ / 2, -sizeY / 2 });
//		side3Geometry.setTextureCoordinate(0, i, new float[] { 0.5f, -0.5f });
//		i++;
//		side3Geometry.setCoordinate(i, new float[] { -sizeX / 2, sizeZ / 2, -sizeY / 2 });
//		side3Geometry.setTextureCoordinate(0, i, new float[] { 0.5f, 0.5f });
//		i++;
//		side3Geometry.setCoordinate(i, new float[] { sizeX / 2, sizeZ / 2, -sizeY / 2 });
//		side3Geometry.setTextureCoordinate(0, i, new float[] { -0.5f, 0.5f });
//		i++;
//		gi = new GeometryInfo(side3Geometry);
//		ng.generateNormals(gi);
//		side3Geometry = gi.getGLSurface();
//		side3Shape.addGeometry(side3Geometry);
//		primitive.addChild(side3Shape);
//		primitive.setShape(SIDE3, side3Shape);
//
//		// Create side4 (left)
//		Shape3D side4Shape = new Shape3D();
//		GLSurface side4Geometry = new QuadArray(4, GLSurface.COORDINATES | GLSurface.NORMALS | GLSurface.TEXTURE_COORDINATE_2);
//		i = 0;
//		side4Geometry.setCoordinate(i, new float[] { -sizeX / 2, -sizeZ / 2, -sizeY / 2 });
//		side4Geometry.setTextureCoordinate(0, i, new float[] { -0.5f, -0.5f });
//		i++;
//		side4Geometry.setCoordinate(i, new float[] { -sizeX / 2, -sizeZ / 2, sizeY / 2 });
//		side4Geometry.setTextureCoordinate(0, i, new float[] { 0.5f, -0.5f });
//		i++;
//		side4Geometry.setCoordinate(i, new float[] { -sizeX / 2, sizeZ / 2, sizeY / 2 });
//		side4Geometry.setTextureCoordinate(0, i, new float[] { 0.5f, 0.5f });
//		i++;
//		side4Geometry.setCoordinate(i, new float[] { -sizeX / 2, sizeZ / 2, -sizeY / 2 });
//		side4Geometry.setTextureCoordinate(0, i, new float[] { -0.5f, 0.5f });
//		i++;
//		gi = new GeometryInfo(side4Geometry);
//		ng.generateNormals(gi);
//		side4Geometry = gi.getGLSurface();
//		side4Shape.addGeometry(side4Geometry);
//		primitive.addChild(side4Shape);
//		primitive.setShape(SIDE4, side4Shape);
//
//		// Create the bottom.
//		Shape3D bottomShape = new Shape3D();
//		GLSurface bottomGeometry = new QuadArray(8, GLSurface.COORDINATES | GLSurface.NORMALS | GLSurface.TEXTURE_COORDINATE_2);
//		i = 0;
//		// bottom bottom
//		bottomGeometry.setCoordinate(i, new float[] { -sizeX / 2, -sizeZ / 2, -sizeY / 2 });
//		bottomGeometry.setTextureCoordinate(0, i, new float[] { -0.5f, -0.5f });
//		i++;
//		bottomGeometry.setCoordinate(i, new float[] { sizeX / 2, -sizeZ / 2, -sizeY / 2 });
//		bottomGeometry.setTextureCoordinate(0, i, new float[] { 0.5f, -0.5f });
//		i++;
//		bottomGeometry.setCoordinate(i, new float[] { sizeX / 2, -sizeZ / 2, sizeY / 2 });
//		bottomGeometry.setTextureCoordinate(0, i, new float[] { 0.5f, 0.5f });
//		i++;
//		bottomGeometry.setCoordinate(i, new float[] { -sizeX / 2, -sizeZ / 2, sizeY / 2 });
//		bottomGeometry.setTextureCoordinate(0, i, new float[] { -0.5f, 0.5f });
//		i++;
//		// under the top layer
//		bottomGeometry.setCoordinate(i, new float[] { -sizeX / 2, sizeZ / 2, -sizeY / 2 });
//		bottomGeometry.setTextureCoordinate(0, i, new float[] { -0.5f, -0.5f });
//		i++;
//		bottomGeometry.setCoordinate(i, new float[] { sizeX / 2, sizeZ / 2, -sizeY / 2 });
//		bottomGeometry.setTextureCoordinate(0, i, new float[] { 0.5f, -0.5f });
//		i++;
//		bottomGeometry.setCoordinate(i, new float[] { sizeX / 2, sizeZ / 2, sizeY / 2 });
//		bottomGeometry.setTextureCoordinate(0, i, new float[] { 0.5f, 0.5f });
//		i++;
//		bottomGeometry.setCoordinate(i, new float[] { -sizeX / 2, sizeZ / 2, sizeY / 2 });
//		bottomGeometry.setTextureCoordinate(0, i, new float[] { -0.5f, 0.5f });
//		i++;
//		gi = new GeometryInfo(bottomGeometry);
//		ng.generateNormals(gi);
//		bottomGeometry = gi.getGLSurface();
//		bottomShape.addGeometry(bottomGeometry);
//		primitive.addChild(bottomShape);
//		primitive.setShape(BOTTOM, bottomShape);
//
//		// Create inside translucency layers. Since they will be oriented toward the camera these only need
//		// to point toward the front.
//		if (sideLayers > 0) {
//			sideTranslucencyShape = new Shape3D();
//			sideTranslucencyShape.setPickable(false);
//			sideTranslucencyGeometry = new QuadArray(12 * sideLayers, GLSurface.BY_REFERENCE | GLSurface.COORDINATES | GLSurface.COLOR_4);
//			sideTranslucencyGeometry.setCapability(GLSurface.ALLOW_REF_DATA_WRITE);
//			sideTranslucencyGeometry.updateData(translucencyGeometryUpdater);
//			// Note: no normals generated because we dont want "shine" from the inside
//			sideTranslucencyShape.addGeometry(sideTranslucencyGeometry);
//
//			// Also create appearance for the translucent layers
//			sideTranslucencyAppearance = new Appearance();
//			sideTranslucencyAppearance.setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_WRITE);
//			Material material = new Material();
//			// material.setCapability(Material.ALLOW_COMPONENT_WRITE);
//			material.setLightingEnable(false);
//			sideTranslucencyAppearance.setMaterial(material);
//			TransparencyAttributes transparencyAttributes = new TransparencyAttributes(TransparencyAttributes.FASTEST, layerTransparency);
//			sideTranslucencyAppearance.setTransparencyAttributes(transparencyAttributes);
//			sideTranslucencyShape.setAppearance(sideTranslucencyAppearance);
//
//			primitive.addChild(sideTranslucencyShape);
//		}
//
//		if (topLayers > 0) {
//			topTranslucencyShape = new Shape3D();
//			topTranslucencyShape.setPickable(false);
//			topTranslucencyGeometry = new QuadArray(8 * topLayers, GLSurface.COORDINATES);
//			i = 0;
//			for (int l = 0; l < topLayers; l++) {
//				float z = sizeZ * (l + 1) / (topLayers + 1) - sizeZ / 2;
//				// top side
//				topTranslucencyGeometry.setCoordinate(i, new float[] { -sizeX / 2, z, sizeY / 2 });
//				//				topTranslucencyGeometry.setTextureCoordinate(0, i, new float[] {0, 0});
//				i++;
//				topTranslucencyGeometry.setCoordinate(i, new float[] { sizeX / 2, z, sizeY / 2 });
//				//				topTranslucencyGeometry.setTextureCoordinate(0, i, new float[] {1, 0});
//				i++;
//				topTranslucencyGeometry.setCoordinate(i, new float[] { sizeX / 2, z, -sizeY / 2 });
//				//				topTranslucencyGeometry.setTextureCoordinate(0, i, new float[] {1, 1});
//				i++;
//				topTranslucencyGeometry.setCoordinate(i, new float[] { -sizeX / 2, z, -sizeY / 2 });
//				//				topTranslucencyGeometry.setTextureCoordinate(0, i, new float[] {0, 1});
//				i++;
//				// bottom side
//				topTranslucencyGeometry.setCoordinate(i, new float[] { -sizeX / 2, z, -sizeY / 2 });
//				//				topTranslucencyGeometry.setTextureCoordinate(0, i, new float[] {0, 0});
//				i++;
//				topTranslucencyGeometry.setCoordinate(i, new float[] { sizeX / 2, z, -sizeY / 2 });
//				//				topTranslucencyGeometry.setTextureCoordinate(0, i, new float[] {1, 0});
//				i++;
//				topTranslucencyGeometry.setCoordinate(i, new float[] { sizeX / 2, z, sizeY / 2 });
//				//				topTranslucencyGeometry.setTextureCoordinate(0, i, new float[] {1, 1});
//				i++;
//				topTranslucencyGeometry.setCoordinate(i, new float[] { -sizeX / 2, z, sizeY / 2 });
//				//				topTranslucencyGeometry.setTextureCoordinate(0, i, new float[] {0, 1});
//				i++;
//			}
//			// Note: no normals generated because we dont want "shine" from the inside
//			topTranslucencyShape.addGeometry(topTranslucencyGeometry);
//
//			// Also create appearance for the translucent layers
//			topTranslucencyAppearance = new Appearance();
//			topTranslucencyAppearance.setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_WRITE);
//			Material material = new Material();
//			//			material.setCapability(Material.ALLOW_COMPONENT_WRITE);
//			Color3f color3f = new Color3f(new Color(layerColor));
//			material.setLightingEnable(false);
//			ColoringAttributes coloringAttributes = new ColoringAttributes(color3f, ColoringAttributes.SHADE_FLAT);
//			topTranslucencyAppearance.setColoringAttributes(coloringAttributes);
//			topTranslucencyAppearance.setMaterial(material);
//			TransparencyAttributes transparencyAttributes = new TransparencyAttributes(TransparencyAttributes.FASTEST, layerTransparency);
//			topTranslucencyAppearance.setTransparencyAttributes(transparencyAttributes);
//			topTranslucencyShape.setAppearance(topTranslucencyAppearance);
//
//			BranchGroup topTranslucencyBG = new BranchGroup();
//			topTranslucencyBG.addChild(topTranslucencyShape);
//			topTranslucencyBG.compile();
//			primitive.addChild(topTranslucencyBG);
//		}

	}

//	/**
//	 * This class dynamically updates the gemotry of the translucency layers so they point to the camera.
//	 */
//	private class TranslucencyGeometryUpdater implements GeometryUpdater {
//		private float pan;
//		private WWVector location = new WWVector(0, 0, 0);
//
//		public void setCamera(float pan, WWVector location) {
//			this.pan = pan;
//			this.location = location;
//		}
//
//		public void updateData(Geometry geometry) {
//			QuadArray sideTranslucencyGeometry = (QuadArray) geometry;
//			float[] coordinates = sideTranslucencyGeometry.getCoordRefFloat();
//			Color4f[] colors = sideTranslucencyGeometry.getColorRef4f();
//			if (coordinates == null) {
//				coordinates = new float[12 * sideLayers * 3];
//				colors = new Color4f[12 * sideLayers];
//				sideTranslucencyGeometry.setCoordRefFloat(coordinates);
//				sideTranslucencyGeometry.setColorRef4f(colors);
//			}
//			Color4f sideColor = new Color4f(new Color(layerColor));
//			Color4f transparentColor = new Color4f(0, 0, 0, 0.0f);
//			// Note: using the taper size causes the edges of the translucency to fade to more transparency.
//			// This makes them less noticable, but leads to effects that when looked "on edge" the translucency
//			// is more transparent.
//			float taperSize = Math.min(sizeZ / 16.0f, 0.03125f / layerDensity);
//			int i = 0;
//			int j = 0;
//			for (int l = 0; l < sideLayers; l++) {
//				// bottom of the layer
//				coordinates[i++] = -sizeX / 2;
//				coordinates[i++] = -sizeZ / 2;
//				coordinates[i++] = sizeY * l / sideLayers - sizeY / 2;
//				colors[j++] = transparentColor;
//				coordinates[i++] = sizeX / 2;
//				coordinates[i++] = -sizeZ / 2;
//				coordinates[i++] = sizeY * l / sideLayers - sizeY / 2;
//				colors[j++] = transparentColor;
//				coordinates[i++] = sizeX / 2;
//				coordinates[i++] = -sizeZ / 2 + taperSize;
//				coordinates[i++] = sizeY * l / sideLayers - sizeY / 2;
//				colors[j++] = sideColor;
//				coordinates[i++] = -sizeX / 2;
//				coordinates[i++] = -sizeZ / 2 + taperSize;
//				coordinates[i++] = sizeY * l / sideLayers - sizeY / 2;
//				colors[j++] = sideColor;
//				// middle of the layer
//				coordinates[i++] = -sizeX / 2;
//				coordinates[i++] = -sizeZ / 2 + taperSize;
//				coordinates[i++] = sizeY * l / sideLayers - sizeY / 2;
//				colors[j++] = sideColor;
//				coordinates[i++] = sizeX / 2;
//				coordinates[i++] = -sizeZ / 2 + taperSize;
//				coordinates[i++] = sizeY * l / sideLayers - sizeY / 2;
//				colors[j++] = sideColor;
//				coordinates[i++] = sizeX / 2;
//				coordinates[i++] = sizeZ / 2 - taperSize;
//				coordinates[i++] = sizeY * l / sideLayers - sizeY / 2;
//				colors[j++] = sideColor;
//				coordinates[i++] = -sizeX / 2;
//				coordinates[i++] = sizeZ / 2 - taperSize;
//				coordinates[i++] = sizeY * l / sideLayers - sizeY / 2;
//				colors[j++] = sideColor;
//				// top of the layer
//				coordinates[i++] = -sizeX / 2;
//				coordinates[i++] = sizeZ / 2 - taperSize;
//				coordinates[i++] = sizeY * l / sideLayers - sizeY / 2;
//				colors[j++] = sideColor;
//				coordinates[i++] = sizeX / 2;
//				coordinates[i++] = sizeZ / 2 - taperSize;
//				coordinates[i++] = sizeY * l / sideLayers - sizeY / 2;
//				colors[j++] = sideColor;
//				coordinates[i++] = sizeX / 2;
//				coordinates[i++] = sizeZ / 2;
//				coordinates[i++] = sizeY * l / sideLayers - sizeY / 2;
//				colors[j++] = transparentColor;
//				coordinates[i++] = -sizeX / 2;
//				coordinates[i++] = sizeZ / 2;
//				coordinates[i++] = sizeY * l / sideLayers - sizeY / 2;
//				colors[j++] = transparentColor;
//			}
//
//			double cospan = Math.cos(Math.toRadians(pan));
//			double sinpan = Math.sin(Math.toRadians(pan));
//
//			// rotate the coordinates for the pan
//			for (i = 0; i < coordinates.length; i += 3) {
//				double x = coordinates[i];
//				double y = coordinates[i + 2];
//				double newx = x * cospan + y * sinpan;
//				double newy = -x * sinpan + y * cospan;
//				coordinates[i] = (float) newx;
//				coordinates[i + 2] = (float) newy;
//			}
//
//			// adjust the coordinates for the camera location
//			if (location != null) {
//				double offset = (location.getX() * sinpan + location.getY() * cospan) % (1 / layerDensity);
//				double offsetx = sinpan * offset;
//				double offsety = cospan * offset;
//				for (i = 0; i < coordinates.length; i += 3) {
//					coordinates[i] += offsetx;
//					coordinates[i + 2] += offsety;
//				}
//			}
//		}
//	}

//	private final TranslucencyGeometryUpdater translucencyGeometryUpdater = new TranslucencyGeometryUpdater();
//
//	private float lastPan;
//	private float lastTilt;
//	private double lastCameraLocationY;
//	private double originalPositionY;
//
//	/**
//	 * Adjust the position of the transparency layers. These will be individually adjusted, but only if the perspective
//	 * has changed significantly.
//	 */
//	public void adjustTranslucencyForPerspective(float pan, float tilt, WWVector cameraLocation, long time) {
//		if (pan == lastPan && tilt == lastTilt && lastCameraLocationY == cameraLocation.getY()) {
//			return;
//		}
//		if (sideTranslucencyAppearance != null) {
//			float transparency = (float) (1 - Math.abs(Math.cos(Math.toRadians(tilt))) * (1 - layerTransparency));
//			TransparencyAttributes transparencyAttributes = new TransparencyAttributes(TransparencyAttributes.FASTEST, transparency);
//			sideTranslucencyAppearance.setTransparencyAttributes(transparencyAttributes);
//		}
//		if (topTranslucencyAppearance != null) {
//			float transparency = (float) (1 - Math.abs(Math.sin(Math.toRadians(tilt))) * (1 - layerTransparency));
//			TransparencyAttributes transparencyAttributes = new TransparencyAttributes(TransparencyAttributes.FASTEST, transparency);
//			topTranslucencyAppearance.setTransparencyAttributes(transparencyAttributes);
//		}
//
//		// Adjust the rotation of the transparency data, to keep the transparency layers perpendicular to the view
//		// sideTranslucencyGeometry.updateData(translucencyGeometryUpdater);
//		translucencyGeometryUpdater.setCamera(pan, cameraLocation);
//		translucencyGeometryUpdater.updateData(sideTranslucencyGeometry);
//
//		// Also, adjust the position slightly so that the layers wont "slide" past the avatar/camera
//		/*
//		 * float layerDistance = 1.0f / layerDensity; double positionY = object.getPositionY(time); double
//		 * positionYoffset = positionY % layerDistance; double positionYtruncated = positionY - positionYoffset; double
//		 * cameraOffset = Math.abs(cameraLocation.getY()) % layerDistance; positionY = positionYtruncated +
//		 * cameraOffset; object.setPositionY(positionY); object.orientJava3dRendering(time);
//		 */
//
//		lastPan = pan;
//		lastTilt = tilt;
//		lastCameraLocationY = cameraLocation.getY();
//	}

	@Override
	public void draw(long worldTime, boolean shading, int picking, boolean drawtrans, int threedee) {
		if (picking > 0 || !drawtrans) {
//			super.draw(worldTime, picking, drawtrans);
			return;
		}
		WWVector p = AndroidClientModel.getClientModel().getDampedCameraLocation();
		GLES10.glPushMatrix();
		WWVector position = object.getPosition(worldTime);
		GLES10.glTranslatef(position.x, position.z, position.y);
		WWVector rotation = object.getRotation(worldTime);
		GLES10.glRotatef(rotation.z, 0.0f, 1.0f, 0.0f);
		GLES10.glRotatef(rotation.y, 0.0f, 0.0f, 1.0f);
		GLES10.glRotatef(rotation.x, 1.0f, 0.0f, 0.0f);
		for (int side = 0; side < WWObject.NSIDES; side++) {
			if (sides[side] != null) {
				if (side != WWObject.SIDE_CUTOUT1) { // save cutout for later
					SideAttributes sideAttributes = null;
					if (object.sideAttributes != null) {
						sideAttributes = object.sideAttributes[side];
					}
					if (sideAttributes == null) {
						sideAttributes = SideAttributes.getDefaultSideAttributes();
					}
					float trans = sideAttributes.transparency;
					if ((drawtrans && trans > 0.0 && trans < 1.0) || (!drawtrans && trans == 0.0)) {
						if (picking > 0) {
							GLSurface geometry = sides[side];
							geometry.draw(false);
						} else {
							float red = sideAttributes.red;
							float green = sideAttributes.green;
							float blue = sideAttributes.blue;
							if (trans == 0.0) {
								GLES10.glDisable(GLES10.GL_BLEND);
								GLES10.glMaterialfv(GLES10.GL_FRONT_AND_BACK, GLES10.GL_AMBIENT_AND_DIFFUSE, new float[] { red, green, blue, 1.0f }, 0);
							} else {
								GLES10.glEnable(GLES10.GL_BLEND);
								//GLES10.glBlendFunc(GLES10.GL_SRC_ALPHA, GLES10.GL_ONE_MINUS_SRC_ALPHA);
								GLES10.glMaterialfv(GLES10.GL_FRONT_AND_BACK, GLES10.GL_AMBIENT_AND_DIFFUSE, new float[] { red, green, blue, 1.0f - trans }, 0);
							}
							// Note: Adding specular lighting distorts the shading on infuse (but not simulator)
							//GLES10.glMaterialfv(GLES10.GL_FRONT_AND_BACK, GLES10.GL_SPECULAR, new float[] { sideColor.getRed(), sideColor.getGreen(), sideColor.getBlue(), 0.0f }, 0);
							//GLES10.glMaterialf(GLES10.GL_FRONT_AND_BACK, GLES10.GL_SHININESS, 1.0f);
							String textureUrl = sideAttributes.textureURL;
							if (textureUrl != null) {
								int textureId = renderer.getTexture(textureUrl);
								GLES10.glMatrixMode(GLES10.GL_TEXTURE);
								GLES10.glPushMatrix();
								GLES10.glLoadIdentity();
								GLES10.glScalef(1.0f / sideAttributes.textureScaleX, 1.0f / sideAttributes.textureScaleY, 1.0f);
								GLES10.glTranslatef(object.getTextureOffsetX(side, worldTime), object.getTextureOffsetY(side, worldTime), 0.0f);
								GLES10.glRotatef(object.getTextureRotation(side, worldTime), 0.0f, 0.0f, 1.0f);
								GLES10.glEnable(GLES10.GL_TEXTURE_2D);
								GLES10.glBindTexture(GLES10.GL_TEXTURE_2D, textureId);
							} else {
								GLES10.glDisable(GLES10.GL_TEXTURE_2D);
							}
							GLSurface geometry = sides[side];
							geometry.draw(shading && !sideAttributes.fullBright);
							if (textureUrl != null) {
								GLES10.glPopMatrix();
								GLES10.glMatrixMode(GLES10.GL_MODELVIEW);
							}
						}
					}
				}
			}
		}
		GLES10.glPopMatrix();
	}

	public void drawCurtain(long worldTime, boolean shading, int picking, boolean drawtrans, int threedee) {
		if (picking > 0 || !drawtrans) {
//			super.draw(worldTime, picking, drawtrans);
			return;
		}
		WWVector p = AndroidClientModel.getClientModel().getDampedCameraLocation();
		GLES10.glPushMatrix();
		WWVector position = object.getPosition(worldTime);
		GLES10.glTranslatef(position.x, position.z, position.y);
		WWVector rotation = object.getRotation(worldTime);
		GLES10.glRotatef(rotation.z, 0.0f, 1.0f, 0.0f);
		GLES10.glRotatef(rotation.y, 0.0f, 0.0f, 1.0f);
		GLES10.glRotatef(rotation.x, 1.0f, 0.0f, 0.0f);
		for (int side = 0; side < WWObject.NSIDES; side++) {
			if (sides[side] != null) {
				if (side == WWObject.SIDE_CUTOUT1) {
					GLES10.glPushMatrix();
					GLES10.glTranslatef((p.x - position.x), 0.0f, (p.y - position.y));
					SideAttributes sideAttributes = null;
					if (object.sideAttributes != null) {
						sideAttributes = object.sideAttributes[side];
					}
					if (sideAttributes == null) {
						sideAttributes = SideAttributes.getDefaultSideAttributes();
					}
					float trans = sideAttributes.transparency;
					if ((drawtrans && trans > 0.0 && trans < 1.0) || (!drawtrans && trans == 0.0)) {
						if (picking > 0) {
							GLSurface geometry = sides[side];
							geometry.draw(false);
						} else {
							float red = sideAttributes.red;
							float green = sideAttributes.green;
							float blue = sideAttributes.blue;
							if (trans == 0.0) {
								GLES10.glDisable(GLES10.GL_BLEND);
								GLES10.glMaterialfv(GLES10.GL_FRONT_AND_BACK, GLES10.GL_AMBIENT_AND_DIFFUSE, new float[] { red, green, blue, 1.0f }, 0);
							} else {
								GLES10.glEnable(GLES10.GL_BLEND);
								//GLES10.glBlendFunc(GLES10.GL_SRC_ALPHA, GLES10.GL_ONE_MINUS_SRC_ALPHA);
								GLES10.glMaterialfv(GLES10.GL_FRONT_AND_BACK, GLES10.GL_AMBIENT_AND_DIFFUSE, new float[] { red, green, blue, 1.0f - trans }, 0);
							}
							// Note: Adding specular lighting distorts the shading on infuse (but not simulator)
							//GLES10.glMaterialfv(GLES10.GL_FRONT_AND_BACK, GLES10.GL_SPECULAR, new float[] { sideColor.getRed(), sideColor.getGreen(), sideColor.getBlue(), 0.0f }, 0);
							//GLES10.glMaterialf(GLES10.GL_FRONT_AND_BACK, GLES10.GL_SHININESS, 1.0f);
							String textureUrl = sideAttributes.textureURL;
							if (textureUrl != null) {
								int textureId = renderer.getTexture(textureUrl);
								GLES10.glMatrixMode(GLES10.GL_TEXTURE);
								GLES10.glPushMatrix();
								GLES10.glLoadIdentity();
								GLES10.glScalef(1.0f / sideAttributes.textureScaleX, 1.0f / sideAttributes.textureScaleY, 1.0f);
								GLES10.glTranslatef(object.getTextureOffsetX(side, worldTime), object.getTextureOffsetY(side, worldTime), 0.0f);
								GLES10.glRotatef(object.getTextureRotation(side, worldTime), 0.0f, 0.0f, 1.0f);
								GLES10.glEnable(GLES10.GL_TEXTURE_2D);
								GLES10.glBindTexture(GLES10.GL_TEXTURE_2D, textureId);
							} else {
								GLES10.glDisable(GLES10.GL_TEXTURE_2D);
							}
							GLSurface geometry = sides[side];
							geometry.draw(shading && !sideAttributes.fullBright);
							if (textureUrl != null) {
								GLES10.glPopMatrix();
								GLES10.glMatrixMode(GLES10.GL_MODELVIEW);
							}
						}
						GLES10.glPopMatrix();
					}
				}
			}
		}
		GLES10.glPopMatrix();
	}

}