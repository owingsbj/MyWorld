package com.gallantrealm.myworld.android.renderer.old;

import java.util.ArrayList;

import android.opengl.GLES10;

import com.gallantrealm.myworld.android.renderer.AndroidRenderer;
import com.gallantrealm.myworld.client.renderer.IRenderer;
import com.gallantrealm.myworld.client.renderer.IRendering;
import com.gallantrealm.myworld.model.WWObject;
import com.gallantrealm.myworld.model.WWTranslucency;
import com.gallantrealm.myworld.model.WWVector;
import com.gallantrealm.myworld.model.WWWorld;

public final class GLWorld implements IRendering {

	final AndroidRenderer renderer;
	final WWWorld world;
	long lastRenderingTime;

//	private final ExponentialFog fog;
//	private final Background background; // the background "sky"
//	private final AmbientLight lightA; // the reflected light that is everywhere
//	private final DirectionalLight lightD1; // the direct sunlight
//	private final DirectionalLight lightD2; // the reflected underglow light (makes things look nice)

	public GLWorld(AndroidRenderer renderer, WWWorld world, long worldTime) {
		this.renderer = renderer;
		this.world = world;

//		// Create the root of the branch graph
//		BranchGroup java3dRendering = this;
//		java3dRendering.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
//		java3dRendering.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
//
//		// Add fog
//		Color3f fogColor3f = new Color3f(new Color(world.getSunColor()));
//		fog = new ExponentialFog(fogColor3f, 0.01f * world.getFogDensity());
//		fog.setCapability(ExponentialFog.ALLOW_DENSITY_WRITE);
//		fog.setCapability(ExponentialFog.ALLOW_COLOR_WRITE);
//		fog.setInfluencingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 100000.0));
//		java3dRendering.addChild(fog);
//
//		// Add the background (sky)
//		background = new Background();
//		background.setCapability(Background.ALLOW_COLOR_WRITE);
//		Color3f backgroundColor3f = new Color3f(new Color(world.getSunColor()));
//		//		backgroundColor3f.setX(0.75f*backgroundColor3f.getX());
//		//		backgroundColor3f.setY(0.75f*backgroundColor3f.getY());  // light blue hue
//		backgroundColor3f.scale(world.getSkyIntensity());
//		background.setColor(backgroundColor3f);
//		background.setApplicationBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 100000.0));
//		java3dRendering.addChild(background);
//
//		// Add sun light
//		lightD1 = new DirectionalLight();
//		lightD1.setCapability(DirectionalLight.ALLOW_COLOR_WRITE);
//		lightD1.setCapability(DirectionalLight.ALLOW_DIRECTION_WRITE);
//		lightD1.setInfluencingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 100000.0));
//		Color3f sunColor3f = new Color3f(new Color(world.getSunColor()));
//		sunColor3f.scale(world.getSunIntensity());
//		lightD1.setColor(sunColor3f);
//		lightD1.setDirection(world.getSunDirectionX(), -world.getSunDirectionZ(), world.getSunDirectionY());
//		java3dRendering.addChild(lightD1);
//
//		// Add ambient light
//		Color3f ambientColor3f = new Color3f(new Color(world.getSunColor()));
//		ambientColor3f.scale(world.getAmbientLightIntensity());
//		lightA = new AmbientLight(true, ambientColor3f);
//		lightA.setCapability(AmbientLight.ALLOW_COLOR_WRITE);
//		lightA.setInfluencingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 100000.0));
//		java3dRendering.addChild(lightA);
//
//		// Add reflected "ground" light
//		lightD2 = new DirectionalLight();
//		lightD2.setCapability(DirectionalLight.ALLOW_COLOR_WRITE);
//		lightD2.setInfluencingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 100000.0));
//		lightD2.setColor(new Color3f(0.60f * world.getUnderglowIntensity(), 1.0f * world.getUnderglowIntensity(), 0.60f * world.getUnderglowIntensity())); // slightly green
//		lightD2.setDirection(0.0f, 1.0f, 0.0f);
//		java3dRendering.addChild(lightD2);
//
//		// Iterate through the objects in the model, creating
//		// nodes for them.
//		/**
//		 * moved to rendering thread for (WWObject object : objects) { if (object != null) { Node node =
//		 * object.getJava3dRendering(); if (node == null) { node = object.createJava3dRendering(worldTime); }
//		 * java3dRendering.addChild(node); } }
//		 **/

	}

	@Override
	public IRenderer getRenderer() {
		return renderer;
	}

	// These vars are related to the group drawing optimization (which works on fixed objects with like texture)
	public boolean drawnOnce;
	public GLSurface[][] drawGroups;
	public WWObject[] drawGroupsObject;
	public long lastDrawTime;

	public void draw(long worldTime, boolean shading, int picking, int threedee) {
		
		WWVector temp = new WWVector();

		// And all the objects
		WWObject[] objects = world.objects;
		int lastObjectIndex = world.lastObjectIndex;

		// determine adjustedcameraposition, used for visibility calculations
		WWVector adjustedCameraPosition = renderer.getAdjustedCameraPosition();

//		if (worldTime < 5000 || worldTime > lastDrawTime + 250) {
//			// iterate through all the non-grouped objects finding all that are visible
//			for (int i = 0; i <= lastObjectIndex; i++) {
//				WWObject object = objects[i];
//				if (object != null) {
//					if (object.alwaysRender) {
//						object.visible = true;
//					} else if (object.group == 0 && object.rendering != null) {
//						object.visible = object.renderit; // renderer.isVisible(adjustedCameraPosition, object, worldTime, temp);
//					} else {
//						object.visible = true; // so a rendering is created
//					}
//				}
//			}
//			lastDrawTime = worldTime;
//		}

		// draw non-translucent (non-groups)
		int largestGroup = 0;
		for (int i = 0; i <= lastObjectIndex; i++) {
			WWObject object = objects[i];
			if (object != null && object.rendering != null) {
				largestGroup = (object.group > largestGroup) ? object.group : largestGroup;
				if (!drawnOnce || ((picking > 0 || object.group == 0) && renderer.isVisible(adjustedCameraPosition, object, worldTime, temp))) {
					((GLRendering) object.rendering).draw(worldTime, shading, picking, false, threedee);
				}
			}
		}

		// draw non-translucent groups
		if (picking == 0 && drawnOnce) {
			if (drawGroups == null) {
				drawGroups = new GLSurface[largestGroup][];
				drawGroupsObject = new WWObject[largestGroup];
				for (int g = 1; g <= largestGroup; g++) {
					WWObject tokenObject = null;
					ArrayList<GLSurface> surfacesList = new ArrayList<GLSurface>();
					for (int i = 0; i <= lastObjectIndex; i++) {
						WWObject object = objects[i];
						if (object != null && object.rendering != null && object.group == g) {
							tokenObject = object;
							GLSurface[] sides = ((GLObject) object.rendering).sides;
							for (int s = 0; s < sides.length; s++) {
								if (sides[s] != null && object.getTransparency(s) < 1) {
									surfacesList.add(sides[s]);
								}
							}
						}
					}
					if (tokenObject != null) {
						GLSurface[] surfaces = new GLSurface[surfacesList.size()];
						surfaces = surfacesList.toArray(surfaces);
						drawGroups[g - 1] = surfaces;
						drawGroupsObject[g - 1] = tokenObject;
					}
				}
			}
			for (int g = 0; g < drawGroups.length; g++) {
				WWObject tokenObject = drawGroupsObject[g];
				if (tokenObject != null && tokenObject.rendering != null) { // && tokenObject.renderit) {
					((GLObject) tokenObject.rendering).drawSurfaces(drawGroups[g], worldTime, shading, picking, false, threedee);
				}
			}
		}

		GLES10.glEnable(GLES10.GL_BLEND);
		GLES10.glDepthMask(false);

		// draw translucent (non-groups)
		for (int i = 0; i <= lastObjectIndex; i++) {
			WWObject object = objects[i];
			if (object != null && object.rendering != null && (object.group == 0 || !drawnOnce) && renderer.isVisible(adjustedCameraPosition, object, worldTime, temp)) {
				((GLRendering) object.rendering).draw(worldTime, shading, picking, true, threedee);
			}
		}

		// draw translucent groups
		if (picking == 0 && drawnOnce) {
			for (int g = 0; g < drawGroups.length; g++) {
				WWObject tokenObject = drawGroupsObject[g];
				if (tokenObject != null && tokenObject.rendering != null) { // && tokenObject.renderit) {
					((GLObject) tokenObject.rendering).drawSurfaces(drawGroups[g], worldTime, shading, picking, true, threedee);
				}
			}
		}

		// finally, draw translucent curtains
		if (picking == 0 && drawnOnce) {
			for (int i = 0; i <= lastObjectIndex; i++) {
				WWObject object = objects[i];
				if (object != null && object.rendering != null && object instanceof WWTranslucency && (object.group == 0 || !drawnOnce) && object.renderit) {
					((GLTranslucency) object.rendering).drawCurtain(worldTime, shading, picking, true, threedee);
				}
			}
		}

		GLES10.glDepthMask(true);
		GLES10.glDisable(GLES10.GL_BLEND);

		drawnOnce = true;
	}
	
	public void updateRendering() {
		// not implemented
	}
	
}
