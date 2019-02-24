package com.gallantrealm.myworld.model;

/**
 * Represents a collision between two objects, where one or both of the objects are physical. These are transient,
 * created by the physics thread as needed to maintain information on the intersection.
 * 
 */
public final class ObjectCollision {
	public WWObject firstObject;
	public WWObject secondObject;
	public WWVector overlapVector;
	public boolean sliding; // to indicate that this collision is sliding (earlier collision was first time)
	public boolean stillSliding; // used when collision is old and the newer version is sliding
	int firstSlidingStreamId;
	int secondSlidingStreamId;

	public ObjectCollision(WWObject firstObject, WWObject secondObject, WWVector overlapVector) {
		this.firstObject = firstObject;
		this.secondObject = secondObject;
		this.overlapVector = overlapVector.clone();
	}

	@Override
	public boolean equals(Object o) {
		return (firstObject == ((ObjectCollision) o).firstObject && secondObject == ((ObjectCollision) o).secondObject);
	}

}
