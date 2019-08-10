package com.gallantrealm.myworld.android.renderer.neu;

import com.gallantrealm.myworld.model.SideAttributes;
import com.gallantrealm.myworld.model.WWObject;
import com.gallantrealm.myworld.model.WWParticleEmitter;

import android.opengl.GLES20;
import android.opengl.Matrix;

/**
 * Creates a primitive with a complex surface shape composed from a two dimensional array of rectangles. Three types of meshes are possible: flat, cylindrical, and spherical. A flat mesh can be used for land and other large flat surfaces.
 * Cylindrical is useful for "carved" objects such as statues and limbs of an avatar. Spherical meshes can simulate round objects like the head of an avatar or a model of a planet.
 */
public class GLParticleEmitter extends GLObject {

	WWParticleEmitter emitter;

	public GLParticleEmitter(NewAndroidRenderer renderer, WWParticleEmitter emitter, long worldTime) {
		super(renderer, emitter, worldTime);
		this.emitter = emitter;

		// Create a simple box to use only for picking draws
		float sizeX = object.sizeX;
		float sizeY = object.sizeY;
		float sizeZ = object.sizeZ;
		Point2f[] polygon = new Point2f[] { new Point2f(-0.5f, -0.5f), new Point2f(0.5f, -0.5f), new Point2f(0.5f, 0.5f), new Point2f(-0.5f, 0.5f) };
		int nvertices = polygon.length;
		Point3f[] sweepPath = new Point3f[] { new Point3f(0.0f, 0.0f, -0.5f), new Point3f(0.0f, 0.0f, 0.5f) };
		Point2f[] sweepRadius = new Point2f[] { new Point2f(1.0f, 1.0f), new Point2f(1.0f, 1.0f) };
		int firstVertex = (int) (0* polygon.length);
		int lastVertex = (int) (1.0f * polygon.length - 0.0001);

		// --------------------------
		// Create the base.
		// --------------------------

		GLSurface baseGeometry;
		baseGeometry = new GLSurface(2, 2, false);
		baseGeometry.setVertex(0, 0, sweepPath[0].x + polygon[0].x * sweepRadius[0].x, sweepPath[0].z, sweepPath[0].y + polygon[0].y * sweepRadius[0].y);
		baseGeometry.setVertex(0, 1, sweepPath[0].x + polygon[1].x * sweepRadius[0].x, sweepPath[0].z, sweepPath[0].y + polygon[1].y * sweepRadius[0].y);
		baseGeometry.setVertex(1, 1, sweepPath[0].x + polygon[2].x * sweepRadius[0].x, sweepPath[0].z, sweepPath[0].y + polygon[2].y * sweepRadius[0].y);
		baseGeometry.setVertex(1, 0, sweepPath[0].x + polygon[3].x * sweepRadius[0].x, sweepPath[0].z, sweepPath[0].y + polygon[3].y * sweepRadius[0].y);
		adjustGeometry(baseGeometry, sizeX, sizeY, sizeZ, 0, 0, 0, 0, 0);
		adjustTextureCoords(baseGeometry, WWObject.SIDE_BOTTOM);
		baseGeometry.generateNormals();
		setSide(WWObject.SIDE_BOTTOM, baseGeometry);

		// --------------------------
		// Create the sides
		// --------------------------

		int nsweeps = sweepPath.length;
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
						} else {
							sideGeometry.setVertex(v - startVertex, i, sweepPath[i].x + polygon[0].x * sweepRadius[i].x, sweepPath[i].z, sweepPath[i].y + polygon[0].y * sweepRadius[i].y);
						}
					}
				}
				adjustGeometry(sideGeometry, sizeX, sizeY, sizeZ, 0, 0, 0, 0, 0);
				sideGeometry.generateNormals();
				adjustTextureCoords(sideGeometry, WWObject.SIDE_SIDE1 + side);
				setSide(WWObject.SIDE_SIDE1 + side, sideGeometry);
			}
		}

		// --------------------------
		// Create the top
		// --------------------------

		GLSurface topGeometry = new GLSurface(2, 2, false);
		topGeometry.setVertex(0, 0, sweepPath[nsweeps - 1].x + polygon[0].x * sweepRadius[nsweeps - 1].x, sweepPath[nsweeps - 1].z, sweepPath[nsweeps - 1].y + polygon[0].y * sweepRadius[nsweeps - 1].y);
		topGeometry.setVertex(1, 0, sweepPath[nsweeps - 1].x + polygon[1].x * sweepRadius[nsweeps - 1].x, sweepPath[nsweeps - 1].z, sweepPath[nsweeps - 1].y + polygon[1].y * sweepRadius[nsweeps - 1].y);
		topGeometry.setVertex(1, 1, sweepPath[nsweeps - 1].x + polygon[2].x * sweepRadius[nsweeps - 1].x, sweepPath[nsweeps - 1].z, sweepPath[nsweeps - 1].y + polygon[2].y * sweepRadius[nsweeps - 1].y);
		topGeometry.setVertex(0, 1, sweepPath[nsweeps - 1].x + polygon[3].x * sweepRadius[nsweeps - 1].x, sweepPath[nsweeps - 1].z, sweepPath[nsweeps - 1].y + polygon[3].y * sweepRadius[nsweeps - 1].y);
		adjustGeometry(topGeometry, sizeX, sizeY, sizeZ, 0, 0, 0, 0, 0);
		topGeometry.generateNormals();
		adjustTextureCoords(topGeometry, WWObject.SIDE_TOP);
		setSide(WWObject.SIDE_TOP, topGeometry);
	}

	float[] particleBuffer;

	@Override
	public void draw(Shader shader, float[] viewMatrix, float[] sunViewMatrix, long worldTime, int drawType, boolean drawtrans) {
		if (drawType == DRAW_TYPE_SHADOW || shader instanceof DepthShader) {
			return;
		}
		if (drawType == DRAW_TYPE_PICKING) {
			float[] modelMatrix = getModelMatrix(worldTime);
			Matrix.multiplyMM(mvMatrix, 0, viewMatrix, 0, modelMatrix, 0);
			Matrix.multiplyMM(sunMvMatrix, 0, sunViewMatrix, 0, modelMatrix, 0);
			Matrix.setIdentityM(textureMatrix, 0);
			float[] color = null;
			int id = object.getId();
			float red = (((id & 0xF00) >> 8) + 0.5f) / 16.0f;
			float green = (((id & 0x00F0) >> 4) + 0.5f) / 16.0f;
			float blue = ((id & 0x00F) + 0.5f) / 16.0f;
			color = new float[] { red, green, blue, 1.0f };
			int textureId = renderer.getTexture("white", true);
			if (textureId != lastTextureId) {
				GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
				GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
				lastTextureId = textureId;
			}
			int bumpTextureId = renderer.getNormalTexture("white", true);
			if (bumpTextureId != lastBumpTextureId) {
				GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
				GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, bumpTextureId);
				lastBumpTextureId = bumpTextureId;
			}
			GLSurface.drawMonolith(shader, sides, drawType, modelMatrix, mvMatrix, sunMvMatrix, textureMatrix, color, 0.0f, true, false);
			return;
		}
		if (emitter.particles == null || !emitter.animating) { // no particles yet
			return;
		}
		SideAttributes sideAttributes = object.sideAttributes[WWObject.SIDE_ALL];
		float trans = sideAttributes.transparency;
		if (trans == 1.0 || trans > 0.0 && !drawtrans) {
			return;
		}
		float red = sideAttributes.red;
		float green = sideAttributes.green;
		float blue = sideAttributes.blue;
		float shininess = sideAttributes.shininess;
		if (drawType == DRAW_TYPE_LEFT_EYE) { // red side
			red = (red * 3 + green + blue) / 5.0f;
			green = 0;
			blue = 0;
		} else if (drawType == DRAW_TYPE_RIGHT_EYE) { // cyan side
			red = 0;
			green = (green * 3 + red) / 4.0f;
			blue = (blue * 3 + red) / 4.0f;
		}
		float[] color = new float[] { red, green, blue, 1.0f - trans };

		int fullBright = 1;
		boolean alphaTest = sideAttributes.alphaTest;
		Matrix.setIdentityM(textureMatrix, 0);
		if (!object.fixed) { // for fixed the texture matrix is baked into the texture coords
			Matrix.scaleM(textureMatrix, 0, 1.0f / sideAttributes.textureScaleX, 1.0f / sideAttributes.textureScaleY, 1.0f);
			Matrix.translateM(textureMatrix, 0, object.getTextureOffsetX(WWObject.SIDE_ALL, worldTime), object.getTextureOffsetY(WWObject.SIDE_ALL, worldTime), 0.0f);
			float textureRotation = object.getTextureRotation(WWObject.SIDE_ALL, worldTime);
			if (textureRotation != 0.0f) {
				Matrix.rotateM(textureMatrix, 0, textureRotation, 0.0f, 0.0f, 1.0f);
			}
		}
		String textureUrl = sideAttributes.textureURL;
		int textureId = renderer.getTexture(textureUrl, sideAttributes.pixelate);
		if (textureId != lastTextureId) {
			GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
			lastTextureId = textureId;
		}
		int bumpTextureId = renderer.getNormalTexture(textureUrl, sideAttributes.pixelate);
		if (bumpTextureId != lastBumpTextureId) {
			GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, bumpTextureId);
			lastBumpTextureId = bumpTextureId;
		}

		// fill in vertices
		float[] vertices = new float[3 * emitter.particleCount];
		short[] extras = new short[2 * emitter.particleCount];
		for (int i = 0; i < emitter.particleCount; i++) {
			vertices[3 * i] = emitter.particles[i].position.x;
			vertices[3 * i + 1] = emitter.particles[i].position.z;
			vertices[3 * i + 2] = emitter.particles[i].position.y;
			// TODO include velocity and time since last move to smoothen animation
			extras[2 * i] = (short) (emitter.particles[i].size * 100);
			extras[2 * i + 1] = (short) (emitter.particles[i].alpha * 100);
			// TODO possibly include size and alpha velocities as well?
		}
		float[] modelMatrix = new float[16];
		Matrix.setIdentityM(modelMatrix, 0);

		// draw points
		shader.drawPoints(emitter.particleCount, vertices, extras, modelMatrix, viewMatrix, sunViewMatrix, textureMatrix, color, shininess, fullBright, alphaTest);
	}

}
