package com.gallantrealm.myworld.android.renderer.neu;

public class CopyOfShadowingTextureShader extends Shader {
	
	public static final 		String vs = "\n" +
			"precision mediump float;\n" +
			"\n" +
			"attribute vec4 aPosition;\n" +
			"attribute vec3 aNormal;\n" +
			"attribute vec3 aTangent;\n" +
			"attribute vec3 aBitangent;\n" +
			"attribute vec2 aTextureCoord;\n" +
			"\n" +
			"uniform vec3 sunPosition;\n" +
			"uniform vec4 sunColor;\n" +
			"uniform float sunIntensity;\n" +
			"uniform float ambientLightIntensity;\n" +
			"uniform mat4 mvMatrix;\n" +
			"uniform mat4 sunMvMatrix;\n" +
			"uniform mat4 modelMatrix;\n" +
			"uniform mat4 viewMatrix;\n" +
			"uniform mat4 colorTextureMatrix;\n" +
			"uniform vec3 viewPosition; \n" +
			"\n" +
			"varying vec2 textureCoord;\n" +
			"varying highp vec4 shadowCoord;\n" +
			"varying vec3 mNormal;\n" +
			"varying vec3 mTangent;\n" +
			"varying vec3 mBitangent;\n" +
			"varying vec3 surfaceToCamera; \n" +
			"varying vec3 surfaceToLight; \n" +
			"varying float fogDepth;\n" +
			"\n" +
			"void main() {\n" +
			"	gl_Position = mvMatrix * aPosition;\n" +
			"	shadowCoord = sunMvMatrix * aPosition;\n" +
			"	shadowCoord = (shadowCoord / shadowCoord.w + 1.0) /2.0;\n" +
			"	mNormal = normalize((modelMatrix * vec4(aNormal, 0.0)).xyz);\n" +
			"	mTangent = normalize((modelMatrix * vec4(aTangent, 0.0)).xyz);\n" +
			"	mBitangent = normalize((modelMatrix * vec4(aBitangent, 0.0)).xyz);\n" +
			"  surfaceToCamera = normalize(viewPosition); \n" +
			"  surfaceToLight = normalize(sunPosition); \n" +
			"	textureCoord = (colorTextureMatrix * vec4(aTextureCoord.x / 100.0, aTextureCoord.y / 100.0, 1.0, 1.0)).xy;\n" +
			"	fogDepth = gl_Position.z;\n" +
			"}";
	
	public static final 		String fs = "\n" +
			"precision mediump float; \n" +
			"\n" +
			"uniform vec4 color; \n" +
			"uniform sampler2D colorTexture; \n" +
			"uniform vec4 sunColor;\n" +
			"uniform float shininess; \n" +
			"uniform float sunIntensity; \n" +
			"uniform bool fullBright; \n" +
			"uniform float ambientLightIntensity; \n" +
			"uniform sampler2D shadowMapTexture; \n" +
			"uniform vec3 sunPosition;\n" +
			"uniform vec3 viewPosition;\n" +
			"uniform sampler2D bumpTexture; \n" +
			"\n" +
			"varying vec2 textureCoord; // the location on the texture \n" +
			"varying vec3 mNormal;\n" +
			"varying vec3 mTangent;\n" +
			"varying vec3 mBitangent;\n" +
			"varying vec3 surfaceToCamera; \n" +
			"varying vec3 surfaceToLight; \n" +
			"varying highp vec4 shadowCoord; // location on the shadow map \n" +
			"\n" +
			"uniform float fogDensity; \n" +
			"varying float fogDepth; \n" +
			"\n" +
			"void main() { \n" +
			"	vec4 textureColor = texture2D(colorTexture, textureCoord); \n" +
			"	float shadow = 1.0; \n" +
			"  if (shadowCoord.x > 0.0 && shadowCoord.y > 0.0 && shadowCoord.x < 1.0 && shadowCoord.y < 1.0) { \n" +
			"    float shadow1 = (texture2D(shadowMapTexture, shadowCoord.xy).z < shadowCoord.z) ? 0.0 : 1.0; \n" +
			"    float shadow2 = (texture2D(shadowMapTexture, vec2(shadowCoord.x+0.0002, shadowCoord.y)).z < shadowCoord.z) ? 0.0 : 1.0; \n" +
			"    float shadow3 = (texture2D(shadowMapTexture, vec2(shadowCoord.x, shadowCoord.y+0.0002)).z < shadowCoord.z) ? 0.0 : 1.0; \n" +
			"    float shadow4 = (texture2D(shadowMapTexture, vec2(shadowCoord.x+0.0001, shadowCoord.y+0.0001)).z < shadowCoord.z) ? 0.0 : 1.0; \n" +
			"    shadow = (shadow1 + shadow2 + shadow3 + shadow4) / 4.0; \n" +
			"	} \n" +
			"  float diffuseLightIntensity = 0.0; \n" +
			"  float specularLightIntensity = 0.0; \n" +
			"  if (fullBright) { \n" +
			"    diffuseLightIntensity = 1.0; \n" +
			"  } else { \n" +
			"    if (shadow >= 0.9) { \n " +
			"      vec4 bump = (texture2D(bumpTexture, textureCoord) - 0.5) * 2.0; \n" +
			"      vec3 bumpNormal = bump.r * mTangent + bump.g * mBitangent + bump.b * mNormal; \n" +
			"	    diffuseLightIntensity = sunIntensity * max(-0.5,dot(sunPosition, -bumpNormal));\n" +
			"	    float specular = max(0.0, dot(surfaceToCamera, reflect(-surfaceToLight, bumpNormal))); \n" +
			"	    specularLightIntensity = sunIntensity * clamp(pow(specular, 1.0+ 50.0* shininess) * 2.5 * shininess, 0.0, 1.0); \n" +
			"    } \n" +
			"  } \n" +
			"	float sValue = max(ambientLightIntensity, diffuseLightIntensity) - min(0.1*diffuseLightIntensity, 0.0); \n" +
			"	gl_FragColor = sunColor * (specularLightIntensity * vec4(1.0) + color * textureColor * vec4(sValue, sValue, sValue, 1.0)); \n" +
			"	if (fogDensity > 0.0) { \n" +
			"		float fog = clamp(exp(-fogDensity*fogDensity * abs(fogDepth) / 25.0  ), 0.0, 1.0); \n" +
			"		gl_FragColor = mix(vec4(1.0), gl_FragColor, fog); \n" +
			"	} \n" +
			"}";

	public static final 		String afs = "\n" +
			"precision mediump float; \n" +
			"\n" +
			"uniform vec4 color; \n" +
			"uniform sampler2D colorTexture; \n" +
			"uniform vec4 sunColor;\n" +
			"uniform float shininess; \n" +
			"uniform float sunIntensity; \n" +
			"uniform bool fullBright; \n" +
			"uniform float ambientLightIntensity; \n" +
			"uniform sampler2D shadowMapTexture; \n" +
			"uniform vec3 sunPosition;\n" +
			"uniform vec3 viewPosition;\n" +
			"uniform sampler2D bumpTexture; \n" +
			"\n" +
			"varying vec2 textureCoord; // the location on the texture \n" +
			"varying vec3 mNormal;\n" +
			"varying vec3 mTangent;\n" +
			"varying vec3 mBitangent;\n" +
			"varying vec3 surfaceToCamera; \n" +
			"varying vec3 surfaceToLight; \n" +
			"varying highp vec4 shadowCoord; // location on the shadow map \n" +
			"\n" +
			"uniform float fogDensity; \n" +
			"varying float fogDepth; \n" +
			"\n" +
			"void main() { \n" +
			"	vec4 textureColor = texture2D(colorTexture, textureCoord); \n" +
			"	if (textureColor.a <= 0.5) {\n" +
			"	  discard;\n" +
			"	} \n" +
			"	float shadow = 1.0; \n" +
			"  if (shadowCoord.x > 0.0 && shadowCoord.y > 0.0 && shadowCoord.x < 1.0 && shadowCoord.y < 1.0) { \n" +
			"    float shadow1 = (texture2D(shadowMapTexture, shadowCoord.xy).z < shadowCoord.z) ? 0.0 : 1.0; \n" +
			"    float shadow2 = (texture2D(shadowMapTexture, vec2(shadowCoord.x+0.0002, shadowCoord.y)).z < shadowCoord.z) ? 0.0 : 1.0; \n" +
			"    float shadow3 = (texture2D(shadowMapTexture, vec2(shadowCoord.x, shadowCoord.y+0.0002)).z < shadowCoord.z) ? 0.0 : 1.0; \n" +
			"    float shadow4 = (texture2D(shadowMapTexture, vec2(shadowCoord.x+0.0001, shadowCoord.y+0.0001)).z < shadowCoord.z) ? 0.0 : 1.0; \n" +
			"    shadow = (shadow1 + shadow2 + shadow3 + shadow4) / 4.0; \n" +
			"	} \n" +
			"  float diffuseLightIntensity = 0.0; \n" +
			"  float specularLightIntensity = 0.0; \n" +
			"  if (fullBright) { \n" +
			"    diffuseLightIntensity = 1.0; \n" +
			"  } else { \n" +
			"    if (shadow >= 0.9) { \n " +
			"      vec4 bump = (texture2D(bumpTexture, textureCoord) - 0.5) * 2.0; \n" +
			"      vec3 bumpNormal = bump.r * mTangent + bump.g * mBitangent + bump.b * mNormal; \n" +
			"	    diffuseLightIntensity = sunIntensity * max(-0.5,dot(sunPosition, -bumpNormal));\n" +
			"	    float specular = max(0.0, dot(surfaceToCamera, reflect(-surfaceToLight, bumpNormal))); \n" +
			"	    specularLightIntensity = sunIntensity * clamp(pow(specular, 1.0+ 50.0* shininess) * 2.5 * shininess, 0.0, 1.0); \n" +
			"    } \n" +
			"  } \n" +
			"	float sValue = max(ambientLightIntensity, diffuseLightIntensity) - min(0.1*diffuseLightIntensity, 0.0); \n" +
			"	gl_FragColor = sunColor * (specularLightIntensity * vec4(1.0) + color * textureColor * vec4(sValue, sValue, sValue, 1.0)); \n" +
			"	if (fogDensity > 0.0) { \n" +
			"		float fog = clamp(exp(-fogDensity*fogDensity * abs(fogDepth) / 25.0  ), 0.0, 1.0); \n" +
			"		gl_FragColor = mix(vec4(1.0), gl_FragColor, fog); \n" +
			"	} \n" +
			"}";

	public CopyOfShadowingTextureShader() {
		init(vs, fs, afs);
	}
	
}
