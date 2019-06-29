package com.gallantrealm.myworld.model;

import java.io.Serializable;

/**
 * A collection of all properties related to a texture.  For ease in setting and manipuating textures.
 */
public class WWTexture implements Serializable, Cloneable {
	private static final long serialVersionUID = 1L;

	public String name;
	public float scaleX = 1.0f;
	public float scaleY = 1.0f;
	public float rotation;
	public float offsetX;
	public float offsetY;
	public float velocityX;
	public float velocityY;
	public float aMomentum;
	public long refreshInterval;
	public boolean pixelate;
	
	public WWTexture() {
	}
	
	public WWTexture(String url) {
		this.name = url;
	}
	
	public WWTexture(String url, float scaleX, float scaleY) {
		this.name = url;
		this.scaleX = scaleX;
		this.scaleY = scaleY;
	}
	
	public WWTexture(String url, float scaleX, float scaleY, float rotation) {
		this.name = url;
		this.scaleX = scaleX;
		this.scaleY = scaleY;
		this.rotation = rotation;
	}
	
	public WWTexture(String url, float scaleX, float scaleY, float rotation, float offsetX, float offsetY) {
		this.name = url;
		this.scaleX = scaleX;
		this.scaleY = scaleY;
		this.rotation = rotation;
		this.offsetX = offsetX;
		this.offsetY = offsetY;
	}
	
	public WWTexture(String url, float scaleX, float scaleY, float rotation, float offsetX, float offsetY, boolean pixelate) {
		this.name = url;
		this.scaleX = scaleX;
		this.scaleY = scaleY;
		this.rotation = rotation;
		this.offsetX = offsetX;
		this.offsetY = offsetY;
		this.pixelate = pixelate;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	public float getScaleX() {
		return scaleX;
	}
	public void setScaleX(float scaleX) {
		this.scaleX = scaleX;
	}
	
	public float getScaleY() {
		return scaleY;
	}
	public void setScaleY(float scaleY) {
		this.scaleY = scaleY;
	}
	
	public float getRotation() {
		return rotation;
	}
	public void setRotation(float rotation) {
		this.rotation = rotation;
	}
	
	public float getOffsetX() {
		return offsetX;
	}
	public void setOffsetX(float offsetX) {
		this.offsetX = offsetX;
	}
	
	public float getOffsetY() {
		return offsetY;
	}
	public void setOffsetY(float offsetY) {
		this.offsetY = offsetY;
	}
	
	public float getVelocityX() {
		return velocityX;
	}
	public void setVelocityX(float velocityX) {
		this.velocityX = velocityX;
	}
	
	public float getVelocityY() {
		return velocityY;
	}
	public void setVelocityY(float velocityY) {
		this.velocityY = velocityY;
	}
	
	public float getaMomentum() {
		return aMomentum;
	}
	public void setaMomentum(float aMomentum) {
		this.aMomentum = aMomentum;
	}
	
	public long getRefreshInterval() {
		return refreshInterval;
	}
	public void setRefreshInterval(long refreshInterval) {
		this.refreshInterval = refreshInterval;
	}
	
	public boolean isPixelate() {
		return pixelate;
	}
	public void setPixelate(boolean pixelate) {
		this.pixelate = pixelate;
	}
	
	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}
}
