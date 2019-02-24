package com.gallantrealm.myworld.android.renderer.old;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import android.opengl.GLES10;
import android.opengl.GLES11;
import android.opengl.Matrix;

/**
 * Encapsulates the creation of triangles for a surface and the normals and texture coordinates for it.
 */
public final class GLSurface {

	public static final int MAX_VERTICES = 65536; // limited in size by indices addressability
	static boolean buffersAllocated;
	static FloatBuffer vertices;
	static ShortBuffer indices;
	static int nextFreeVertex = 1;
	static int nextFreeIndex = 1;

	static int vertexBufferId;
	static int indicesBufferId;
	public static boolean needsBufferBinding;

	public static void initializeVertexBuffer() {
		System.out.println("initializeVertexBuffer entered");

		if (!buffersAllocated) {
			GLES11.glDeleteBuffers(2, new int[] { vertexBufferId, indicesBufferId }, 0);

			ByteBuffer bb = ByteBuffer.allocateDirect(MAX_VERTICES * 4 * 3 + MAX_VERTICES * 4 * 3 + MAX_VERTICES * 4 * 2);
			bb.order(ByteOrder.nativeOrder());
			vertices = bb.asFloatBuffer();

			bb = ByteBuffer.allocateDirect(MAX_VERTICES * 2 * 2 * 3);
			bb.order(ByteOrder.nativeOrder());
			indices = bb.asShortBuffer();

			int[] buffer = new int[2];
			GLES11.glGenBuffers(2, buffer, 0);
			vertexBufferId = buffer[0];
			indicesBufferId = buffer[1];
			buffersAllocated = true;
		}

		needsBufferBinding = true;

		vertices.clear();
		indices.clear();
		nextFreeVertex = 1;
		nextFreeIndex = 1;
		System.out.println("initializeVertexBuffer leaving");
	}

	int width;
	int height;
	boolean smooth;
	int baseVertex;
	int baseIndex;
	int nindices;

	public GLSurface(int width, int height, boolean smooth) {
		this.width = width;
		this.height = height;
		this.smooth = smooth;
		int nvertices = width * height;
		if (nextFreeVertex + nvertices >= MAX_VERTICES) {
			System.err.println("No more free vertices!!!");
			return;
		}
		baseVertex = nextFreeVertex;
		nextFreeVertex += nvertices;

		nindices = (width - 1) * (height - 1) * 2 * 3;

		baseIndex = nextFreeIndex;
		nextFreeIndex += nindices;

		generateTextureCoords(1.0f, 1.0f);

		generateIndices();
	}

	public int getVertexCount() {
		return width * height;
	}

	public void getVertex(int x, int y, Point3f point) {
		if (baseVertex == 0) { // overflow
			return;
		}
		int vertex = baseVertex + (y * width + x);
		point.x = vertices.get(vertex * 8);
		point.y = vertices.get(vertex * 8 + 1);
		point.z = vertices.get(vertex * 8 + 2);
	}

	public void setVertex(int x, int y, Point3f point) {
		if (baseVertex == 0) { // overflow
			return;
		}
		int vertex = baseVertex + (y * width + x);
		vertices.put(vertex * 8, point.x);
		vertices.put(vertex * 8 + 1, point.y);
		vertices.put(vertex * 8 + 2, point.z);
	}

	public void setVertex(int x, int y, float px, float py, float pz) {
		if (baseVertex == 0) { // overflow
			return;
		}
		int vertex = baseVertex + (y * width + x);
		vertices.put(vertex * 8, px);
		vertices.put(vertex * 8 + 1, py);
		vertices.put(vertex * 8 + 2, pz);
	}

	public void getVertex(int vertex, Point3f point) {
		if (baseVertex == 0) { // overflow
			return;
		}
		vertex += baseVertex;
		point.x = vertices.get(vertex * 8);
		point.y = vertices.get(vertex * 8 + 1);
		point.z = vertices.get(vertex * 8 + 2);
	}

	public void setVertex(int vertex, Point3f point) {
		if (baseVertex == 0) { // overflow
			return;
		}
		vertex += baseVertex;
		vertices.put(vertex * 8, point.x);
		vertices.put(vertex * 8 + 1, point.y);
		vertices.put(vertex * 8 + 2, point.z);
	}

	public void getNormal(int x, int y, Point3f normal) {
		if (baseVertex == 0) { // overflow
			return;
		}
		int vertex = baseVertex + (y * width + x);
		normal.x = vertices.get(vertex * 8 + 3);
		normal.y = vertices.get(vertex * 8 + 4);
		normal.z = vertices.get(vertex * 8 + 5);
	}

	public void setNormal(int x, int y, Point3f normal) {
		if (baseVertex == 0) { // overflow
			return;
		}
		int vertex = baseVertex + (y * width + x);
		vertices.put(vertex * 8 + 3, normal.x);
		vertices.put(vertex * 8 + 4, normal.y);
		vertices.put(vertex * 8 + 5, normal.z);
	}

	public void setTextureCoordinate(int a, int b, float[] c) {

	}

	public void generateIndices() {
		if (baseVertex == 0) { // overflow
			return;
		}
		int index = baseIndex; // * 2 * 3;
		for (int y = 0; y < height - 1; y++) {
			for (int x = 0; x < width - 1; x++) {
				int vertex = baseVertex + (y * width + x);
				indices.put(index++, (short) (vertex));
				indices.put(index++, (short) (vertex + 1));
				indices.put(index++, (short) (vertex + width));
				indices.put(index++, (short) (vertex + 1));
				indices.put(index++, (short) (vertex + width + 1));
				indices.put(index++, (short) (vertex + width));
			}
		}
	}

	public void generateTextureCoords(float sizex, float sizey) {
		if (baseVertex == 0) { // overflow
			return;
		}
		int index = baseVertex;
		for (int y = 0; y < height; y++) {
			float ycoord = sizey / (height - 1) * (height - y - 1) - 0.5f;
			for (int x = 0; x < width; x++) {
				float xcoord = 0.5f - sizex / (width - 1) * x;
				vertices.put(index * 8 + 6, xcoord);
				vertices.put(index * 8 + 7, ycoord);
				index++;
			}
		}
	}

	public void adjustTextureCoords(float[] textureMatrix) {
		if (baseVertex == 0) { // overflow
			return;
		}
		int index = baseVertex;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				float xcoord = vertices.get(index * 8 + 6);
				float ycoord = vertices.get(index * 8 + 7);

				float[] rhsVec = new float[] { xcoord, ycoord, 1, 1 };
				float[] resultVec = new float[4];
				Matrix.multiplyMV(resultVec, 0, textureMatrix, 0, rhsVec, 0);
				xcoord = resultVec[0];
				ycoord = resultVec[1];

				vertices.put(index * 8 + 6, xcoord);
				vertices.put(index * 8 + 7, ycoord);
				index++;
			}
		}
	}

	public void generateNormals() {
		generateNormals(false, false);
	}

	public void generateNormals(boolean stitchx, boolean stitchy) {
		if (baseVertex == 0) { // overflow
			return;
		}
		Point3f p1 = new Point3f();
		Point3f p2 = new Point3f();
		Point3f p3 = new Point3f();
		Point3f p4 = new Point3f();
		Point3f v1 = new Point3f();
		Point3f v2 = new Point3f();
		Point3f normal = new Point3f();

		// calculate all the mid-point normals
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				getSafeVertex(x - 1, y - 1, p1, stitchx, stitchy);
				getSafeVertex(x + 1, y - 1, p2, stitchx, stitchy);
				getSafeVertex(x - 1, y + 1, p3, stitchx, stitchy);
				getSafeVertex(x + 1, y + 1, p4, stitchx, stitchy);
				calculateVector(p1, p4, v1);
				calculateVector(p2, p3, v2);
//				calculateVector(p1, p4, v3);
				calculateCrossProduct(v1, v2, normal);
				normalize(normal);
				setNormal(x, y, normal);
			}
		}
	}

	final void getSafeVertex(int x, int y, Point3f p, boolean stitchx, boolean stitchy) {
		if (x < 0) {
			if (stitchx) {
				x = width - 2;
			} else {
				x = 0;
			}
		} else if (x >= width) {
			if (stitchx) {
				x = 1;
			} else {
				x = width - 1;
			}
		}
		if (y < 0) {
			if (stitchy) {
				y = height - 2;
			} else {
				y = 0;
			}
		} else if (y >= height) {
			if (stitchy) {
				y = 1;
			} else {
				y = height - 1;
			}
		}
		getVertex(x, y, p);
	}

	final void calculateVector(Point3f p1, Point3f p2, Point3f v) {
		v.x = p2.x - p1.x;
		v.y = p2.y - p1.y;
		v.z = p2.z - p1.z;
	}

	final void calculateCrossProduct(Point3f v1, Point3f v2, Point3f v3) {
		v3.x = v1.y * v2.z - v2.y * v1.z;
		v3.y = v1.z * v2.x - v2.z * v1.x;
		v3.z = v1.x * v2.y - v2.x * v1.y;
	}

	final void normalize(Point3f v) {
		float mag = (float) Math.sqrt(v.x * v.x + v.y * v.y + v.z * v.z);
		v.x = v.x / mag;
		v.y = v.y / mag;
		v.z = v.z / mag;
	}

	/**
	 * Binds the buffers so that they will be able to be "sent" to the graphics chip.
	 */
	static final void bindBuffers() {
		System.out.println("binding buffers");
		GLES11.glBindBuffer(GLES11.GL_ARRAY_BUFFER, vertexBufferId);
		GLES11.glBufferData(GLES11.GL_ARRAY_BUFFER, nextFreeVertex * 4 * 8, vertices, GLES11.GL_STATIC_DRAW);
		GLES11.glVertexPointer(3, GLES10.GL_FLOAT, 8 * 4, 0);
		GLES11.glNormalPointer(GLES10.GL_FLOAT, 8 * 4, 3 * 4);
		GLES11.glTexCoordPointer(2, GLES10.GL_FLOAT, 8 * 4, 6 * 4);

		GLES11.glBindBuffer(GLES11.GL_ELEMENT_ARRAY_BUFFER, indicesBufferId);
		GLES11.glBufferData(GLES11.GL_ELEMENT_ARRAY_BUFFER, nextFreeIndex * 2, indices, GLES11.GL_STATIC_DRAW);

		needsBufferBinding = false;
		System.out.println("binding buffers completed");
	}

	static boolean lastShade;

	/**
	 * Draws the surface
	 */
	public final void draw(boolean shade) {
		if (baseVertex == 0) { // overflow
			return;
		}
		if (needsBufferBinding) {
			bindBuffers();
		}
//		if (!smooth) {
//			GLES10.glShadeModel(GLES10.GL_FLAT);
//		}
		if (shade != lastShade) {
			if (shade) {
				GLES10.glEnableClientState(GLES10.GL_NORMAL_ARRAY);
			} else {
				GLES10.glDisableClientState(GLES10.GL_NORMAL_ARRAY);
			}
			lastShade = shade;
		}
		GLES11.glDrawElements(GLES10.GL_TRIANGLES, (width - 1) * (height - 1) * 2 * 3, GLES10.GL_UNSIGNED_SHORT, baseIndex * 2);
//		if (!smooth) {
//			GLES10.glShadeModel(GLES10.GL_SMOOTH);
//		}
	}

	/**
	 * This variant of the draw will draw a group of surfaces that are adjacent in the buffers. This can be used to
	 * reduce the number of GL calls that are made for drawing an object. The requirement however is that all of the
	 * surfaces have the same texture parameters.
	 * 
	 * @param shade
	 */
	public static final void drawMonolith(GLSurface[] surfaces, boolean shade) {
		if (needsBufferBinding) {
			bindBuffers();
		}
		if (shade != lastShade) {
			if (shade) {
				GLES10.glEnableClientState(GLES10.GL_NORMAL_ARRAY);
			} else {
				GLES10.glDisableClientState(GLES10.GL_NORMAL_ARRAY);
			}
			lastShade = shade;
		}
		int baseIndex = 1000000; //32000;
		int nindices = 0;
		int slen = surfaces.length;
		for (int i = 0; i < slen; i++) {
			if (surfaces[i] != null) {
				if (surfaces[i].baseVertex == 0) { // overflow
					return;
				}
				int newIndex = surfaces[i].baseIndex;
				if (newIndex < baseIndex) {
					baseIndex = newIndex;
				}
				nindices += (surfaces[i].width - 1) * (surfaces[i].height - 1) * 2 * 3;
			}
		}
		GLES11.glDrawElements(GLES10.GL_TRIANGLES, nindices, GLES10.GL_UNSIGNED_SHORT, baseIndex * 2);
	}

}
