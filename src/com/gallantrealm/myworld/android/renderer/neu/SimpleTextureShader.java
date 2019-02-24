package com.gallantrealm.myworld.android.renderer.neu;

public class SimpleTextureShader extends Shader {
	
	public static final 		String vs = "\n" +
			"precision mediump float;\n" +
			"\n" +
			"attribute vec4 aPosition;\n" +
			"attribute vec3 aNormal;\n" +
			"attribute vec2 aTextureCoord;\n" +
			"\n" +
			"uniform mat4 mvMatrix;\n" +
			"uniform mat4 modelMatrix;\n" +
			"uniform mat4 colorTextureMatrix;\n" +
			"\n" +
			"varying vec2 textureCoord;\n" +
			"varying vec3 mNormal;\n" +
			"\n" +
			"void main() {\n" +
			"	gl_Position = mvMatrix * aPosition;\n" +
			"	textureCoord = (colorTextureMatrix * vec4(aTextureCoord.x, aTextureCoord.y, 1.0, 1.0)).xy;\n" +
			"	mNormal = normalize((modelMatrix * vec4(aNormal, 0.0)).xyz);\n" +
			"}";

	public static final 		String fs = "\n" +
			"precision mediump float;\n" +
			"\n" +
			"uniform vec4 color; // the color of the material\n" +
			"uniform sampler2D colorTexture; // the texture of the material\n" +
			"uniform vec4 sunColor;\n" +
			"uniform vec3 sunPosition;\n" +
			"uniform float sunIntensity;\n" +
			"uniform bool fullBright; \n" +
			"uniform float ambientLightIntensity;\n" +
			"\n" +
			"varying vec2 textureCoord; // the location on the texture\n" +
			"varying vec3 mNormal;\n" +
			"\n" +
			"void main() {\n" +
			"	vec4 textureColor = texture2D(colorTexture, textureCoord); \n" +
			"	float diffuseLightIntensity = (sunIntensity + 0.1) * max(0.0,dot(sunPosition, -mNormal));\n" +
			"	float sValue = fullBright ? 1.0 : max(ambientLightIntensity, diffuseLightIntensity); \n" +
			"	gl_FragColor = sunColor * (color * textureColor * vec4(sValue, sValue, sValue, 1.0)); \n" +
			"}";

	public static final 		String afs = "\n" +
			"precision mediump float;\n" +
			"\n" +
			"uniform vec4 color; // the color of the material\n" +
			"uniform sampler2D colorTexture; // the texture of the material\n" +
			"uniform vec4 sunColor;\n" +
			"uniform vec3 sunPosition;\n" +
			"uniform float sunIntensity;\n" +
			"uniform bool fullBright; \n" +
			"uniform float ambientLightIntensity;\n" +
			"\n" +
			"varying vec2 textureCoord; // the location on the texture\n" +
			"varying vec4 lightZ;\n" +
			"varying float directLightIntensity;\n" +
			"varying vec3 mNormal;\n" +
			"\n" +
			"void main() {\n" +
			"	vec4 textureColor = texture2D(colorTexture, textureCoord); \n" +
			"	if (textureColor.a < 0.5) {\n" +
			"	  discard;\n" +
			"	} \n" +
			"	float diffuseLightIntensity = (sunIntensity + 0.1) * max(0.0,dot(sunPosition, -mNormal));\n" +
			"	float sValue = fullBright ? 1.0 : max(ambientLightIntensity, diffuseLightIntensity); \n" +
			"	gl_FragColor = sunColor * (color * textureColor * vec4(sValue, sValue, sValue, 1.0)); \n" +
			"}";

	public SimpleTextureShader() {
		init(vs, fs, afs);
	}
}
