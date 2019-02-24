package com.gallantrealm.myworld.android.renderer.old;

import android.opengl.GLES10;
import android.opengl.Matrix;

import com.gallantrealm.myworld.FastMath;
import com.gallantrealm.myworld.android.renderer.AndroidRenderer;
import com.gallantrealm.myworld.client.renderer.IRenderer;
import com.gallantrealm.myworld.model.SideAttributes;
import com.gallantrealm.myworld.model.WWColor;
import com.gallantrealm.myworld.model.WWConstant;
import com.gallantrealm.myworld.model.WWObject;
import com.gallantrealm.myworld.model.WWVector;

/**
 * This is the superclass of all OpenGL primitives used in MyWorld.
 */
public abstract class GLObject extends GLRendering {

	final WWObject object;
	final AndroidRenderer renderer;

	GLSurface[] sides = new GLSurface[WWObject.NSIDES];

	GLObject(AndroidRenderer renderer, WWObject object, long worldTime) {
		this.renderer = renderer;
		this.object = object;
	}

	public final WWObject getObject() {
		return object;
	}

	@Override
	public IRenderer getRenderer() {
		return renderer;
	}

	public final void setSide(int side, GLSurface geometry) {
		sides[side] = geometry;
	}

	public final GLSurface getSide(int side) {
		return sides[side];
	}

	/**
	 * Apply all of the distortion effects to the geometry. These effects do not require any knowledge of the object
	 * itself, just its coordinates, so they can be applied after the initial geometry has been determined.
	 */
	final void adjustGeometry(GLSurface surface, float sizeX, float sizeY, float sizeZ, float taperX, float taperY, float shearX, float shearY, float twist) {
		int vertexCount = surface.getVertexCount();
		Point3f coordinate = new Point3f();

		for (int i = 0; i < vertexCount; i++) {
			surface.getVertex(i, coordinate);

			// Note: The order of these actions is important. They are done so editing the object is easier.
			// Note: coordinate.z is the y coordinate, and coordinate.y is the z coordinate (java3d and myworld are
			// different)

			// Taper
			float zoffset = coordinate.y + 0.5f;
			coordinate.x = coordinate.x * (1.0f - taperX * zoffset);
			coordinate.z = coordinate.z * (1.0f - taperY * zoffset);

			// Shear
			coordinate.x = coordinate.x + shearX * zoffset;
			coordinate.z = coordinate.z + shearY * zoffset;

			// Resize
			coordinate.x = coordinate.x * sizeX;
			coordinate.y = coordinate.y * sizeZ;
			coordinate.z = coordinate.z * sizeY;

			// Twist
			float r = (float) Math.sqrt(coordinate.x * coordinate.x + coordinate.z * coordinate.z);
			float theta = FastMath.atan2(coordinate.x, coordinate.z);
			float thetaDelta = (coordinate.y / sizeZ + 0.5f) * twist * 2.0f * (float) Math.PI;
			coordinate.x = r * (float) Math.sin(theta + thetaDelta);
			coordinate.z = r * (float) Math.cos(theta + thetaDelta);

			// Model matrix -- add in only when grouped
			if (object.group != 0) {
				float[] lhsMat = getModelMatrix(0);
				float[] rhsVec = new float[] { coordinate.x, coordinate.y, coordinate.z, 1 };
				float[] resultVec = new float[4];
				Matrix.multiplyMV(resultVec, 0, lhsMat, 0, rhsVec, 0);
				coordinate.x = resultVec[0];
				coordinate.y = resultVec[1];
				coordinate.z = resultVec[2];
			}

			surface.setVertex(i, coordinate);

		}
	}

	final void adjustTextureCoords(GLSurface surface, int side) {
		if (object.fixed) {
			String textureUrl = object.sideAttributes[side].textureURL;
			if (textureUrl != null) {
				float[] textureMatrix = new float[16];
				Matrix.setIdentityM(textureMatrix, 0);
				Matrix.scaleM(textureMatrix, 0, 1.0f / object.sideAttributes[side].textureScaleX, 1.0f / object.sideAttributes[side].textureScaleY, 1.0f);
				Matrix.translateM(textureMatrix, 0, object.getTextureOffsetX(side, 0), object.getTextureOffsetY(side, 0), 0);
				Matrix.rotateM(textureMatrix, 0, object.getTextureRotation(side, 0), 0, 0, 1);
				surface.adjustTextureCoords(textureMatrix);
			}
		}
	}

	float[] modelMatrix;
	long modelMatrixTime = -1;
	boolean parentIsFullyFixed;

	public final float[] getModelMatrix(long worldTime) {
//		if (object.fixed) {
//			if (fixedMatrix == null) {
//				fixedMatrix = getFixedMatrix(worldTime);
//			}
//			if (unfixedParent != null) {
//				float[] parentModelMatrix = ((GLObject) unfixedParent.getRendering()).getModelMatrix(worldTime);
//				float[] tMatrix = new float[16]; //parentModelMatrix.clone();
//				Matrix.multiplyMM(tMatrix, 0, parentModelMatrix, 0, fixedMatrix, 0);
//				return tMatrix;
//			} else {
//				return fixedMatrix;
//			}
//		} else {
		if (modelMatrix == null || (!object.fixed && modelMatrixTime != worldTime)) {
			modelMatrix = new float[16];
			Matrix.setIdentityM(modelMatrix, 0);
			object.getAnimatedPosition(position, worldTime);
			Matrix.translateM(modelMatrix, 0, position.x, position.z, position.y);
			object.getAnimatedRotation(rotation, worldTime);
			if (rotation.z != 0) {
				Matrix.rotateM(modelMatrix, 0, rotation.z, 0, 1, 0);
			}
			if (rotation.y != 0) {
				Matrix.rotateM(modelMatrix, 0, rotation.y, 0, 0, 1);
			}
			if (rotation.x != 0) {
				Matrix.rotateM(modelMatrix, 0, rotation.x, 1, 0, 0);
			}
			modelMatrixTime = worldTime;
		}
		if (object.parentId == 0 || parentIsFullyFixed) {
			return modelMatrix;
		} else {
			WWObject parent = object.world.objects[object.parentId];
			if (parent == null || parent.rendering == null) {
				// not good but don't want to cause issues
				return modelMatrix;
			} else {
				float[] parentModelMatrix = ((GLObject) parent.rendering).getModelMatrix(worldTime);
				float[] tMatrix = parentModelMatrix.clone();
				Matrix.multiplyMM(tMatrix, 0, parentModelMatrix, 0, modelMatrix, 0);
				if (object.fixed && parent.fixed && parent.parentId == 0) { // the (only) parent is also fixed, so no need to do this again
					modelMatrix = tMatrix;
					parentIsFullyFixed = true;
				}
				return tMatrix;
			}
		}
//		}
	}

//	float[] fixedMatrix;
//	WWObject unfixedParent;
//
//	/**
//	 * Determines the fixed multiplication of matrices, returning the matrix. Also, by side-effect, the parent that is
//	 * not fixed is set.
//	 * 
//	 * @param worldTime
//	 * @param matrix
//	 * @return
//	 */
//	private final float[] getFixedMatrix(long worldTime) {
//		if (fixedMatrix == null) {
//			fixedMatrix = new float[16];
//			Matrix.setIdentityM(fixedMatrix, 0);
//			object.getAnimatedPosition(position, worldTime);
//			Matrix.translateM(fixedMatrix, 0, position.x, position.z, position.y);
//			object.getAnimatedRotation(rotation, worldTime);
//			if (rotation.z != 0) {
//				Matrix.rotateM(fixedMatrix, 0, rotation.z, 0, 1, 0);
//			}
//			if (rotation.y != 0) {
//				Matrix.rotateM(fixedMatrix, 0, rotation.y, 0, 0, 1);
//			}
//			if (rotation.x != 0) {
//				Matrix.rotateM(fixedMatrix, 0, rotation.x, 1, 0, 0);
//			}
//
//			if (object.parentId != 0) {
//				WWObject parent = object.world.objects[object.parentId];
//				if (!parent.fixed) {
//					unfixedParent = parent;
//				} else {
//					float[] parentMatrix = ((GLObject) parent.getRendering()).getFixedMatrix(worldTime);
//					float[] tMatrix = new float[16];
//					Matrix.multiplyMM(tMatrix, 0, parentMatrix, 0, fixedMatrix, 0);
//					fixedMatrix = tMatrix;
//				}
//			}
//		}
//		return fixedMatrix;
//	}

	/**
	 * Adjust position and rotations for GL according to parent(s)
	 * 
	 * @param parent
	 */
	final void parentalAdjust(int parentId, long worldTime) {
		WWObject parent = object.world.objects[parentId];
		parentId = parent.parentId;
		if (parentId != 0) {
			parentalAdjust(parentId, worldTime);
		}
		WWVector position = new WWVector();
		parent.getAnimatedPosition(position, worldTime);
		GLES10.glTranslatef(position.x, position.z, position.y);
		WWVector rotation = new WWVector();
		parent.getAnimatedRotation(rotation, worldTime);
		if (rotation.z != 0) {
			GLES10.glRotatef(rotation.z, 0.0f, 1.0f, 0.0f);
		}
		if (rotation.y != 0) {
			GLES10.glRotatef(rotation.y, 0.0f, 0.0f, 1.0f);
		}
		if (rotation.x != 0) {
			GLES10.glRotatef(rotation.x, 1.0f, 0.0f, 0.0f);
		}
	}

	final WWVector position = new WWVector();
	final WWVector rotation = new WWVector();

	private static boolean lastTextureMatrixIdentity = false;

	@Override
	public void draw(long worldTime, boolean shading, int picking, boolean drawtrans, int threedee) {

		if (object.monolithic) {

			// Monolithic drawing.  Draw all surfaces together
			String lastTextureUrl = null;

			SideAttributes sideAttributes = object.sideAttributes[WWObject.SIDE_ALL];

			float trans = sideAttributes.transparency;
			if ((drawtrans && trans > 0.0 && trans < 1.0) || (!drawtrans && trans == 0.0)) {
				if (picking == 1) { // object picking
//						if (!object.isPickable()) {
//							return;
//						}
					int id = object.getId();
					float red = (((id & 0xF00) >> 8) + 0.5f) / 16.0f;
					float green = (((id & 0x00F0) >> 4) + 0.5f) / 16.0f;
					float blue = ((id & 0x00F) + 0.5f) / 16.0f;
					GLES10.glColor4f(red, green, blue, 1.0f);
				}
				GLES10.glPushMatrix();
				float[] modelMatrix = getModelMatrix(worldTime);
				GLES10.glMultMatrixf(modelMatrix, 0);
				if (picking == 1) { // object picking
					GLSurface.drawMonolith(sides, false);
				} else if (picking == 2) { // surface point picking
					int textureId = renderer.getTexture("surface_select");
					lastTextureUrl = null;
					lastTextureId = textureId;
					GLES10.glLightfv(GLES10.GL_LIGHT0, GLES10.GL_DIFFUSE, new float[] { 1, 1, 1, 0 }, 0);
					GLES10.glLightfv(GLES10.GL_LIGHT0, GLES10.GL_AMBIENT, new float[] { 1, 1, 1, 0 }, 0);
					GLES10.glMaterialfv(GLES10.GL_FRONT_AND_BACK, GLES10.GL_AMBIENT_AND_DIFFUSE, new float[] { 1, 1, 1, 1 }, 0);
					GLES10.glMatrixMode(GLES10.GL_TEXTURE);
					GLES10.glLoadIdentity();
					GLES10.glTranslatef(0.5f, 0.5f, 0.0f);
					GLES10.glEnable(GLES10.GL_TEXTURE_2D);
					GLES10.glBindTexture(GLES10.GL_TEXTURE_2D, textureId);
					GLSurface.drawMonolith(sides, false);
					GLES10.glDisable(GLES10.GL_TEXTURE_2D);
					GLES10.glMatrixMode(GLES10.GL_MODELVIEW);
					lastTextureMatrixIdentity = false;
				} else if (picking == 3) { // surface point picking - vertical
					int textureId = renderer.getTexture("surface_select");
					lastTextureUrl = null;
					lastTextureId = textureId;
					GLES10.glLightfv(GLES10.GL_LIGHT0, GLES10.GL_DIFFUSE, new float[] { 1, 1, 1, 0 }, 0);
					GLES10.glLightfv(GLES10.GL_LIGHT0, GLES10.GL_AMBIENT, new float[] { 1, 1, 1, 0 }, 0);
					GLES10.glMaterialfv(GLES10.GL_FRONT_AND_BACK, GLES10.GL_AMBIENT_AND_DIFFUSE, new float[] { 1, 1, 1, 1 }, 0);
					GLES10.glMatrixMode(GLES10.GL_TEXTURE);
					GLES10.glLoadIdentity();
					GLES10.glTranslatef(0.5f, 0.5f, 0.0f);
					GLES10.glRotatef(90, 0.0f, 0.0f, 1.0f);
					GLES10.glEnable(GLES10.GL_TEXTURE_2D);
					GLES10.glBindTexture(GLES10.GL_TEXTURE_2D, textureId);
					GLSurface.drawMonolith(sides, false);
					GLES10.glDisable(GLES10.GL_TEXTURE_2D);
					GLES10.glMatrixMode(GLES10.GL_MODELVIEW);
					lastTextureMatrixIdentity = false;
				} else {
					float red = sideAttributes.red;
					float green = sideAttributes.green;
					float blue = sideAttributes.blue;
					float shininess = sideAttributes.shininess;
					if (threedee == 1) { // red side
						red = (red * 3 + green + blue) / 5.0f;
						green = 0;
						blue = 0;
					} else if (threedee == 2) { // cyan side
						red = 0;
						green = (green * 3 + red) / 4.0f;
						blue = (blue * 3 + red) / 4.0f;
					}
					GLES10.glMaterialfv(GLES10.GL_FRONT_AND_BACK, GLES10.GL_AMBIENT_AND_DIFFUSE, new float[] { red, green, blue, 1.0f - trans }, 0);
//					GLES10.glMaterialfv(GLES10.GL_FRONT_AND_BACK, GLES10.GL_SPECULAR, new float[] { shininess / 2, shininess / 2, shininess / 2, 1 }, 0);
//					GLES10.glMaterialf(GLES10.GL_FRONT_AND_BACK, GLES10.GL_SHININESS, (128.0f * shininess));

					String textureUrl = sideAttributes.textureURL;
					if (textureUrl != null) {
						int textureId = lastTextureId;
						if (textureUrl != lastTextureUrl) {
							textureId = renderer.getTexture(textureUrl);
							lastTextureUrl = textureUrl;
						}
						if (!object.fixed) { // note: texture scale/rotation/offsets are "baked" into coordinates for fixed
							GLES10.glMatrixMode(GLES10.GL_TEXTURE);
							GLES10.glLoadIdentity();
							GLES10.glScalef(1.0f / sideAttributes.textureScaleX, 1.0f / sideAttributes.textureScaleY, 1.0f);
							GLES10.glTranslatef(object.getTextureOffsetX(WWObject.SIDE_ALL, worldTime), object.getTextureOffsetY(WWObject.SIDE_ALL, worldTime), 0.0f);
							float textureRotation = object.getTextureRotation(WWObject.SIDE_ALL, worldTime);
							if (textureRotation != 0.0f) {
								GLES10.glRotatef(textureRotation, 0.0f, 0.0f, 1.0f);
							}
							lastTextureMatrixIdentity = false;
						} else if (!lastTextureMatrixIdentity) {
							GLES10.glMatrixMode(GLES10.GL_TEXTURE);
							GLES10.glLoadIdentity();
							lastTextureMatrixIdentity = true;
						}
						if (textureId != 0 && textureId != lastTextureId) {
							GLES10.glEnable(GLES10.GL_TEXTURE_2D);
							GLES10.glBindTexture(GLES10.GL_TEXTURE_2D, textureId);
							lastTextureId = textureId;
							if (sideAttributes.alphaTest) {
								GLES10.glEnable(GLES10.GL_ALPHA_TEST);
								GLES10.glAlphaFunc(GLES10.GL_GREATER, 0);
							}
						}
					} else if (lastTextureId != -1) {
						GLES10.glDisable(GLES10.GL_TEXTURE_2D);
						lastTextureId = -1;
						GLES10.glDisable(GLES10.GL_ALPHA_TEST);
					}
					if (sideAttributes.fullBright) {
						GLES10.glLightfv(GLES10.GL_LIGHT0, GLES10.GL_DIFFUSE, new float[] { 1, 1, 1, 0 }, 0);
						GLES10.glLightfv(GLES10.GL_LIGHT0, GLES10.GL_AMBIENT, new float[] { 1, 1, 1, 0 }, 0);
						GLSurface.drawMonolith(sides, false);
						WWColor sunColor = object.world.getSunColor();
						float sunIntensity = object.world.getSunIntensity();
						float ambientLightIntensity = object.world.getAmbientLightIntensity();
						GLES10.glLightfv(GLES10.GL_LIGHT0, GLES10.GL_DIFFUSE, new float[] { sunIntensity * sunColor.getRed(), sunIntensity * sunColor.getGreen(), sunIntensity * sunColor.getBlue(), 0.0f }, 0);
						GLES10.glLightfv(GLES10.GL_LIGHT0, GLES10.GL_AMBIENT, new float[] { ambientLightIntensity * sunColor.getRed(), ambientLightIntensity * sunColor.getGreen(), ambientLightIntensity * sunColor.getBlue(), 0.0f }, 0);
					} else {
						GLSurface.drawMonolith(sides, shading);
					}
					if (textureUrl != null) {
						GLES10.glMatrixMode(GLES10.GL_MODELVIEW);
					}
				}
				GLES10.glPopMatrix();
			}

		} else {

			// Non-monolithic drawing.  Draw each surface separately
			boolean sideDrawn = false;
			String lastTextureUrl = null;

			for (int side = 0; side < WWObject.NSIDES; side++) {
				if (sides[side] != null) {
					SideAttributes sideAttributes = null;
					if (object.sideAttributes != null) {
						sideAttributes = object.sideAttributes[side];
					}
					if (sideAttributes == null) {
						sideAttributes = SideAttributes.getDefaultSideAttributes();
					}
					float trans = sideAttributes.transparency;
					if ((drawtrans && trans > 0.0 && trans < 1.0) || (!drawtrans && trans == 0.0)) {
						if (!sideDrawn) {
							if (picking == 1) {
//								if (!object.isPickable()) {
//									return;
//								}
								int id = object.getId();
								float red = (((id & 0xF00) >> 8) + 0.5f) / 16.0f;
								float green = (((id & 0x00F0) >> 4) + 0.5f) / 16.0f;
								float blue = ((id & 0x00F) + 0.5f) / 16.0f;
								GLES10.glColor4f(red, green, blue, 1.0f);
							}
							GLES10.glPushMatrix();
							float[] modelMatrix = getModelMatrix(worldTime);
							GLES10.glMultMatrixf(modelMatrix, 0);
							sideDrawn = true;
						}
						if (picking == 1) {
							GLSurface geometry = sides[side];
							geometry.draw(false);
						} else if (picking == 2) { // surface point picking - horizontal
							int textureId = renderer.getTexture("surface_select");
							lastTextureUrl = null;
							lastTextureId = textureId;
							GLES10.glLightfv(GLES10.GL_LIGHT0, GLES10.GL_DIFFUSE, new float[] { 1, 1, 1, 0 }, 0);
							GLES10.glLightfv(GLES10.GL_LIGHT0, GLES10.GL_AMBIENT, new float[] { 1, 1, 1, 0 }, 0);
							GLES10.glMaterialfv(GLES10.GL_FRONT_AND_BACK, GLES10.GL_AMBIENT_AND_DIFFUSE, new float[] { 1, 1, 1, 1 }, 0);
							GLES10.glMatrixMode(GLES10.GL_TEXTURE);
							GLES10.glLoadIdentity();
							GLES10.glTranslatef(0.5f, 0.5f, 0.0f);
							GLES10.glEnable(GLES10.GL_TEXTURE_2D);
							GLES10.glBindTexture(GLES10.GL_TEXTURE_2D, textureId);
							GLSurface geometry = sides[side];
							geometry.draw(false);
							GLES10.glDisable(GLES10.GL_TEXTURE_2D);
							GLES10.glMatrixMode(GLES10.GL_MODELVIEW);
							lastTextureMatrixIdentity = false;
						} else if (picking == 3) { // surface point picking - vertical
							int textureId = renderer.getTexture("surface_select");
							lastTextureUrl = null;
							lastTextureId = textureId;
							GLES10.glLightfv(GLES10.GL_LIGHT0, GLES10.GL_DIFFUSE, new float[] { 1, 1, 1, 0 }, 0);
							GLES10.glLightfv(GLES10.GL_LIGHT0, GLES10.GL_AMBIENT, new float[] { 1, 1, 1, 0 }, 0);
							GLES10.glMaterialfv(GLES10.GL_FRONT_AND_BACK, GLES10.GL_AMBIENT_AND_DIFFUSE, new float[] { 1, 1, 1, 1 }, 0);
							GLES10.glMatrixMode(GLES10.GL_TEXTURE);
							GLES10.glLoadIdentity();
							GLES10.glTranslatef(0.5f, 0.5f, 0.0f);
							GLES10.glRotatef(90, 0.0f, 0.0f, 1.0f);
							GLES10.glEnable(GLES10.GL_TEXTURE_2D);
							GLES10.glBindTexture(GLES10.GL_TEXTURE_2D, textureId);
							GLSurface geometry = sides[side];
							geometry.draw(false);
							GLES10.glDisable(GLES10.GL_TEXTURE_2D);
							GLES10.glMatrixMode(GLES10.GL_MODELVIEW);
							lastTextureMatrixIdentity = false;
						} else {
							float red = sideAttributes.red;
							float green = sideAttributes.green;
							float blue = sideAttributes.blue;
							float shininess = sideAttributes.shininess;
							if (threedee == 1) { // red side
								red = (red * 3 + green + blue) / 5.0f;
								green = 0;
								blue = 0;
							} else if (threedee == 2) { // cyan side
								red = 0;
								green = (green * 3 + red) / 4.0f;
								blue = (blue * 3 + red) / 4.0f;
							}
							GLES10.glMaterialfv(GLES10.GL_FRONT_AND_BACK, GLES10.GL_AMBIENT_AND_DIFFUSE, new float[] { red, green, blue, 1.0f - trans }, 0);
//							GLES10.glMaterialfv(GLES10.GL_FRONT_AND_BACK, GLES10.GL_SPECULAR, new float[] { shininess / 2, shininess / 2, shininess / 2, 1 }, 0);
//							GLES10.glMaterialf(GLES10.GL_FRONT_AND_BACK, GLES10.GL_SHININESS, (128.0f * shininess));
							String textureUrl = sideAttributes.textureURL;
							if (textureUrl != null) {
								int textureId = lastTextureId;
								if (textureUrl != lastTextureUrl) {
									textureId = renderer.getTexture(textureUrl);
									lastTextureUrl = textureUrl;
								}
								if (!object.fixed) {
									GLES10.glMatrixMode(GLES10.GL_TEXTURE);
									GLES10.glLoadIdentity();
									GLES10.glScalef(1.0f / sideAttributes.textureScaleX, 1.0f / sideAttributes.textureScaleY, 1.0f);
									GLES10.glTranslatef(object.getTextureOffsetX(side, worldTime), object.getTextureOffsetY(side, worldTime), 0.0f);
									float textureRotation = object.getTextureRotation(side, worldTime);
									if (textureRotation != 0.0f) {
										GLES10.glRotatef(textureRotation, 0.0f, 0.0f, 1.0f);
									}
									lastTextureMatrixIdentity = false;
								} else if (!lastTextureMatrixIdentity) {
									GLES10.glMatrixMode(GLES10.GL_TEXTURE);
									GLES10.glLoadIdentity();
									lastTextureMatrixIdentity = true;
								}
								if (textureId != lastTextureId) {
									GLES10.glEnable(GLES10.GL_TEXTURE_2D);
									GLES10.glBindTexture(GLES10.GL_TEXTURE_2D, textureId);
									lastTextureId = textureId;
									if (sideAttributes.alphaTest) {
										GLES10.glEnable(GLES10.GL_ALPHA_TEST);
									}
								}
							} else if (lastTextureId != -1) {
								GLES10.glDisable(GLES10.GL_TEXTURE_2D);
								lastTextureId = -1;
								GLES10.glDisable(GLES10.GL_ALPHA_TEST);
							}
							GLSurface geometry = sides[side];
							if (sideAttributes.fullBright) {
								GLES10.glLightfv(GLES10.GL_LIGHT0, GLES10.GL_DIFFUSE, new float[] { 1, 1, 1, 0 }, 0);
								GLES10.glLightfv(GLES10.GL_LIGHT0, GLES10.GL_AMBIENT, new float[] { 1, 1, 1, 0 }, 0);
								geometry.draw(false);
								WWColor sunColor = object.world.getSunColor();
								float sunIntensity = object.world.getSunIntensity();
								float ambientLightIntensity = object.world.getAmbientLightIntensity();
								GLES10.glLightfv(GLES10.GL_LIGHT0, GLES10.GL_DIFFUSE, new float[] { sunIntensity * sunColor.getRed(), sunIntensity * sunColor.getGreen(), sunIntensity * sunColor.getBlue(), 0.0f }, 0);
								GLES10.glLightfv(GLES10.GL_LIGHT0, GLES10.GL_AMBIENT, new float[] { ambientLightIntensity * sunColor.getRed(), ambientLightIntensity * sunColor.getGreen(), ambientLightIntensity * sunColor.getBlue(), 0.0f }, 0);
							} else {
								geometry.draw(shading);
							}
							if (textureUrl != null) {
								GLES10.glMatrixMode(GLES10.GL_MODELVIEW);
							}
						}
					}
				}
			}
			if (sideDrawn) {
				GLES10.glPopMatrix();
			}
		}
	}

	/**
	 * Draw a group of surfaces. The surfaces belong to this object and all objects in the same group, and were
	 * collected earlier using collectSurfaces. Since all surfaces have similar texture, the texture attributes are
	 * applied once for all and a monolithic draw is done.
	 */
	public final void drawSurfaces(GLSurface[] groupSurfaces, long worldTime, boolean shading, int picking, boolean drawtrans, int threedee) {

		// Monolithic drawing.  Draw all surfaces together
		String lastTextureUrl = null;

		// choose a reference side, the first non-transparent side
		SideAttributes sideAttributes = null;
		for (int side = WWObject.SIDE_ALL; side <= WWObject.SIDE_CUTOUT2; side++) {
			if (sideAttributes == null && object.sideAttributes[side].transparency < 1) {
				sideAttributes = object.sideAttributes[side];
			}
		}
		if (sideAttributes == null) { // all transparent
			return;
		}

		float trans = sideAttributes.transparency;
		if ((drawtrans && trans > 0.0 && trans < 1.0) || (!drawtrans && trans == 0.0)) {
			float red = sideAttributes.red;
			float green = sideAttributes.green;
			float blue = sideAttributes.blue;
			float shininess = sideAttributes.shininess;
			if (threedee == 1) { // red side
				red = (red * 3 + green + blue) / 5.0f;
				green = 0;
				blue = 0;
			} else if (threedee == 2) { // cyan side
				red = 0;
				green = (green * 3 + red) / 4.0f;
				blue = (blue * 3 + red) / 4.0f;
			}
			GLES10.glMaterialfv(GLES10.GL_FRONT_AND_BACK, GLES10.GL_AMBIENT_AND_DIFFUSE, new float[] { red, green, blue, 1.0f - trans }, 0);
			GLES10.glMaterialfv(GLES10.GL_FRONT_AND_BACK, GLES10.GL_SPECULAR, new float[] { shininess / 2, shininess / 2, shininess / 2, 1 }, 0);
			GLES10.glMaterialf(GLES10.GL_FRONT_AND_BACK, GLES10.GL_SHININESS, (128.0f * shininess));
			String textureUrl = sideAttributes.textureURL;
			if (textureUrl != null) {
				int textureId = lastTextureId;
				if (textureUrl != lastTextureUrl) {
					textureId = renderer.getTexture(textureUrl);
					lastTextureUrl = textureUrl;
				}
				if (!lastTextureMatrixIdentity) {
					GLES10.glMatrixMode(GLES10.GL_TEXTURE);
					GLES10.glLoadIdentity();
					lastTextureMatrixIdentity = true;
				}
				if (textureId != lastTextureId) {
					GLES10.glEnable(GLES10.GL_TEXTURE_2D);
					GLES10.glBindTexture(GLES10.GL_TEXTURE_2D, textureId);
					lastTextureId = textureId;
					if (sideAttributes.alphaTest) {
						GLES10.glEnable(GLES10.GL_ALPHA_TEST);
					}
				}
			} else if (lastTextureId != -1) {
				GLES10.glDisable(GLES10.GL_TEXTURE_2D);
				lastTextureId = -1;
				GLES10.glDisable(GLES10.GL_ALPHA_TEST);
			}
			if (sideAttributes.fullBright) {
				GLES10.glLightfv(GLES10.GL_LIGHT0, GLES10.GL_DIFFUSE, new float[] { 1, 1, 1, 0 }, 0);
				GLES10.glLightfv(GLES10.GL_LIGHT0, GLES10.GL_AMBIENT, new float[] { 1, 1, 1, 0 }, 0);
				GLSurface.drawMonolith(groupSurfaces, false);
				WWColor sunColor = object.world.getSunColor();
				float sunIntensity = object.world.getSunIntensity();
				float ambientLightIntensity = object.world.getAmbientLightIntensity();
				GLES10.glLightfv(GLES10.GL_LIGHT0, GLES10.GL_DIFFUSE, new float[] { sunIntensity * sunColor.getRed(), sunIntensity * sunColor.getGreen(), sunIntensity * sunColor.getBlue(), 0.0f }, 0);
				GLES10.glLightfv(GLES10.GL_LIGHT0, GLES10.GL_AMBIENT, new float[] { ambientLightIntensity * sunColor.getRed(), ambientLightIntensity * sunColor.getGreen(), ambientLightIntensity * sunColor.getBlue(), 0.0f }, 0);
			} else {
				GLSurface.drawMonolith(groupSurfaces, shading);
			}
			if (textureUrl != null) {
				GLES10.glMatrixMode(GLES10.GL_MODELVIEW);
			}
		}

	}

	static int lastTextureId;

}
