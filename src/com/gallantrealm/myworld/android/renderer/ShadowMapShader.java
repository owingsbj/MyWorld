package com.gallantrealm.myworld.android.renderer;

public class ShadowMapShader extends Shader {

	public ShadowMapShader() {
		String vs = "" + //
				"precision highp float; \n" + //
				"" + //
				"attribute vec4 aPosition; \n" + //
				"uniform mat4 sunMvMatrix; \n" + //
				"uniform vec3 viewPosition; \n" + //
				"" + //
				"void main() { \n" + //
				"	gl_Position = sunMvMatrix  * aPosition; \n" + //
				"} \n";
		String fs = "" + //
				"precision highp float; \n" + //
				"" + //
				"void main() { \n" + //
				"	gl_FragColor = vec4(1.0); // needed or linker errors sometimes \n" + //
				"} \n";
		init(vs, fs, fs);
	}

}
