package com.gallantrealm.myworld.client.renderer;

/**
 * All objects that are renderable implement this interface.
 */
public interface IRenderable {

	/**
	 * Creates a rendering for the object if it does not exist.
	 * 
	 * @param renderer
	 *            the platform specific renderer
	 * @param worldTime
	 *            the current world time
	 * @return a rendering appropriate for the object
	 */
	void createRendering(IRenderer renderer, long worldTime);

	IRendering getRendering();

	void dropRendering();

}
