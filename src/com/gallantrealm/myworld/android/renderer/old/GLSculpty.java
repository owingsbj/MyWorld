package com.gallantrealm.myworld.android.renderer.old;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import com.gallantrealm.myworld.android.renderer.AndroidRenderer;
import com.gallantrealm.myworld.model.WWColor;
import com.gallantrealm.myworld.model.WWObject;
import com.gallantrealm.myworld.model.WWSculpty;

import de.matthiasmann.twl.utils.PNGDecoder;
import de.matthiasmann.twl.utils.PNGDecoder.Format;

/**
 * Creates a primitive with a complex surface shape composed from a two dimensional array of rectangles. Three types of
 * meshes are possible: flat, cylindrical, and spherical. A flat mesh can be used for land and other large flat
 * surfaces. Cylindrical is useful for "carved" objects such as statues and limbs of an avatar. Spherical meshes can
 * simulate round objects like the head of an avatar or a model of a planet.
 */
public final class GLSculpty extends GLObject {

	public GLSculpty(AndroidRenderer renderer, WWSculpty sculpty, long worldTime) {
		super(renderer, sculpty, worldTime);
		try {
			InputStream is = renderer.context.getAssets().open(sculpty.sculptyTexture + ".png"); // asset
			PNGDecoder decoder = new PNGDecoder(is);
			int width = decoder.getWidth();
			int height = decoder.getHeight();
			ByteBuffer buf = ByteBuffer.allocateDirect(4 * decoder.getWidth() * decoder.getHeight());
			decoder.decode(buf, decoder.getWidth() * 4, Format.RGBA);
			buf.flip();

			is.close();
			if (decoder.getWidth() > 0) {
				WWColor color = new WWColor();

				GLSurface surface;
				if (sculpty.closed) {
					surface = new GLSurface(width + 1, height, sculpty.smooth);
					for (int i = 0; i <= width; i++) {
						for (int j = 0; j < height; j++) {
							int x = i;
							if (i == width) {
								x = 0;
							}
							int y = j;
							if (y == height) {
								y = 0;
							}
							int rgb = buf.getInt(4 * (x + width * y));
							color.color = rgb;
							float red = color.getRed() - 0.5f;
							float green = color.getGreen() - 0.5f;
							float blue = color.getBlue() - 0.5f;
							float alpha = color.getAlpha() - 0.5f;
							surface.setVertex(i, j, -alpha, green, red);
						}
					}
					adjustGeometry(surface, sculpty.sizeX, sculpty.sizeY, sculpty.sizeZ, sculpty.taperX, sculpty.taperY, sculpty.shearX, sculpty.shearY, sculpty.twist);
					surface.generateNormals(true, true);
					adjustTextureCoords(surface, WWObject.SIDE_SIDE1);
				} else {
					surface = new GLSurface(width, height, sculpty.smooth);
					for (int i = 0; i < width; i++) {
						for (int j = 0; j < height; j++) {
							int rgb = buf.getInt(4 * (i + width * j));
							color.color = rgb;
							float red = color.getRed() - 0.5f;
							float green = color.getGreen() - 0.5f;
							float blue = color.getBlue() - 0.5f;
							float alpha = color.getAlpha() - 0.5f;
							surface.setVertex(i, j, -alpha, green, red);
						}
					}
					adjustGeometry(surface, sculpty.sizeX, sculpty.sizeY, sculpty.sizeZ, sculpty.taperX, sculpty.taperY, sculpty.shearX, sculpty.shearY, sculpty.twist);
					surface.generateNormals(false, false);
					adjustTextureCoords(surface, WWObject.SIDE_SIDE1);
				}
				setSide(WWObject.SIDE_SIDE1, surface);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Read the sculpty image asset into a bitmap, ready for use
	 * 
	 * @param selectedImage
	 * @return
	 */
	public final Bitmap readSculptyTexture(Uri selectedImage) {
		Bitmap bm = null;
		BitmapFactory.Options options = new BitmapFactory.Options();
		AssetFileDescriptor fileDescriptor = null;
		try {
			fileDescriptor = renderer.context.getContentResolver().openAssetFileDescriptor(selectedImage, "r");
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
		return bm;
	}

}
