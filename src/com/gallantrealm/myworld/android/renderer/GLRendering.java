package com.gallantrealm.myworld.android.renderer;

import com.gallantrealm.myworld.client.renderer.IRendering;

public abstract class GLRendering implements IRendering {

	public abstract void snap(long worldTime);
	
	public abstract void draw(Shader shader, float[] viewMatrix, float[] sunViewMatrix, long worldTime, int drawType, boolean transparency);

	public void updateRendering() {
	}
}
