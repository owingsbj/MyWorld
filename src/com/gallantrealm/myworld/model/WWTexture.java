package com.gallantrealm.myworld.model;

import java.io.Serializable;

/**
 * A collection of all properties related to a texture.  For ease in setting and manipuating textures.
 */
public class WWTexture implements Serializable {
	private static final long serialVersionUID = 1L;

	public String url;
	public float scaleX = 1.0f;
	public float scaleY = 1.0f;
	public float rotation;
	public float offsetX;
	public float offsetY;
	public float velocityX;
	public float velocityY;
	public float aMomentum;
	public long refreshInterval;
	
	public WWTexture() {
	}
	
	public WWTexture(String url) {
		this.url = url;
	}
	
	public WWTexture(String url, float scaleX, float scaleY) {
		this.url = url;
		this.scaleX = scaleX;
		this.scaleY = scaleY;
	}
	
	public WWTexture(String url, float scaleX, float scaleY, float offsetX, float offsetY) {
		this.url = url;
		this.scaleX = scaleX;
		this.scaleY = scaleY;
		this.offsetX = offsetX;
		this.offsetY = offsetY;
	}
	
	public WWTexture(String url, float scaleX, float scaleY, float offsetX, float offsetY, float rotation) {
		this.url = url;
		this.scaleX = scaleX;
		this.scaleY = scaleY;
		this.offsetX = offsetX;
		this.offsetY = offsetY;
		this.rotation = rotation;
	}
	
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
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
}
