package com.gallantrealm.myworld.android.renderer.neu;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
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
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.Matrix;

public final class NewAndroidRenderer extends AndroidRenderer implements IRenderer, GLSurfaceView.Renderer {

	public static boolean USE_DEPTH_SHADER = false; // setting to true causes issues on Mali GPU's (Galaxy S6)

	public DepthShader depthShader;
	public ShadowMapShader shadowMapShader;
	public Shader textureShader;
	public Shader simpleTextureShader;
	public Shader shadowingTextureShader;

	public static HashMap<String, GLSurface[]> geometryCache = new HashMap();

	float[] projectionMatrix;
	float[] viewMatrix;
	float[] sunViewMatrix;

	static final int SHADOW_MAP_WIDTH = 2048;
	static final int SHADOW_MAP_HEIGHT = 2048;
	int[] staticShadowMapFb, shadowMapFb, depthRb;
	int staticShadowMapTextureId, shadowMapTextureId;
	int staticShadowMapDrawCount;
	WWVector lastShadowCameraViewPosition = new WWVector();

	boolean surfaceCreated; // set to true once onSurfaceCreated finishes to avoid premature onDrawFrame work

	boolean is3DDevice; // set to true if this is on a device that supports 3d (using side-by-side viewports)

	boolean hasDepthTexture; // set to true if the depth textures are supported (so shadows can be shown)

	public NewAndroidRenderer(Context context, GLSurfaceView view) {
		super(context, view);
		this.projectionMatrix = new float[16];
		this.viewMatrix = new float[16];
		this.sunViewMatrix = new float[16];
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

	private void initializeStandardDraw() {

		// Select appropriate final scene rendering shaders
		if (clientModel.isSimpleRendering()) {
			textureShader = simpleTextureShader;
		} else {
			textureShader = shadowingTextureShader;
		}

		// bind default framebuffer
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

		GLES20.glViewport(0, 0, viewportWidth, viewportHeight);
		float ratio;
		if (viewportWidth > viewportHeight) {
			ratio = (float) viewportWidth / (float) viewportHeight;
		} else {
			ratio = (float) viewportHeight / (float) viewportWidth;
		}
		Matrix.frustumM(projectionMatrix, 0, -ratio / 2.0f * CLOSENESS, ratio / 2.0f * CLOSENESS, -0.5f * CLOSENESS, 0.5f * CLOSENESS, 1.0f * CLOSENESS, 16384 * CLOSENESS);

		GLES20.glDisable(GLES20.GL_DITHER);

		// Depth buffer settings
		GLES20.glEnable(GLES20.GL_CULL_FACE);
		GLES20.glCullFace(GLES20.GL_BACK);
		GLES20.glDepthFunc(GLES20.GL_LESS);
		GLES20.glEnable(GLES20.GL_DEPTH_TEST);
		GLES20.glFrontFace(GLES20.GL_CW);

		// Texture buffer settings
		GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
		// GLES20.glEnable(GLES20.GL_BLEND); // enabled before drawing translucent textures

		// set color texture properties
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);

		// set shadow map texture properties
		if (!clientModel.isSimpleRendering() && shadowMapTextureId != 0) {
			GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT); // GL_CLAMP_TO_EDGE);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT); // GL_CLAMP_TO_EDGE);
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, shadowMapTextureId);
		}

		// set bump map texture properties
		GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);

		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
	}

	private long lastDrawFrameTime = 0;

	public static void checkGlError() {
		int error;
		while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
			throw new RuntimeException("GLES20 Error " + error + ": " + GLU.gluErrorString(error));
		}
	}

	public static void ignoreGlError() {
		int error;
		while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
		}
	}

	@Override
	public void onSurfaceCreated(GL10 unused, EGLConfig config) {
		System.out.println(">onSurfaceCreated");
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

		// The depth texture extension is required for shadows
		String extensions = GLES20.glGetString(GLES20.GL_EXTENSIONS);
		System.out.println(extensions);
		if (extensions.contains("GL_OES_depth_texture")  && (extensions.contains("GL_OES_depth24") || extensions.contains("GL_OES_depth32"))) {
			hasDepthTexture = true;
		}

		// Initialize shaders
		simpleTextureShader = new SimpleTextureShader();
		shadowingTextureShader = new ShadowingTextureShader();
		if (USE_DEPTH_SHADER) {
			depthShader = new DepthShader();
		}
		if (hasDepthTexture) {
			shadowMapShader = new ShadowMapShader();
			setupShadowMap();
		}
		GLES20.glReleaseShaderCompiler();
		GLES20.glGetError(); // to clear as releaseShaderCompiler might not be supported

		if (clientModel != null && clientModel.world != null) {
			long time = clientModel.world.getWorldTime();
			clientModel.world.createRendering(this, time);
		}

		initializeStandardDraw();

		surfaceCreated = true;
		System.out.println("<onSurfaceCreated");
	}

	int viewportWidth;
	int viewportHeight;

	@Override
	public void onSurfaceChanged(GL10 unused, int width, int height) {
		System.out.println(">onSurfaceChanged");

		clearTextureCache();

		viewportWidth = width;
		viewportHeight = height;

		if (hasDepthTexture) {
			setupShadowMap();
		}
		initializeStandardDraw();

		System.out.println("<onSurfaceChanged");
	}

	int drawFrameCount;

	@Override
	public void onDrawFrame(GL10 unused) {
		GLES20.glGetError(); // to clear error flag

		if (!surfaceCreated) { // happens early when creating surface
			return;
		}

		WWWorld world = clientModel.world;
		if (world == null) { // happens on on disconnect
			return;
		} else if (world.getRendering() == null) { // this happens on disconnect sometimes too
			return;
		}

		try { // catch any other rendering exceptions

			long drawFrameTime = System.currentTimeMillis();
			if (lastDrawFrameTime == 0) {
				lastDrawFrameTime = drawFrameTime - 25;
			}
			if (world.getPhysicsIterationTime() == 0) { // physics running on rendering thread
				world.performPhysicsIteration(Math.min(drawFrameTime - lastDrawFrameTime, 50));
			}

			// wait long enough for 30 fps (or 10 fps for power saver)
			try {
				if (clientModel.isPowerSaver()) {
					Thread.sleep(Math.max(0, 100 - (drawFrameTime - lastDrawFrameTime)));
				} else {
					Thread.sleep(Math.max(0, 33 - (drawFrameTime - lastDrawFrameTime)));
				}
			} catch (InterruptedException e) {
			}

			// Enable the code below to print frame rate
//			if (drawFrameCount++ % 90 == 0) {
//				System.out.println(1000 / (drawFrameTime - lastDrawFrameTime));
//			}
			lastDrawFrameTime = drawFrameTime;

			GLWorld worldRendering = (GLWorld) world.getRendering();

			long time;
			synchronized (world) {
				time = world.getWorldTime();
				worldRendering.snap(time);
			}
			preRender(time);
			
			// Generate the shadow map texture(s)
			if (hasDepthTexture && !clientModel.isSimpleRendering() && world.supportsShadows() && worldRendering.drawnOnce) {
				// generate view matrix for perspective shadow map
				WWVector dampCameraDistanceVector = new WWVector(0, dampCameraDistance, 0);
				WWVector dampCameraRotationVector = new WWVector(-dampCameraTilt, -dampCameraLean, dampCameraPan);
				rotate(dampCameraDistanceVector, dampCameraRotationVector);
				float x = dampXCamera + dampCameraDistanceVector.x;
				float y = dampYCamera + dampCameraDistanceVector.y;
				float z = dampZCamera + dampCameraDistanceVector.z;
				clientModel.setDampedCameraLocation(x, y, z);
				Matrix.translateM(viewMatrix, 0, projectionMatrix, 0, 0, 0, 0);
				Matrix.rotateM(viewMatrix, 0, dampCameraTilt, 1, 0, 0);
				Matrix.rotateM(viewMatrix, 0, dampCameraLean, 0, 0, 1);
				Matrix.rotateM(viewMatrix, 0, -dampCameraPan, 0, 1, 0);
				Matrix.translateM(viewMatrix, 0, -x, -z, -y);
				shadowMapShader.setViewPosition(dampCameraDistanceVector.x, dampCameraDistanceVector.y, dampCameraDistanceVector.z);
				generateShadowMap(time, viewMatrix);
			}

			WWColor sunColor = world.getSunColor();
			float sunIntensity = world.getSunIntensity();
			float ambientLightIntensity = world.getAmbientLightIntensity();

			// Sunlight (note, must be done each frame for some unknown reason)
			WWVector normalizedSunPosition = clientModel.world.getSunDirection().normalize();
			textureShader.setSunPosition(normalizedSunPosition.x, normalizedSunPosition.y, normalizedSunPosition.z);
			textureShader.setSunColor(sunColor.getRed(), sunColor.getGreen(), sunColor.getBlue());
			textureShader.setSunIntensity(sunIntensity);
			textureShader.setAmbientLightIntensity(ambientLightIntensity);

			if (threadWaitingForPick != null) {
				pickingDraw(time, viewMatrix);
				initializeStandardDraw();
			}

			// Fog
			textureShader.setFogDensity(world.getFogDensity());

			// Clear to background color
			WWColor skyColor = world.getSkyColor();
			GLES20.glClearColor(skyColor.getRed(), skyColor.getGreen(), skyColor.getBlue(), 1);
			GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

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
				Matrix.translateM(viewMatrix, 0, projectionMatrix, 0, 0, 0, 0);
				Matrix.rotateM(viewMatrix, 0, stereoAmount * 1.0f, 0, 1, 0);
				Matrix.rotateM(viewMatrix, 0, dampCameraTilt, 1, 0, 0);
				Matrix.rotateM(viewMatrix, 0, dampCameraLean, 0, 0, 1);
				Matrix.rotateM(viewMatrix, 0, -dampCameraPan, 0, 1, 0);
				Matrix.translateM(viewMatrix, 0, -x, -z, -y);
				textureShader.setViewMatrix(viewMatrix);
				textureShader.setViewPosition(dampCameraDistanceVector.x, dampCameraDistanceVector.y, dampCameraDistanceVector.z);

				GLES20.glColorMask(true, false, false, true);

				// draw
				worldRendering.draw(textureShader, viewMatrix, sunViewMatrix, time, GLWorld.DRAW_TYPE_LEFT_EYE);

				// transform according to camera - right eye
				dampCameraDistanceVector = new WWVector(0, dampCameraDistance, 0);
				dampCameraDistanceVector.add(stereoAmount * 0.02f * dampCameraDistance, 0, 0);
				dampCameraRotationVector = new WWVector(-dampCameraTilt, -dampCameraLean, dampCameraPan);
				rotate(dampCameraDistanceVector, dampCameraRotationVector);
				x = dampXCamera + dampCameraDistanceVector.x;
				y = dampYCamera + dampCameraDistanceVector.y;
				z = dampZCamera + dampCameraDistanceVector.z;
				clientModel.setDampedCameraLocation(x, y, z);
				Matrix.translateM(viewMatrix, 0, projectionMatrix, 0, 0, 0, 0);
				Matrix.rotateM(viewMatrix, 0, stereoAmount * -1.0f, 0, 1, 0);
				Matrix.rotateM(viewMatrix, 0, dampCameraTilt, 1, 0, 0);
				Matrix.rotateM(viewMatrix, 0, dampCameraLean, 0, 0, 1);
				Matrix.rotateM(viewMatrix, 0, -dampCameraPan, 0, 1, 0);
				Matrix.translateM(viewMatrix, 0, -x, -z, -y);
				textureShader.setViewMatrix(viewMatrix);
				textureShader.setViewPosition(dampCameraDistanceVector.x, dampCameraDistanceVector.y, dampCameraDistanceVector.z);

				GLES20.glColorMask(false, true, true, true);

				// draw again
				GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT);
				worldRendering.draw(textureShader, viewMatrix, sunViewMatrix, time, GLWorld.DRAW_TYPE_RIGHT_EYE);

				GLES20.glColorMask(true, true, true, true);

			} else {

				// transform according to camera
				WWVector dampCameraDistanceVector = new WWVector(0, dampCameraDistance, 0);
				WWVector dampCameraRotationVector = new WWVector(-dampCameraTilt, -dampCameraLean, dampCameraPan);
				rotate(dampCameraDistanceVector, dampCameraRotationVector);
				float x = dampXCamera + dampCameraDistanceVector.x;
				float y = dampYCamera + dampCameraDistanceVector.y;
				float z = dampZCamera + dampCameraDistanceVector.z;
				clientModel.setDampedCameraLocation(x, y, z);
				Matrix.translateM(viewMatrix, 0, projectionMatrix, 0, 0, 0, 0);
				Matrix.rotateM(viewMatrix, 0, dampCameraTilt, 1, 0, 0);
				Matrix.rotateM(viewMatrix, 0, dampCameraLean, 0, 0, 1);
				Matrix.rotateM(viewMatrix, 0, -dampCameraPan, 0, 1, 0);
				Matrix.translateM(viewMatrix, 0, -x, -z, -y);
				textureShader.setViewMatrix(viewMatrix);
				textureShader.setViewPosition(dampCameraDistanceVector.x, dampCameraDistanceVector.y, dampCameraDistanceVector.z);

				if (USE_DEPTH_SHADER) {
					// Do a depth-only draw
					GLES20.glColorMask(false, false, false, false);
					worldRendering.draw(depthShader, viewMatrix, sunViewMatrix, time, GLWorld.DRAW_TYPE_SHADOW);
					// Followed by a full draw
					GLES20.glDepthFunc(GLES20.GL_LEQUAL);
					GLES20.glColorMask(true, true, true, true);
					GLES20.glDepthMask(false);
					worldRendering.draw(textureShader, viewMatrix, sunViewMatrix, time, GLWorld.DRAW_TYPE_MONO);
					GLES20.glDepthMask(true);
					GLES20.glDepthFunc(GLES20.GL_LESS);
				} else {
					worldRendering.draw(textureShader, viewMatrix, sunViewMatrix, time, GLWorld.DRAW_TYPE_MONO);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// checkGlError();

		if (clientModel.world != null) {
			clientModel.world.setRendered(true);
		}
		// Note: Used to delay physics start till here, but this caused a physics thread leak (would restart physics while pausing world)
		world.setRendered(true);
	}

	public final void rotate(WWVector point, WWVector rotation) {

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
			y = r * (float) Math.sin(newTheta);
			z = r * (float) Math.cos(newTheta);
		}

		// Rotate around y axis
		if (rotationY != 0.0) {
			r = (float) Math.sqrt(x * x + z * z);
			theta = FastMath.atan2(x, z);
			newTheta = theta + TORADIAN * -rotationY;
			x = r * (float) Math.sin(newTheta);
			z = r * (float) Math.cos(newTheta);
		}

		// Rotate around z axis
		if (rotationZ != 0.0) {
			r = (float) Math.sqrt(x * x + y * y);
			theta = FastMath.atan2(x, y);
			newTheta = theta + TORADIAN * rotationZ;
			x = r * (float) Math.sin(newTheta);
			y = r * (float) Math.cos(newTheta);
		}

		point.x = x;
		point.y = y;
		point.z = z;
	}

	private void preRender(long time) {
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
									if (clientModel.getAvatar() == object) {
										if (clientModel.getCameraObject() == null) {
											clientModel.setCameraObject(object);
										}
										if (clientModel.getCameraObject() == object) {
											if (clientModel.cameraInitiallyFacingAvatar) {
												clientModel.setCameraPan(180);
												clientModel.setCameraDistance(clientModel.initiallyFacingDistance);
												clientModel.setCameraTilt(30.0f);
											}
										}
										// clientModel.setCameraDistance(2.0f);
										// clientModel.setCameraTilt(5.0f);
										// clientModel.setCameraPan(0.0f);
										// clientModel.setCameraLean(0.0f);
									}
								}

								// Update the rendering if the object has been updated
								// else if (object.getLastRenderingTime() < object.getLastModifyTime()) {
								// IRendering rendering = object.getRendering();
								// rendering.update();
								// }

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
	public WWObject pickedObject;

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
	 * Creates a texture and frame buffer object (FBO) used for a shadow map. Returns the texture id for it.
	 */
	private void setupShadowMap() {
		System.out.println(">setupShadowMap");
		int texW = SHADOW_MAP_WIDTH;
		int texH = SHADOW_MAP_HEIGHT;

		// create the ints for the framebuffer, depth render buffer and texture
		shadowMapFb = new int[1];
		int[] depthRb = new int[1];
		int[] renderTex = new int[1];

		// generate and bind the frame buffer
		GLES20.glGenFramebuffers(1, shadowMapFb, 0);
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, shadowMapFb[0]);

		// generate and bind the depth texture
		GLES20.glGenTextures(1, renderTex, 0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, renderTex[0]);

		// generate the textures
		GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES30.GL_DEPTH_COMPONENT, texW, texH, 0, GLES20.GL_DEPTH_COMPONENT, GLES20.GL_UNSIGNED_INT, null);

		// create render buffer and bind 16-bit depth buffer
//		GLES20.glGenRenderbuffers(1, depthRb, 0);
//		GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, depthRb[0]);
//		GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, texW, texH);
//		GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, depthRb[0]);

		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
		GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_TEXTURE_2D, renderTex[0], 0);

		shadowMapTextureId = renderTex[0];

		// set shadow map texture properties
		GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT); // GL_CLAMP_TO_EDGE);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT); // GL_CLAMP_TO_EDGE);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, shadowMapTextureId);

		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

		System.out.println("<setupShadowMap");
	}

	/**
	 * Generates a shadow map for the moving objects.
	 */
	private void generateShadowMap(long time, float[] viewMatrix) {

		// bind the shadow framebuffer
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, shadowMapFb[0]);

		// Cull front faces for shadow generation
		GLES20.glCullFace(GLES20.GL_FRONT);

		// viewport
		GLES20.glViewport(0, 0, SHADOW_MAP_WIDTH, SHADOW_MAP_HEIGHT);

		// Clear buffers
		GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT); // only the depth buffer is used

		// View from the light's perspective
		float FAR_OUT = 1000000.0f;
		WWVector sunPosition = clientModel.world.getSunDirection().normalize().scale(FAR_OUT);
		float ratio = (float) SHADOW_MAP_WIDTH / SHADOW_MAP_HEIGHT;
		float[] sunProjectionMatrix = new float[16];
		float zoom = 0.08f / FastMath.max(0.25f, (float) Math.sqrt(clientModel.getCameraDistance() / (1.0f + clientModel.getCameraTilt() / 30.0f))); // + (15 - clientModel.getCameraTilt() / 6.0f));
		Matrix.frustumM(sunProjectionMatrix, 0, -ratio / zoom, ratio / zoom, -1 / zoom, 1 / zoom, FAR_OUT - 1000.0f, FAR_OUT + 1000.0f); // sunPosition.length() * 0.99f, 100000.0f);

		WWObject cameraObject = clientModel.getCameraObject();
		if (cameraObject != null) {
			lastShadowCameraViewPosition = cameraObject.getPosition();
			lastShadowCameraViewPosition.x -= FastMath.sinDeg(clientModel.getCameraPan() + cameraObject.getRotation().z) * 20 * (1.0f - clientModel.getCameraTilt() / 90.0f);
			lastShadowCameraViewPosition.y -= FastMath.cosDeg(clientModel.getCameraPan() + cameraObject.getRotation().z) * 20 * (1.0f - clientModel.getCameraTilt() / 90.0f);
			Matrix.setLookAtM(sunViewMatrix, 0, //
					lastShadowCameraViewPosition.x + sunPosition.x, sunPosition.z + lastShadowCameraViewPosition.z, lastShadowCameraViewPosition.y + sunPosition.y, // sun position
					lastShadowCameraViewPosition.x, lastShadowCameraViewPosition.z, lastShadowCameraViewPosition.y, // center (where the light is looking at)
					0, 1, 0 // up vector
			);
		} else {
			Matrix.setLookAtM(sunViewMatrix, 0, //
					sunPosition.x, sunPosition.z, sunPosition.y, // sun position
					0, 0, 0, // center (where the light is looking at)
					0, 1, 0 // up vector
			);
		}

		// modelviewprojection matrix
		Matrix.multiplyMM(sunViewMatrix, 0, sunProjectionMatrix, 0, sunViewMatrix, 0);

		GLES20.glColorMask(false, false, false, false); // no sense drawing colors

		// Draw the objects
		if (clientModel.world != null) {
			GLWorld worldRendering = (GLWorld) clientModel.world.getRendering();
			worldRendering.draw(shadowMapShader, sunViewMatrix, sunViewMatrix, time, GLWorld.DRAW_TYPE_SHADOW);
		}

		// restore settings to what's needed for normal draw
		GLES20.glColorMask(true, true, true, true);
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
		GLES20.glViewport(0, 0, viewportWidth, viewportHeight);
		GLES20.glCullFace(GLES20.GL_BACK);
	}

	/**
	 * Perform a special frame draw that draws each object in a different color, to determine what object is at a particular pixel.
	 */
	public void pickingDraw(long time, float[] viewMatrix) {
		// TODO GLES20.glEnableClientState(GLES20.GL_VERTEX_ARRAY);
		// TODO GLES20.glDisableClientState(GLES20.GL_NORMAL_ARRAY);
		// TODO GLES20.glDisableClientState(GLES20.GL_TEXTURE_COORD_ARRAY);
		GLES20.glEnable(GLES20.GL_CULL_FACE);
		GLES20.glDepthFunc(GLES20.GL_LEQUAL);
		GLES20.glEnable(GLES20.GL_DEPTH_TEST);
		GLES20.glFrontFace(GLES20.GL_CW);
		// GLES20.glMatrixMode(GLES20.GL_MODELVIEW);
		// GLES20.glLoadIdentity();

		// TODO GLES20.glDisable(GLES20.GL_LIGHTING);
		// GLES20.glDisable(GLES20.GL_FOG);
		textureShader.setFogDensity(0.0f);

		// Clear to black
		GLES20.glClearColor(0, 0, 0, 1);
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

		// transform according to camera
		WWVector dampCameraDistanceVector = new WWVector(0, dampCameraDistance, 0);
		WWVector dampCameraRotationVector = new WWVector(-dampCameraTilt, -dampCameraLean, dampCameraPan);
		rotate(dampCameraDistanceVector, dampCameraRotationVector);
		float x = dampXCamera + dampCameraDistanceVector.x;
		float y = dampYCamera + dampCameraDistanceVector.y;
		float z = dampZCamera + dampCameraDistanceVector.z;
		clientModel.setDampedCameraLocation(x, y, z);
		Matrix.translateM(viewMatrix, 0, projectionMatrix, 0, 0, 0, 0);
		Matrix.rotateM(viewMatrix, 0, dampCameraTilt, 1, 0, 0);
		Matrix.rotateM(viewMatrix, 0, dampCameraLean, 0, 0, 1);
		Matrix.rotateM(viewMatrix, 0, -dampCameraPan, 0, 1, 0);
		Matrix.translateM(viewMatrix, 0, -x, -z, -y);

		GLWorld worldRendering = (GLWorld) clientModel.world.getRendering();
		worldRendering.draw(textureShader, viewMatrix, sunViewMatrix, time, GLWorld.DRAW_TYPE_PICKING);
		GLES20.glFlush();
		GLES20.glFinish();

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
		if (objectId == 0) {
			pickedObject = null;
		} else {
			pickedObject = clientModel.world.objects[objectId];
		}
		Thread thread = threadWaitingForPick;
		threadWaitingForPick = null;
		synchronized (thread) {
			thread.notify();
		}
	}

	private int getObjectIdAtPixel(int x, int y) {
		ByteBuffer pixelBuffer = ByteBuffer.allocateDirect(4);
		pixelBuffer.order(ByteOrder.nativeOrder());
		GLES20.glPixelStorei(GLES20.GL_PACK_ALIGNMENT, 1);
		GLES20.glReadPixels(x, y, 1, 1, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixelBuffer);
		pixelBuffer.rewind();
		byte b[] = new byte[4];
		pixelBuffer.get(b);
		// System.out.println("x=" + x + " y=" + y + " " + b[0] + " " + b[1] + " " + b[2] + " " + b[3]);
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
		return new GLParticleEmitter(this, particles, worldTime);
	}

}
