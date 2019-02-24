package com.gallantrealm.myworld.android.renderer.old;

import com.gallantrealm.myworld.client.renderer.IRendering;

public abstract class GLRendering implements IRendering {

	public abstract void draw(long worldTime, boolean shading, int picking, boolean drawtrans, int threedee);
	
	public void updateRendering() {
	}

}
