package com.gallantrealm.myworld.android.renderer;

public class ShadowingTextureShader extends Shader {
	
	public static final 		String vs = "\n" + //
			"precision highp float;\n" + //
			"\n" + //
			"attribute vec4 aPosition;\n" + //
			"attribute lowp vec3 aNormal;\n" + //
			"attribute lowp vec3 aTangent;\n" + //
			"attribute lowp vec3 aBitangent;\n" + //
			"attribute lowp vec2 aTextureCoord;\n" + //
			"\n" + //
			"uniform vec3 sunPosition;\n" + //
			"uniform lowp vec4 sunColor;\n" + //
			"uniform lowp float sunIntensity;\n" + //
			"uniform lowp float ambientLightIntensity;\n" + //
			"uniform mat4 mvMatrix;\n" + //
			"uniform mat4 sunMvMatrix;\n" + //
			"uniform mat4 modelMatrix;\n" + //
			"uniform mat4 viewMatrix;\n" + //
			"uniform mat4 colorTextureMatrix;\n" + //
			"uniform vec3 viewPosition; \n" + //
			"uniform bool pointDraw; \n" + //
			"\n" + //
			"varying lowp vec2 textureCoord;\n" + //
			"varying highp vec4 shadowCoord;\n" + //
			"varying lowp vec3 mNormal;\n" + //
			"varying lowp vec3 mTangent;\n" + //
			"varying lowp vec3 mBitangent;\n" + //
			"varying vec3 surfaceToCamera; \n" + //
			"varying vec3 surfaceToLight; \n" + //
			"varying float fogDepth;\n" + //
			"varying float pointAlpha; \n" + //
			"\n" + //
			"void main() {\n" + //
			"	gl_Position = mvMatrix * aPosition;\n" + //
			"	shadowCoord = sunMvMatrix * aPosition; \n" + //
			"	shadowCoord = (shadowCoord / shadowCoord.w + 1.0) /2.0;\n" + //
			"	mNormal = normalize((modelMatrix * vec4(aNormal, 0.0)).xyz);\n" + //
			"	mTangent = normalize((modelMatrix * vec4(aTangent, 0.0)).xyz);\n" + //
			"	mBitangent = normalize((modelMatrix * vec4(aBitangent, 0.0)).xyz);\n" + //
			"  surfaceToCamera = normalize(viewPosition); \n" + //
			"  surfaceToLight = normalize(sunPosition); \n" + //
			"	textureCoord = (colorTextureMatrix * vec4(aTextureCoord.x, aTextureCoord.y, 1.0, 1.0)).xy;\n" + //
			"	fogDepth = gl_Position.z;\n" + //
			"  if (pointDraw) { \n" + //
			"    gl_PointSize = aTextureCoord.x * 10.0 / (1.0 + gl_Position.z); \n" + //
			"    pointAlpha = aTextureCoord.y / 100.0; \n" + //
			"  } \n" + //
			"}";
	
	public static final 		String fs = "\n" + //
			"precision highp float; \n" + //
			"\n" + //
			"uniform lowp vec4 color; \n" + //
			"uniform lowp sampler2D colorTexture; \n" + //
			"uniform lowp vec4 sunColor;\n" + //
			"uniform lowp float shininess; \n" + //
			"uniform lowp float sunIntensity; \n" + //
			"uniform bool fullBright; \n" + //
			"uniform lowp float ambientLightIntensity; \n" + //
			"uniform sampler2D shadowMapTexture; \n" + //
			"uniform vec3 sunPosition;\n" + //
			"uniform vec3 viewPosition;\n" + //
			"uniform lowp sampler2D bumpTexture; \n" + //
			"uniform bool pointDraw; \n" + //
			"\n" + //
			"varying lowp vec2 textureCoord; // the location on the texture \n" + //
			"varying lowp vec3 mNormal;\n" + //
			"varying lowp vec3 mTangent;\n" + //
			"varying lowp vec3 mBitangent;\n" + //
			"varying vec3 surfaceToCamera; \n" + //
			"varying vec3 surfaceToLight; \n" + //
			"varying highp vec4 shadowCoord; // location on the shadow map \n" + //
			"varying float pointAlpha; \n" + //
			"\n" + //
			"uniform lowp float fogDensity; \n" + //
			"varying float fogDepth; \n" + //
			"\n" + //
			"void main() { \n" + //
			"	lowp vec4 textureColor = texture2D(colorTexture, textureCoord); \n" + //
			"	if (pointDraw) { \n" + //
			"	  textureColor = texture2D(colorTexture, gl_PointCoord) * vec4(1.0, 1.0, 1.0, pointAlpha); \n" + //
			"	} \n" + //
			"	lowp float shadow = 1.0; \n" + //
			"  if (shadowCoord.x > 0.0 && shadowCoord.y > 0.0 && shadowCoord.x < 1.0 && shadowCoord.y < 1.0) { \n" + //
			"    shadow = (texture2D(shadowMapTexture, shadowCoord.xy).z <= shadowCoord.z) ? 0.0 : 1.0; \n" + //
			"    lowp float shadow2 = (texture2D(shadowMapTexture, vec2(shadowCoord.x-0.0006, shadowCoord.y)).z <= shadowCoord.z) ? 0.0 : 1.0; \n" + //
			"    lowp float shadow3 = (texture2D(shadowMapTexture, vec2(shadowCoord.x+0.0006, shadowCoord.y)).z <= shadowCoord.z) ? 0.0 : 1.0; \n" + //
			"    lowp float shadow4 = (texture2D(shadowMapTexture, vec2(shadowCoord.x, shadowCoord.y-0.0006)).z <= shadowCoord.z) ? 0.0 : 1.0; \n" + //
			"    lowp float shadow5 = (texture2D(shadowMapTexture, vec2(shadowCoord.x, shadowCoord.y+0.0006)).z <= shadowCoord.z) ? 0.0 : 1.0; \n" + //
			"    lowp float shadow6 = (texture2D(shadowMapTexture, vec2(shadowCoord.x+0.00042, shadowCoord.y+0.00042)).z <= shadowCoord.z) ? 0.0 : 1.0; \n" + //
			"    lowp float shadow7 = (texture2D(shadowMapTexture, vec2(shadowCoord.x+0.00042, shadowCoord.y-0.00042)).z <= shadowCoord.z) ? 0.0 : 1.0; \n" + //
			"    lowp float shadow8 = (texture2D(shadowMapTexture, vec2(shadowCoord.x-0.00042, shadowCoord.y+0.00042)).z <= shadowCoord.z) ? 0.0 : 1.0; \n" + //
			"    lowp float shadow9 = (texture2D(shadowMapTexture, vec2(shadowCoord.x-0.00042, shadowCoord.y-0.00042)).z <= shadowCoord.z) ? 0.0 : 1.0; \n" + //
			"    shadow = (shadow + shadow2 + shadow3 + shadow4 + shadow5 + shadow6 + shadow7 + shadow8 + shadow9) / 9.0; \n" + // 
			"	} \n" + //
			"  lowp float diffuseLightIntensity = 0.0; \n" + //
			"  lowp float specularLightIntensity = 0.0; \n" + //
			"  lowp float bumpReveal = 0.0;\n" + //
			"  if (fullBright) { \n" + //
			"    diffuseLightIntensity = 1.0; \n" + //
			"  } else { \n" + //
			"      lowp vec4 bump = (texture2D(bumpTexture, textureCoord) - 0.5) * 2.0; \n" + //
			"      lowp vec3 bumpNormal = bump.r * mTangent + bump.g * mBitangent + bump.b * mNormal; \n" + //
			"	    diffuseLightIntensity = shadow * sunIntensity * max(0.0,dot(sunPosition, -bumpNormal));\n" + //
			"      bumpReveal = min(0.0, shadow * dot(sunPosition, -bumpNormal) * 0.25 * ambientLightIntensity); \n" + //
			"	    lowp float specular = max(0.0, dot(surfaceToCamera, reflect(-surfaceToLight, bumpNormal))); \n" + //
			"	    specularLightIntensity = shadow * sunIntensity * clamp(pow(specular, 1.0 + 50.0* shininess) * 2.5 * shininess, 0.0, 1.0); \n" + //
			"  } \n" + //
			"	lowp float sValue = max(ambientLightIntensity+bumpReveal, diffuseLightIntensity); \n" + //
			"	gl_FragColor = sunColor * (specularLightIntensity * vec4(1.0) + color * textureColor * vec4(sValue, sValue, sValue, 1.0)); \n" + //
			"	if (fogDensity > 0.0) { \n" + //
			"		lowp float fog = clamp(exp(-fogDensity*fogDensity * abs(fogDepth) * abs(fogDepth) / 5000.0  ), 0.0, 1.0); \n" + //
			"		gl_FragColor = mix(vec4(1.0), gl_FragColor, fog); \n" + //
			"	} \n" + //
			"}";

	public static final 		String afs = "\n" + //
			"precision highp float; \n" + //
			"\n" + //
			"uniform lowp vec4 color; \n" + //
			"uniform lowp sampler2D colorTexture; \n" + //
			"uniform lowp vec4 sunColor;\n" + //
			"uniform lowp float shininess; \n" + //
			"uniform lowp float sunIntensity; \n" + //
			"uniform bool fullBright; \n" + //
			"uniform lowp float ambientLightIntensity; \n" + //
			"uniform sampler2D shadowMapTexture; \n" + //
			"uniform vec3 sunPosition;\n" + //
			"uniform vec3 viewPosition;\n" + //
			"uniform lowp sampler2D bumpTexture; \n" + //
			"uniform bool pointDraw; \n" + //
			"\n" + //
			"varying lowp vec2 textureCoord; // the location on the texture \n" + //
			"varying lowp vec3 mNormal;\n" + //
			"varying lowp vec3 mTangent;\n" + //
			"varying lowp vec3 mBitangent;\n" + //
			"varying vec3 surfaceToCamera; \n" + //
			"varying vec3 surfaceToLight; \n" + //
			"varying highp vec4 shadowCoord; // location on the shadow map \n" + //
			"varying float pointAlpha; \n" + //
			"\n" + //
			"uniform lowp float fogDensity; \n" + //
			"varying float fogDepth; \n" + //
			"\n" + //
			"void main() { \n" + //
			"	lowp vec4 textureColor = texture2D(colorTexture, textureCoord); \n" + //
			"	if (pointDraw) { \n" + //
			"	  textureColor = texture2D(colorTexture, gl_PointCoord) * vec4(1.0, 1.0, 1.0, pointAlpha); \n" + //
			"	} \n" + //
			"	if (textureColor.a <= 0.5) {\n" + //
			"	  discard;\n" + //
			"	} \n" + //
			"  lowp float shadow = (texture2D(shadowMapTexture, shadowCoord.xy).z < shadowCoord.z) ? 0.0 : 1.0; \n" + //
			"  lowp float diffuseLightIntensity = 0.0; \n" + //
			"  lowp float specularLightIntensity = 0.0; \n" + //
			"  if (fullBright) { \n" + //
			"    diffuseLightIntensity = 1.0; \n" + //
			"  } else { \n" + //
			"      lowp vec4 bump = (texture2D(bumpTexture, textureCoord) - 0.5) * 2.0; \n" + //
			"      lowp vec3 bumpNormal = bump.r * mTangent + bump.g * mBitangent + bump.b * mNormal; \n" + //
			"	    diffuseLightIntensity = shadow * sunIntensity * max(-0.0,dot(sunPosition, -bumpNormal));\n" + //
			"	    lowp float specular = max(0.0, dot(surfaceToCamera, reflect(-surfaceToLight, bumpNormal))); \n" + //
			"	    specularLightIntensity = shadow * sunIntensity * clamp(pow(specular, 1.0+ 50.0* shininess) * 2.5 * shininess, 0.0, 1.0); \n" + //
			"  } \n" + //
			"	lowp float sValue = max(ambientLightIntensity, diffuseLightIntensity); \n" + //
			"	gl_FragColor = sunColor * (specularLightIntensity * vec4(1.0) + color * textureColor * vec4(sValue, sValue, sValue, 1.0)); \n" + //
			"	if (fogDensity > 0.0) { \n" + //
			"		lowp float fog = clamp(exp(-fogDensity*fogDensity * abs(fogDepth) * abs(fogDepth) / 5000.0  ), 0.0, 1.0); \n" + //
			"		gl_FragColor = mix(vec4(1.0), gl_FragColor, fog); \n" + //
			"	} \n" + //
			"}";

	public ShadowingTextureShader() {
		init(vs, fs, afs);
	}
	
}
