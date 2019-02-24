package com.gallantrealm.myworld.android;

import java.io.Serializable;

import com.gallantrealm.myworld.client.model.ClientModelChangedEvent;
import com.gallantrealm.myworld.model.WWAction;

public class PauseAction extends WWAction implements Serializable {

	boolean isAvatarAction;

	public PauseAction() {
		isAvatarAction = false;
	}

	public PauseAction(boolean isAvatarAction) {
		this.isAvatarAction = isAvatarAction;
	}

	@Override
	public String getName() {
		if (AndroidClientModel.getClientModel().paused) {
			return "Resume";
		} else {
			return "Pause";
		}
	}

	@Override
	public void start() {
		AndroidClientModel clientModel = AndroidClientModel.getClientModel();
		if (clientModel.paused) {
			clientModel.resumeWorld();
		} else {
			clientModel.pauseWorld();
			clientModel.flashMessage("Touch screen to pan and zoom", false);
		}
		if (isAvatarAction) {
			clientModel.fireClientModelChanged(ClientModelChangedEvent.EVENT_TYPE_AVATAR_ACTIONS_CHANGED);
		} else {
			clientModel.fireClientModelChanged(ClientModelChangedEvent.EVENT_TYPE_WORLD_ACTIONS_CHANGED);
		}
	}

}
