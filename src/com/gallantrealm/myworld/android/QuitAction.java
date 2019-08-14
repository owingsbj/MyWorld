package com.gallantrealm.myworld.android;

import java.io.Serializable;

import com.gallantrealm.myworld.client.model.ClientModel;
import com.gallantrealm.myworld.model.WWAction;

public class QuitAction extends WWAction implements Serializable {
	private static final long serialVersionUID = 1L;

	public QuitAction() {
	}

	@Override
	public String getName() {
		return "Quit";
	}

	@Override
	public void start() {
		final ClientModel clientModel = AndroidClientModel.getClientModel();
		clientModel.getContext().runOnUiThread(new Runnable() {
			public void run() {
				((ShowWorldActivity)clientModel.getContext()).doQuit();
			}
		});
	}

}
