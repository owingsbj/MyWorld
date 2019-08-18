package com.gallantrealm.myworld.android.renderer;

public class DepthShader extends Shader {
	
	public static final String vs = "\n" +
			"precision mediump float; \n" +
			"\n" +
			"attribute vec4 aPosition; \n" +
			"uniform mat4 mvMatrix; \n" +
			"\n" +
			"void main() { \n" +
			"	gl_Position = mvMatrix * aPosition; \n" +
			"}";
	
	public static final String fs = "\n" +
			"precision mediump float; \n" +
			"\n" +
			"void main() { \n" +
			"	gl_FragColor = vec4(1.0); // needed or linker errors sometimes \n" +
			"}"; 

	public DepthShader() {
		init(vs, fs, fs);
	}

}
