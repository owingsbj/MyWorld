package com.gallantrealm.myworld.android.renderer.neu;

import com.gallantrealm.myworld.model.SideAttributes;
import com.gallantrealm.myworld.model.WWObject;
import com.gallantrealm.myworld.model.WWParticleEmitter;
import android.opengl.GLES20;
import android.opengl.Matrix;

/**
 * Creates a primitive with a complex surface shape composed from a two dimensional array of rectangles. Three types of
 * meshes are possible: flat, cylindrical, and spherical. A flat mesh can be used for land and other large flat
 * surfaces. Cylindrical is useful for "carved" objects such as statues and limbs of an avatar. Spherical meshes can
 * simulate round objects like the head of an avatar or a model of a planet.
 */
public class GLParticleEmitter extends GLObject {

	WWParticleEmitter emitter;

	public GLParticleEmitter(NewAndroidRenderer renderer, WWParticleEmitter emitter, long worldTime) {
		super(renderer, emitter, worldTime);
		this.emitter = emitter;
	}

	float[] particleBuffer;

	@Override
	public void draw(Shader shader, float[] viewMatrix, float[] sunViewMatrix, long worldTime, int drawType, boolean drawtrans) {
		if (shader instanceof ShadowMapShader || shader instanceof DepthShader) {
			return;
		}
		if (emitter.particles == null || !emitter.animating) {  // no particles yet
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
		float[] vertices = new float[3*emitter.particleCount];
		short[] extras = new short[2 * emitter.particleCount];
		for (int i = 0; i < emitter.particleCount; i++) {
			vertices[3*i] = emitter.particles[i].position.x;
			vertices[3*i+1] = emitter.particles[i].position.z;
			vertices[3*i+2] = emitter.particles[i].position.y;
			// TODO include velocity and time since last move to smoothen animation
			extras[2*i] = (short)(emitter.particles[i].size * 100);
			extras[2*i + 1] = (short)(emitter.particles[i].alpha * 100);
			// TODO possibly include size and alpha velocities as well?
		}
		float[] modelMatrix = new float[16];
		Matrix.setIdentityM(modelMatrix, 0);
		
		// draw points
		shader.drawPoints(emitter.particleCount, vertices, extras, modelMatrix, viewMatrix, sunViewMatrix, textureMatrix, color, shininess, fullBright, alphaTest);
	}
	
}
