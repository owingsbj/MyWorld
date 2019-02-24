package com.gallantrealm.myworld.android.renderer.neu;

import com.gallantrealm.myworld.model.WWSculpty;

/**
 * Creates a primitive with a complex surface shape composed from a two dimensional array of rectangles. Three types of
 * meshes are possible: flat, cylindrical, and spherical. A flat mesh can be used for land and other large flat
 * surfaces. Cylindrical is useful for "carved" objects such as statues and limbs of an avatar. Spherical meshes can
 * simulate round objects like the head of an avatar or a model of a planet.
 */
public class GLSculpty extends GLObject {

	public static final int BASE_SHAPE_BOX = 0;
	public static final int BASE_SHAPE_CYLINDER = 1;
	public static final int BASE_SHAPE_SPHERE = 2;

	public GLSculpty(NewAndroidRenderer renderer, WWSculpty sculpty, long worldTime) {
		super(renderer, sculpty, worldTime);
	}

}
