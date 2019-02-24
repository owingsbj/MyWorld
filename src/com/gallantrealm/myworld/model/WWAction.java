package com.gallantrealm.myworld.model;


/**
 * An action is a client capability that can be performed for a user or upon an object in the world. Actions differ from
 * behaviors in that actions are performed on the client while behaviors are performed on the server. Actions will
 * typically be shown in a menu or toolbar. When in a toolbar, they can start/stop depending on when the user is
 * pressing or clicking on the action.
 * <p>
 * By being part of the model, it is possible to create actions that are platform independent. However, some actions are
 * likely to be platform dependent (displaying dialogs or performing platform specific functions).
 */
public abstract class WWAction {

	boolean enabled = true;

	public abstract String getName();

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public void start(float x, float y) {
		start();
	}

	public void start() {
	}

	public void repeat(float x, float y) {
	}

	public void stop() {
	}

}
