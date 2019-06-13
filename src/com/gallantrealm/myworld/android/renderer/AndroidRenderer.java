package com.gallantrealm.myworld.android.renderer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.Buffer;
import java.util.HashMap;
import com.gallantrealm.myworld.FastMath;
import com.gallantrealm.myworld.android.AndroidClientModel;
import com.gallantrealm.myworld.android.R;
import com.gallantrealm.myworld.android.renderer.neu.NewAndroidRenderer;
import com.gallantrealm.myworld.android.renderer.old.OldAndroidRenderer;
import com.gallantrealm.myworld.client.renderer.IRenderer;
import com.gallantrealm.myworld.client.renderer.ISoundGenerator;
import com.gallantrealm.myworld.model.WWObject;
import com.gallantrealm.myworld.model.WWVector;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.opengl.ETC1;
import android.opengl.ETC1Util;
import android.opengl.ETC1Util.ETC1Texture;
import android.opengl.GLES11;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;

public abstract class AndroidRenderer implements IRenderer, GLSurfaceView.Renderer {

	// These values are used in the positionCamera to damp the positional movements
	public float dampXCamera;
	public float dampYCamera;
	public float dampZCamera;
	public float dampCameraVelocityX;
	public float dampCameraVelocityY;
	public float dampCameraVelocityZ;
	public float dampCameraPan;
	public float dampCameraTilt;
	public float dampCameraLean;
	public float dampCameraDistance;

	public long lastCameraAdjustTime = 0;

	public WWVector cameraPoint = new WWVector();

	public WWObject pickedObject;
	public int pickedSide;
	public float pickedOffsetX;
	public float pickedOffsetY;

	public static final float CLOSENESS = 0.25f;
	public static final float TORADIAN = 0.0174532925f;
	public static final float TODEGREES = FastMath.TODEGREES;

	public float stereoAmount = 1.0f; // 0.5 - 2.0 are good range

	public final AndroidClientModel clientModel = AndroidClientModel.getClientModel();
	public float lastLimitedCameraDistance = clientModel.getCameraDistance();
	public static AndroidRenderer androidRenderer;

	public static AndroidRenderer createAndroidRenderer(Context context, GLSurfaceView view, boolean newRenderer) {
		if (newRenderer) {
			androidRenderer = new NewAndroidRenderer(context, view);
		} else {
			androidRenderer = new OldAndroidRenderer(context, view);
		}
		return androidRenderer;
	}

	public final AndroidSoundGenerator soundGenerator;
	public Context context;
	public GLSurfaceView view;
	public static HashMap<String, Integer> textureCache = new HashMap<String, Integer>();
	public static HashMap<String, Integer> normalTextureCache = new HashMap<String, Integer>();

	public AndroidRenderer(Context context, GLSurfaceView view) {
		System.out.println("AndroidRenderer.constructor");
		this.context = context;
		this.view = view;
		this.soundGenerator = new AndroidSoundGenerator(context);
	}

	public static AndroidRenderer getAndroidRenderer(Context context) {
		// androidRenderer.context = context;
		return androidRenderer;
	}

	private static final int viswindow = 60; // this needs to be based on zoom someday

	/**
	 * Returns true if the object is fully or partially visible (not occluded) and within rendering threshold. May return true even when not visible (as the implementation may improve over time).
	 */
	public final boolean isVisible(WWVector adjustedCameraPosition, WWObject object, long worldTime, WWVector temp) {
		if (!object.renderit) {
			return false;
		}
		if (Math.abs(dampCameraTilt) > 45) {
			return true;
		}

		object.getAbsoluteAnimatedPosition(temp, worldTime);
		temp.subtract(adjustedCameraPosition);

		if (temp.length() < object.extent) { // to big and close to accurately determine
			return true;
		}

		float theta = TODEGREES * FastMath.atan2(temp.y, temp.x);
		float offpan = Math.abs(theta + dampCameraPan + 36000) % 360 - 270;

		return offpan > -viswindow && offpan < viswindow;
	}

	public final WWVector getAdjustedCameraPosition() {
		float x = dampXCamera + dampCameraDistance * (float) Math.sin(TORADIAN * dampCameraPan) * (float) Math.cos(TORADIAN * dampCameraTilt);
		float y = dampYCamera + dampCameraDistance * (float) Math.cos(TORADIAN * dampCameraPan) * (float) Math.cos(TORADIAN * dampCameraTilt);
		float z = dampZCamera + (float) Math.sin(TORADIAN * dampCameraTilt) * dampCameraDistance;
		WWVector adjustedCameraPosition = new WWVector(x, y, z);
// if (Math.abs(dampCameraTilt) < 45) {
// adjustedCameraPosition.add(-clientModel.world.renderingThreshold * FastMath.sin(TORADIAN * dampCameraPan), -clientModel.world.renderingThreshold * FastMath.cos(TORADIAN * dampCameraPan), 0);
// }
		return adjustedCameraPosition;
	}

	public static boolean clearRenderings;
	public static long nrenders = 0;

	public static void clearRenderings() {
		clearRenderings = true;
		nrenders = 0;
	}

	/**
	 * Wait for a special draw to reveal the object or the position on from an object that the user is touching
	 * 
	 * @param object
	 *            the object to determine position from, or null if determining the object
	 * @param px
	 *            x on screen
	 * @param py
	 *            y on screen
	 * @return the object being touched
	 */
	public abstract WWObject waitForPickingDraw(WWObject object, int px, int py);

	Bitmap regenBitmap;
	String regenTextureName;

	public synchronized final int getNormalTexture(String textureName) {
		if (textureName == null) {
			textureName = "white";
		}
		Integer idInteger = normalTextureCache.get(textureName);
		if (idInteger != null) {
			return idInteger;
		} else {
			InputStream is = null;
			boolean isPkm = false;
			try {
				System.out.println("loading bitmap " + textureName + "_nrm");
				Bitmap bitmap = null;
				if (textureName.contains(":")) { // a url
					Uri uri = Uri.parse(textureName + "_nrm");
					Bitmap unscaledBitmap = readImageTexture(uri);
					if (unscaledBitmap != null) {
						bitmap = Bitmap.createScaledBitmap(unscaledBitmap, 256, 256, false);
						if (unscaledBitmap != bitmap) {
							unscaledBitmap.recycle();
						}
					}
				} else {
					int id = 0;
					if (textureName.equals("white")) {
						id = R.raw.white_nrm;
					}
					if (isPkm) {
						return genCompressedTexture(textureName + "_nrm");
					} else {
						if (id != 0) { // open a resource
							is = context.getResources().openRawResource(id); // raw resource
						} else { // open an asset or local file
							try {
								try {
									is = context.getAssets().open(textureName + "_nrm.dds"); // dds is faster?
									return genCompressedTexture(textureName + "_nrm");
								} catch (Exception e) {
									is = context.getAssets().open(textureName + "_nrm.png"); // asset
								}
							} catch (Exception e) {
								try {
									File file = new File(context.getFilesDir(), textureName + "_nrm.png"); // local file
									is = new BufferedInputStream(new FileInputStream(file), 65536);
								} catch (Exception e2) {
									// not there
								}
							}
						}
						if (is == null) {
							is = context.getResources().openRawResource(R.raw.white_nrm); // raw resource
						}
						bitmap = BitmapFactory.decodeStream(is);
					}
				}
				if (bitmap == null) {  // a failure
					int textureId = getNormalTexture("white");  // use white
					normalTextureCache.put(textureName, textureId);
					return textureId;
				}
				int textureId = genTexture(bitmap, textureName);
				normalTextureCache.put(textureName, textureId);
				return textureId;
			} finally {
				try {
					if (is != null) {
						is.close();
					}
				} catch (IOException e) {
					// Ignore.
				}
			}
		}
	}

	public synchronized final int getTexture(String textureName) {
		if (textureName == null) {
			textureName = "white";
		}
		Integer idInteger = textureCache.get(textureName);
		if (idInteger != null) {
			if (textureName.equals(regenTextureName)) {
				updateTexture(regenBitmap, idInteger);
				regenTextureName = null;
				regenBitmap = null;
			}
			return idInteger;
		} else {
			InputStream is = null;
			boolean isPkm = false;
			try {
				System.out.println("loading bitmap " + textureName);
				Bitmap bitmap = null;
				if (textureName.equals("surface_select")) { // a special bitmap
					bitmap = Bitmap.createBitmap(512, 512, Config.RGB_565);
					for (int i = 0; i < 512; i++) {
						int red = ((i & 0x1C0) >> 1) | 0x10;
						int green = (i & 0x03F) << 2;
						int blue = 0;
						int color = 0xFF000000 | (red << 16) | (green << 8) | blue;
						for (int j = 0; j < 512; j++) {
							bitmap.setPixel(i, j, color);
						}
					}
				} else if (textureName.contains(":")) { // a url
					Uri uri = Uri.parse(textureName);
					Bitmap unscaledBitmap = readImageTexture(uri);
					if (unscaledBitmap != null) {
						bitmap = Bitmap.createScaledBitmap(unscaledBitmap, 256, 256, false);
						if (unscaledBitmap != bitmap) {
							unscaledBitmap.recycle();
						}
					}
				} else {
					int id = 0;
					if (textureName.equals("surface_select")) {
						id = R.raw.surface_select;
					} else if (textureName.equals("white")) {
						id = R.raw.white;
					}
					if (isPkm) {
						return genCompressedTexture(textureName);
					} else {
						if (id != 0) { // open a resource
							is = context.getResources().openRawResource(id); // raw resource
						} else { // open an asset or local file
							try {
//								try {
//									is = context.getAssets().open(textureName + ".dds"); // dds is faster?
//									return genCompressedTexture(textureName);
//								} catch (Exception e) {
								is = context.getAssets().open(textureName + ".png"); // asset
//								}
							} catch (Exception e) {
								try {
									File file = new File(context.getFilesDir(), textureName + ".png"); // local file
									is = new BufferedInputStream(new FileInputStream(file), 65536);
								} catch (Exception e2) {
									// not there
								}
							}
						}
						if (is == null) {
							return 0;
						}
						bitmap = BitmapFactory.decodeStream(is);
					}
				}
				if (bitmap == null) { // problems
					int textureId = getTexture("white");   // use white texture
					textureCache.put(textureName, textureId);
					return textureId;
				}
				int textureId = genTexture(bitmap, textureName);
				textureCache.put(textureName, textureId);
				return textureId;
			} finally {
				try {
					if (is != null) {
						is.close();
					}
				} catch (IOException e) {
					// Ignore.
				}
			}
		}
	}

	final int genTexture(Bitmap bitmap, String textureName) {
		System.out.println("generating texture " + textureName);
		int[] textureIds = new int[1];
		GLES20.glGenTextures(1, textureIds, 0);
		int textureId = textureIds[0];

		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);

		if (textureName.equals("surface_select")) {
			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
		} else {
			if (this instanceof OldAndroidRenderer) {
				GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES11.GL_GENERATE_MIPMAP, GLES20.GL_FALSE);
			}
			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST_MIPMAP_NEAREST);
			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
		}
// GLES20.glTexParameterx(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
// GLES20.glTexParameterx(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);

		// Generate textures (original and mipmaps)
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		int level = 0;
		while (height >= 1 || width >= 1) {

			GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, level, bitmap, 0);
			// GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, level, GLES20.GL_RGB, bitmap, GLES20.GL_UNSIGNED_SHORT_5_6_5, 0);
			// GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, level, GLES20.GL_RGBA, bitmap, GLES20.GL_UNSIGNED_BYTE, 0);

			if (height <= 1 || width <= 1) {
				break;
			}
			if (textureName.equals("surface_select") || height > 1024 || width > 1024) { // avoid mipmapping large textures
				bitmap.recycle();
				break;
			}

			// Increase the mipmap level
			level++;

			// gen a scaled mipmap
			height /= 2;
			width /= 2;
			Bitmap bitmap2 = Bitmap.createScaledBitmap(bitmap, width, height, true);

			// Clean up
			bitmap.recycle();
			bitmap = bitmap2;
		}
		return textureId;
	}

	final int genCompressedTexture(String textureName) {
		System.out.println("generating compressed texture " + textureName);

		int[] textureIds = new int[1];
		GLES20.glGenTextures(1, textureIds, 0);
		int textureId = textureIds[0];
		textureCache.put(textureName, textureId);

		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
		if (this instanceof OldAndroidRenderer) {
			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES11.GL_GENERATE_MIPMAP, GLES20.GL_FALSE);
		}
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST_MIPMAP_NEAREST);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
// GLES20.glTexParameterx(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
// GLES20.glTexParameterx(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);

		try {
			int level = 0;
			while (level <= 9) {
				InputStream is = context.getAssets().open(textureName + "_mip_" + level + ".pkm"); // asset
				ETC1Texture etc1texture = ETC1Util.createTexture(is);

				// Generate textures (original and mipmaps)
				int width = etc1texture.getWidth();
				int height = etc1texture.getHeight();
				Buffer data = etc1texture.getData();
				int imageSize = data.remaining();
				GLES20.glCompressedTexImage2D(GLES20.GL_TEXTURE_2D, level, ETC1.ETC1_RGB8_OES, width, height, 0, imageSize, data);
				level++;
			}

		} catch (IOException e) {
			// e.printStackTrace();
		}
		return textureId;
	}

	final void updateTexture(Bitmap bitmap, int textureId) {
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);

		// avoid mipmapping for updated textures to save time
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);

		// Generate textures (original and mipmaps)
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		int level = 0;

		GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D, level, 0, 0, bitmap);
		// GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D, level, 0, 0, bitmap, GLES20.GL_RGB, GLES20.GL_UNSIGNED_SHORT_5_6_5);
		// GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D, level, 0, 0, bitmap, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE);
	}

	public final synchronized void regenTexture(Bitmap bitmap, String textureName) {
		this.regenBitmap = bitmap;
		this.regenTextureName = textureName;
	}

	/**
	 * Read the image into a 256x256 bitmap, ready for texture scaling
	 * 
	 * @param selectedImage
	 * @return
	 */
	public final Bitmap readImageTexture(Uri selectedImage) {
		if (selectedImage.getScheme() != null && selectedImage.getScheme().startsWith("http")) {
			// from the web
			Bitmap bm = null;
			try {
				HttpURLConnection connection = (HttpURLConnection) (new URL(selectedImage.toString())).openConnection();
				InputStream instream = connection.getInputStream();
				bm = BitmapFactory.decodeStream(instream);
				return bm;
			} catch (Exception e) {
				System.err.println("AndroidRenderer.readImageTexture: URL not found -- " + selectedImage);
			}
			return bm;
		} else {
			// from assets
			Bitmap bm = null;
			BitmapFactory.Options options = new BitmapFactory.Options();
			AssetFileDescriptor fileDescriptor = null;
			try {
				fileDescriptor = context.getContentResolver().openAssetFileDescriptor(selectedImage, "r");

				// first, get the bitmap size
				options.inJustDecodeBounds = true;
				bm = BitmapFactory.decodeFileDescriptor(fileDescriptor.getFileDescriptor(), null, options);

				// then, get a sampled-down bitmap large enough for the texture
				options.inJustDecodeBounds = false;
				options.inSampleSize = Math.min(options.outWidth, options.outHeight) / 512;
				bm = BitmapFactory.decodeFileDescriptor(fileDescriptor.getFileDescriptor(), null, options);
			} catch (FileNotFoundException e) {
				System.err.println("AndroidRenderer.readImageTexture: File not found -- " + selectedImage);
			} finally {
				try {
					if (fileDescriptor != null) {
						fileDescriptor.close();
					}
				} catch (IOException e) {
				}
			}

			// Determine the image rotation and antirotate if necessary
			try {
				ExifInterface exif = new ExifInterface(selectedImage.getPath());
				int exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
				System.out.println("EXIF rotation = " + exifOrientation);
				int rotation;
				if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
					rotation = 90;
				} else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
					rotation = 180;
				} else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
					rotation = 270;
				} else {
					rotation = 0;
				}
				if (rotation != 0) {
					Matrix matrix = new Matrix();
					matrix.preRotate(rotation);
					bm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			return bm;
		}
	}

	public final void clearTextureCache() {
		textureCache = new HashMap<String, Integer>(); // clear doesn't work
		normalTextureCache = new HashMap<String, Integer>();
	}

	@Override
	public final ISoundGenerator getSoundGenerator() {
		return soundGenerator;
	}

	/**
	 * Move the camera to the current camera position, pan, tilt and distance. This also dampens the camera movements, to make the shifting of the camera more obvious and natural.
	 */
	protected final void positionCamera(long time) {
		if (clientModel.world == null) {
			return;
		}
		clientModel.getCameraPoint(cameraPoint);
		float cameraPan = clientModel.getCameraPan();
		float cameraPanVelocity = clientModel.getCameraPanVelocity();
		float cameraTilt = clientModel.getCameraTilt();
		float cameraTiltVelocity = clientModel.getCameraTiltVelocity();
		float cameraLean = clientModel.getCameraLean();
		float cameraLeanVelocity = clientModel.getCameraLeanVelocity();
		float cameraDistance = clientModel.getCameraDistance();
		float cameraDistanceVelocity = clientModel.getCameraDistanceVelocity();
		WWObject cameraObject = clientModel.getCameraObject();

		WWObject avatar = clientModel.getAvatar();
		WWVector cameraObjectRotation;
		if (cameraObject != null) {
			cameraObjectRotation = cameraObject.getAbsoluteAnimatedRotation(time);
		} else {
			cameraObjectRotation = new WWVector();
		}
		float cameraSlideX = clientModel.getCameraSlideX();
		float cameraSlideY = clientModel.getCameraSlideY();
		float cameraSlideZ = clientModel.getCameraSlideZ();
		float cameraSlideXVelocity = clientModel.getCameraSlideXVelocity();
		float cameraSlideYVelocity = clientModel.getCameraSlideYVelocity();
		float cameraSlideZVelocity = clientModel.getCameraSlideZVelocity();

		// Apply camera velocities
		if (lastCameraAdjustTime > 0 && clientModel.world != null) {
			float timeDelta = (time - lastCameraAdjustTime) / 1000.0f;
			cameraSlideX += cameraSlideXVelocity * timeDelta;
			cameraSlideY += cameraSlideYVelocity * timeDelta;
			cameraSlideZ += cameraSlideZVelocity * timeDelta;
			cameraPan += cameraPanVelocity * timeDelta;
			cameraTilt += cameraTiltVelocity * timeDelta;
			cameraTilt = FastMath.min(89, FastMath.max(-89, cameraTilt));
			cameraLean += cameraLeanVelocity * timeDelta;
			cameraDistance += (cameraDistance * cameraDistanceVelocity) * timeDelta;
			clientModel.setCameraSlide(cameraSlideX, cameraSlideY, cameraSlideZ);
			clientModel.setCameraPan(cameraPan);
			clientModel.setCameraTilt(cameraTilt);
			clientModel.setCameraLean(cameraLean);
			clientModel.setCameraDistance(cameraDistance);
		}
		lastCameraAdjustTime = time;

		// Adjust camera position based on tracking object
		if (cameraObject != null) {
			cameraObject.getAbsoluteAnimatedPosition(cameraPoint, time);
			if (avatar == cameraObject && cameraDistance < 10) {
				WWVector point = new WWVector(0, 0, 0.75f);
				cameraObject.rotate(point, cameraObjectRotation, time);
				cameraPoint.add(point); // so avatar is in lower part of screen
			}
			clientModel.setCameraPoint(cameraPoint);
		}

		// Adjust camera position based on camera slide
		float slideCameraPointX = cameraPoint.x + (float) Math.cos(TORADIAN * cameraPan) * cameraSlideX - (float) Math.sin(TORADIAN * cameraPan) * (float) Math.sin(TORADIAN * cameraTilt) * cameraSlideY
				- (float) Math.sin(TORADIAN * cameraPan) * (float) Math.cos(TORADIAN * cameraTilt) * cameraSlideZ;
		float slideCameraPointY = cameraPoint.y - (float) Math.sin(TORADIAN * cameraPan) * cameraSlideX - (float) Math.cos(TORADIAN * cameraPan) * (float) Math.sin(TORADIAN * cameraTilt) * cameraSlideY
				- (float) Math.cos(TORADIAN * cameraPan) * (float) Math.cos(TORADIAN * cameraTilt) * cameraSlideZ;
		float slideCameraPointZ = cameraPoint.z + (float) Math.cos(TORADIAN * cameraTilt) * cameraSlideY - (float) Math.sin(TORADIAN * cameraTilt) * cameraSlideZ;

		// limit camera according to unpenetratable objects
		float limitedCameraDistance = cameraDistance;
		if (clientModel.limitCameraDistance) {
			if (lastLimitedCameraDistance < cameraDistance) {
				limitedCameraDistance = Math.min(cameraDistance, lastLimitedCameraDistance / 0.5f);
			} else {
				limitedCameraDistance = cameraDistance;
			}
			WWObject[] objects = clientModel.world.getObjects();
			int lastObjectIndex = clientModel.world.lastObjectIndex;
			WWVector cameraLocation = new WWVector();
			WWVector position = new WWVector();
			WWVector rotation = new WWVector();
			WWVector tempPoint = new WWVector();
			WWVector penetrationVector = new WWVector();
			boolean penetrated = true;
			while (penetrated) {
				penetrated = false;
				for (int i = 0; i <= lastObjectIndex; i++) {
					WWObject object = objects[i];
					if (object != null && !object.deleted && !object.penetratable && object.solid && !object.phantom) {
						if (cameraObject == avatar) {
							cameraLocation.x = slideCameraPointX + limitedCameraDistance * (float) Math.sin(TORADIAN * (cameraPan + cameraObjectRotation.z)) * (float) Math.cos(TORADIAN * (cameraTilt + cameraObjectRotation.x));
							if (cameraDistance < 10) {
								cameraLocation.y = slideCameraPointY + limitedCameraDistance * (float) Math.cos(TORADIAN * (cameraPan + cameraObjectRotation.z)) * (float) Math.cos(TORADIAN * (cameraTilt + cameraObjectRotation.x));
								cameraLocation.z = slideCameraPointZ + (float) Math.sin(TORADIAN * cameraTilt) * limitedCameraDistance;
							} else {
								cameraLocation.y = slideCameraPointY + limitedCameraDistance * (float) Math.cos(TORADIAN * cameraPan) * (float) Math.cos(TORADIAN * cameraTilt);
								cameraLocation.z = slideCameraPointZ + (float) Math.sin(TORADIAN * cameraTilt) * limitedCameraDistance;
							}
						} else {
							cameraLocation.x = slideCameraPointX + limitedCameraDistance * (float) Math.sin(TORADIAN * cameraPan) * (float) Math.cos(TORADIAN * cameraTilt);
							cameraLocation.y = slideCameraPointY + limitedCameraDistance * (float) Math.cos(TORADIAN * cameraPan) * (float) Math.cos(TORADIAN * cameraTilt);
							cameraLocation.z = slideCameraPointZ + (float) Math.sin(TORADIAN * cameraTilt) * limitedCameraDistance;
						}
						// First, see if the objects are "close". If they are, it is worth
						// determining if they actually overlap
						object.getPosition(position, time);
						float extent = object.extent;
						if (object.parentId != 0 || (Math.abs(position.x - cameraLocation.x) < extent && Math.abs(position.y - cameraLocation.y) < extent && Math.abs(position.z - cameraLocation.z) < extent)) {
							object.getRotation(rotation, time);
							object.getPenetration(cameraLocation, position, rotation, time, tempPoint, penetrationVector);
							if (penetrationVector != null && penetrationVector.length() > 0 && limitedCameraDistance > 0.25) {
								if (Math.abs(penetrationVector.z) > FastMath.max(Math.abs(penetrationVector.x), Math.abs(penetrationVector.y)) && cameraTilt < 45.0) {
									cameraTilt += 15.0;
								} else {
									limitedCameraDistance = limitedCameraDistance * 0.5f;
								}
								penetrated = true;
								// System.out.println("penetrated: " + object.hashCode() + " " + cameraLocation + " " + position + " " + rotation + " " + penetrationVector);
							}
						}
					}
				}
			}
		}
		limitedCameraDistance = FastMath.max(0.5f, limitedCameraDistance);
		lastLimitedCameraDistance = limitedCameraDistance;

		// Dampen camera, to give the user a better understanding of the position change
		if (cameraObject != null && clientModel.cameraDampRate > 0) {
			if (clientModel.world.dampenCamera()) {
				dampXCamera = (slideCameraPointX + clientModel.cameraDampRate * dampXCamera) / (clientModel.cameraDampRate + 1);
				dampYCamera = (slideCameraPointY + clientModel.cameraDampRate * dampYCamera) / (clientModel.cameraDampRate + 1);
				dampZCamera = (slideCameraPointZ + clientModel.cameraDampRate * dampZCamera) / (clientModel.cameraDampRate + 1);
			} else {
				// dampXCamera = (2 * slideCameraPointX + dampXCamera) / 3.0f;
				// dampYCamera = (2 * slideCameraPointY + dampYCamera) / 3.0f;
				// dampZCamera = (2 * slideCameraPointZ + dampZCamera) / 3.0f;

				WWVector cameraVelocity = cameraObject.getVelocity();

				dampCameraVelocityX = (cameraVelocity.x + clientModel.cameraDampRate * dampCameraVelocityX) / (clientModel.cameraDampRate + 1);
				dampCameraVelocityY = (cameraVelocity.y + clientModel.cameraDampRate * dampCameraVelocityY) / (clientModel.cameraDampRate + 1);
				dampCameraVelocityZ = (cameraVelocity.z + clientModel.cameraDampRate * dampCameraVelocityZ) / (clientModel.cameraDampRate + 1);

				dampXCamera = slideCameraPointX - 0.01f * dampCameraVelocityX;
				dampYCamera = slideCameraPointY - 0.01f * dampCameraVelocityY;
				dampZCamera = slideCameraPointZ - 0.01f * dampCameraVelocityZ;
			}

		} else {
			dampXCamera = slideCameraPointX;
			dampYCamera = slideCameraPointY;
			dampZCamera = slideCameraPointZ; // to avoid shaky's
		}

		if (cameraObject != null && avatar == cameraObject) {
			dampCameraPan = ((cameraPan + cameraObjectRotation.z) + clientModel.cameraDampRate * dampCameraPan) / (clientModel.cameraDampRate + 1);
			if (cameraDistance < 10) {
				dampCameraTilt = ((cameraTilt - cameraObjectRotation.x) + 4 * clientModel.cameraDampRate * dampCameraTilt) / (4 * clientModel.cameraDampRate + 1);
				dampCameraLean = ((cameraLean - cameraObjectRotation.y) + 4 * clientModel.cameraDampRate * dampCameraLean) / (4 * clientModel.cameraDampRate + 1);
			} else {
				dampCameraTilt = (cameraTilt + clientModel.cameraDampRate * dampCameraTilt) / (clientModel.cameraDampRate + 1);
				dampCameraLean = (cameraLean + clientModel.cameraDampRate * dampCameraLean) / (clientModel.cameraDampRate + 1);
			}
		} else {
			dampCameraPan = (cameraPan + clientModel.cameraDampRate * dampCameraPan) / (clientModel.cameraDampRate + 1);
			dampCameraTilt = (cameraTilt + clientModel.cameraDampRate * dampCameraTilt) / (clientModel.cameraDampRate + 1);
			dampCameraLean = (cameraLean + clientModel.cameraDampRate * dampCameraLean) / (clientModel.cameraDampRate + 1);
		}
		dampCameraDistance = (limitedCameraDistance + clientModel.cameraDampRate * dampCameraDistance) / (clientModel.cameraDampRate + 1);
	}

	public void initializeCameraPosition() {
		dampXCamera = clientModel.getCameraSlideX();
		dampYCamera = clientModel.getCameraSlideY();
		dampZCamera = clientModel.getCameraSlideZ();
		dampCameraPan = clientModel.getCameraPan();
		dampCameraTilt = clientModel.getCameraTilt();
		dampCameraLean = clientModel.getCameraLean();
		dampCameraDistance = clientModel.getCameraDistance();
		lastCameraAdjustTime = 0;
		lastLimitedCameraDistance = clientModel.getCameraDistance();
	}

}
