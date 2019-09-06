package com.gallantrealm.myworld.model;

import com.gallantrealm.myworld.client.renderer.IRenderer;

/**
 * This primitive can efficiently represent a plant. A texture is used that is folded multiple times to form a plant.
 * The plant is by default non-solid, so it doesn't interfere with a moving avatar.
 */
public class WWPlant extends WWObject {
	static final long serialVersionUID = 1L;

	public WWPlant() {
		setPhantom(true);
	}

	public WWPlant(float sizeX, float sizeY, float sizeZ) {
		super(sizeX, sizeY, sizeZ);
		setPhantom(true);
	}

	public void createRendering(IRenderer renderer, long worldTime) {
		rendering = renderer.createPlantRendering(this, worldTime);
	}

	@Override
	public void getPenetration(WWVector point, WWVector position, WWVector rotation, long worldTime, WWVector tempPoint, WWVector penetrationVector) {
		// plants can always be penetrated (always phantom)
		penetrationVector.zero();
	}

	@Override
	public void getPenetration(WWVector point, float[] positionMatrix, long worldTime, WWVector tempPoint, WWVector penetrationVector) {
		// plants can always be penetrated (always phantom)
		penetrationVector.zero();
	}

}
