package com.gallantrealm.myworld.model;

/**
 * This thread performs updates to the world according to physical properties. This involves detecting collision and
 * forces between objects and adjusting the position, orientation, velocity and angular momentum to match.
 * <p>
 * Note that this thread does not actually handle moving or rotating objects due to their own velocity and angular
 * momentum. This is handled within the WWObject itself.
 */
public abstract class PhysicsThread extends Thread {

	final int iterationTime;

	final WWWorld world;
	public boolean safeStop;

	public PhysicsThread(WWWorld world, int iterationTime) {
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
						//System.out.println("peaked");
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

	public abstract void performIteration(long timeIncrement);

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
				((WWParticleEmitter)object).updateAnimation(worldTime);
			}
		}
	}

}
