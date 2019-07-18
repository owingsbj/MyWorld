package com.gallantrealm.myworld.model.physics;

import java.util.ArrayList;
import com.gallantrealm.myworld.FastMath;
import com.gallantrealm.myworld.model.WWBehavior;
import com.gallantrealm.myworld.model.WWObject;
import com.gallantrealm.myworld.model.WWVector;
import com.gallantrealm.myworld.model.WWWorld;

/**
 * This thread performs updates to the world according to physical properties. This involves detecting collision and forces between objects and adjusting the position, orientation, velocity and angular momentum to match.
 * <p>
 * Note that this thread does not actually handle moving or rotating objects due to their own velocity and angular momentum. That is handled within WWObject itself.
 * <p>
 * This is the "New" version of physics. It mimics real world more precisely. In particular, it applies thrust and torque first, before other forces are determined. This causes the thrust and torque to be limited by other forces, such as
 * what occurs in the real world.
 */
public class NewPhysicsThread extends PhysicsThread {

	private static final boolean queueUpdates = false;

	ArrayList<ObjectCollision> previousPreviousCollidedObjects = new ArrayList<ObjectCollision>();
	ArrayList<ObjectCollision> previousCollidedObjects = new ArrayList<ObjectCollision>();
	ArrayList<ObjectCollision> newCollidedObjects = new ArrayList<ObjectCollision>();

	int slideStyle;

	public NewPhysicsThread(WWWorld world, int iterationTime, int slideStyle) {
		super(world, iterationTime);
		this.slideStyle = slideStyle;
	}

	class QueuedUpdate {
		public WWObject object;
		public WWVector position, rotation, velocity, aMomentum;
		public long worldTime;

		public QueuedUpdate(WWObject object, WWVector position, WWVector rotation, WWVector velocity, WWVector aMomentum, long worldTime) {
			this.object = object;
			this.position = position;
			this.rotation = rotation;
			this.velocity = velocity;
			this.aMomentum = aMomentum;
			this.worldTime = worldTime;
		}
	}

	@Override
	public void performIteration(long timeIncrement) {
		long worldTime = world.getWorldTime() + timeIncrement;
		float deltaTime = timeIncrement / 1000.0f;

		ArrayList<QueuedUpdate> queuedUpdates = null;
		if (queueUpdates) {
			queuedUpdates = new ArrayList<QueuedUpdate>();
		}
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

			// For physical objects, determine forces upon the object. This done by summing
			// up all forces on the object, ignoring those where freedom is restricted.
			// Only examine physical objects that are also solid. Non-solid (liquid, gas)
			// objects are not influenced by other objects (but do influence other objects
			// by being tested in the inner loop below).
			if (object != null && object.physical && object.solid && !object.deleted) {

//				synchronized (object) { // to keep it from being modified

				long originalLastMoveTime = object.lastMoveTime;

				// Get current orientation and momentum values.
				object.getAbsolutePosition(position, worldTime);
				object.getAbsoluteRotation(rotation, worldTime);
				object.getVelocity(velocity);
				object.getAMomentum(aMomentum);
				WWVector thrust = object.getThrust();
				WWVector thrustVelocity = object.getThrustVelocity();
				WWVector torque = object.getTorque();
				WWVector torqueVelocity = object.getTorqueVelocity();

				WWVector originalPosition = position.clone();
				WWVector originalRotation = rotation.clone();
				WWVector originalVelocity = velocity.clone();
				WWVector originalAMomentum = aMomentum.clone();

				// start with thrust and torque by the object itself
				// note that these are attenuated (by thrust and torque velocity) if the velocity
				// and torque are already high.
				WWVector totalForce = thrust.clone();
				if (thrustVelocity.x > 0 && velocity.x > thrustVelocity.x) {
					totalForce.x = 0;
				} else if (thrustVelocity.x < 0 && velocity.x < thrustVelocity.x) {
					totalForce.x = 0;
				}
				if (thrustVelocity.y > 0 && velocity.y > thrustVelocity.y) {
					totalForce.y = 0;
				} else if (thrustVelocity.y < 0 && velocity.y < thrustVelocity.y) {
					totalForce.y = 0;
				}
				if (thrustVelocity.z > 0 && velocity.z > thrustVelocity.z) {
					totalForce.z = 0;
				} else if (thrustVelocity.z < 0 && velocity.z < thrustVelocity.z) {
					totalForce.z = 0;
				}
				WWObject.rotate(totalForce, rotation, worldTime);
				WWVector totalTorque = torque.clone();
				if (torqueVelocity.x > 0 && velocity.x > torqueVelocity.x) {
					totalTorque.x = 0;
				} else if (torqueVelocity.x < 0 && velocity.x < torqueVelocity.x) {
					totalTorque.x = 0;
				}
				if (torqueVelocity.y > 0 && velocity.y > torqueVelocity.y) {
					totalTorque.y = 0;
				} else if (torqueVelocity.y < 0 && velocity.y < torqueVelocity.y) {
					totalTorque.y = 0;
				}
				if (torqueVelocity.z > 0 && velocity.z > torqueVelocity.z) {
					totalTorque.z = 0;
				} else if (torqueVelocity.z < 0 && velocity.z < torqueVelocity.z) {
					totalTorque.z = 0;
				}

				// sum in gravitational force only if object has mass
				if (object.getDensity() > 0.0) {
					totalForce.add(world.getGravityForce(position.x, position.y, position.z));
				}

				// - Next, apply interactions of other objects that overlap the physical object
				float objectExtent = object.extent;
				for (int j = 1; j <= lastObjectIndex; j++) {
					WWObject object2 = objects[j];
					if (object2 != null && !object2.phantom && object2 != object && !object2.deleted && !object.isDescendant(object2)) {

						// First, see if the objects are close enough to possibly overlap.
						// If they are, it is worth determining if they actually do overlap
						float maxExtentx = objectExtent + object2.extentx;
						float maxExtenty = objectExtent + object2.extenty;
						float maxExtentz = objectExtent + object2.extentz;
						object2.getAbsolutePosition(position2, worldTime);
						float px = position2.x - position.x;
						float py = position2.y - position.y;
						float pz = position2.z - position.z;
						if (px < 0)
							px = -px;
						if (py < 0)
							py = -py;
						if (pz < 0)
							pz = -pz;
						if (/* object2.parentId != 0 || */(px <= maxExtentx && py <= maxExtenty && pz <= maxExtentz)) {

							// Determine if the objects overlap, and the vector of overlap. This
							// vector points in the direction of the deepest overlap, and the length of the
							// vector indicates the amount of overlap
							object2.getAbsoluteRotation(rotation2, worldTime);
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
										unitOverlapVector = overlapVector.clone(); // new WWVector(-force.x, -force.y, -force.z);
									}
									unitOverlapVector.normalize();
									if (FastMath.avg(object.elasticity, object2.elasticity) > 0.0) { // bounce both objects off of each other
										if (object2.physical) {
											float objectMass = object.density * object.sizeX * object.sizeY * object.sizeZ;
											float object2Mass = object2.density * object2.sizeX * object2.sizeY * object2.sizeZ;
											WWVector forceVector = velocity.clone().scale(objectMass);
											WWVector force2Vector = object2.getVelocity().scale(object2Mass);
											WWVector totalForceVector = forceVector.add(force2Vector);
											WWVector mirrorVector = unitOverlapVector.clone();
											mirrorVector.scale(-1.0f);
											WWVector mirrorForceVector = totalForceVector.getReflection(mirrorVector);
											float elasticity = FastMath.avg(object.elasticity, object2.elasticity);
											velocity.x = mirrorForceVector.x * elasticity;
											velocity.y = mirrorForceVector.y * elasticity;
											velocity.z = mirrorForceVector.z * elasticity;
										} else { // simple bounce-back
											WWVector velocityVector = new WWVector(velocity.x, velocity.y, velocity.z);
											WWVector mirrorVector = unitOverlapVector.clone();
											mirrorVector.scale(-1.0f);
											WWVector mirrorVelocityVector = velocityVector.getReflection(mirrorVector);
											float elasticity = Math.max(object.elasticity, object2.elasticity);
											velocity.x = mirrorVelocityVector.x * elasticity;
											velocity.y = mirrorVelocityVector.y * elasticity;
											velocity.z = mirrorVelocityVector.z * elasticity;
										}
									} else { // slide on the object

										if (slideStyle == 0) { // used in rtr

											WWVector antiForceVector = unitOverlapVector.clone();
											antiForceVector.scale(0.99f); // a small fudge factor to keep object sliding
											antiForceVector.scale(-totalForce.length());
											totalForce.add(antiForceVector);
											velocity.scale(1.0f, 1.0f, 1.0f - Math.abs(unitOverlapVector.z));

										} else if (slideStyle == 1) { // used in bonkers

											WWVector unitVelocity = velocity.clone().normalize();
											float repelMag = 1 - unitVelocity.add(unitOverlapVector).length();
											WWVector repelForce = unitOverlapVector.clone().scale(velocity.length() / deltaTime * repelMag);
											totalForce.add(repelForce);

										} else if (slideStyle == 2) { // most recent (best?)

											WWVector unitVelocity = velocity.clone().normalize();
											float repelMag = -Math.abs(unitVelocity.dot(unitOverlapVector));
											WWVector repelForce = unitOverlapVector.clone().scale(velocity.length() / deltaTime * repelMag);
											totalForce.add(repelForce);

										}

									}

									// Adjust angular momentum as well
									// TODO implement angular momentum adjustment

								} // solid

								// If the object2 is non-solid, apply solid-to-liquid/gas physics
								else {

									// boyancy
									if (object.isFreedomMoveZ() && object2.getDensity() > 0.0) {
										// Note: pressure is determined by how deep into the object. This is an
										// estimate here, based on the extent. This will be correct only if the object is level (flat)
										float pressure = FastMath.max(position2.z + object2.sizeZ / 2.0f - position.z, 0.0f);
										float boyancy = object2.getDensity() * pressure - object.getDensity();
										if (boyancy > 0) {
											velocity.z += (boyancy * deltaTime) * 30.0;
										}
									}

								} // nonsolid

								// Depending on friction forces, slow the object movement
								if (object2.isSolid() && object.getElasticity() + object2.getElasticity() > 0) {
									// no friction for solids touching with elasticity
								} else {
									float friction = FastMath.min(object.friction, object2.friction) * 2.0f;
									if (friction > 0) {
										
										// Friction is a force acting opposite of relative velocity/amomentum of the two items colliding.
										WWVector frictionVForce = object2.getVelocity();    // TODO include object2 amomentum (if object2 is large)
										frictionVForce.subtract(velocity);
										frictionVForce.scale(friction);
										if (frictionVForce.length() > 1) {
											frictionVForce.scale(frictionVForce.length());
										}
										totalForce.add(frictionVForce);
										WWVector frictionAForce = object2.getAMomentum();   // TODO include object2 position (if  object2 is large)
										frictionAForce.subtract(aMomentum);
										frictionAForce.scale(friction);
										if (frictionAForce.length() > 1) {
											frictionAForce.scale(frictionAForce.length());
										}
										totalTorque.add(frictionAForce);

									}
								} // friction

							} // if overlapping
						} // if near each other

					} // if object != object2
				} // for object2

					// Cap force to avoid really bad behaviors
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

				// Cap velocity according to terminal velocity
//					if (velocity.length() > thrustVelocity.length()) {
//						velocity.scale(1.0f / (1.0f + velocity.length() - thrustVelocity.length()));
//					}

				aMomentum.x += totalTorque.x * deltaTime;
				aMomentum.y += totalTorque.y * deltaTime;
				aMomentum.z += totalTorque.z * deltaTime;
				
				if (!object.freedomRotateX) {
					aMomentum.x = 0;
				}
				if (!object.freedomRotateY) {
					aMomentum.y = 0;
				}
				if (!object.freedomRotateZ) {
					aMomentum.z = 0;
				}
				
				// Cap amomentum according to terminal amomentum
//				if (aMomentum.length() > torqueVelocity.length()) {
//					aMomentum.scale(1.0f / (1.0f + aMomentum.length() - torqueVelocity.length()));
//				}


				// Update the position, rotation, velocity and angular momentum values on the object if any have changed due to
				// physical interaction with another object, but only if the object has not been moved by some other thread
				if (object.lastMoveTime == originalLastMoveTime && (!position.equals(originalPosition) || !rotation.equals(originalRotation) || !velocity.equals(originalVelocity) || !aMomentum.equals(originalAMomentum))) {

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

					if (queueUpdates) {
						queuedUpdates.add(new QueuedUpdate(object, position.clone().subtract(originalPosition), rotation.clone().subtract(originalRotation), velocity.clone().subtract(originalVelocity),
								aMomentum.clone().subtract(originalAMomentum), worldTime));
					} else {
						object.setOrientation(position, rotation, velocity, aMomentum, worldTime);
					}
				}

//				} // synchronize object

			} // if physical

		} // for object

		world.updateWorldTime(timeIncrement);

		// performed queued updates. This done with world locked so that the updates are "instantaneous"
		if (queueUpdates) {
			synchronized (world) {
				WWVector tposition = new WWVector();
				WWVector trotation = new WWVector();
				WWVector tvelocity = new WWVector();
				WWVector tmomentum = new WWVector();
				int qsize = queuedUpdates.size();
				for (int i = 0; i < qsize; i++) {
					QueuedUpdate queuedUpdate = queuedUpdates.get(i);
					if (queuedUpdate.worldTime > queuedUpdate.object.lastMoveTime) { // don't update if changed externally
						queuedUpdate.object.getPosition(tposition, worldTime);
						queuedUpdate.object.getRotation(trotation, worldTime);
						queuedUpdate.object.getVelocity(tvelocity);
						queuedUpdate.object.getAMomentum(tmomentum);
						queuedUpdate.object.setOrientation(queuedUpdate.position.add(tposition), queuedUpdate.rotation.add(trotation), queuedUpdate.velocity.add(tvelocity), queuedUpdate.aMomentum.add(tmomentum), queuedUpdate.worldTime);
					}
				}
			}
		}

		// fire collide and slide events
		int ncosize = newCollidedObjects.size();
		int pcosize = previousCollidedObjects.size();
		int ppcosize = previousPreviousCollidedObjects.size();
		for (int i = 0; i < ncosize; i++) {
			ObjectCollision newCollision = newCollidedObjects.get(i);
			for (int j = 0; j < pcosize; j++) {
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
				for (int j = 0; j < ppcosize; j++) {
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
		for (int j = 0; j < ppcosize; j++) {
			ObjectCollision previousPreviousCollision = previousPreviousCollidedObjects.get(j);
			if (previousPreviousCollision.sliding && !previousPreviousCollision.stillSliding) {
				world.stopSlidingObject(previousPreviousCollision);
			}
		}

		// update timers and invoke timer events on object behaviors
		for (int i = 0; i <= lastObjectIndex; i++) {
			WWObject object = objects[i];
			if (object != null && !object.deleted && object.behaviors != null) {
				int behaviorCount = object.behaviors.length;
				for (int b = 0; b < behaviorCount; b++) {
					WWBehavior behavior = object.behaviors[b].behavior;
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
		}

	}
}
