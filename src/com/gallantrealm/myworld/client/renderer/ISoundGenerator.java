package com.gallantrealm.myworld.client.renderer;

import com.gallantrealm.myworld.model.WWVector;

public interface ISoundGenerator {
	
	void loadSound(String urlString);

	void playSound(String sound, int priority, WWVector position, float volume, float pitch);

	int startPlayingSound(String sound, int priority, WWVector position, float volume, float pitch);

	int adjustPlayingSound(int streamId, WWVector position, float volume, float pitch);

	void stopPlayingSound(int streamId);

	void playSong(String songname, float volume);

	void stopPlayingSong();

	void pause();

	void resume();

	void stop();

	void reset();

	void destroy();
}
