/**
 * Represents a shader object
 */

package com.gallantrealm.myworld.android.renderer.neu;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import android.opengl.GLES20;

public abstract class Shader {

	// programs, both non-alpha testing (faster) and alpha testing (slower)
	private int program, alphaProgram;

	private int aPositionLocation = -1;
	private int aNormalLocation = -1;
	private int aTangentLocation = -1;
	private int aBitangentLocation = -1;
	private int aTextureCoordLocation = -1;

	private int mvMatrixLocation = -1;
	private int sunMvMatrixLocation = -1;
	private int modelMatrixLocation = -1;
	private int viewMatrixLocation = -1;
	private int colorTextureMatrixLocation = -1;

	private int colorTextureLocation = -1;
	private int shadowMapTextureLocation = -1;
	private int staticShadowMapTextureLocation = -1;
	private int bumpMapTextureLocation = -1;

	private int sunPositionLocation = -1;
	private int sunColorLocation = -1;
	private int sunIntensityLocation = -1;
	private int ambientLightIntensityLocation = -1;
	private int viewPositionLocation = -1;
	private int fullBrightLocation = -1;
	private int colorLocation = -1;
	private int shininessLocation = -1;
	private int fogDensityLocation = -1;
	private int pointDrawLocation = -1;

	public final void setSunPosition(float x, float y, float z) {
		if (sunPositionLocation >= 0) {
			GLES20.glUseProgram(alphaProgram);
			GLES20.glUniform3fv(sunPositionLocation, 1, new float[] { x, z, y }, 0);
			GLES20.glUseProgram(program);
			GLES20.glUniform3fv(sunPositionLocation, 1, new float[] { x, z, y }, 0);
			lastProgram = -1;
		}
	}

	public final void setViewPosition(float x, float y, float z) {
		if (viewPositionLocation >= 0) {
			GLES20.glUseProgram(alphaProgram);
			GLES20.glUniform3fv(viewPositionLocation, 1, new float[] { x, z, y }, 0);
			GLES20.glUseProgram(program);
			GLES20.glUniform3fv(viewPositionLocation, 1, new float[] { x, z, y }, 0);
			lastProgram = -1;
		}
	}

	public final void setSunColor(float red, float green, float blue) {
		if (sunColorLocation >= 0) {
			GLES20.glUseProgram(alphaProgram);
			GLES20.glUniform4fv(sunColorLocation, 1, new float[] { red, green, blue, 1.0f }, 0);
			GLES20.glUseProgram(program);
			GLES20.glUniform4fv(sunColorLocation, 1, new float[] { red, green, blue, 1.0f }, 0);
			lastProgram = -1;
		}
	}

	public final void setSunIntensity(float sunIntensity) {
		if (sunIntensityLocation >= 0) {
			GLES20.glUseProgram(alphaProgram);
			GLES20.glUniform1f(sunIntensityLocation, sunIntensity);
			GLES20.glUseProgram(program);
			GLES20.glUniform1f(sunIntensityLocation, sunIntensity);
			lastProgram = -1;
		}
	}

	public final void setAmbientLightIntensity(float ambientLightIntensity) {
		if (ambientLightIntensityLocation >= 0) {
			GLES20.glUseProgram(alphaProgram);
			GLES20.glUniform1f(ambientLightIntensityLocation, ambientLightIntensity);
			GLES20.glUseProgram(program);
			GLES20.glUniform1f(ambientLightIntensityLocation, ambientLightIntensity);
			lastProgram = -1;
		}
	}

	public final void setFogDensity(float fogDensity) {
		if (fogDensityLocation >= 0) {
			GLES20.glUseProgram(alphaProgram);
			GLES20.glUniform1f(fogDensityLocation, fogDensity);
			GLES20.glUseProgram(program);
			GLES20.glUniform1f(fogDensityLocation, fogDensity);
			lastProgram = -1;
		}
	}

	public final void setViewMatrix(float[] viewMatrix) {
		if (viewMatrixLocation >= 0) {
			GLES20.glUseProgram(alphaProgram);
			GLES20.glUniformMatrix4fv(viewMatrixLocation, 1, false, viewMatrix, 0);
			GLES20.glUseProgram(program);
			GLES20.glUniformMatrix4fv(viewMatrixLocation, 1, false, viewMatrix, 0);
			lastProgram = -1;
		}
	}

	static int lastProgram;
	static int verticesBufferId;
	static int normalsBufferId;
	static int tangentsBufferId;
	static int bitangentsBufferId;
	static int textureCoordsBufferId;
	static int indicesBufferId;
	static int pointVerticesBufferId;
	static FloatBuffer pointVerticesBuffer;
	static int extrasBufferId;
	static ShortBuffer extrasBuffer;

	public static final void setBuffers(int verticesBufferId, int normalsBufferId, int tangentsBufferId, int bitangentsBufferId, int textureCoordsBufferId, int indicesBufferId, int pointVerticesBufferId, FloatBuffer pointVerticesBuffer, int extrasBufferId, ShortBuffer extrasBuffer) {
		Shader.verticesBufferId = verticesBufferId;
		Shader.normalsBufferId = normalsBufferId;
		Shader.tangentsBufferId = tangentsBufferId;
		Shader.bitangentsBufferId = bitangentsBufferId;
		Shader.textureCoordsBufferId = textureCoordsBufferId;
		Shader.indicesBufferId = indicesBufferId;
		Shader.pointVerticesBufferId = pointVerticesBufferId;
		Shader.pointVerticesBuffer = pointVerticesBuffer;
		Shader.extrasBufferId = extrasBufferId;
		Shader.extrasBuffer = extrasBuffer;
	}

	/**
	 * Draw all triangles given the id's of the buffers containing vertex information.
	 * 
	 * @param nindices
	 * @param baseIndex
	 */
	public final void drawTriangles(int nindices, int baseIndex, float[] modelMatrix, float[] mvMatrix, float[] sunMvMatrix, float[] textureMatrix, float[] color, float shininess, int fullBright, boolean alphaTest) {
		int currentProgram;
		if (alphaTest) {
			currentProgram = alphaProgram;
		} else {
			currentProgram = program;
		}
		if (currentProgram != lastProgram) {
			GLES20.glUseProgram(currentProgram);
		}
		if (modelMatrixLocation >= 0) {
			GLES20.glUniformMatrix4fv(modelMatrixLocation, 1, false, modelMatrix, 0);
			//NewAndroidRenderer.checkGlError();
		}
		if (mvMatrixLocation >= 0) {
			GLES20.glUniformMatrix4fv(mvMatrixLocation, 1, false, mvMatrix, 0);
			//NewAndroidRenderer.checkGlError();
		}
		if (sunMvMatrixLocation >= 0) {
			GLES20.glUniformMatrix4fv(sunMvMatrixLocation, 1, false, sunMvMatrix, 0);
			//NewAndroidRenderer.checkGlError();
		}
		if (colorTextureMatrixLocation >= 0) {
			GLES20.glUniformMatrix4fv(colorTextureMatrixLocation, 1, false, textureMatrix, 0);
			//NewAndroidRenderer.checkGlError();
		}
		if (colorLocation >= 0) {
			GLES20.glUniform4fv(colorLocation, 1, color, 0);
			//NewAndroidRenderer.checkGlError();
		}
		if (shininessLocation >= 0) {
			GLES20.glUniform1f(shininessLocation, shininess);
		}
		if (fullBrightLocation >= 0) {
			GLES20.glUniform1i(fullBrightLocation, fullBright);
		}
		if (pointDrawLocation >= 0) {
			GLES20.glUniform1i(pointDrawLocation, 0);
		}

		if (currentProgram != lastProgram) { // need to rebind
			if (aPositionLocation >= 0) {
				GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, verticesBufferId);
				GLES20.glEnableVertexAttribArray(aPositionLocation);
				GLES20.glVertexAttribPointer(aPositionLocation, 3, GLES20.GL_FLOAT, false, 3 * 4, 0); // vertices);
			}
			if (aNormalLocation >= 0) {
				GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, normalsBufferId);
				GLES20.glEnableVertexAttribArray(aNormalLocation);
				GLES20.glVertexAttribPointer(aNormalLocation, 3, GLES20.GL_BYTE, false, 3 * 1, 0); // normals);
			}
			if (aTangentLocation >= 0) {
				GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, tangentsBufferId);
				GLES20.glEnableVertexAttribArray(aTangentLocation);
				GLES20.glVertexAttribPointer(aTangentLocation, 3, GLES20.GL_BYTE, false, 3 * 1, 0); // tangents);
			}
			if (aBitangentLocation >= 0) {
				GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bitangentsBufferId);
				GLES20.glEnableVertexAttribArray(aBitangentLocation);
				GLES20.glVertexAttribPointer(aBitangentLocation, 3, GLES20.GL_BYTE, false, 3 * 1, 0); // bitangents);
			}
			if (aTextureCoordLocation >= 0) {
				GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, textureCoordsBufferId);
				GLES20.glEnableVertexAttribArray(aTextureCoordLocation);
				GLES20.glVertexAttribPointer(aTextureCoordLocation, 2, GLES20.GL_FLOAT, false, 2 * 4, 0); // textureCoords);
			}
			GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indicesBufferId);
			lastProgram = currentProgram;
		}
		GLES20.glDrawElements(GLES20.GL_TRIANGLES, nindices, GLES20.GL_UNSIGNED_SHORT, baseIndex * 2); // indices);
		//NewAndroidRenderer.checkGlError();
	}

	/**
	 * Draw all points given the id's of the buffers containing vertex information.
	 * 
	 * @param nindices
	 * @param baseIndex
	 */
	public final void drawPoints(int nindices, float[] pointVertices, short[] extras, float[] modelMatrix, float[] mvMatrix, float[] sunMvMatrix, float[] textureMatrix, float[] color, float shininess, int fullBright, boolean alphaTest) {
		int currentProgram;
		if (alphaTest) {
			currentProgram = alphaProgram;
		} else {
			currentProgram = program;
		}
		if (currentProgram != lastProgram) {
			GLES20.glUseProgram(currentProgram);
		}
		// use a unique vertex buffer since point data changes
		if (aPositionLocation >= 0) {
			pointVerticesBuffer.clear();
			pointVerticesBuffer.put(pointVertices);
			pointVerticesBuffer.position(0);
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, pointVerticesBufferId);
			GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, nindices * 4 * 3,  pointVerticesBuffer, GLES20.GL_DYNAMIC_DRAW);
			GLES20.glEnableVertexAttribArray(aPositionLocation);
			GLES20.glVertexAttribPointer(aPositionLocation, 3, GLES20.GL_FLOAT, false, 3 * 4, 0); // vertices);
		}
		if (aTextureCoordLocation >= 0) {
			extrasBuffer.clear();
			extrasBuffer.put(extras);
			extrasBuffer.position(0);
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, extrasBufferId);
			GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, nindices * 2 * 2,  extrasBuffer, GLES20.GL_DYNAMIC_DRAW);
			GLES20.glEnableVertexAttribArray(aTextureCoordLocation);
			GLES20.glVertexAttribPointer(aTextureCoordLocation, 2, GLES20.GL_SHORT, false, 2 * 2, 0); // textureCoords);
		}
		
		if (modelMatrixLocation >= 0) {
			GLES20.glUniformMatrix4fv(modelMatrixLocation, 1, false, modelMatrix, 0);
			//NewAndroidRenderer.checkGlError();
		}
		if (mvMatrixLocation >= 0) {
			GLES20.glUniformMatrix4fv(mvMatrixLocation, 1, false, mvMatrix, 0);
			//NewAndroidRenderer.checkGlError();
		}
		if (sunMvMatrixLocation >= 0) {
			GLES20.glUniformMatrix4fv(sunMvMatrixLocation, 1, false, sunMvMatrix, 0);
			//NewAndroidRenderer.checkGlError();
		}
		if (colorTextureMatrixLocation >= 0) {
			GLES20.glUniformMatrix4fv(colorTextureMatrixLocation, 1, false, textureMatrix, 0);
			//NewAndroidRenderer.checkGlError();
		}
		if (colorLocation >= 0) {
			GLES20.glUniform4fv(colorLocation, 1, color, 0);
			//NewAndroidRenderer.checkGlError();
		}
		if (shininessLocation >= 0) {
			GLES20.glUniform1f(shininessLocation, shininess);
		}
		if (fullBrightLocation >= 0) {
			GLES20.glUniform1i(fullBrightLocation, 1); //fullBright);
		}
		if (pointDrawLocation >= 0) {
			GLES20.glUniform1i(pointDrawLocation, 1);
		}

		GLES20.glDrawArrays(GLES20.GL_POINTS, 0, nindices);
		//NewAndroidRenderer.checkGlError();
		
		// restore usual vertex and texture buffers
		if (aPositionLocation >= 0) {
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, verticesBufferId);
			GLES20.glEnableVertexAttribArray(aPositionLocation);
			GLES20.glVertexAttribPointer(aPositionLocation, 3, GLES20.GL_FLOAT, false, 3 * 4, 0); // vertices);
		}
		if (aTextureCoordLocation >= 0) {
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, textureCoordsBufferId);
			GLES20.glEnableVertexAttribArray(aTextureCoordLocation);
			GLES20.glVertexAttribPointer(aTextureCoordLocation, 2, GLES20.GL_SHORT, false, 2 * 2, 0); // textureCoords);
		}
		
	}

	/**
	 * Sets up everything
	 * 
	 * @param vs
	 *            the vertex shader
	 * @param fs
	 *            the fragment shader
	 * @param afs
	 *            alpha-testing fragment shader
	 */
	public final void init(String vs, String fs, String afs) {

		// create the programs
		program = createProgram(vs, fs);
		alphaProgram = createProgram(vs, afs);

		// get the locations of all shader vars (assumed same for program and alphaProgram)
		aPositionLocation = GLES20.glGetAttribLocation(program, "aPosition");
		aNormalLocation = GLES20.glGetAttribLocation(program, "aNormal");
		aTangentLocation = GLES20.glGetAttribLocation(program, "aTangent");
		aBitangentLocation = GLES20.glGetAttribLocation(program, "aBitangent");
		aTextureCoordLocation = GLES20.glGetAttribLocation(program, "aTextureCoord");

		sunPositionLocation = GLES20.glGetUniformLocation(program, "sunPosition");
		sunColorLocation = GLES20.glGetUniformLocation(program, "sunColor");
		sunIntensityLocation = GLES20.glGetUniformLocation(program, "sunIntensity");
		ambientLightIntensityLocation = GLES20.glGetUniformLocation(program, "ambientLightIntensity");
		fullBrightLocation = GLES20.glGetUniformLocation(program, "fullBright");
		mvMatrixLocation = GLES20.glGetUniformLocation(program, "mvMatrix");
		sunMvMatrixLocation = GLES20.glGetUniformLocation(program, "sunMvMatrix");
		modelMatrixLocation = GLES20.glGetUniformLocation(program, "modelMatrix");
		viewMatrixLocation = GLES20.glGetUniformLocation(program, "viewMatrix");
		viewPositionLocation = GLES20.glGetUniformLocation(program, "viewPosition");
		colorTextureMatrixLocation = GLES20.glGetUniformLocation(program, "colorTextureMatrix");

		colorLocation = GLES20.glGetUniformLocation(program, "color");
		shininessLocation = GLES20.glGetUniformLocation(program, "shininess");
		colorTextureLocation = GLES20.glGetUniformLocation(program, "colorTexture");
		shadowMapTextureLocation = GLES20.glGetUniformLocation(program, "shadowMapTexture");
		staticShadowMapTextureLocation = GLES20.glGetUniformLocation(program, "staticShadowMapTexture");
		bumpMapTextureLocation = GLES20.glGetUniformLocation(program, "bumpTexture");
		fogDensityLocation = GLES20.glGetUniformLocation(program, "fogDensity");
		pointDrawLocation = GLES20.glGetUniformLocation(program, "pointDraw");

		// Some of the glGetUniforms or glUniforms fail (expected)
		NewAndroidRenderer.ignoreGlError();

		GLES20.glUseProgram(alphaProgram);
		NewAndroidRenderer.checkGlError();
		GLES20.glUniform1i(colorTextureLocation, 0);
		NewAndroidRenderer.checkGlError();
		GLES20.glUniform1i(shadowMapTextureLocation, 1);
		NewAndroidRenderer.checkGlError();
		GLES20.glUniform1i(staticShadowMapTextureLocation, 2);
		NewAndroidRenderer.checkGlError();
		GLES20.glUniform1i(bumpMapTextureLocation, 3);
		NewAndroidRenderer.checkGlError();
		
		GLES20.glUseProgram(program);
		GLES20.glUniform1i(colorTextureLocation, 0);
		GLES20.glUniform1i(shadowMapTextureLocation, 1);
		GLES20.glUniform1i(staticShadowMapTextureLocation, 2);
		GLES20.glUniform1i(bumpMapTextureLocation, 3);
		NewAndroidRenderer.checkGlError();

	}

	/**
	 * Creates a shader program.
	 * 
	 * @param vertexSource
	 * @param fragmentSource
	 * @return returns program
	 */
	private final int createProgram(String vs, String fs) {
		
		System.out.println(this.getClass().getSimpleName()+": Loading vertex shader");
		int _vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vs);
		NewAndroidRenderer.checkGlError();
		
		System.out.println(this.getClass().getSimpleName()+": Loading fragment shader");
		int _pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fs);
		NewAndroidRenderer.checkGlError();

		// Create the program
		System.out.println(this.getClass().getSimpleName()+":  Linking shader ");
		int program = GLES20.glCreateProgram();
		GLES20.glAttachShader(program, _vertexShader);
		NewAndroidRenderer.checkGlError();
		GLES20.glAttachShader(program, _pixelShader);
		NewAndroidRenderer.checkGlError();
		GLES20.glLinkProgram(program);
		int[] linkStatus = new int[1];
		GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
		System.err.println("  link status = "+linkStatus[0]);
		if (linkStatus[0] != GLES20.GL_TRUE) {
			System.err.println("Linking of "+this.getClass().getSimpleName()+" failed: " + GLES20.glGetProgramInfoLog(program));
			GLES20.glDeleteProgram(program);
		}
		NewAndroidRenderer.checkGlError();

		return program;
	}

	/**
	 * Loads a shader (either vertex or fragment) given the source
	 * 
	 * @param shaderType
	 *            VERTEX or PIXEL
	 * @param source
	 *            The string data representing the shader code
	 * @return handle for shader
	 */
	private final int loadShader(int shaderType, String source) {
		int shader = GLES20.glCreateShader(shaderType);
		NewAndroidRenderer.checkGlError();
		if (shader != 0) {
			GLES20.glShaderSource(shader, source);
			GLES20.glCompileShader(shader);
			NewAndroidRenderer.checkGlError();
			int[] compiled = new int[1];
			GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
			if (compiled[0] == 0) {
				System.err.println("Could not compile shader " + shaderType + ":" + GLES20.glGetShaderInfoLog(shader));
				NewAndroidRenderer.checkGlError();
			}
		}
		return shader;
	}

}
