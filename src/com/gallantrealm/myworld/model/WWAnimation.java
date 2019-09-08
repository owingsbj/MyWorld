package com.gallantrealm.myworld.model;

/**
 * An animation is a special type of behavior used to cause repeated motion of an object or group of objects. The motion
 * is only known on the client, so it doesn't partake in physics.
 */
public abstract class WWAnimation extends WWBehavior {

	static final long serialVersionUID = 1L;

	transient WWObject owner;
	transient int timer;

	/**
	 * Returns the object that this animation is associated with.
	 */
	@Override
	public WWObject getOwner() {
		return owner;
	}

	/**
	 * Returns true if the animation is running client-side. Animations typically only occur on clients.
	 * 
	 * @return
	 */
	@Override
	public boolean isOnClient() {
		return owner.world.isOnClient();
	}

	/**
	 * Allows adjusting of the position according to animation. The object passed in is either the owner object or a
	 * child of the owner. The position vector is modified to reflect adjustment by the animation.
	 */
	public abstract void getAnimatedPosition(WWObject object, WWVector position, long time);

	/**
	 * Allows adjusting of the rotation according to animation. The object passed in is either the owner object or a
	 * child of the owner. The rotation vector is modified to reflect adjustment by the animation.
	 */
	public abstract void getAnimatedRotation(WWObject object, WWVector rotation, long time);
	
	public void animatePositionMatrix(WWObject object, float[] matrix, long time) {
	}

}
