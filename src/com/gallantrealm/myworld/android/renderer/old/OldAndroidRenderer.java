package com.gallantrealm.myworld.android.renderer.old;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.HashMap;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;
import com.gallantrealm.myworld.FastMath;
import com.gallantrealm.myworld.android.renderer.AndroidRenderer;
import com.gallantrealm.myworld.client.renderer.IRenderer;
import com.gallantrealm.myworld.client.renderer.IRendering;
import com.gallantrealm.myworld.client.renderer.ITextureRenderer;
import com.gallantrealm.myworld.client.renderer.IVideoTextureRenderer;
import com.gallantrealm.myworld.model.WWBox;
import com.gallantrealm.myworld.model.WWColor;
import com.gallantrealm.myworld.model.WWCylinder;
import com.gallantrealm.myworld.model.WWMesh;
import com.gallantrealm.myworld.model.WWObject;
import com.gallantrealm.myworld.model.WWParticleEmitter;
import com.gallantrealm.myworld.model.WWPlant;
import com.gallantrealm.myworld.model.WWSculpty;
import com.gallantrealm.myworld.model.WWSphere;
import com.gallantrealm.myworld.model.WWTorus;
import com.gallantrealm.myworld.model.WWTranslucency;
import com.gallantrealm.myworld.model.WWVector;
import com.gallantrealm.myworld.model.WWWorld;
import com.htc.view.DisplaySetting;
import com.lge.real3d.Real3D;
import com.lge.real3d.Real3DInfo;
import android.content.Context;
import android.opengl.GLES10;
import android.opengl.GLSurfaceView;

public final class OldAndroidRenderer extends AndroidRenderer implements IRenderer, GLSurfaceView.Renderer {

	public static HashMap<String, GLSurface[]> geometryCache = new HashMap();

	boolean surfaceCreated; // set to true once onSurfaceCreated finishes to avoid premature onDrawFrame work

	boolean is3DDevice; // set to true if this is on a device that supports 3d (using side-by-side viewports)

	public OldAndroidRenderer(Context context, GLSurfaceView view) {
		super(context, view);
	}

	@Override
	public IRendering createWorldRendering(WWWorld world, long worldTime) {
		return new GLWorld(this, world, worldTime);
	}

	@Override
	public IRendering createBoxRendering(WWBox box, long worldTime) {
		return new GLSimpleShape(this, box, worldTime);
	}

	@Override
	public IRendering createCylinderRendering(WWCylinder cylinder, long worldTime) {
		return new GLSimpleShape(this, cylinder, worldTime);
	}

	@Override
	public IRendering createSphereRendering(WWSphere sphere, long worldTime) {
		return new GLSimpleShape(this, sphere, worldTime);
	}

	@Override
	public IRendering createTorusRendering(WWTorus torus, long worldTime) {
		return new GLSimpleShape(this, torus, worldTime);
	}

	@Override
	public IRendering createMeshRendering(WWMesh mesh, long worldTime) {
		return new GLMesh(this, mesh, worldTime);
	}

	@Override
	public IRendering createSculptyRendering(WWSculpty sculpty, long worldTime) {
		return new GLSculpty(this, sculpty, worldTime);
	}

	@Override
	public IRendering createPlantRendering(WWPlant plant, long worldTime) {
		return new GLPlant(this, plant, worldTime);
	}

	@Override
	public IRendering createTranslucencyRendering(WWTranslucency translucency, long worldTime) {
		return new GLTranslucency(this, translucency, worldTime);
	}

	@Override
	public ITextureRenderer getTextureRenderer() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IVideoTextureRenderer getVideoTextureRenderer() {
		// TODO Auto-generated method stub
		return null;
	}

	final void initializeStandardDraw() {

// gl.glViewport(0, 0, screenWidth, screenHeight);
		GLES10.glDisable(GLES10.GL_DITHER);
		GLES10.glDisable(GLES10.GL_MULTISAMPLE);
		GLES10.glDisable(GLES10.GL_NORMALIZE);
		GLES10.glEnableClientState(GLES10.GL_VERTEX_ARRAY);
		GLES10.glEnable(GLES10.GL_CULL_FACE);
		GLES10.glShadeModel(GLES10.GL_SMOOTH);
		GLES10.glDepthFunc(GLES10.GL_LEQUAL);
		GLES10.glEnable(GLES10.GL_DEPTH_TEST);
		GLES10.glFrontFace(GLES10.GL_CW);
// GLES10.glAlphaFunc(GLES10.GL_GREATER, 0.25f); // for use when GL_ALPHA_TEST is enabled
		GLES10.glDisable(GLES10.GL_ALPHA_TEST);

		GLES10.glEnableClientState(GLES10.GL_NORMAL_ARRAY);
		GLES10.glEnable(GLES10.GL_LIGHTING);

		GLES10.glEnableClientState(GLES10.GL_TEXTURE_COORD_ARRAY);
		GLES10.glTexEnvx(GLES10.GL_TEXTURE_ENV, GLES10.GL_TEXTURE_ENV_MODE, GLES10.GL_MODULATE);

		GLES10.glBlendFunc(GLES10.GL_SRC_ALPHA, GLES10.GL_ONE_MINUS_SRC_ALPHA);
		// GLES11.glEnable(GLES11.GL_BLEND); // enabled before drawing translucent textures

		GLES10.glEnable(GLES10.GL_TEXTURE_2D);
		GLES10.glActiveTexture(GLES10.GL_TEXTURE0);
// int[] textures = new int[1];
// GLES10.glGenTextures(1, textures, 0);
// int textureId = textures[0];
// GLES10.glBindTexture(GLES10.GL_TEXTURE_2D, textureId);
		GLES10.glHint(GLES10.GL_PERSPECTIVE_CORRECTION_HINT, GLES10.GL_FASTEST);
// GLES10.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_GENERATE_MIPMAP, GL11.GL_FALSE);
// GLES10.glTexParameterf(GLES10.GL_TEXTURE_2D, GLES10.GL_TEXTURE_MIN_FILTER, GLES10.GL_NEAREST); //GL_LINEAR);
// GLES10.glTexParameterf(GLES10.GL_TEXTURE_2D, GLES10.GL_TEXTURE_MAG_FILTER, GLES10.GL_NEAREST); //GL_LINEAR);
// GLES10.glTexParameterx(GLES10.GL_TEXTURE_2D, GLES10.GL_TEXTURE_WRAP_S, GLES10.GL_REPEAT);
// GLES10.glTexParameterx(GLES10.GL_TEXTURE_2D, GLES10.GL_TEXTURE_WRAP_T, GLES10.GL_REPEAT);

		// GLES10.glHint(GLES10.GL_POLYGON_SMOOTH_HINT, GLES10.GL_NICEST);

		// Note, color material is not used
// GLES10.glEnableClientState(GLES10.GL_COLOR_ARRAY);
// GLES10.glEnable(GLES10.GL_COLOR_MATERIAL);

	}

	long lastDrawFrameTime = 0;

	@Override
	public void onDrawFrame(GL10 gl) {
		try {

			if (!surfaceCreated) { // happens early when creating surface
				return;
			}

			WWWorld world = clientModel.world;
			if (world == null) { // happens on on disconnect
				return;
			} else if (world.getRendering() == null) { // this happens on disconnect sometimes too
				return;
			}

			if (threadWaitingForPick != null) {
				pickingDraw(gl);
				initializeStandardDraw();
			}

			WWColor sunColor = world.getSunColor();

			// Fog
			if (world.getFogDensity() > 0) {
				GLES10.glEnable(GLES10.GL_FOG);
				GLES10.glFogx(GLES10.GL_FOG_MODE, GLES10.GL_EXP);
				GLES10.glFogfv(GLES10.GL_FOG_COLOR, FloatBuffer.wrap(new float[] { 0.75f * sunColor.getRed(), 0.75f * sunColor.getGreen(), 0.75f * sunColor.getBlue() }));
				GLES10.glFogf(GLES10.GL_FOG_DENSITY, 0.01f * world.getFogDensity());
			}

			// Sunlight (note, must be done each frame for some unknown reason)
			GLES10.glLightModelfv(GLES10.GL_LIGHT_MODEL_AMBIENT, new float[] { 0, 0, 0, 1 }, 0);

			GLES10.glEnable(GLES10.GL_LIGHT0);
			GLES10.glLightfv(GLES10.GL_LIGHT0, GLES10.GL_DIFFUSE, new float[] { world.getSunIntensity() * sunColor.getRed(), world.getSunIntensity() * sunColor.getGreen(), world.getSunIntensity() * sunColor.getBlue(), 1.0f }, 0);
			GLES10.glLightfv(GLES10.GL_LIGHT0, GLES10.GL_AMBIENT,
					new float[] { world.getAmbientLightIntensity() * sunColor.getRed(), world.getAmbientLightIntensity() * sunColor.getGreen(), world.getAmbientLightIntensity() * sunColor.getBlue(), 1.0f }, 0);
			GLES10.glLightfv(GLES10.GL_LIGHT0, GLES10.GL_POSITION, new float[] { world.getSunDirection().x, -world.getSunDirection().z, world.getSunDirection().y, 0.0f }, 0);
			GLES10.glLightfv(GLES10.GL_LIGHT0, GLES10.GL_SPECULAR, new float[] { world.getSunIntensity() * sunColor.getRed(), world.getSunIntensity() * sunColor.getGreen(), world.getSunIntensity() * sunColor.getBlue(), 0.0f }, 0);

			GLES10.glMatrixMode(GLES10.GL_MODELVIEW);
			GLES10.glLoadIdentity();

			// Clear to background color
			if (world.drawsFully()) {
				GLES10.glClear(GLES10.GL_DEPTH_BUFFER_BIT);
			} else {
				WWColor skyColor = world.getSkyColor();
				GLES10.glClearColor(skyColor.getRed(), skyColor.getGreen(), skyColor.getBlue(), 1);
				GLES10.glClear(GLES10.GL_COLOR_BUFFER_BIT | GLES10.GL_DEPTH_BUFFER_BIT);
				GLES10.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
			}

			long time;
			time = world.getRenderingWorldTime(); // getWorldTime();
			preRender(time);

			GLWorld worldRendering = (GLWorld) world.getRendering();
			if (clientModel.isStereoscopic()) {

				// transform according to camera - left eye
				WWVector dampCameraDistanceVector = new WWVector(0, dampCameraDistance, 0);
				dampCameraDistanceVector.add(stereoAmount * -0.02f * dampCameraDistance, 0, 0);
				WWVector dampCameraRotationVector = new WWVector(-dampCameraTilt, -dampCameraLean, dampCameraPan);
				rotate(dampCameraDistanceVector, dampCameraRotationVector);
				float x = dampXCamera + dampCameraDistanceVector.x;
				float y = dampYCamera + dampCameraDistanceVector.y;
				float z = dampZCamera + dampCameraDistanceVector.z;
				clientModel.setDampedCameraLocation(x, y, z);
				GLES10.glRotatef(stereoAmount * 1.0f, 0.0f, 1.0f, 0.0f);
				GLES10.glRotatef(dampCameraTilt, 1.0f, 0.0f, 0.0f);
				GLES10.glRotatef(dampCameraLean, 0.0f, 0.0f, 1.0f);
				GLES10.glRotatef(-dampCameraPan, 0.0f, 1.0f, 0.0f);
				GLES10.glTranslatef(-x, -z, -y);

				if (is3DDevice) {
					gl.glViewport(0, 0, screenWidth / 2, screenHeight);
					worldRendering.draw(time, true, 0, 0);
				} else {
					GLES10.glColorMask(true, false, false, true);
					worldRendering.draw(time, true, 0, 1);
				}

				// transform according to camera - right eye
				GLES10.glMatrixMode(GLES10.GL_MODELVIEW);
				GLES10.glLoadIdentity();
				dampCameraDistanceVector = new WWVector(0, dampCameraDistance, 0);
				dampCameraDistanceVector.add(stereoAmount * 0.02f * dampCameraDistance, 0, 0);
				dampCameraRotationVector = new WWVector(-dampCameraTilt, -dampCameraLean, dampCameraPan);
				rotate(dampCameraDistanceVector, dampCameraRotationVector);
				x = dampXCamera + dampCameraDistanceVector.x;
				y = dampYCamera + dampCameraDistanceVector.y;
				z = dampZCamera + dampCameraDistanceVector.z;
				clientModel.setDampedCameraLocation(x, y, z);
				GLES10.glRotatef(stereoAmount * -1.0f, 0.0f, 1.0f, 0.0f);
				GLES10.glRotatef(dampCameraTilt, 1.0f, 0.0f, 0.0f);
				GLES10.glRotatef(dampCameraLean, 0.0f, 0.0f, 1.0f);
				GLES10.glRotatef(-dampCameraPan, 0.0f, 1.0f, 0.0f);
				GLES10.glTranslatef(-x, -z, -y);

				if (is3DDevice) {
					gl.glViewport(screenWidth / 2, 0, screenWidth / 2, screenHeight);
					worldRendering.draw(time, true, 0, 0);
				} else {
					GLES10.glColorMask(false, true, true, true);
					GLES10.glClear(GLES10.GL_DEPTH_BUFFER_BIT);
					worldRendering.draw(time, true, 0, 2);
				}

				if (is3DDevice) {

				} else {
					GLES10.glColorMask(true, true, true, true);
				}

			} else {

				// transform according to camera
				WWVector dampCameraDistanceVector = new WWVector(0, dampCameraDistance, 0);
				WWVector dampCameraRotationVector = new WWVector(-dampCameraTilt, -dampCameraLean, dampCameraPan);
				rotate(dampCameraDistanceVector, dampCameraRotationVector);
				float x = dampXCamera + dampCameraDistanceVector.x;
				float y = dampYCamera + dampCameraDistanceVector.y;
				float z = dampZCamera + dampCameraDistanceVector.z;
				clientModel.setDampedCameraLocation(x, y, z);
				GLES10.glRotatef(dampCameraTilt, 1.0f, 0.0f, 0.0f);
				GLES10.glRotatef(dampCameraLean, 0.0f, 0.0f, 1.0f);
				GLES10.glRotatef(-dampCameraPan, 0.0f, 1.0f, 0.0f);
				GLES10.glTranslatef(-x, -z, -y);

				// draw
				worldRendering.draw(time, true, 0, 0);
			}

			long drawFrameTime = System.currentTimeMillis();
			if (lastDrawFrameTime == 0) {
				lastDrawFrameTime = drawFrameTime - 25;
			}
			if (world.getPhysicsIterationTime() == 0) { // physics running on rendering thread
				world.performPhysicsIteration(Math.min(drawFrameTime - lastDrawFrameTime, 50));
			}

			// wait long enough for 30 fps (or 10 fps for power saver)
			try {
				long sleepTime = clientModel.getRefreshRate() - (drawFrameTime - lastDrawFrameTime);
				if (sleepTime > 0) {
					Thread.sleep(sleepTime);
				} else {
					Thread.sleep(0);
				}
			} catch (InterruptedException e) {
			}
			lastDrawFrameTime = drawFrameTime;

			// Note: Used to delay physics start till here, but this caused a physics thread leak (would restart physics while pausing world)
			world.setRendered(true);

		} catch (Throwable t) {
			t.printStackTrace();
			// no sense failing on a draw
		}
	}

	public void rotate(WWVector point, WWVector rotation) {

		float x = point.x;
		float y = point.y;
		float z = point.z;
		float r;
		float theta;
		float newTheta;

		float rotationX = rotation.x;
		float rotationY = rotation.y;
		float rotationZ = rotation.z;

		// Rotate around x axis
		if (rotationX != 0.0) {
			r = (float) Math.sqrt(y * y + z * z);
			theta = FastMath.atan2(y, z);
			newTheta = theta + TORADIAN * rotationX;
			y = r * FastMath.sin(newTheta);
			z = r * FastMath.cos(newTheta);
		}

		// Rotate around y axis
		if (rotationY != 0.0) {
			r = (float) Math.sqrt(x * x + z * z);
			theta = FastMath.atan2(x, z);
			newTheta = theta + TORADIAN * -rotationY;
			x = r * FastMath.sin(newTheta);
			z = r * FastMath.cos(newTheta);
		}

		// Rotate around z axis
		if (rotationZ != 0.0) {
			r = (float) Math.sqrt(x * x + y * y);
			theta = FastMath.atan2(x, y);
			newTheta = theta + TORADIAN * rotationZ;
			x = r * FastMath.sin(newTheta);
			y = r * FastMath.cos(newTheta);
		}

		point.x = x;
		point.y = y;
		point.z = z;
	}

	int screenWidth;
	int screenHeight;

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		System.out.println(">OldAndroidRenderer.onSurfaceChanged");
		screenWidth = width;
		screenHeight = height;

		GLES10.glViewport(0, 0, width, height);

		/*
		 * Set our projection matrix. This doesn't have to be done each time we draw, but usually a new projection needs to be set when the viewport is resized.
		 */

		float ratio = (float) width / (float) height;
		float zoom = 1.0f;
		if (ratio < 1) {
			zoom *= ratio;
		}
		GLES10.glMatrixMode(GLES10.GL_PROJECTION);
		GLES10.glLoadIdentity();
		GLES10.glFrustumf(-ratio / 2.0f * CLOSENESS / zoom, ratio / 2.0f * CLOSENESS / zoom, -0.5f * CLOSENESS / zoom, 0.5f * CLOSENESS / zoom, 1.0f * CLOSENESS, 10000.0f);

		initializeStandardDraw();
		System.out.println("<OldAndroidRenderer.onSurfaceChanged");
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		System.out.println(">OldAndroidRenderer.onSurfaceCreated");
		if (clientModel.isStereoscopic()) {
			try {
				is3DDevice = DisplaySetting.setStereoscopic3DFormat(view.getHolder().getSurface(), DisplaySetting.STEREOSCOPIC_3D_FORMAT_SIDE_BY_SIDE);
			} catch (Throwable e) {
				e.printStackTrace();
				System.out.println("No HTC 3D device support: " + e.getMessage());
			}
			if (!is3DDevice) {
				try {
					Real3D real3D = new Real3D(view.getHolder());
					is3DDevice = real3D.setReal3DInfo(new Real3DInfo(true, Real3D.REAL3D_TYPE_SS, Real3D.REAL3D_ORDER_LR));
				} catch (Throwable e) {
					System.out.println("No LG 3D device support: " + e.getMessage());
				}
			}
		}
		clearTextureCache();
		clearRenderings();
		if (clientModel != null && clientModel.world != null) {
			long time = clientModel.world.getWorldTime();
			clientModel.world.createRendering(this, time);
		}
		surfaceCreated = true;
		System.out.println("<OldAndroidRenderer.onSurfaceCreated");
	}

	final void preRender(long time) {
		try {
			WWWorld world = clientModel.world;
			if (world == null) {
				// TODO delete all rendered objects?
			} else {

				// Check for world property changes
				long lastModifyTime = world.getLastModifyTime();
				if (world.getLastRenderingTime() < lastModifyTime) {
// world.updateRendering();
					world.setLastRenderingTime(lastModifyTime);
				}

				WWObject avatar = clientModel.getAvatar();

				// Get avatar position
				WWVector avatarPosition = new WWVector();
				if (avatar != null) {
					avatar.getPosition(avatarPosition, time);
				}

				// position the camera
				positionCamera(time);

				// Three times a second, check for changes to the rendering and render/derender objects
				if ((nrenders % (333 / clientModel.getRefreshRate())) == 0) {

					if (clearRenderings) {
						System.out.println("clear renderings entered");

// synchronized (world) {
						WWObject[] objects = world.getObjects();
						for (int i = 0; i < objects.length; i++) {
							WWObject object = objects[i];
							if (object != null) {
								IRendering rendering = object.getRendering();
								if (rendering != null) {
									object.dropRendering();
								}
							}
						}
						((GLWorld) world.getRendering()).drawnOnce = false;
						geometryCache.clear();
						GLSurface.initializeVertexBuffer();
						((GLWorld) world.getRendering()).drawnOnce = false;
						((GLWorld) world.getRendering()).drawGroups = null;
						// textureCache.clear();
						nrenders = 0;
						clearRenderings = false;
// }

						System.out.println("creating renderings for objects that are part of rendering groups");
						// note this is needed to make sure the surfaces are adjacent in the vertex buffer
						int largestGroup = 0;
						for (int i = 0; i <= world.lastObjectIndex; i++) {
							WWObject object = world.objects[i];
							if (object != null) {
								largestGroup = (object.group > largestGroup) ? object.group : largestGroup;
							}
						}
						for (int g = 1; g <= largestGroup; g++) {
							for (int i = 0; i <= world.lastObjectIndex; i++) {
								WWObject object = world.objects[i];
								if (object != null && object.group == g) {
									object.createRendering(this, time);
								}
							}
						}

						System.out.println("clear renderings leave");
					}

					WWObject[] objects = world.getObjects();
					WWVector objectPosition = new WWVector();
					for (int i = 0; i <= world.lastObjectIndex; i++) {
						WWObject object = objects[i];
						if (object != null) {

							// If the object is deleted
							if (object.deleted) {

								// Delete the rendering if it exists
								IRendering rendering = object.getRendering();
								if (rendering != null) {
									object.dropRendering();
								}

								// Else the object is not deleted
							} else {

								// Create the rendering if not created
								if (object.getRendering() == null) {
									object.createRendering(this, time);
									// If the new object is the user's avatar, set camera position on it
									if (clientModel.getSelectedObject() == null && clientModel.getAvatar() == object) {
										clientModel.setCameraObject(object);
										if (clientModel.cameraInitiallyFacingAvatar) {
											clientModel.setCameraPan(180);
											clientModel.setCameraDistance(clientModel.initiallyFacingDistance);
											clientModel.setCameraTilt(30.0f);
										}
										// clientModel.setCameraDistance(2.0f);
										// clientModel.setCameraTilt(5.0f);
										// clientModel.setCameraPan(0.0f);
										// clientModel.setCameraLean(0.0f);
									}
								}

								// Update the rendering if the object has been updated
//								else if (object.getLastRenderingTime() < object.getLastModifyTime()) {
//									IRendering rendering = object.getRendering();
//									rendering.update();
//								}

								// Reorient the rendering if the object has been moved
// else if (object.isDynamic() || object.getLastRenderingTime() < object.getLastMoveTime()) {
// IRendering rendering = object.getRendering();
// rendering.orient(time);
// }

								// If it is a translucency, reorient the translucency layers to face the camera
								if (object instanceof WWTranslucency) {
									float transparencyTilt = dampCameraTilt;
									float transparencyPan = dampCameraPan;
// Primitive primitive = ((WWTranslucency) object).getJava3dPrimitive();
// ((TranslucencyPrimitive) primitive).adjustTranslucencyForPerspective((float) transparencyPan, (float) transparencyTilt, clientModel.getCameraLocation(time), time);
								}

								object.getPosition(objectPosition, time);

								// If the object is within the rendering threshold (considering size), mark it for rendering
								if (object.parentId == 0) {
									if (avatarPosition.distanceFrom(objectPosition) / object.extent <= clientModel.world.getRenderingThreshold()) {
										object.renderit = true;
									} else {
										object.renderit = false;
									}
								} else {
									WWObject parentObject = world.objects[object.parentId];
									if (parentObject.parentId == 0) {
										if (avatarPosition.distanceFrom(parentObject.getPosition()) / object.extent <= clientModel.world.getRenderingThreshold()) {
											object.renderit = true;
										} else {
											object.renderit = false;
										}
									} else {
										object.renderit = true;
									}
								}

							}
						} // object != null
					} // for all objects
				}

				nrenders++;

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Thread threadWaitingForPick;
	public int pickX;
	public int pickY;

	@Override
	public WWObject waitForPickingDraw(WWObject object, int px, int py) {
		pickX = px;
		pickY = py;
		pickedObject = object;
		WWWorld world = clientModel.world;
		if (threadWaitingForPick == null && world != null && world.getRendered()) {
			threadWaitingForPick = Thread.currentThread();
			try {
				synchronized (Thread.currentThread()) {
					Thread.currentThread().wait();
				}
			} catch (InterruptedException e) {
			}
		}
		return pickedObject;
	}

	/**
	 * Perform a special frame draw that draws each object in a different color, to determine what object is at a particular pixel.
	 */
	public void pickingDraw(GL10 gl) {
		gl.glViewport(0, 0, screenWidth, screenHeight);
		GLES10.glEnableClientState(GLES10.GL_VERTEX_ARRAY);
		GLES10.glDisableClientState(GLES10.GL_NORMAL_ARRAY);
		GLES10.glDisableClientState(GLES10.GL_TEXTURE_COORD_ARRAY);
// GLES10.glEnable(GLES10.GL_CULL_FACE);
// GLES10.glDepthFunc(GLES10.GL_LEQUAL);
// GLES10.glEnable(GLES10.GL_DEPTH_TEST);
// GLES10.glFrontFace(GLES10.GL_CW);
// GLES10.glMatrixMode(GLES10.GL_MODELVIEW);
// GLES10.glLoadIdentity();

		GLES10.glDisable(GLES10.GL_LIGHTING);
		GLES10.glDisable(GLES10.GL_TEXTURE_2D);
		GLES10.glDisable(GLES10.GL_FOG);

		// Clear to black
		GLES10.glClearColor(0, 0, 0, 1);
		GLES10.glClear(GLES10.GL_COLOR_BUFFER_BIT | GLES10.GL_DEPTH_BUFFER_BIT);

		// transform according to camera
		float x = dampXCamera + dampCameraDistance * FastMath.sin(TORADIAN * dampCameraPan) * FastMath.cos(TORADIAN * dampCameraTilt);
		float y = dampYCamera + dampCameraDistance * FastMath.cos(TORADIAN * dampCameraPan) * FastMath.cos(TORADIAN * dampCameraTilt);
		float z = dampZCamera + FastMath.sin(TORADIAN * dampCameraTilt) * dampCameraDistance;
// GLES10.glRotatef(dampCameraTilt, 1.0f, 0.0f, 0.0f);
// GLES10.glRotatef(-dampCameraPan, 0.0f, 1.0f, 0.0f);
// GLES10.glTranslatef(-x, -z, -y);

		long time = clientModel.world.getWorldTime();
		GLWorld worldRendering = (GLWorld) clientModel.world.getRendering();

		if (pickedObject == null) {
			worldRendering.draw(time, false, 1, 0);

			int objectId = getObjectIdAtPixel(pickX, pickY);
			if (objectId == 0) {
				objectId = getObjectIdAtPixel(pickX - 2, pickY);
				if (objectId == 0) {
					objectId = getObjectIdAtPixel(pickX + 2, pickY);
					if (objectId == 0) {
						objectId = getObjectIdAtPixel(pickX, pickY - 2);
						if (objectId == 0) {
							objectId = getObjectIdAtPixel(pickX, pickY + 2);
							if (objectId == 0) {
								objectId = getObjectIdAtPixel(pickX - 2, pickY - 2);
								if (objectId == 0) {
									objectId = getObjectIdAtPixel(pickX + 2, pickY - 2);
									if (objectId == 0) {
										objectId = getObjectIdAtPixel(pickX - 2, pickY + 2);
										if (objectId == 0) {
											objectId = getObjectIdAtPixel(pickX + 2, pickY + 2);
											if (objectId == 0) {
												objectId = getObjectIdAtPixel(pickX - 4, pickY);
												if (objectId == 0) {
													objectId = getObjectIdAtPixel(pickX + 4, pickY);
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
			if (objectId == 0 || clientModel.world == null) {
				pickedObject = null;
			} else {
				pickedObject = clientModel.world.objects[objectId];
			}
		}

		// Now that the object has been determined, do another special draw to find out where
		// on the object
		if (pickedObject != null) {
			GLES10.glEnableClientState(GLES10.GL_TEXTURE_COORD_ARRAY);
			GLES10.glTexParameterf(GLES10.GL_TEXTURE_2D, GL11.GL_GENERATE_MIPMAP, GLES10.GL_FALSE);
			GLES10.glEnable(GLES10.GL_LIGHTING);
			GLES10.glEnable(GLES10.GL_TEXTURE_2D);

			GLES10.glClear(GLES10.GL_COLOR_BUFFER_BIT | GLES10.GL_DEPTH_BUFFER_BIT);
			// worldRendering.draw(time, false, 2, 0);
			((GLRendering) pickedObject.rendering).draw(time, false, 2, false, 0);
			ByteBuffer pixelBuffer = ByteBuffer.allocateDirect(4);
			pixelBuffer.order(ByteOrder.nativeOrder());
			GLES10.glFlush();
			GLES10.glPixelStorei(GLES10.GL_PACK_ALIGNMENT, 1);
			GLES10.glReadPixels(pickX, pickY, 1, 1, GLES10.GL_RGBA, GLES10.GL_UNSIGNED_BYTE, pixelBuffer);
			pixelBuffer.rewind();
			byte b[] = new byte[4];
			pixelBuffer.get(b);
			int rr = b[0];
			int gg = b[1];
			rr = (rr << 1) & 0x1C0;
			gg = (gg >> 2) & 0x3F;
			pickedOffsetX = (rr | gg) / 512.0f;

			GLES10.glClear(GLES10.GL_COLOR_BUFFER_BIT | GLES10.GL_DEPTH_BUFFER_BIT);
			// worldRendering.draw(time, false, 3, 0);
			((GLRendering) pickedObject.rendering).draw(time, false, 3, false, 0);
			pixelBuffer = ByteBuffer.allocateDirect(4);
			pixelBuffer.order(ByteOrder.nativeOrder());
			GLES10.glFlush();
			GLES10.glPixelStorei(GLES10.GL_PACK_ALIGNMENT, 1);
			GLES10.glReadPixels(pickX, pickY, 1, 1, GLES10.GL_RGBA, GLES10.GL_UNSIGNED_BYTE, pixelBuffer);
			pixelBuffer.rewind();
			pixelBuffer.get(b);
			rr = b[0];
			gg = b[1];
			rr = (rr << 1) & 0x1C0;
			gg = (gg >> 2) & 0x3F;
			pickedOffsetY = (rr | gg) / 512.0f;
		}

		Thread thread = threadWaitingForPick;
		threadWaitingForPick = null;
		if (thread != null) {
			synchronized (thread) {
				thread.notify();
			}
		}
	}

	final int getObjectIdAtPixel(int x, int y) {
		ByteBuffer pixelBuffer = ByteBuffer.allocateDirect(4);
		pixelBuffer.order(ByteOrder.nativeOrder());
		GLES10.glFlush();
		GLES10.glPixelStorei(GLES10.GL_PACK_ALIGNMENT, 1);
		GLES10.glReadPixels(x, y, 1, 1, GLES10.GL_RGBA, GLES10.GL_UNSIGNED_BYTE, pixelBuffer);
		pixelBuffer.rewind();
		byte b[] = new byte[4];
		pixelBuffer.get(b);
		b[0] >>= 4;
		b[0] &= 0xF;
		b[1] >>= 4;
		b[1] &= 0xF;
		b[2] >>= 4;
		b[2] &= 0xF;
		int objectId = (b[0] << 8) + (b[1] << 4) + b[2];
		return objectId;
	}

	@Override
	public IRendering createParticlesRendering(WWParticleEmitter particles, long worldTime) {
		// TODO Auto-generated method stub
		return null;
	}

}
