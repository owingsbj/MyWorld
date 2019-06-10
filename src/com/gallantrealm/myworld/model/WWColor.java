package com.gallantrealm.myworld.model;

import java.io.Serializable;

public class WWColor implements Serializable {
	private static final long serialVersionUID = 1L;

	public int color;

	public WWColor() {
	}

	public WWColor(int rgb) {
		color = rgb;
	}

	public WWColor(float red, float green, float blue) {
		color = ((int) (red * 255) << 16) + ((int) (green * 255) << 8) + (int) (blue * 255);
	}

	public final int getRGB() {
		return color;
	}

	public final void set(float red, float green, float blue) {
		color = ((int) (red * 255) << 16) + ((int) (green * 255) << 8) + (int) (blue * 255);
	}

	public final float getAlpha() {
		float alpha = ((color >> 24) & 0xFF) / 255.0f;
		return alpha;
	}

	public final float getRed() {
		float red = ((color >> 16) & 0xFF) / 255.0f;
		return red;
	}

	public final float getGreen() {
		float green = ((color >> 8) & 0xFF) / 255.0f;
		return green;
	}

	public final float getBlue() {
		float blue = (color & 0xFF) / 255.0f;
		return blue;
	}

	public final WWColor darker() {
		return new WWColor(getRed() / 2.0f, getGreen() / 2.0f, getBlue() / 2.0f);
	}

	/**
	 * Multiply a color into this color.
	 * 
	 * @param color
	 */
	public final void mix(WWColor color) {
		float red = getRed() * color.getRed();
		float green = getGreen() * color.getGreen();
		float blue = getBlue() * color.getBlue();
		set(red, green, blue);
	}

}
