package com.gallantrealm.myworld.client.renderer;

/**
 * The interface for objects representing rendered version of 3d world objects
 */
public interface IRendering {

	public static final int DRAW_TYPE_MONO = 0;
	public static final int DRAW_TYPE_LEFT_EYE = 1;
	public static final int DRAW_TYPE_RIGHT_EYE = 2;
	public static final int DRAW_TYPE_PICKING = 3;
	public static final int DRAW_TYPE_SHADOW = 4;

	IRenderer getRenderer();

	public void updateRendering();
}
