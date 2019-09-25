package com.gallantrealm.myworld.model;

import java.io.IOException;
import com.gallantrealm.myworld.FastMath;
import com.gallantrealm.myworld.client.renderer.IRenderer;
import com.gallantrealm.myworld.communication.DataInputStreamX;
import com.gallantrealm.myworld.communication.DataOutputStreamX;

/**
 * A particle emitter provides explosion, smoke, and other animations consisting of smaller particles. Each particle is a camera-facing square painted with the color and texture of SIDE_ALL.
 */
public class WWParticleEmitter extends WWObject {
	static final long serialVersionUID = 1L;

	public static final int TYPE_RANDOM = 0;

	public int particleCount = 100;
	public float particleSize = 0.1f;
	public int particleLifetime = 100; // number of ticks till particle is considered gone
	public WWParticleAnimation animation;
	public transient Particle[] particles;

	/**
	 * Indicates that the particles should move and turn if the emitter moves and turns
	 */
	public boolean relative;

	public class Particle {
		public WWVector position = new WWVector();
		public WWVector velocity = new WWVector();
		public float size = 0.1f;
		public float alpha = 1.0f;
		public int age = 0;
		public boolean stopped;
	}

	public WWParticleEmitter() {
		physical = false;
		phantom = true;
		penetratable = true;
	}

	public final void setAnimation(WWParticleAnimation animation) {
		this.animation = animation;
	}

	public final void setParticleSize(float particleSize) {
		this.particleSize = particleSize;
	}

	public final void setParticleCount(int count) {
		this.particleCount = count;
		particles = null;
	}
	
	public int getParticleLifetime() {
		return particleLifetime;
	}

	public void setParticleLifetime(int particleLifetime) {
		this.particleLifetime = particleLifetime;
	}

	public int getParticleCount() {
		return particleCount;
	}

	public float getParticleSize() {
		return particleSize;
	}

	@Override
	public void send(DataOutputStreamX os) throws IOException {
		super.send(os);
		os.writeInt(particleCount);
		os.writeFloat(particleSize);
		os.writeObject(animation);
		os.writeInt(particleLifetime);
		os.writeBoolean(relative);
	}

	@Override
	public void receive(DataInputStreamX is) throws IOException {
		super.receive(is);
		particleCount = is.readInt();
		particleSize = is.readFloat();
		animation = (WWParticleAnimation) is.readObject();
		particleLifetime = is.readInt();
		relative = is.readBoolean();
	}

	public void createRendering(IRenderer renderer, long worldTime) {
		rendering = renderer.createParticlesRendering(this, worldTime);
	}

	public boolean animating;
	int nextParticle;
	private float particlesToRecycle = 0.0f;

	public void startAnimation(long worldTime) {
		particles = new Particle[particleCount];
		for (int i = 0; i < particleCount; i++) {
			particles[i] = new Particle();
		}
		nextParticle = 0;
		int nstart;
		if (animation.getParticleRate() == 0) {
			nstart = particleCount;
		} else {
			nstart = (int)FastMath.min(animation.getParticleRate(), particleCount);
		}
		for (int i = 0; i < nstart; i++) {
			animation.startParticle(this, particles[i], worldTime);
		}
		animating = true;
	}
	
	public void updateAnimation(long worldTime) {
		if (!animating) {
			return;
		}
		if (particles == null) {
			startAnimation(worldTime);
		}
		boolean particlesStillGoing = false;
		for (int i = 0; i < particleCount; i++) {
			particles[i].age++;
			if (particles[i].age > particleLifetime) {
				if (!particles[i].stopped) {
					animation.stopParticle(this, particles[i], worldTime);
					particles[i].stopped = true;
				}
			} else {
				particlesStillGoing = true;
				animation.updateParticle(this, particles[i], worldTime);
			}
		}
		particlesToRecycle += animation.getParticleRate();
		while (particlesToRecycle > 1.0f) {
			animation.stopParticle(this, particles[nextParticle], worldTime);
			if (animating) {
				particles[nextParticle].age = 0;
				particles[nextParticle].size = 0.1f;
				particles[nextParticle].alpha = 1.0f;
				particles[nextParticle].stopped = false;
				particlesStillGoing = true;
				animation.startParticle(this, particles[nextParticle], worldTime);
			}
			nextParticle++;
			if (nextParticle >= particleCount) {
				nextParticle = 0;
			}
			particlesToRecycle -= 1.0f;
		}
		if (!particlesStillGoing) {
			animating = false;
		}
	}

	public void stopAnimation() {
		for (int i = 0; i < particleCount; i++) {
			animation.stopParticle(this, particles[i], getWorldTime());
			particles[i].stopped = true;
		}
		animating = false;
	}

	@Override
	public void getPenetration(WWVector point, float[] positionMatrix, long worldTime, WWVector tempPoint, WWVector penetrationVector) {
		// particle emitters can always be penetrated (always phantom)
		penetrationVector.zero();
	}

}
