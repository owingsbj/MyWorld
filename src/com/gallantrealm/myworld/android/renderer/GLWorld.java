package com.gallantrealm.myworld.android.renderer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import android.opengl.GLES20;
import android.opengl.Matrix;

import com.gallantrealm.myworld.android.AndroidClientModel;
import com.gallantrealm.myworld.client.renderer.IRenderer;
import com.gallantrealm.myworld.client.renderer.IRendering;
import com.gallantrealm.myworld.model.WWConstant;
import com.gallantrealm.myworld.model.WWObject;
import com.gallantrealm.myworld.model.WWTranslucency;
import com.gallantrealm.myworld.model.WWVector;
import com.gallantrealm.myworld.model.WWWorld;

public class GLWorld implements IRendering {

	private final AndroidRenderer renderer;
	private final WWWorld world;

	public GLWorld(AndroidRenderer renderer, WWWorld world, long worldTime) {
		this.renderer = renderer;
		this.world = world;
	}

	@Override
	public IRenderer getRenderer() {
		return renderer;
	}

	/**
	 * Record the location of all objects in the world at the given time
	 * 
	 * @param worldTime
	 */
	public void snap(long worldTime) {
		WWObject[] objects = world.objects;
		int lastObjectIndex = world.lastObjectIndex;
		for (int i = 0; i <= lastObjectIndex; i++) {
			WWObject object = objects[i];
			if (object != null && object.rendering != null) {
				((GLRendering) object.rendering).snap(worldTime);
			}
		}
	}

	// These vars are related to the group drawing optimization (which works on fixed objects with like texture)
	public boolean drawnOnce;
	public GLSurface[][] drawGroups;
	public WWObject[] drawGroupsObject;

	public void draw(Shader shader, float[] viewMatrix, float[] sunViewMatrix, long worldTime, int drawType) {

		// And all the objects
		WWObject[] objects = world.objects;
		int lastObjectIndex = world.lastObjectIndex;

		GLES20.glDisable(GLES20.GL_BLEND);
		GLES20.glDepthMask(true);

		// draw non-translucent (non-groups)
		int largestGroup = 0;
		for (int i = 0; i <= lastObjectIndex; i++) {
			WWObject object = objects[i];
			if (object != null && object.rendering != null) {
				largestGroup = (object.group > largestGroup) ? object.group : largestGroup;
				if (!drawnOnce || ((drawType == DRAW_TYPE_PICKING || object.group == 0))) { // && renderer.isVisible(adjustedCameraPosition, object, worldTime, temp))) {
					if (!object.shadowless || drawType != DRAW_TYPE_SHADOW) {
						((GLRendering) object.rendering).draw(shader, viewMatrix, sunViewMatrix, worldTime, drawType, false);
					}
				}
			}
		}

		// draw non-translucent groups
		if (drawType != DRAW_TYPE_PICKING && drawnOnce) {
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
								if (sides[s] != null) {
									surfacesList.add(sides[s]);
								}
							}
						}
					}
					if (tokenObject != null) {
						GLSurface[] surfaces = new GLSurface[surfacesList.size()];
						surfacesList.toArray(surfaces);
						drawGroups[g - 1] = surfaces;
						drawGroupsObject[g - 1] = tokenObject;
					}
				}
			}
			for (int g = 0; g < largestGroup; g++) {
				WWObject tokenObject = drawGroupsObject[g];
				if (tokenObject != null && tokenObject.rendering != null && tokenObject.sideAttributes[WWConstant.SIDE_ALL].transparency == 0) { // && tokenObject.renderit) {
					if (!tokenObject.shadowless || drawType != DRAW_TYPE_SHADOW) {
						((GLObject) tokenObject.rendering).drawSurfaces(shader, drawGroups[g], viewMatrix, sunViewMatrix, worldTime, drawType, false);
					}
				}
			}
		}

		if (drawType != DRAW_TYPE_SHADOW && !(shader instanceof DepthShader)) {

			GLES20.glEnable(GLES20.GL_BLEND);
			GLES20.glDepthMask(false);

			// draw translucent (non-groups)
			for (int i = 0; i <= lastObjectIndex; i++) {
				WWObject object = objects[i];
				if (object != null && object.rendering != null && (object.group == 0 || !drawnOnce)) { // && renderer.isVisible(adjustedCameraPosition, object, worldTime, temp)) {
					((GLRendering) object.rendering).draw(shader, viewMatrix, sunViewMatrix, worldTime, drawType, true);
				}
			}

			// draw translucent groups (sorted for better effect)
			if (drawType != DRAW_TYPE_PICKING && drawnOnce) {
				for (int g = 0; g < drawGroups.length; g++) {
					WWObject tokenObject = drawGroupsObject[g];
					if (tokenObject != null && tokenObject.rendering != null && tokenObject.sideAttributes[WWConstant.SIDE_ALL].transparency > 0) { // && tokenObject.renderit) {
						sortSurfaces(drawGroups[g], viewMatrix);
// ((GLObject) tokenObject.rendering).drawSurfaces(shader, drawGroups[g], viewMatrix, sunViewMatrix, worldTime, drawType, true);
						GLSurface[] surface = new GLSurface[1];
						for (int s = 0; s < drawGroups[g].length; s++) {
							surface[0] = drawGroups[g][s];
							((GLObject) tokenObject.rendering).drawSurfaces(shader, surface, viewMatrix, sunViewMatrix, worldTime, drawType, true);
						}
					}
				}
			}

			// finally, draw translucent curtains
			if (drawType != DRAW_TYPE_PICKING && drawnOnce) {
				for (int i = 0; i <= lastObjectIndex; i++) {
					WWObject object = objects[i];
					if (object != null && object.rendering != null && object instanceof WWTranslucency && (object.group == 0 || !drawnOnce) && object.renderit) {
						// ((GLTranslucency) object.rendering).drawCurtain(shader, worldTime, viewMatrix, sunViewMatrix, drawType, true);
					}
				}
			}

			GLES20.glDisable(GLES20.GL_BLEND);
			GLES20.glDepthMask(true);

		}
		drawnOnce = true;
	}

	private void sortSurfaces(GLSurface[] surfaces, float[] viewMatrix) {
		int slen = surfaces.length;
		for (int i = 0; i < slen; i++) {
			Matrix.multiplyMV(surfaces[i].sortVec, 0, viewMatrix, 0, surfaces[i].firstVertex, 0);
		}
		try {
			Arrays.sort(surfaces, new Comparator<GLSurface>() {
				public int compare(GLSurface lhs, GLSurface rhs) {
					return (int) (rhs.sortVec[2] - lhs.sortVec[2]); // z-depth
				}
			});
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
	
	public void updateRendering() {
		// not implemented
	}
	
}
