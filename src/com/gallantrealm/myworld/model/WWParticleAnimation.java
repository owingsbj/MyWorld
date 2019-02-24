package com.gallantrealm.myworld.model;

import java.io.IOException;
import java.io.Serializable;

import com.gallantrealm.myworld.communication.DataInputStreamX;
import com.gallantrealm.myworld.communication.DataOutputStreamX;
import com.gallantrealm.myworld.communication.Sendable;

/**
 * Used to describe the animation of a particle.
 */
public abstract class WWParticleAnimation implements Serializable, Sendable {
	static final long serialVersionUID = 1L;

	/**
	 * Number of particles to create/recycle each iteration.  A rate of zero is an explosion.
	 * @return
	 */
	public abstract int getParticleRate();
	
	public abstract void startParticle(WWParticleEmitter emitter, WWParticleEmitter.Particle particle, long worldTime);
	
	public abstract void updateParticle(WWParticleEmitter emitter, WWParticleEmitter.Particle particle, long worldTime);

	/**
	 * By default, particles stop by becoming transparent and zero in size
	 */
	public void stopParticle(WWParticleEmitter emitter, WWParticleEmitter.Particle particle, long worldTime) {
		particle.size = 0.0f;
		particle.alpha = 0.0f;
	}

	public void send(DataOutputStreamX os) throws IOException {
	}

	public void receive(DataInputStreamX is) throws IOException {
	}
}
