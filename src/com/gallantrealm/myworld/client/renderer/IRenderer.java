package com.gallantrealm.myworld.client.renderer;

import com.gallantrealm.myworld.model.WWBox;
import com.gallantrealm.myworld.model.WWCylinder;
import com.gallantrealm.myworld.model.WWMesh;
import com.gallantrealm.myworld.model.WWParticleEmitter;
import com.gallantrealm.myworld.model.WWSculpty;
import com.gallantrealm.myworld.model.WWPlant;
import com.gallantrealm.myworld.model.WWSphere;
import com.gallantrealm.myworld.model.WWTorus;
import com.gallantrealm.myworld.model.WWTranslucency;
import com.gallantrealm.myworld.model.WWWorld;

/**
 * Interface for platform specific rendering capabilities. Each platform implements a version of this interface to
 * provide rendering appropriate for the platform.
 */
public interface IRenderer {

	IRendering createWorldRendering(WWWorld world, long worldTime);

	IRendering createBoxRendering(WWBox box, long worldTime);

	IRendering createCylinderRendering(WWCylinder cylinder, long worldTime);

	IRendering createSphereRendering(WWSphere sphere, long worldTime);

	IRendering createTorusRendering(WWTorus torus, long worldTime);

	IRendering createMeshRendering(WWMesh mesh, long worldTime);

	IRendering createSculptyRendering(WWSculpty meshBox, long worldTime);

	IRendering createPlantRendering(WWPlant plant, long worldTime);

	IRendering createTranslucencyRendering(WWTranslucency translucency, long worldTime);
	
	IRendering createParticlesRendering(WWParticleEmitter particles, long worldTime);

	ITextureRenderer getTextureRenderer();

	IVideoTextureRenderer getVideoTextureRenderer();

	ISoundGenerator getSoundGenerator();

}
