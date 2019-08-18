package com.gallantrealm.myworld.android.renderer;

import com.gallantrealm.myworld.client.renderer.IRenderer;

import android.content.Context;
import android.opengl.GLSurfaceView;

public final class NewAndroidRenderer extends AndroidRenderer implements IRenderer, GLSurfaceView.Renderer {

	public NewAndroidRenderer(Context context, GLSurfaceView view, boolean simpleRendering) {
		super(context, view, simpleRendering);
		this.projectionMatrix = new float[16];
		this.viewMatrix = new float[16];
		this.sunViewMatrix = new float[16];
	}

}
