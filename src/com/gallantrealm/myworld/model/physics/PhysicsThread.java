package com.gallantrealm.myworld.model.physics;

import java.util.ArrayList;
import com.gallantrealm.myworld.FastMath;
import com.gallantrealm.myworld.model.WWBehavior;
import com.gallantrealm.myworld.model.WWObject;
import com.gallantrealm.myworld.model.WWParticleEmitter;
import com.gallantrealm.myworld.model.WWVector;
import com.gallantrealm.myworld.model.WWWorld;
import android.opengl.Matrix;

/**
 * This thread performs updates to the world according to physical properties. This involves detecting collision and forces between objects and adjusting the position, orientation, velocity and angular momentum to match.
 * <p>
 * Note that this thread does not actually handle moving or rotating objects due to their own velocity and angular momentum. This is handled within the WWObject itself.
 */
public class PhysicsThread extends Thread {

	final int iterationTime;

	final WWWorld world;
	public boolean safeStop;

	ArrayList<ObjectCollision> previousPreviousCollidedObjects = new ArrayList<ObjectCollision>();

	ArrayList<ObjectCollision> previousCollidedObjects = new ArrayList<ObjectCollision>();

	ArrayList<ObjectCollision> newCollidedObjects = new ArrayList<ObjectCollision>();

	protected int slideStyle;

	public PhysicsThread(WWWorld world, int iterationTime) {
		setName("PhysicsThread");
		this.world = world;
		this.iterationTime = iterationTime;
		setPriority(Thread.MAX_PRIORITY - 1);
		setDaemon(true);
	}

	public PhysicsThread(WWWorld world, int iterationTime, int slideStyle) {
		this(world, iterationTime);
		this.slideStyle = slideStyle;
	}

	@Override
	public void run() {
		if (iterationTime == 0) {
			return;
		}
		System.out.println("PhysicsThread: starting");
		long timeSinceLastSoundUpdate = 0;
		try {
			Thread.sleep(500); // let things settle before starting physics
			while (world.onClient && !world.rendered) {
				System.out.println("PhysicsThread: waiting for first rendering");
				Thread.sleep(500); // let things settle before starting physics
			}
			System.out.println("PhysicsThread: starting performIteration loop");
			long lastStartTime = System.currentTimeMillis();
			while (!safeStop && world.physicsThread == this) {
				try {
					long startTime = System.currentTimeMillis();
					long timeIncrement = Math.min(startTime - lastStartTime, iterationTime);
					performIteration(timeIncrement);
					updateParticles(world.getWorldTime());

					// Wait to even loop time
					long loopTime = System.currentTimeMillis() - startTime;
					if (loopTime < iterationTime) {
						Thread.sleep(iterationTime - loopTime);
					} else {
						Thread.sleep(0);
						// System.out.println("peaked");
					}
					lastStartTime = startTime;
					timeSinceLastSoundUpdate += loopTime;
					if (timeSinceLastSoundUpdate > 100) {
						updateSounds();
						timeSinceLastSoundUpdate = 0;
					}
				} catch (InterruptedException e) {
					System.out.println("PhysicsThread: stopping physics loop -- interrupted");
					return;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			System.out.println("PhysicsThread: stopping physics loop -- safestop");

		} catch (InterruptedException e) {
			System.out.println("PhysicsThread: stopping physics loop -- interrupted before first iteration");
		}
	}

	void updateSounds() {
		WWObject[] objects = world.objects;
		int lastObjectIndex = world.lastObjectIndex;
		for (int i = 0; i <= lastObjectIndex; i++) {
			WWObject object = objects[i];
			if (object != null && object.sound != null) {
				object.updateSound();
			}
		}
	}

	void updateParticles(long worldTime) {
		WWObject[] objects = world.objects;
		int lastObjectIndex = world.lastObjectIndex;
		for (int i = 0; i <= lastObjectIndex; i++) {
			WWObject object = objects[i];
			if (object != null && object instanceof WWParticleEmitter) {
				((WWParticleEmitter) object).updateAnimation(worldTime);
			}
		}
	}

	public void performIteration(long timeIncrement) {
		if (timeIncrement <= 0) {
			return;
		}
		long worldTime = world.getWorldTime() + timeIncrement;
		float deltaTime = timeIncrement / 1000.0f;

		previousPreviousCollidedObjects = previousCollidedObjects;
		previousCollidedObjects = newCollidedObjects;
		newCollidedObjects = new ArrayList<ObjectCollision>();

		float[] positionMatrix = new float[16];
		WWVector velocity = new WWVector();
		WWVector aMomentum = new WWVector();
		float[] positionMatrix2 = new float[16];
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

				long originalLastMoveTime = object.lastMoveTime;

				// Get current orientation and momentum values.
				object.getAbsolutePositionMatrix(positionMatrix, worldTime);
				object.getVelocity(velocity);
				object.getAMomentum(aMomentum);
				WWVector thrust = object.getThrust();
				WWVector thrustVelocity = object.getThrustVelocity();
				WWVector torque = object.getTorque();
				WWVector torqueVelocity = object.getTorqueVelocity();

				// start with thrust and torque by the object itself
				// note that these are attenuated (by thrust and torque velocity) if the velocity
				// and torque are already high.
				WWVector totalForce = thrust.clone();
				WWObject.rotate(totalForce, positionMatrix);
				WWObject.rotate(thrustVelocity, positionMatrix);
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
				WWVector totalTorque = torque.clone();
				if (torqueVelocity.x > 0 && aMomentum.x > torqueVelocity.x) {
					totalTorque.x = 0;
				} else if (torqueVelocity.x < 0 && aMomentum.x < torqueVelocity.x) {
					totalTorque.x = 0;
				}
				if (torqueVelocity.y > 0 && aMomentum.y > torqueVelocity.y) {
					totalTorque.y = 0;
				} else if (torqueVelocity.y < 0 && aMomentum.y < torqueVelocity.y) {
					totalTorque.y = 0;
				}
				if (torqueVelocity.z > 0 && aMomentum.z > torqueVelocity.z) {
					totalTorque.z = 0;
				} else if (torqueVelocity.z < 0 && aMomentum.z < torqueVelocity.z) {
					totalTorque.z = 0;
				}

				// sum in gravitational force only if object has mass
				if (object.getDensity() > 0.0) {
					totalForce.add(world.getGravityForce(positionMatrix[12], positionMatrix[14], positionMatrix[13]));
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
						object2.getAbsolutePositionMatrix(positionMatrix2, worldTime);
						float px = positionMatrix2[12] - positionMatrix[12];
						float py = positionMatrix2[14] - positionMatrix[14];
						float pz = positionMatrix2[13] - positionMatrix[13];
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
							object.getOverlap(object2, positionMatrix, positionMatrix2, worldTime, tempPoint, tempPoint2, overlapPoint, overlapVector);

							if (!overlapVector.isZero()) {

								ObjectCollision collision = new ObjectCollision(object, object2, overlapVector);
								newCollidedObjects.add(collision);

								// If the object2 is solid, apply solid-to-solid physics
								if (object2.solid) {

									// calculate unit overlap vector (for later)
									WWVector unitOverlapVector;
									if (overlapVector.length() > 0) {
										unitOverlapVector = new WWVector(overlapVector);
									} else { // use force
										unitOverlapVector = overlapVector.clone(); // new WWVector(-force.x, -force.y, -force.z);
									}
									unitOverlapVector.normalize();

									// Adjust the position of the objects so that they are not overlapping
									if (!object.freedomMoveX || !object.freedomMoveY || !object.freedomMoveZ) {
										WWObject.rotate(overlapVector, positionMatrix);
										if (!object.freedomMoveX) {
											overlapVector.x = 0;
										}
										if (!object.freedomMoveY) {
											overlapVector.y = 0;
										}
										if (!object.freedomMoveZ) {
											overlapVector.z = 0;
										}
									}
									WWObject.antiRotate(overlapVector, positionMatrix);
									Matrix.translateM(positionMatrix, 0, -overlapVector.x, -overlapVector.z, -overlapVector.y);

									// If the object is moving toward object2, stop or repell it (according to elasticity)
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
									// take cross product of unitoverlapvector and vector of overlappoint->centerpoint
									// then combine with velocity in some way to form an addition to the torque
									WWVector position = new WWVector(positionMatrix[12], positionMatrix[14], positionMatrix[13]);
									totalTorque.add(position.clone().subtract(overlapPoint).cross(unitOverlapVector).scale(1000f));

								} // solid

								// If the object2 is non-solid, apply solid-to-liquid/gas physics
								else {

									// boyancy
									if (object.isFreedomMoveZ() && object2.getDensity() > 0.0) {
										// Note: pressure is determined by how deep into the object. This is an
										// estimate here, based on the extent. This will be correct only if the object is level (flat)
										float pressure = FastMath.max(positionMatrix2[13] + object2.sizeZ / 2.0f - positionMatrix[13], 0.0f);
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
									float friction = FastMath.min(object.friction, object2.friction);
									if (friction > 0) {

										// Friction is a force acting opposite of relative velocity/amomentum of the two items colliding.
										WWVector frictionVForce = object2.getVelocity(); // TODO include object2 amomentum (if object2 is large)
										frictionVForce.subtract(velocity);
										frictionVForce.scale(10 * friction / FastMath.range(frictionVForce.length(), 0.01f, 1f));
										totalForce.add(frictionVForce);

										WWVector frictionAForce = object2.getAMomentum(); // TODO include object2 position (if object2 is large)
										frictionAForce.subtract(aMomentum);
										frictionAForce.scale(10 * friction / FastMath.range(frictionAForce.length() / 10.0f, 0.01f, 1f));
										totalTorque.add(frictionAForce);

									}
								} // friction

							} // if overlapping
						} // if near each other

					} // if object != object2
				} // for object2

				// Cap forces to avoid really bad behaviors
				if (totalForce.length() > 10) {
					totalForce.scale(10 / totalForce.length());
				}
				if (totalTorque.length() > 360) {
					totalTorque.scale(360 / totalTorque.length());
				}
				
				// Apply forces to object's velocity
				velocity.x += totalForce.x * deltaTime;
				velocity.y += totalForce.y * deltaTime;
				velocity.z += totalForce.z * deltaTime;

				// Limit velocity according to freedom
				if (!object.freedomMoveX || !object.freedomMoveY || !object.freedomMoveZ) {
					WWObject.antiRotate(velocity, positionMatrix);
					if (!object.freedomMoveX) {
						velocity.x = 0.0f;
					}
					if (!object.freedomMoveY) {
						velocity.y = 0.0f;
					}
					if (!object.freedomMoveZ) {
						velocity.z = 0.0f;
					}
					WWObject.rotate(velocity, positionMatrix);
				}

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
				
				// Update the position, rotation, velocity and angular momentum values on the object if any have changed due to
				// physical interaction with another object, but only if the object has not been moved by some other thread
				if (object.lastMoveTime == originalLastMoveTime) {
					object.setOrientation(positionMatrix, velocity, aMomentum, worldTime);
				}

			} // if physical

		} // for object

		world.updateWorldTime(timeIncrement);

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
