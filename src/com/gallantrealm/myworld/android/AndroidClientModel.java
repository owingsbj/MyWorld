package com.gallantrealm.myworld.android;

import com.gallantrealm.myworld.client.model.ClientModel;

import android.annotation.SuppressLint;

@SuppressLint("NewApi")
public class AndroidClientModel extends ClientModel {

	public static AndroidClientModel getClientModel() {
		if (clientModel == null) {
			clientModel = new AndroidClientModel();
		}
		return clientModel;
	}

}
