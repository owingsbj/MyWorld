package com.gallantrealm.myworld.android.renderer.neu;

public final class Point3f {

	public float x;
	public float y;
	public float z;

	public Point3f() {
	}

	public Point3f(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public boolean equals(Point3f p) {
		return (x == p.x && y == p.y && z == p.z);
	}
}
