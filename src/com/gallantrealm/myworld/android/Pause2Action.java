package com.gallantrealm.myworld.android;


public class Pause2Action extends PauseAction {

	@Override
	public String getName() {
		if (AndroidClientModel.getClientModel().paused) {
			return "     Paused";
		} else {
			return "";
		}
	}

}
