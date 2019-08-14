package com.gallantrealm.myworld.android;

import com.gallantrealm.myworld.client.model.ClientModel;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;

/**
 * This activity can be subclassed for menus within a game. It provides controller support within the menus.
 */
public class GallantActivity extends Activity implements com.bda.controller.ControllerListener {

	public ClientModel clientModel = AndroidClientModel.getClientModel();
	public int songId = 0;

	/** Moga support. */
	public com.bda.controller.Controller mogaController;
	View currentFocusView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		fullscreen();
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
	    super.onWindowFocusChanged(hasFocus);
	    if (hasFocus) {
	    	fullscreen();
	    }
	}
	
	private void fullscreen() {
	    // Enables sticky immersive mode
	    View decorView = getWindow().getDecorView();
	    decorView.setSystemUiVisibility(
	            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
	            // Set the content to appear under the system bars so that the
	            // content doesn't resize when the system bars hide and show.
	            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
	            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
	            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
	            // Hide the nav bar and status bar
	            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
	            | View.SYSTEM_UI_FLAG_FULLSCREEN);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected void onStart() {
		super.onStart();
		clientModel.setContext(this);
		if (songId != 0) {
			clientModel.playSong(songId);
		}
		try {
			mogaController = com.bda.controller.Controller.getInstance(this);
			mogaController.init();
			mogaController.setListener(this, new Handler());
		} catch (Exception e) { // fails on Android 5.0
			System.err.println("You need to set android:targetSdkVersion=\"20\" for MOGA controller!");
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (songId != 0) {
			clientModel.stopSong();
		}
		mogaController.exit();
	}

	@Override
	protected void onPause() {
		super.onPause();
		mogaController.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		mogaController.onResume();
		clientModel.setContext(this);
	}

	// ------------------------
	// Mapping controllers to keyboard
	// ------------------------

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
//		System.out.println("Keycode: " + keyCode);
		if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_BUTTON_A || keyCode == KeyEvent.KEYCODE_BUTTON_SELECT) {
			View v = getWindow().getCurrentFocus();
			if (v instanceof Button || v instanceof CheckBox || v instanceof RadioButton) {
				v.performClick();
			} else {
				onOkay();
			}
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE || keyCode == KeyEvent.KEYCODE_BUTTON_B) {
			onCancel();
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_BUTTON_START) {
			onStartGame();
		} else if (keyCode == KeyEvent.KEYCODE_BUTTON_L1 || keyCode == KeyEvent.KEYCODE_BUTTON_L2) {
			onPrevious();
		} else if (keyCode == KeyEvent.KEYCODE_BUTTON_R1 || keyCode == KeyEvent.KEYCODE_BUTTON_R2) {
			onNext();
		}
		return false;
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
//		System.out.println("Keycode: " + keyCode);
		return false;
	}

	private void sendKey(final int keyCode) {
		new Thread() {
			@Override
			public void run() {
				try {
					Instrumentation inst = new Instrumentation();
					inst.sendKeyDownUpSync(keyCode);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
	}

	/**
	 * Override to start the game. Default launches 3D world.
	 */
	public void onStartGame() {
		clientModel.setCustomizeMode(false);
		Intent intent = new Intent(this, StartWorldActivity.class);
		startActivity(intent);
	}

	/**
	 * Override to provide behavior to show next item to select from in a list
	 */
	public void onNext() {

	}

	/**
	 * Override to provide behavior to show previous item to select from in a list
	 */
	public void onPrevious() {

	}

	/**
	 * Override to give a behavior on okay pressed. Default is to finish.
	 */
	public void onOkay() {
		finish();
	}

	/**
	 * Override to give a behavior on cancel pressed. Default is to finish.
	 */
	public void onCancel() {
		finish();
	}

	// ------------------------
	// MOGA support
	// ------------------------

	@Override
	public void onKeyEvent(com.bda.controller.KeyEvent event) {
// if (currentDialog != null) {
// currentDialog.dispatchKeyEvent(new KeyEvent(event.getAction(), event.getKeyCode()));
// } else {
// dispatchKeyEvent(new KeyEvent(event.getAction(), event.getKeyCode()));
// }
		if (event.getAction() == com.bda.controller.KeyEvent.ACTION_DOWN) {
			clientModel.usingMoga = true;
			sendKey(event.getKeyCode());
		}
	}

	@Override
	public void onMotionEvent(com.bda.controller.MotionEvent arg0) {
	}

	@Override
	public void onStateEvent(com.bda.controller.StateEvent arg0) {
	}

}
