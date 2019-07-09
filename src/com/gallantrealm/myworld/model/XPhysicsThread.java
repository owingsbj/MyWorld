package com.gallantrealm.myworld.model;

import java.util.ArrayList;
import com.gallantrealm.myworld.FastMath;

/**
 * This thread performs updates to the world according to physical properties. This involves detecting collision and
 * forces between objects and adjusting the position, orientation, velocity and angular momentum to match.
 * <p>
 * Note that this thread does not actually handle moving or rotating objects due to their own velocity and angular
 * momentum. This is handled within the WWObject itself.
 */
public class XPhysicsThread extends Thread {

	private final int iterationTime;

	private final WWWorld world;

	ArrayList<ObjectCollision> previousPreviousCollidedObjects = new ArrayList<ObjectCollision>();
	ArrayList<ObjectCollision> previousCollidedObjects = new ArrayList<ObjectCollision>();
	ArrayList<ObjectCollision> newCollidedObjects = new ArrayList<ObjectCollision>();

	public XPhysicsThread(WWWorld world, int iterationTime) {
		setName("PhysicsThread");
		this.world = world;
		this.iterationTime = iterationTime;
		setPriority(Thread.MAX_PRIORITY - 1);
		setDaemon(true);
	}

	@Override
	public void run() {
		if (iterationTime == 0) {
			return;
		}
		System.out.println("Starting physics loop");
		long timeSinceLastSoundUpdate = 0;
		try {
			Thread.sleep(100); // let things settle before starting physics
			long lastStartTime = System.currentTimeMillis();
			while (true) {
				try {
					long startTime = System.currentTimeMillis();

					synchronized (world) {
						performIteration(Math.min(startTime - lastStartTime, (2 * iterationTime)));
					}

					// Wait if necessary, so loop is 100 milliseconds minimum
					long loopTime = System.currentTimeMillis() - startTime;
					if (loopTime < iterationTime) {
						Thread.sleep(Math.max(0, iterationTime - loopTime));
					} else {
						//System.out.println("peaked");
					}
					lastStartTime = startTime;
					timeSinceLastSoundUpdate += loopTime;
					if (timeSinceLastSoundUpdate > 1000) {
						updateSounds();
						timeSinceLastSoundUpdate = 0;
					}
				} catch (InterruptedException e) {
					System.out.println("Stopping physics loop");
					return;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		} catch (InterruptedException e) {
			System.out.println("Stopping physics loop");
		}
	}

	public void performIteration(long timeIncrement) {
		world.updateWorldTime(timeIncrement);

		long worldTime = world.getWorldTime();
		float deltaTime = timeIncrement / 1000.0f;

		previousPreviousCollidedObjects = previousCollidedObjects;
		previousCollidedObjects = newCollidedObjects;
		newCollidedObjects = new ArrayList<ObjectCollision>();

		WWVector position = new WWVector();
		WWVector rotation = new WWVector();
		WWVector velocity = new WWVector();
		WWVector aMomentum = new WWVector();
		WWVector position2 = new WWVector();
		WWVector rotation2 = new WWVector();
		WWVector tempPoint = new WWVector();
		WWVector tempPoint2 = new WWVector();
		WWVector overlapPoint = new WWVector();
		WWVector overlapVector = new WWVector();

		WWObject[] objects = world.getObjects();
		int lastObjectIndex = world.lastObjectIndex;
		for (int i = 0; i <= lastObjectIndex; i++) {
			WWObject object = objects[i];

			// For physical objects, determine forces upon the object.  This done by summing 
			// up all forces on the object, ignoring those where freedom is restricted.
			// Only examine physical objects that are also solid.  Non-solid (liquid, gas)
			// objects are not influenced by other objects (but do influence other objects
			// by being tested in the inner loop below).
			if (object != null && object.physical && object.solid && !object.deleted) {

				synchronized (object) { // to keep it from being modified

					// Get current orientation and momentum values.
					object.getPosition(position, worldTime);
					object.getRotation(rotation, worldTime);
					object.getVelocity(velocity);
					object.getAMomentum(aMomentum);
					WWVector thrust = object.getThrust();
					float maxThrustInfluence = object.getThrustVelocity().length();
					WWVector torque = object.getTorque();
					float maxTorqueInfluence = object.getTorqueVelocity().length();

					WWVector originalPosition = position.clone();
					WWVector originalRotation = rotation.clone();
					WWVector originalVelocity = velocity.clone();
					WWVector originalAMomentum = aMomentum.clone();

					WWVector totalForce = new WWVector();
					WWVector totalTorque = new WWVector();

					// sum in gravitational force only if object has mass							
					if (object.getDensity() > 0.0) {
						totalForce.z -= world.getGravity();
					}

					// - Next, apply interactions of other objects that overlap the physical object
					float objectExtent = object.extent;
					for (int j = 1; j <= lastObjectIndex; j++) {
						WWObject object2 = objects[j];
						if (object2 != null && object2 != object && !object2.deleted && !object2.phantom && !object2.isChildOf(object)) {

							// First, see if the objects are "close".  If they are, it is worth
							// determining if they actually overlap
							float maxExtent = FastMath.max(objectExtent, object2.extent);
							object2.getPosition(position2, worldTime);
							if (/* object2.parentId != 0 || */(Math.abs(position2.x - position.x) < maxExtent && Math.abs(position2.y - position.y) < maxExtent && Math.abs(position2.z - position.z) < maxExtent)) {

								// Determine if the objects overlap, and the vector of overlap.  This
								// vector points in the direction of the deepest overlap, and the length of the
								// vector indicates the amount of overlap
								object2.getRotation(rotation2, worldTime);
								object.getOverlap(object2, position, rotation, object.rotationPoint, position2, rotation2, worldTime, tempPoint, tempPoint2, overlapPoint, overlapVector);

								if (!overlapVector.isZero()) {

									ObjectCollision collision = new ObjectCollision(object, object2, overlapVector);
									newCollidedObjects.add(collision);

									// If the object2 is solid, apply solid-to-solid physics
									if (object2.solid) {

										// Adjust the position of the objects so that they are not overlapping
										position.x -= overlapVector.x;
										position.y -= overlapVector.y;
										position.z -= overlapVector.z;

										// If the object is moving toward object2, stop or repell it (according to elasticity)
										WWVector unitOverlapVector;
										if (overlapVector.length() > 0) {
											unitOverlapVector = new WWVector(overlapVector);
										} else { // use force
											unitOverlapVector = overlapVector.clone(); //new WWVector(-force.x, -force.y, -force.z);
										}
										unitOverlapVector.normalize();
										if (object.getElasticity() > 0.0) { // bounce off the object  
											WWVector velocityVector = new WWVector(velocity.x, velocity.y, velocity.z);
											WWVector mirrorVector = unitOverlapVector.clone();
											mirrorVector.scale(-1.0f);
											WWVector mirrorVelocityVector = velocityVector.getReflection(mirrorVector);
											velocity.x = mirrorVelocityVector.x * object.getElasticity();
											velocity.y = mirrorVelocityVector.y * object.getElasticity();
											velocity.z = mirrorVelocityVector.z * object.getElasticity();
										} else { // slide on the object
											WWVector antiForceVector = unitOverlapVector.clone();
											antiForceVector.scale(-totalForce.length());
											totalForce.add(antiForceVector);
											// TODO add velocity attuning (and do not stop in all dimensions)
											//velocity.scale(Math.min(1.0f, 1.0f + unitOverlapVector.x), Math.min(1.0f, 1.0f + unitOverlapVector.y), Math.min(1.0f, 1.0f + unitOverlapVector.z));
											velocity.scale(1.0f, 1.0f, 1.0f + unitOverlapVector.z);
										}

										// Adjust angular momentum as well
										// TODO implement angular momentum adjustment

									} // solid

									// If the object2 is non-solid, apply solid-to-liquid/gas physics
									else {

										// boyancy
										if (object.isFreedomMoveZ() && object2.getDensity() > 0.0) {
											// Note: pressure is determined by how deep into the object.  This is an
											// estimate here, based on the extent.  This will be correct only if the object is level (flat)
											float pressure = FastMath.max(position2.z + object2.sizeZ / 2.0f - position.z, 0.0f);
											float boyancy = object2.getDensity() * pressure - object.getDensity();
											if (boyancy > 0) {
												velocity.z += (boyancy * deltaTime) * 30.0;
											}
										}

									} // nonsolid

									// Depending on friction forces, slow the object movement
									if (object2.isSolid() && object.getElasticity() > 0 || object2.getElasticity() > 0) {
										// no friction for solids touching with elasticity
									} else {
										float friction = FastMath.min(object.friction, object2.friction) * 10.0f;
										if (friction > 0) {

											// Friction is a force acting opposite of relative velocity/amomentum of the two items colliding.
											WWVector frictionVForce = object2.getVelocity();
											frictionVForce.subtract(velocity);
											frictionVForce.scale(friction);
											totalForce.add(frictionVForce);
											WWVector frictionAForce = object2.getAMomentum();
											frictionAForce.subtract(aMomentum);
											frictionAForce.scale(friction);
											totalTorque.add(frictionAForce);

										}
									} // friction

								} // if overlapping
							} // if near each other

						} // if object != object2
					} // for object2

//					// Cap forces to avoid really bad behaviors
//					if (totalForce.length() > 10) {
//						totalForce.scale(10 / totalForce.length());
//					}

					// Apply forces to object's velocity
					velocity.x += totalForce.x * deltaTime;
					velocity.y += totalForce.y * deltaTime;
					velocity.z += totalForce.z * deltaTime;

					// Limit velocity according to freedom
					object.antiRotate(velocity, rotation, worldTime);
					if (!object.freedomMoveX) {
						velocity.x = 0.0f;
					}
					if (!object.freedomMoveY) {
						velocity.y = 0.0f;
					}
					if (!object.freedomMoveZ) {
						velocity.z = 0.0f;
					}
					object.rotate(velocity, rotation, worldTime);

					// Apply torque to object.
//					object.antiRotate(torque, rotation); // so torque is relative to current object orientation
//					object.antiRotate(torqueVelocity, rotation); // so torque is relative to current object orientation
					aMomentum.x += totalTorque.x * deltaTime; // * 10f;
					aMomentum.y += totalTorque.y * deltaTime; // * 10f;
					aMomentum.z += totalTorque.z * deltaTime; // * 10f;

//				if (torque.length() != 0.0) {

					// Limit angular momentum according to freedom
					if (!object.freedomRotateX) {
						aMomentum.x = 0;
					}
					if (!object.freedomRotateY) {
						aMomentum.y = 0;
					}
					if (!object.freedomRotateZ) {
						aMomentum.z = 0;
					}

					// apply the object's own thrust and torque last, so it always influences
					// but only if less than the max influence (this is a game after all)
					if (velocity.length() < maxThrustInfluence) {
						object.rotate(thrust, rotation, worldTime);
						velocity.x += thrust.x * deltaTime;
						velocity.y += thrust.y * deltaTime;
						velocity.z += thrust.z * deltaTime;
					}
					if (aMomentum.length() < maxTorqueInfluence) {
						aMomentum.x += torque.x * deltaTime * 100.0f;
						aMomentum.y += torque.y * deltaTime * 100.0f;
						aMomentum.z += torque.z * deltaTime * 100.0f;
					}

//				}

					// Update the position, rotation, velocity and angular momentum values on the object if any have changed due to
					// physical interaction with another object
					if (!position.equals(originalPosition) || !rotation.equals(originalRotation) || !velocity.equals(originalVelocity) || !aMomentum.equals(originalAMomentum)) {

//						// Cap the movements, to cure possible physics ills
//						if (position.x - originalPosition.x > 1f) {
//							position.x = originalPosition.x + 1f;
//						} else if (position.x - originalPosition.x < -0.5f) {
//							position.x = originalPosition.x - 0.5f;
//						}
//						if (position.y - originalPosition.y > 0.5f) {
//							position.y = originalPosition.y + 0.5f;
//						} else if (position.y - originalPosition.y < -0.5f) {
//							position.y = originalPosition.y - 0.5f;
//						}
//						if (position.z - originalPosition.z > 0.5f) {
//							position.z = originalPosition.z + 0.5f;
//						} else if (position.z - originalPosition.z < -0.5f) {
//							position.z = originalPosition.z - 0.5f;
//						}

						object.setOrientation(position, rotation, velocity, aMomentum, worldTime);
					}

				} // synchronize object

			} // if physical

			// update timers and invoke timer events on object behaviors
			if (object != null && !object.deleted) {
				int behaviorCount = object.getBehaviorCount();
				for (int b = 0; b < behaviorCount; b++) {
					WWBehavior behavior = object.getBehavior(b);
					if (behavior.timer > 0) {
						behavior.timer = (int) Math.max(0, behavior.timer - timeIncrement);
						if (behavior.timer == 0) {
							if (world.behaviorThread != null) {
								world.behaviorThread.queue("timer", object, null, null);
							} else {
								object.invokeBehavior("timer", null, null);
							}
						}
					}
				}
			}

		} // for object

		// fire collide and slide events
		for (int i = 0; i < newCollidedObjects.size(); i++) {
			ObjectCollision newCollision = newCollidedObjects.get(i);
			for (int j = 0; j < previousCollidedObjects.size(); j++) {
				ObjectCollision previousCollision = previousCollidedObjects.get(j);
				if (newCollision.equals(previousCollision)) {
					newCollision.firstSlidingStreamId = previousCollision.firstSlidingStreamId;
					newCollision.secondSlidingStreamId = previousCollision.secondSlidingStreamId;
					world.slideObject(newCollision);
					newCollision.sliding = true;
					previousCollision.stillSliding = true;
				}
			}
			if (!newCollision.sliding) { // check previous previous
				for (int j = 0; j < previousPreviousCollidedObjects.size(); j++) {
					ObjectCollision previousPreviousCollision = previousPreviousCollidedObjects.get(j);
					if (newCollision.equals(previousPreviousCollision)) {
						newCollision.firstSlidingStreamId = previousPreviousCollision.firstSlidingStreamId;
						newCollision.secondSlidingStreamId = previousPreviousCollision.secondSlidingStreamId;
						world.slideObject(newCollision);
						newCollision.sliding = true;
						previousPreviousCollision.stillSliding = true;
					}
				}
			}
			if (!newCollision.sliding) {
				world.collideObject(newCollision);
			}
		}
		for (int j = 0; j < previousPreviousCollidedObjects.size(); j++) {
			ObjectCollision previousPreviousCollision = previousPreviousCollidedObjects.get(j);
			if (previousPreviousCollision.sliding && !previousPreviousCollision.stillSliding) {
				world.stopSlidingObject(previousPreviousCollision);
			}
		}

	}

	void updateSounds() {
		WWObject[] objects = world.getObjects();
		int lastObjectIndex = world.lastObjectIndex;
		for (int i = 0; i <= lastObjectIndex; i++) {
			WWObject object = objects[i];
			if (object != null) {
				object.updateSound();
			}
		}
	}

}
