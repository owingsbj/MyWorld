package com.gallantrealm.myworld.model;

/**
 * This class is subclassed to create behaviors that can be added to world objects. The methods ending in "Event" on
 * this class are invoked for certain events that influence the object. The default implementation of these methods does
 * nothing, but the method can be overridden to provide logic to take action upon the event, such as changing properties
 * of the object or its surroundings.
 * <p>
 * Behavior classes are limited in their capabilities. Firstly, they are only a single class, although it is possible to
 * share logic by subclassing behaviors. Secondly, it is protected from impacting the state of any entity other than the
 * one in which the behavior is associated, although it is possible for behaviors to "message" to each other.
 */
public abstract class WWBehavior extends WWEntity {

	static final long serialVersionUID = 1L;

	WWObject owner;
	int timer;

	/**
	 * Returns the object that this behavior is associated with.
	 */
	public WWObject getOwner() {
		return owner;
	}

	/**
	 * Returns true if the behavior is running client-side. This allows a behavior to prompt the user (but avoid errors
	 * prompting on the server).
	 * 
	 * @return
	 */
	public boolean isOnClient() {
		return owner.world.isOnClient();
	}

	/**
	 * This event is invoked when the object is touched.
	 * 
	 * @deprecated use touchEvent(object, toucher, side, x, y)
	 * 
	 * @param toucher
	 *            the user that touched this object.
	 */
	@Deprecated
	public boolean touchEvent(WWObject object, WWEntity toucher) {
		return false;
	}

	/**
	 * This event is invoked when an object is first touched
	 * 
	 * @param toucher
	 *            the user that touched this object.
	 */
	public boolean pressEvent(WWObject object, WWEntity toucher, int side, float x, float y) {
		return false;
	}

	/**
	 * This event is invoked repeatedly while the object is swiped (finger or pointer is dragged across object).
	 * 
	 * @param toucher
	 *            the user that touched this object.
	 */
	public boolean dragEvent(WWObject object, WWEntity toucher, int side, float x, float y) {
		return false;
	}

	/**
	 * This event is invoked when an object is no longer touched
	 * 
	 * @param toucher
	 *            the user that touched this object.
	 */
	public boolean releaseEvent(WWObject object, WWEntity toucher, int side, float x, float y) {
		return false;
	}

	/**
	 * This event is invoked when the object is touched (pressed and release without much movement)
	 * 
	 * @param toucher
	 *            the user that touched this object.
	 */
	public boolean touchEvent(WWObject object, WWEntity toucher, int side, float x, float y) {
		return touchEvent(object, toucher);
	}

	/**
	 * This event is invoked when an object comes near to this object or actually collides with this object.
	 * 
	 * @param nearObject
	 *            the object that is near or colliding
	 * @param proximity
	 *            a vector with the distance of the object. If the vector is negative, the object is colliding
	 */
	public boolean collideEvent(WWObject object, WWObject nearObject, WWVector proximity) {
		return false;
	}

	public boolean slideEvent(WWObject object, WWObject nearObject, WWVector proximity) {
		return false;
	}

	public boolean stopSlideEvent(WWObject object, WWObject nearObject, WWVector proximity) {
		return false;
	}

	public int getTimer() {
		return timer;
	}

	public void setTimer(int millis) {
		timer = millis;
	}

	/**
	 * This event is invoked when the timer times down to zero. The timer can be reset within this event for repeated
	 * timing, for tasks like sequencing animations or adjusting physics
	 * @deprecated
	 */
	public boolean timerEvent() {
		return false;
	}
	
	/**
	 * This event is invoked when the timer times down to zero. The timer can be reset within this event for repeated
	 * timing, for tasks like sequencing animations or adjusting physics
	 */
	public boolean timerEvent(WWObject object) {
		return timerEvent();
	}

	// TODO add other needed events:
	// - changed event

	// TODO add events to mimick those of a typical control, then 3d user interfaces can be designed
	// - focus event
	// - keyboard events
	// - mouse events
}
