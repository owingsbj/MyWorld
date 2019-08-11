package com.gallantrealm.myworld.android;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import com.gallantrealm.myworld.FastMath;
import com.gallantrealm.myworld.android.renderer.AndroidRenderer;
import com.gallantrealm.myworld.client.model.AlertListener;
import com.gallantrealm.myworld.client.model.ClientModel;
import com.gallantrealm.myworld.client.model.ClientModelChangedEvent;
import com.gallantrealm.myworld.client.model.ClientModelChangedListener;
import com.gallantrealm.myworld.client.model.InputResponseHandler;
import com.gallantrealm.myworld.client.model.SelectColorHandler;
import com.gallantrealm.myworld.client.model.SelectResponseHandler;
import com.gallantrealm.myworld.model.WWColor;
import com.gallantrealm.myworld.model.WWObject;
import com.gallantrealm.myworld.model.WWVector;
import com.gallantrealm.myworld.model.WWWorld;
import com.zeemote.zc.event.ButtonEvent;
import com.zeemote.zc.event.IButtonListener;
import com.zeemote.zc.event.IJoystickListener;
import com.zeemote.zc.event.JoystickEvent;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.Instrumentation;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import yuku.ambilwarna.AmbilWarnaDialog;
//import com.google.android.gms.ads.AdRequest;
//import com.google.android.gms.ads.AdSize;
//import com.google.android.gms.ads.AdView;
//import com.google.android.gms.common.ConnectionResult;
//import com.google.android.gms.common.GooglePlayServicesUtil;

public class ShowWorldActivity extends Activity implements OnTouchListener, ClientModelChangedListener, AlertListener, SensorEventListener, IButtonListener, IJoystickListener, com.bda.controller.ControllerListener {

	PowerManager.WakeLock wakelock;
	SensorManager sensorManager;
	Sensor orientationSensor;
	Sensor accelerometerSensor;
	Sensor magneticFieldSensor;

	private final ClientModel clientModel = AndroidClientModel.getClientModel();

	private TextView titleText;
	private TextView statusText;

	private GLSurfaceView worldView;
	private AndroidRenderer worldRenderer;

	private View worldActionsView;
	private View avatarActionsView;

	private Button rightActionButton1;
	private Button rightActionButton2;
	private Button rightActionButton3;
	private Button rightActionButton4;
	private Button rightActionButton5;
	private Button rightActionButton6;
	private Button rightActionButton7;

	private Button leftActionButton1;
	private Button leftActionButton2;
	private Button leftActionButton3;
	private Button leftActionButton4;
	private Button leftActionButton5;
	private Button leftActionButton6;
	private Button leftActionButton7;

	private ImageButton joyButton;
	private View joyThumb;
	private RelativeLayout.LayoutParams originalJoyButtonLayoutParams;

	private TextView flashMessageText;
	private TextView logText;

	private Dialog currentDialog;

	RerenderThread rerenderThread;

	Timer dPadTimer;
	TimerTask dPadTimerTask;

	/** Moga support. */
	public com.bda.controller.Controller mogaController;

	/** Called when the activity is first created. */
	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		System.out.println(">ShowWorldActivity.onCreate");
		super.onCreate(savedInstanceState);
		clientModel.setContext(this);
		setContentView(R.layout.show_world);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		this.setVolumeControlStream(AudioManager.STREAM_MUSIC);

		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wakelock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "My Tag");

		worldView = (GLSurfaceView) findViewById(R.id.worldView);
		if (clientModel.isSimpleRendering() || !clientModel.supportsOpenGLES20()) {
			System.out.println("Using old OpenGLES1.1");
			worldRenderer = AndroidRenderer.createAndroidRenderer(this, worldView, false);
		} else {
			// New GLES20 rendering (shader based)
			System.out.println("Using OpenGLES2.0");
			worldView.setEGLContextClientVersion(2);
			// Note: setEGLConfigChooser fails on different systems, no matter what I do. So going with defaults (usually 8,8,8,16 but not necessarily)
//			worldView.setEGLConfigChooser(new MyWorldConfigChooser());
//			worldView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
			worldRenderer = AndroidRenderer.createAndroidRenderer(this, worldView, true);
		}
		worldView.setRenderer(worldRenderer);
		worldRenderer.initializeCameraPosition();

		if (!pm.isScreenOn()) { // avoid rendering continuously if screen is off
			worldView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
		}

		clientModel.addClientModelChangedListener(this);
		clientModel.setAlertListener(this);

		titleText = (TextView) findViewById(R.id.titleText);
		statusText = (TextView) findViewById(R.id.statusText);

		worldActionsView = findViewById(R.id.worldActionsView);
		avatarActionsView = findViewById(R.id.avatarActionsView);

		rightActionButton1 = (Button) findViewById(R.id.rightActionButton1);
		rightActionButton2 = (Button) findViewById(R.id.rightActionButton2);
		rightActionButton3 = (Button) findViewById(R.id.rightActionButton3);
		rightActionButton4 = (Button) findViewById(R.id.rightActionButton4);
		rightActionButton5 = (Button) findViewById(R.id.rightActionButton5);
		rightActionButton6 = (Button) findViewById(R.id.rightActionButton6);
		rightActionButton7 = (Button) findViewById(R.id.rightActionButton7);
		leftActionButton1 = (Button) findViewById(R.id.leftActionButton1);
		leftActionButton2 = (Button) findViewById(R.id.leftActionButton2);
		leftActionButton3 = (Button) findViewById(R.id.leftActionButton3);
		leftActionButton4 = (Button) findViewById(R.id.leftActionButton4);
		leftActionButton5 = (Button) findViewById(R.id.leftActionButton5);
		leftActionButton6 = (Button) findViewById(R.id.leftActionButton6);
		leftActionButton7 = (Button) findViewById(R.id.leftActionButton7);
		joyButton = (ImageButton) findViewById(R.id.joyButton);
		joyThumb = findViewById(R.id.joyThumb);

		Typeface typeface = clientModel.getTypeface(this);
		if (typeface != null) {
			rightActionButton1.setTypeface(typeface);
			rightActionButton2.setTypeface(typeface);
			rightActionButton3.setTypeface(typeface);
			rightActionButton4.setTypeface(typeface);
			rightActionButton5.setTypeface(typeface);
			rightActionButton6.setTypeface(typeface);
			rightActionButton7.setTypeface(typeface);
			leftActionButton1.setTypeface(typeface);
			leftActionButton2.setTypeface(typeface);
			leftActionButton3.setTypeface(typeface);
			leftActionButton4.setTypeface(typeface);
			leftActionButton5.setTypeface(typeface);
			leftActionButton6.setTypeface(typeface);
			leftActionButton7.setTypeface(typeface);
		}

		flashMessageText = (TextView) findViewById(R.id.flashMessageText);
		logText = (TextView) findViewById(R.id.logText);

		titleText.setTypeface(typeface);
		statusText.setTypeface(typeface);
		flashMessageText.setTypeface(typeface);

		logText.setText("");
		clientModel.clearLog();

		worldView.setOnTouchListener(this);
		rightActionButton2.setOnTouchListener(this);
		rightActionButton1.setOnTouchListener(this);
		rightActionButton3.setOnTouchListener(this);
		rightActionButton4.setOnTouchListener(this);
		rightActionButton5.setOnTouchListener(this);
		rightActionButton6.setOnTouchListener(this);
		rightActionButton7.setOnTouchListener(this);
		leftActionButton2.setOnTouchListener(this);
		leftActionButton1.setOnTouchListener(this);
		leftActionButton3.setOnTouchListener(this);
		leftActionButton4.setOnTouchListener(this);
		leftActionButton5.setOnTouchListener(this);
		leftActionButton6.setOnTouchListener(this);
		leftActionButton7.setOnTouchListener(this);
		joyButton.setOnTouchListener(this);
		avatarActionsView.setOnTouchListener(this);
		worldActionsView.setOnTouchListener(this);

		System.out.println("<ShowWorldActivity.onCreate");
	}

	@Override
	protected void onStart() {
		System.out.println(">ShowWorldActivity.onStart");
		super.onStart();
		try {
			mogaController = com.bda.controller.Controller.getInstance(this);
			mogaController.init();
			mogaController.setListener(this, new Handler());
		} catch (Exception e) { // fails on android 5.0
			System.out.println(e);
		}

		clientModel.setContext(this);

		clientModel.loadPreferences(this);

		if (clientModel.useSensors()) {
			sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
			if (sensorManager != null) {
				// orientationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
				accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
				magneticFieldSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
			}
		}
		if (clientModel.useZeemote()) {
			clientModel.getZeeController().addButtonListener(this);
			clientModel.getZeeController().addJoystickListener(this);
		}

		updateAvatarActions(false);
		updateWorldActions(false);
		placeButtonsBasedOnPrefs();

		// if the layout changes, need to zero the thumb again
		((RelativeLayout) findViewById(R.id.mainLayout)).addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
			public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
				updateJoyThumb(0, 0);
			};
		});

		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		if (pm.isScreenOn()) { // don't resume rendering and physics unless screen is actually displayed

			worldView.onResume();

			if (clientModel.isLocalWorld()) {
				if (clientModel.getLocalServer() != null) {
					clientModel.getLocalServer().startServer(true);
				}
				clientModel.world.run();
			}

			// rerenderThread = new RerenderThread();
			// rerenderThread.start();

		}
		updateTitleAndStatus();
		clientModel.cameraToViewpoint();
		clientModel.forceAvatar(0, 0, 0, 0, 0, 0);
		wakelock.acquire();

		if (clientModel.world != null) {
			clientModel.world.displayed();
		}

		updateAvatarActions(false);
		updateLogText();

		System.out.println("<ShowWorldActivity.onStart");
	}

	private void updateWorldActions(final boolean restoring) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				worldActionsView.setVisibility(View.VISIBLE);
				boolean buttonShowing = false;
				Animation animation;
				if (restoring) {
					animation = new TranslateAnimation(-100, 0, 0, 0);
				} else {
					animation = new TranslateAnimation(100, 0, 0, 0);
				}
				animation.setDuration(250);
				worldActionsView.startAnimation(animation);
				if (clientModel.getWorldActionLabel(0) == null) {
					leftActionButton1.setVisibility(Button.GONE);
					clientModel.stopWorldAction(0);
				} else {
					leftActionButton1.setVisibility(Button.VISIBLE);
					leftActionButton1.setText(clientModel.getWorldActionLabel(0));
					buttonShowing = true;
				}
				if (clientModel.getWorldActionLabel(1) == null) {
					leftActionButton2.setVisibility(Button.GONE);
					clientModel.stopWorldAction(1);
				} else {
					leftActionButton2.setVisibility(Button.VISIBLE);
					leftActionButton2.setText(clientModel.getWorldActionLabel(1));
					buttonShowing = true;
				}
				if (clientModel.getWorldActionLabel(2) == null) {
					leftActionButton3.setVisibility(Button.GONE);
					clientModel.stopWorldAction(2);
				} else {
					leftActionButton3.setVisibility(Button.VISIBLE);
					leftActionButton3.setText(clientModel.getWorldActionLabel(2));
					buttonShowing = true;
				}
				if (clientModel.getWorldActionLabel(3) == null) {
					leftActionButton4.setVisibility(Button.GONE);
					clientModel.stopWorldAction(3);
				} else {
					leftActionButton4.setVisibility(Button.VISIBLE);
					leftActionButton4.setText(clientModel.getWorldActionLabel(3));
					buttonShowing = true;
				}
				if (clientModel.getWorldActionLabel(4) == null) {
					leftActionButton5.setVisibility(Button.GONE);
					clientModel.stopWorldAction(4);
				} else {
					leftActionButton5.setVisibility(Button.VISIBLE);
					leftActionButton5.setText(clientModel.getWorldActionLabel(4));
					buttonShowing = true;
				}
				if (clientModel.getWorldActionLabel(5) == null) {
					leftActionButton6.setVisibility(Button.GONE);
					clientModel.stopWorldAction(5);
				} else {
					leftActionButton6.setVisibility(Button.VISIBLE);
					leftActionButton6.setText(clientModel.getWorldActionLabel(5));
					buttonShowing = true;
				}
				if (clientModel.getWorldActionLabel(6) == null) {
					leftActionButton7.setVisibility(Button.GONE);
					clientModel.stopWorldAction(6);
				} else {
					leftActionButton7.setVisibility(Button.VISIBLE);
					leftActionButton7.setText(clientModel.getWorldActionLabel(6));
					buttonShowing = true;
				}
				if (!buttonShowing) {
					worldActionsView.setVisibility(View.GONE);
				}
			}
		});
	}

	private void updateAvatarActions(final boolean restoring) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				avatarActionsView.setVisibility(View.VISIBLE);
				boolean buttonShowing = false;
				Animation animation;
				if (restoring) {
					animation = new TranslateAnimation(-100, 0, 0, 0);
				} else {
					animation = new TranslateAnimation(100, 0, 0, 0);
				}
				animation.setDuration(250);
				avatarActionsView.startAnimation(animation);
				if (clientModel.getAvatarActionLabel(0) == null) {
					rightActionButton1.setVisibility(Button.GONE);
					clientModel.stopAvatarAction(0);
				} else {
					rightActionButton1.setVisibility(Button.VISIBLE);
					setButtonLabel(rightActionButton1, clientModel.getAvatarActionLabel(0));
					buttonShowing = true;
				}
				if (clientModel.getAvatarActionLabel(1) == null) {
					rightActionButton2.setVisibility(Button.GONE);
					clientModel.stopAvatarAction(1);
				} else {
					rightActionButton2.setVisibility(Button.VISIBLE);
					setButtonLabel(rightActionButton2, clientModel.getAvatarActionLabel(1));
					buttonShowing = true;
				}
				if (clientModel.getAvatarActionLabel(2) == null) {
					rightActionButton3.setVisibility(Button.GONE);
					clientModel.stopAvatarAction(2);
				} else {
					rightActionButton3.setVisibility(Button.VISIBLE);
					setButtonLabel(rightActionButton3, clientModel.getAvatarActionLabel(2));
					buttonShowing = true;
				}
				if (clientModel.getAvatarActionLabel(3) == null) {
					rightActionButton4.setVisibility(Button.GONE);
					clientModel.stopAvatarAction(3);
				} else {
					rightActionButton4.setVisibility(Button.VISIBLE);
					setButtonLabel(rightActionButton4, clientModel.getAvatarActionLabel(3));
					buttonShowing = true;
				}
				if (clientModel.getAvatarActionLabel(4) == null) {
					rightActionButton5.setVisibility(Button.GONE);
					clientModel.stopAvatarAction(4);
				} else {
					rightActionButton5.setVisibility(Button.VISIBLE);
					setButtonLabel(rightActionButton5, clientModel.getAvatarActionLabel(4));
					buttonShowing = true;
				}
				if (clientModel.getAvatarActionLabel(5) == null) {
					rightActionButton6.setVisibility(Button.GONE);
					clientModel.stopAvatarAction(5);
				} else {
					rightActionButton6.setVisibility(Button.VISIBLE);
					setButtonLabel(rightActionButton6, clientModel.getAvatarActionLabel(5));
					buttonShowing = true;
				}
				if (clientModel.getAvatarActionLabel(6) == null) {
					rightActionButton7.setVisibility(Button.GONE);
					clientModel.stopAvatarAction(6);
				} else {
					rightActionButton7.setVisibility(Button.VISIBLE);
					setButtonLabel(rightActionButton7, clientModel.getAvatarActionLabel(6));
					buttonShowing = true;
				}
				if (!buttonShowing) {
					avatarActionsView.setVisibility(View.GONE);
				}
			}
		});
	}

	private void setButtonLabel(Button button, String label) {
		if (label.contains(":")) {
			int level = Integer.parseInt(label.substring(0, 1));
			button.getBackground().setLevel(level);
			button.setText(label.substring(2));
			button.invalidate();
		} else {
			button.setText(label);
		}
	}

	private void placeButtonsBasedOnPrefs() {
		if (clientModel == null || clientModel.world == null) {
			return;
		}
		runOnUiThread(new Runnable() {
			@SuppressLint("NewApi")
			@Override
			public void run() {
				if (originalJoyButtonLayoutParams == null) {
					originalJoyButtonLayoutParams = (RelativeLayout.LayoutParams) joyButton.getLayoutParams();
				}
				if (clientModel.world.usesController() && clientModel.useScreenControl()) {
					System.out.println("Using JoyButton");
					joyButton.setVisibility(Button.VISIBLE);
					joyThumb.setVisibility(View.VISIBLE);
					try {
						if (clientModel.isControlOnLeft()) {
							RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(originalJoyButtonLayoutParams);
							params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);
							params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
							params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
							joyButton.setLayoutParams(params);
							params = new RelativeLayout.LayoutParams(avatarActionsView.getLayoutParams());
							params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
							params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 0);
							params.addRule(RelativeLayout.BELOW, R.id.titleText);
							avatarActionsView.setLayoutParams(params);
							params = new RelativeLayout.LayoutParams(worldActionsView.getLayoutParams());
							params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);
							params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
							params.addRule(RelativeLayout.BELOW, R.id.titleText);
							worldActionsView.setLayoutParams(params);
						} else {
							RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(originalJoyButtonLayoutParams);
							params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
							params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 0);
							params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
							joyButton.setLayoutParams(params);
							params = new RelativeLayout.LayoutParams(avatarActionsView.getLayoutParams());
							params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);
							params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
							params.addRule(RelativeLayout.BELOW, R.id.titleText);
							avatarActionsView.setLayoutParams(params);
							params = new RelativeLayout.LayoutParams(worldActionsView.getLayoutParams());
							params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
							params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 0);
							params.addRule(RelativeLayout.BELOW, R.id.titleText);
							worldActionsView.setLayoutParams(params);
						}
					} catch (Throwable e) {
						// fails on some, due to NoSuchMethodError: android.widget.RelativeLayout$LayoutParams.<init>
					}
					joyThumb.bringToFront();
					joyButton.bringToFront();
				} else {
					System.out.println("Not using JoyButton");
					joyButton.setVisibility(Button.GONE);
					joyThumb.setVisibility(View.GONE);
					RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(avatarActionsView.getLayoutParams());
					params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
					params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 0);
					params.addRule(RelativeLayout.BELOW, R.id.titleText);
					avatarActionsView.setLayoutParams(params);
					params = new RelativeLayout.LayoutParams(worldActionsView.getLayoutParams());
					params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);
					params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
					params.addRule(RelativeLayout.BELOW, R.id.titleText);
					worldActionsView.setLayoutParams(params);
				}
			}
		});
	}

	/**
	 * Move the thumb in the joy button to reflect the controller position.
	 */
	private void updateJoyThumb(final int x, final int y) {
		runOnUiThread(new Runnable() {
			public void run() {
				int thumbSize = joyThumb.getWidth();
				int centerX = (joyButton.getWidth() - thumbSize) / 2;
				int centerY = (joyButton.getHeight() - thumbSize) / 2;
				float xRange = (joyButton.getWidth() - thumbSize) / 100.0f * FastMath.cos(y / 200.0f * FastMath.PI);
				float yRange = (joyButton.getHeight() - thumbSize) / 100.0f * FastMath.cos(x / 200.0f * FastMath.PI);
				joyThumb.setLeft((int) (joyButton.getLeft() + centerX - x * xRange));
				joyThumb.setRight((int) (joyButton.getLeft() + centerX - x * xRange + thumbSize));
				joyThumb.setTop((int) (joyButton.getTop() + centerY - y * yRange));
				joyThumb.setBottom((int) (joyButton.getTop() + centerY - y * yRange + thumbSize));
			}
		});
	}

	@Override
	protected void onResume() {
		System.out.println(">ShowWorldActivity.onResume");
		clientModel.setContext(this);

		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		if (pm.isScreenOn()) { // don't actually resume unless screen is displayed

			clientModel.flashMessage(" ", false);

			// clientModel.initializeCameraPosition();
			// worldRenderer.initializeCameraPosition();
			// bjo worldRenderer.clearTextureCache();
			// bjo worldRenderer.clearRenderings();
			worldRenderer.getSoundGenerator().resume();
			if (sensorManager != null && clientModel.useSensors()) {
				System.out.println("Registering acceleromentor listener");
				sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
				// sensorManager.registerListener(this, magneticFieldSensor, SensorManager.SENSOR_DELAY_GAME);
			}
			// if (clientModel.useDPad()) {
			dPadTimerTask = new DPadTimerTask();
			dPadTimer = new Timer();
			dPadTimer.schedule(dPadTimerTask, 50, 50); // same time as sensor delay game (40ms)
			// }
		}
		super.onResume();
		mogaController.onResume();
		System.out.println("<ShowWorldActivity.onResume");
	}

	@Override
	protected void onPause() {
		System.out.println(">ShowWorldActivity.onPause");
		super.onPause();
		if (sensorManager != null && clientModel.useSensors()) {
			sensorManager.unregisterListener(this);
		}
		if (dPadTimer != null) {
			dPadTimer.cancel();
			dPadTimer = null;
			dPadTimerTask.cancel();
			dPadTimerTask = null;
		}
		mogaController.onPause();

		worldRenderer.getSoundGenerator().pause();
		System.out.println("<ShowWorldActivity.onPause");
	}

	@Override
	protected void onStop() {
		System.out.println(">ShowWorldActivity.onStop");
		super.onStop();
		wakelock.release();
		worldView.onPause();
		if (rerenderThread != null) {
			rerenderThread.safeStop();
		}
		mogaController.exit();
		if (clientModel.useZeemote()) {
			try {
				clientModel.getZeeController().removeButtonListener(this);
				clientModel.getZeeController().removeJoystickListener(this);
				clientModel.getZeeController().disconnect();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
		if (clientModel.isLocalWorld()) {
			System.out.println("onStop -- is local world");
			if (clientModel.world != null) {
				System.out.println("onStop -- pausing the local world");
				clientModel.world.pause();
			}
			if (clientModel.getLocalServer() != null) {
				clientModel.getLocalServer().stopServer();
			}
		}
		clientModel.savePreferences(this);

		worldRenderer.getSoundGenerator().stop();

		System.out.println("<ShowWorldActivity.onStop");
	}

	@Override
	protected void onDestroy() {
		System.out.println(">ShowWorldActivity.onDestroy");
		worldRenderer.getSoundGenerator().destroy();
		super.onDestroy();
		System.out.println("<ShowWorldActivity.onDestroy");
	}

	private boolean moving;
	private int movingPointerId;
	private boolean pinching;
	private boolean dragging;
	private boolean zooming;
	private float startingX;
	private float startingY;
	private float startingPinchX;
	private float startingPinchY;
	private float startingPinchSize;
	private float startingCameraPan;
	private float startingCameraTilt;
	private float startingCameraDistance;
	private WWObject lastPressedObject;
	private WWVector initialPressedObjectPosition;
	private boolean movingObject;

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		try {
			int i = event.getActionIndex();
			float x = event.getX(i);
			float y = event.getY(i);
			int location[] = new int[2];
			v.getLocationOnScreen(location);
			x = x + location[0];
			y = y + location[1];

			if (event.getActionMasked() == MotionEvent.ACTION_DOWN || event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {

				if (isPointInsideView(x, y, joyButton) && event.getActionMasked() != MotionEvent.ACTION_POINTER_DOWN) { // respect joy button only on first finger
					int[] buttonLocation = new int[2];
					joyButton.getLocationOnScreen(buttonLocation);
					float deltaX = FastMath.min(50, FastMath.max(-50, 100.0f * (buttonLocation[0] + joyButton.getWidth() / 2 - x) / joyButton.getWidth()));
					float deltaY = FastMath.min(50, FastMath.max(-50, 100.0f * (buttonLocation[1] + joyButton.getHeight() / 2 - y) / joyButton.getHeight()));
					controller(deltaX, deltaY);
					moving = true;
					movingPointerId = event.getPointerId(i);
				} else if (isPointInsideView(x, y, rightActionButton1)) {
					clientModel.startAvatarAction(0, getViewX(x, rightActionButton1), getViewY(y, rightActionButton1));
				} else if (isPointInsideView(x, y, rightActionButton2)) {
					clientModel.startAvatarAction(1, getViewX(x, rightActionButton2), getViewY(y, rightActionButton2));
				} else if (isPointInsideView(x, y, rightActionButton3)) {
					clientModel.startAvatarAction(2, getViewX(x, rightActionButton3), getViewY(y, rightActionButton3));
				} else if (isPointInsideView(x, y, rightActionButton4)) {
					clientModel.startAvatarAction(3, getViewX(x, rightActionButton4), getViewY(y, rightActionButton4));
				} else if (isPointInsideView(x, y, rightActionButton5)) {
					clientModel.startAvatarAction(4, getViewX(x, rightActionButton5), getViewY(y, rightActionButton5));
				} else if (isPointInsideView(x, y, rightActionButton6)) {
					clientModel.startAvatarAction(5, getViewX(x, rightActionButton6), getViewY(y, rightActionButton6));
				} else if (isPointInsideView(x, y, rightActionButton7)) {
					clientModel.startAvatarAction(6, getViewX(x, rightActionButton7), getViewY(y, rightActionButton7));
				} else if (isPointInsideView(x, y, leftActionButton1)) {
					clientModel.startWorldAction(0);
				} else if (isPointInsideView(x, y, leftActionButton2)) {
					clientModel.startWorldAction(1);
				} else if (isPointInsideView(x, y, leftActionButton3)) {
					clientModel.startWorldAction(2);
				} else if (isPointInsideView(x, y, leftActionButton4)) {
					clientModel.startWorldAction(3);
				} else if (isPointInsideView(x, y, leftActionButton5)) {
					clientModel.startWorldAction(4);
				} else if (isPointInsideView(x, y, leftActionButton6)) {
					clientModel.startWorldAction(5);
				} else if (isPointInsideView(x, y, leftActionButton7)) {
					clientModel.startWorldAction(6);
				} else if (isPointInsideView(x, y, worldView)) {
					if (getViewX(x, worldView) < 0.5f) {
						clientModel.startWorldAction(7);
					} else {
						clientModel.startAvatarAction(7, getViewX(x, worldView), getViewY(y, worldView));
					}
					if (clientModel.world.allowPicking()) {
						WWObject pickedObject = worldRenderer.waitForPickingDraw(null, (int) (event.getX(i)), (int) (worldView.getHeight() - event.getY(i)));
						if (pickedObject != null) {
							clientModel.world.pressObject(pickedObject, worldRenderer.pickedSide, worldRenderer.pickedOffsetX, worldRenderer.pickedOffsetY, clientModel.world.getUser(clientModel.getUserId()));
							initialPressedObjectPosition = pickedObject.getPosition();
						}
						lastPressedObject = pickedObject;
					}
					dragging = false;
					if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
						if ((event.getMetaState() & KeyEvent.META_CTRL_ON) != 0) {
							pinching = true;
							startingPinchX = x;
							startingPinchY = y;
						} else {
							pinching = false;
							startingX = x;
							startingY = y;
						}
					} else if (event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {
						pinching = true;
						startingPinchX = x;
						startingPinchY = y;
					}
				}

			} else if ((event.getActionMasked() == MotionEvent.ACTION_UP || event.getActionMasked() == MotionEvent.ACTION_POINTER_UP || event.getActionMasked() == MotionEvent.ACTION_CANCEL)) {

				if (moving && movingPointerId == event.getPointerId(i)) {
					controller(0, 0);
					moving = false;
				} else if (isPointInsideView(x, y, rightActionButton1)) {
					clientModel.stopAvatarAction(0);
				} else if (isPointInsideView(x, y, rightActionButton2)) {
					clientModel.stopAvatarAction(1);
				} else if (isPointInsideView(x, y, rightActionButton3)) {
					clientModel.stopAvatarAction(2);
				} else if (isPointInsideView(x, y, rightActionButton4)) {
					clientModel.stopAvatarAction(3);
				} else if (isPointInsideView(x, y, rightActionButton5)) {
					clientModel.stopAvatarAction(4);
				} else if (isPointInsideView(x, y, rightActionButton6)) {
					clientModel.stopAvatarAction(5);
				} else if (isPointInsideView(x, y, rightActionButton7)) {
					clientModel.stopAvatarAction(6);
				} else if (isPointInsideView(x, y, leftActionButton1)) {
					clientModel.stopWorldAction(0);
				} else if (isPointInsideView(x, y, leftActionButton2)) {
					clientModel.stopWorldAction(1);
				} else if (isPointInsideView(x, y, leftActionButton3)) {
					clientModel.stopWorldAction(2);
				} else if (isPointInsideView(x, y, leftActionButton4)) {
					clientModel.stopWorldAction(3);
				} else if (isPointInsideView(x, y, leftActionButton5)) {
					clientModel.stopWorldAction(4);
				} else if (isPointInsideView(x, y, leftActionButton6)) {
					clientModel.stopWorldAction(5);
				} else if (isPointInsideView(x, y, leftActionButton7)) {
					clientModel.stopWorldAction(6);
				} else if (isPointInsideView(x, y, worldView)) {
					if (getViewX(x, worldView) < 0.5f) {
						clientModel.stopWorldAction(7);
					} else {
						clientModel.stopAvatarAction(7);
					}
					if (dragging || pinching || zooming) {
						pinching = false;
						if (event.getActionMasked() == MotionEvent.ACTION_UP) {
							zooming = false;
						}
						if (clientModel.world.allowPicking()) {
							WWObject pickedObject = worldRenderer.waitForPickingDraw(null, (int) (event.getX(i)), (int) (worldView.getHeight() - event.getY(i)));
							if (pickedObject != null) {
								clientModel.world.releaseObject(pickedObject, worldRenderer.pickedSide, worldRenderer.pickedOffsetX, worldRenderer.pickedOffsetY, clientModel.world.getUser(clientModel.getUserId()));
							}
						}
					} else {
						if (movingObject) {
							clientModel.world.doneMovingObject(lastPressedObject);
							movingObject = false;
						} else if (clientModel.world.allowPicking()) {
							WWObject pickedObject = worldRenderer.waitForPickingDraw(null, (int) (event.getX(i)), (int) (worldView.getHeight() - event.getY(i)));
							if (pickedObject != null) {
								if (pickedObject.isPickable()) {
									WWObject previouslyPickedObject = clientModel.getSelectedObject();
									clientModel.setSelectedObject(pickedObject);
									WWObject avatar = clientModel.getAvatar();
									if (avatar != null) {
										if (avatar == previouslyPickedObject && avatar != pickedObject) {
											clientModel.setCameraObject(pickedObject);
											clientModel.setCameraDistance(FastMath.max(clientModel.getCameraDistance(), pickedObject.extent * 4));
											clientModel.setCameraPan(clientModel.getCameraPan() + avatar.getRotation(clientModel.world.getWorldTime()).getZ());
										} else if (avatar != previouslyPickedObject && avatar == pickedObject) {
											clientModel.setCameraObject(pickedObject);
											clientModel.setCameraDistance(FastMath.max(clientModel.getCameraDistance(), pickedObject.extent * 4));
											clientModel.setCameraPan(clientModel.getCameraPan() - avatar.getRotation(clientModel.world.getWorldTime()).getZ());
										}
									}
								}
								clientModel.world.touchObject(pickedObject, worldRenderer.pickedSide, worldRenderer.pickedOffsetX, worldRenderer.pickedOffsetY, clientModel.world.getUser(clientModel.getUserId()));
							}
						}
					}
				}

			} else if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {

				if (moving && movingPointerId == event.getPointerId(i)) {
					int[] buttonLocation = new int[2];
					joyButton.getLocationOnScreen(buttonLocation);
					float deltaX = FastMath.min(50, FastMath.max(-50, 100.0f * (buttonLocation[0] + joyButton.getWidth() / 2 - x) / joyButton.getWidth()));
					float deltaY = FastMath.min(50, FastMath.max(-50, 100.0f * (buttonLocation[1] + joyButton.getHeight() / 2 - y) / joyButton.getHeight()));
					controller(deltaX, deltaY);
				} else if (isPointInsideView(x, y, rightActionButton1)) {
					// nothing
				} else if (isPointInsideView(x, y, rightActionButton2)) {
					// nothing
				} else if (isPointInsideView(x, y, rightActionButton3)) {
					// nothing
				} else if (isPointInsideView(x, y, rightActionButton4)) {
					// nothing
				} else if (isPointInsideView(x, y, rightActionButton5)) {
					// nothing
				} else if (isPointInsideView(x, y, rightActionButton6)) {
					// nothing
				} else if (isPointInsideView(x, y, rightActionButton7)) {
					// nothing
				} else if (isPointInsideView(x, y, leftActionButton1)) {
					// nothing
				} else if (isPointInsideView(x, y, leftActionButton2)) {
					// nothing
				} else if (isPointInsideView(x, y, leftActionButton3)) {
					// nothing
				} else if (isPointInsideView(x, y, leftActionButton4)) {
					// nothing
				} else if (isPointInsideView(x, y, leftActionButton5)) {
					// nothing
				} else if (isPointInsideView(x, y, leftActionButton6)) {
					// nothing
				} else if (isPointInsideView(x, y, leftActionButton7)) {
					// nothing
				} else if (isPointInsideView(x, y, worldView)) {
					if (clientModel.world.allowPicking()) {
						WWObject pickedObject = worldRenderer.waitForPickingDraw(null, (int) (event.getX(i)), (int) (worldView.getHeight() - event.getY(i)));
						if (pickedObject != null) {
							// deactivated as it causes issues with zooming/panning
							// if (pickedObject.isPickable()) {
							// clientModel.setSelectedObject(pickedObject);
							// WWObject avatar = clientModel.getAvatar();
							// if (avatar != null && clientModel.getCameraObject() == avatar && pickedObject != avatar) {
							// clientModel.setCameraPan(avatar.getRotation(clientModel.world.getWorldTime()).getZ());
							// }
							// clientModel.setCameraObject(pickedObject);
							// clientModel.setCameraDistance(pickedObject.extent * 4);
							// }
							clientModel.world.dragObject(pickedObject, worldRenderer.pickedSide, worldRenderer.pickedOffsetX, worldRenderer.pickedOffsetY, clientModel.world.getUser(clientModel.getUserId()));
						}
					}
					clientModel.world.touch((x - worldView.getLeft() - (worldView.getWidth() - worldView.getHeight()) / 2) / worldView.getHeight(), (y - worldView.getTop()) / worldView.getHeight());
					if (!pinching && !zooming && clientModel.world.allowObjectMoving() && lastPressedObject == clientModel.getSelectedObject()) {
						WWObject newObject = (WWObject) lastPressedObject.clone();
						WWVector position = initialPressedObjectPosition;
						float deltaX = x - startingX;
						float deltaY = y - startingY;
						deltaX *= clientModel.cameraDistance / 10000.0;
						deltaY *= clientModel.cameraDistance / 10000.0;
						if (clientModel.cameraTilt > 45) {
							position.x += deltaX * FastMath.cos(clientModel.cameraPan * FastMath.TORADIAN) + deltaY * FastMath.sin(clientModel.cameraPan * FastMath.TORADIAN);
							position.y += -deltaX * FastMath.sin(clientModel.cameraPan * FastMath.TORADIAN) + deltaY * FastMath.cos(clientModel.cameraPan * FastMath.TORADIAN);
						} else {
							if (Math.abs(FastMath.sin(clientModel.cameraPan * FastMath.TORADIAN)) > 0.5f) {
								position.y -= deltaX * FastMath.sin(clientModel.cameraPan * FastMath.TORADIAN);
								position.z -= deltaY * FastMath.cos(clientModel.cameraTilt * FastMath.TORADIAN);
							} else {
								position.x += deltaX * FastMath.cos(clientModel.cameraPan * FastMath.TORADIAN);
								position.z -= deltaY * FastMath.cos(clientModel.cameraTilt * FastMath.TORADIAN);
							}
						}
						newObject.setPosition(position);
						clientModel.world.moveObject(lastPressedObject.getId(), newObject);
						movingObject = true;
					} else if (clientModel.world.allowCameraPositioning()) {
						if (pinching) {
							if (!zooming) {
								startingCameraDistance = clientModel.getCameraDistance();
								startingPinchSize = (float) Math.sqrt((startingX - startingPinchX) * (startingX - startingPinchX) + (startingY - startingPinchY) * (startingY - startingPinchY));
								zooming = true;
							}
							float pinchSize = startingPinchSize;
							if (event.getPointerCount() < 2) { // use only the one finger
								startingPinchSize = (float) Math.sqrt((startingPinchX - event.getX(0)) * (startingPinchX - event.getX(0)) + (startingPinchY - event.getY(0)) * (startingPinchY - event.getY(0)));
							} else { // 2 fingers
								pinchSize = (float) Math.sqrt((event.getX(0) - event.getX(1)) * (event.getX(0) - event.getX(1)) + (event.getY(0) - event.getY(1)) * (event.getY(0) - event.getY(1)));
							}
							float newCameraDistance = startingCameraDistance / pinchSize * startingPinchSize;
							clientModel.setCameraDistance(newCameraDistance);
						} else {
							int moveDistance = (int) Math.sqrt((startingX - x) * (startingX - x) + (startingY - y) * (startingY - y));
							if (moveDistance > 10) {
								if (!zooming) {
									if (!dragging) {
										startingCameraPan = clientModel.getCameraPan();
										startingCameraTilt = clientModel.getCameraTilt();
										dragging = true;
									}
									clientModel.setCameraPan(startingCameraPan + (startingX - x) / 2.5f);
									clientModel.setCameraTilt(Math.max(startingCameraTilt - (startingY - y) / 2.5f, -30.0f));
								}
							}
						}
					}
				}
			}
		} catch (Exception e) { // reported by customers, better to ignore than crash
			e.printStackTrace();
		}
		return true;
	}

	/**
	 * Zeemote button pressed
	 */
	@Override
	public void buttonPressed(ButtonEvent buttonEvent) {
		if (hasWindowFocus()) {
			if (buttonEvent.getButtonGameAction() == ButtonEvent.BUTTON_A) {
				clientModel.startAvatarAction(0, 0, 0);
			} else if (buttonEvent.getButtonGameAction() == ButtonEvent.BUTTON_B) {
				clientModel.startAvatarAction(1, 0, 0);
			} else if (buttonEvent.getButtonGameAction() == ButtonEvent.BUTTON_C) {
				clientModel.startWorldAction(1);
			} else if (buttonEvent.getButtonGameAction() == ButtonEvent.BUTTON_D) {
				clientModel.startWorldAction(0);
			}
		}
	}

	/**
	 * Zeemote button release
	 */
	@Override
	public void buttonReleased(ButtonEvent buttonEvent) {
		if (buttonEvent.getButtonGameAction() == ButtonEvent.BUTTON_A) {
			clientModel.stopAvatarAction(0);
		} else if (buttonEvent.getButtonGameAction() == ButtonEvent.BUTTON_B) {
			clientModel.stopAvatarAction(1);
		} else if (buttonEvent.getButtonGameAction() == ButtonEvent.BUTTON_C) {
			clientModel.stopWorldAction(1);
		} else if (buttonEvent.getButtonGameAction() == ButtonEvent.BUTTON_D) {
			clientModel.stopWorldAction(0);
		}
	}

	/**
	 * Zeemote joystick move
	 */
	@Override
	public void joystickMoved(JoystickEvent joystickEvent) {
		controller(-joystickEvent.getScaledX(-50, 50), -joystickEvent.getScaledY(-50, 50));
	}

	/**
	 * All forms of avatar control funnel into this method.
	 * 
	 * @param deltaX the movement in the x dimension, from -50 to 50
	 * @param deltaY the movement in the y dimension, from -50 to 50
	 */
	public void controller(float deltaX, float deltaY) {
		updateJoyThumb((int) deltaX, (int) deltaY);
		WWWorld world = clientModel.world;
		if (world == null || !world.isRunning()) {
			return;
		}
		deltaX *= (0.25f + clientModel.getControlSensitivity());
		if (world.controller(deltaX, deltaY)) { // a custom controller returns true
			return;
		}
		float xrange = 25.0f;
		float yrange = 25.0f;
		float thrust = clientModel.getAvatarThrust();
		float turn = clientModel.getAvatarTurn();
		float lift = clientModel.getAvatarLift();
		float tilt = clientModel.getAvatarTilt();
		float lean = clientModel.getAvatarLean();
		float slide = clientModel.getAvatarSlide();
		if (world.getMoveXType() == WWWorld.MOVE_TYPE_TURN) {
			float[] moveX = world.getMoveXTurn();
			float deltaXtrunced = Math.min(Math.max(deltaX, -xrange), xrange);
			int deltaXindex = (int) ((deltaXtrunced + xrange) * moveX.length / (xrange * 2 + 1));
			turn = moveX[deltaXindex];
		} else {
			turn = clientModel.getAvatarTurn();
		}
		if (world.getMoveXType() == WWWorld.MOVE_TYPE_LEAN) {
			float[] moveX = world.getMoveXLean();
			float deltaXtrunced = Math.min(Math.max(deltaX, -xrange), xrange);
			int deltaXindex = (int) ((deltaXtrunced + xrange) * moveX.length / (xrange * 2 + 1));
			lean = moveX[deltaXindex];
		} else {
			lean = clientModel.getAvatarLean();
		}
		if (world.getMoveXType() == WWWorld.MOVE_TYPE_SLIDE) {
			float[] moveX = world.getMoveXSlide();
			float deltaXtrunced = Math.min(Math.max(deltaX, -xrange), xrange);
			int deltaXindex = (int) ((deltaXtrunced + xrange) * moveX.length / (xrange * 2 + 1));
			slide = moveX[deltaXindex];
		} else {
			slide = clientModel.getAvatarSlide();
		}
		if (world.getMoveYType() == WWWorld.MOVE_TYPE_TILT) {
			float[] moveY = world.getMoveYTilt();
			float deltaYtrunced = Math.min(Math.max(deltaY, -yrange), yrange);
			int deltaYindex = (int) ((deltaYtrunced + yrange) * moveY.length / (yrange * 2 + 1));
			tilt = moveY[deltaYindex];
		} else {
			tilt = clientModel.getAvatarTilt();
		}
		if (world.getMoveYType() == WWWorld.MOVE_TYPE_THRUST) {
			float[] moveY = world.getMoveYThrust();
			float deltaYtrunced = Math.min(Math.max(deltaY, -yrange), yrange);
			int deltaYindex = (int) ((deltaYtrunced + yrange) * moveY.length / (yrange * 2 + 1));
			thrust = moveY[deltaYindex];
		} else {
			thrust = clientModel.getAvatarThrust();
		}
		if (world.getMoveYType() == WWWorld.MOVE_TYPE_LIFT) {
			float[] moveY = world.getMoveYThrust();
			float deltaYtrunced = Math.min(Math.max(deltaY, -yrange), yrange);
			int deltaYindex = (int) ((deltaYtrunced + yrange) * moveY.length / (yrange * 2 + 1));
			lift = moveY[deltaYindex];
		} else {
			lift = clientModel.getAvatarLift();
		}
		clientModel.forceAvatar(thrust, turn, lift, tilt, lean, slide);
	}

	/**
	 * Determines if given points are inside view
	 * 
	 * @param x    - x coordinate of point
	 * @param y    - y coordinate of point
	 * @param view - view object to compare
	 * @return true if the points are within view bounds, false otherwise
	 */
	private boolean isPointInsideView(float x, float y, View view) {
		if (view.getVisibility() == View.VISIBLE) {
			int location[] = new int[2];
			view.getLocationOnScreen(location);
			int viewX = location[0];
			int viewY = location[1];

			// point is inside view bounds
			if ((x > viewX && x < (viewX + view.getWidth())) && (y > viewY && y < (viewY + view.getHeight()))) {
				return true;
			}
		}
		return false;
	}

	private float getViewX(float x, View view) {
		int location[] = new int[2];
		view.getLocationOnScreen(location);
		int viewX = location[0];
		int viewY = location[1];

		return (x - viewX) / view.getWidth();
	}

	private float getViewY(float y, View view) {
		int location[] = new int[2];
		view.getLocationOnScreen(location);
		int viewX = location[0];
		int viewY = location[1];

		return (y - viewY) / view.getHeight();
	}

	@Override
	public void clientModelChanged(ClientModelChangedEvent event) {
		if (event.getEventType() == ClientModelChangedEvent.EVENT_TYPE_CONNECTED) {
			// createJava3dRendering();
			// MyWorldCanvas.this.requestFocus(); // so keyboard will immediately move avatar
		} else if (event.getEventType() == ClientModelChangedEvent.EVENT_TYPE_DISCONNECTED) {
			MainMenuActivity.showPopupAd = true;
			this.finish();
		} else if (event.getEventType() == ClientModelChangedEvent.EVENT_TYPE_FIELD_OF_VIEW_CHANGED) {
			// getView().setFieldOfView(Math.toRadians(clientModel.getFieldOfView()));
		} else if (event.getEventType() == ClientModelChangedEvent.EVENT_TYPE_REFRESH_RATE_CHANGED) {
			// universe.getViewer().getView().setMinimumFrameCycleTime(clientModel.getRefreshRate());
		} else if (event.getEventType() == ClientModelChangedEvent.EVENT_TYPE_ANTIALIAS_CHANGED) {
			// universe.getViewer().getView().setSceneAntialiasingEnable(clientModel.getAntialias());
		} else if (event.getEventType() == ClientModelChangedEvent.EVENT_TYPE_WWMODEL_UPDATED) {
			updateTitleAndStatus();
		} else if (event.getEventType() == ClientModelChangedEvent.EVENT_TYPE_MESSAGE_RECEIVED) {
			System.out.println(clientModel.getLastMessageReceived());
		} else if (event.getEventType() == ClientModelChangedEvent.EVENT_TYPE_MESSAGE_FLASHED) {
			Thread flashThread = new Thread() {
				@Override
				public void run() {
					try {
						ShowWorldActivity.this.setFlashMessage(clientModel.getFlashMessage());
						if (clientModel.isFlashMessageBlink()) {
							sleep(750);
							ShowWorldActivity.this.setFlashMessage(" ");
							sleep(250);
							ShowWorldActivity.this.setFlashMessage(clientModel.getFlashMessage());
							sleep(750);
							ShowWorldActivity.this.setFlashMessage(" ");
						} else {
							sleep(2500);
							ShowWorldActivity.this.setFlashMessage(" ");
						}
					} catch (InterruptedException e) {
					}
				}
			};
			flashThread.start();
		} else if (event.getEventType() == ClientModelChangedEvent.EVENT_TYPE_AVATAR_ACTIONS_CHANGED) {
			updateAvatarActions(false);
		} else if (event.getEventType() == ClientModelChangedEvent.EVENT_TYPE_AVATAR_ACTIONS_RESTORED) {
			updateAvatarActions(true);
		} else if (event.getEventType() == ClientModelChangedEvent.EVENT_TYPE_WORLD_ACTIONS_CHANGED) {
			updateWorldActions(false);
		} else if (event.getEventType() == ClientModelChangedEvent.EVENT_TYPE_WORLD_ACTIONS_RESTORED) {
			updateWorldActions(true);
		} else if (event.getEventType() == ClientModelChangedEvent.EVENT_TYPE_SELECTED_AVATAR_CHANGED || event.getEventType() == ClientModelChangedEvent.EVENT_TYPE_OBJECT_SELECTED) {
			updateAvatarActions(false);
		} else if (event.getEventType() == ClientModelChangedEvent.EVENT_TYPE_USE_CONTROLLER_CHANGED) {
			placeButtonsBasedOnPrefs();
		} else if (event.getEventType() == ClientModelChangedEvent.EVENT_TYPE_CALIBRATE_SENSORS) {
			calibrateSensors();
		} else if (event.getEventType() == ClientModelChangedEvent.EVENT_TYPE_LOG_UPDATED) {
			updateLogText();
		}
	}

	public void updateLogText() {
		runOnUiThread(new Runnable() {
			public void run() {
				TextView logText = (TextView) findViewById(R.id.logText);
				StringBuffer text = new StringBuffer();
				for (int i = 0; i < AndroidClientModel.MAX_LOG_LINES; i++) {
					if (clientModel.logMessages[i] != null) {
						text.append(clientModel.logMessages[i]);
					}
					if (i < AndroidClientModel.MAX_LOG_LINES - 1) {
						text.append("\n");
					}
				}
				logText.setText(text.toString());
			}
		});
	}

	public void updateTitleAndStatus() {
		// Title (include app name, world name, level)
		if (clientModel.world == null) {
			return;
		}
		if (clientModel.world.getLevel() > 0) {
			setTitle(/* getString(R.string.app_name) + " - " + */clientModel.world.getName() + ", " + getString(R.string.levelLabel) + " " + clientModel.world.getLevel());
		} else {
			setTitle(/* getString(R.string.app_name) + " - " + */clientModel.world.getName());
		}

		// Status (include score and anything the world added as status)
		String status = clientModel.world.getStatus();
		if (clientModel.world.getScore() >= 0) {
			status += " " + getString(R.string.scoreLabel) + " " + clientModel.world.getScore();
		}
		setStatus(status);
	}

	public class RerenderThread extends Thread {

		public RerenderThread() {
			setName("RerenderThread");
		}

		boolean stopit;

		public void safeStop() {
			stopit = true;
		}

		@Override
		public void run() {
			while (!stopit) {
				long startTime = System.currentTimeMillis();
				worldView.requestRender();
				long loopTime = System.currentTimeMillis() - startTime;
				updatePosition();
				try {
					if (clientModel.getRefreshRate() - loopTime > 0) {
						Thread.sleep(Math.max(0, clientModel.getRefreshRate() - loopTime));
					}
				} catch (InterruptedException e) {
					return;
				}
			}
			stopit = false;
		}
	}

	public void setTitle(final String title) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				titleText.setText(title);
			}
		});
	}

	public void setStatus(final String status) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				statusText.setText(status);
			}
		});
	}

	public void setFlashMessage(final String message) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				((TextView) findViewById(R.id.flashMessageText)).setText(message);
			}
		});
	}

	public void updatePosition() {
		// WWObject avatar = clientModel.getAvatar();
		// if (avatar != null) {
		// WWVector position = avatar.getPosition();
		// int x = (int) (position.x);
		// int y = (int) (position.y);
		// int z = (int) (position.z);
		// final String positionText = clientModel.getUserNameField() + " at (" + x + "," + y + "," + z + ")";
		// runOnUiThread(new Runnable() {
		// public void run() {
		// ((TextView) findViewById(R.id.positionText)).setText(positionText);
		// }
		// });
		// }
	}

	private class ReturnValue {
		public int rc;
	}

	/**
	 * Display a prompt dialog.
	 */
	@Override
	public int onAlert(final String title, final String message, final String[] options, final String checkinMessage) {
		if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
			throw new IllegalThreadStateException("This can't be called on the looper thread");
		}
		final ReturnValue returnValue = new ReturnValue();
		this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				final MessageDialog messageDialog = new MessageDialog(ShowWorldActivity.this, title, message, options, checkinMessage);
				currentDialog = messageDialog;
				messageDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
					@Override
					public void onDismiss(DialogInterface dialogInterface) {
						synchronized (returnValue) {
							returnValue.rc = messageDialog.getButtonPressed();
							returnValue.notify();
						}
						currentDialog = null;
					}
				});
				try {
					messageDialog.show();
				} catch (Exception e) {
					System.err.println("Couldn't display alert: " + message);
					e.printStackTrace();
				}
			}
		});
		try {
			synchronized (returnValue) {
				returnValue.wait();
			}
		} catch (InterruptedException e) {
		}
		return returnValue.rc;
	}

	/**
	 * Display a prompt dialog.
	 */
	@Override
	public int onAlert(final String title, final String message, final String[] options, final String leaderboardId, final long score, final String scoreMsg) {
		if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
			throw new IllegalThreadStateException("This can't be called on the looper thread");
		}
		final ReturnValue returnValue = new ReturnValue();
		this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				final MessageDialog messageDialog = new MessageDialog(ShowWorldActivity.this, title, message, options, leaderboardId, score, scoreMsg);
				currentDialog = messageDialog;
				messageDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
					@Override
					public void onDismiss(DialogInterface dialogInterface) {
						synchronized (returnValue) {
							returnValue.rc = messageDialog.getButtonPressed();
							returnValue.notify();
						}
						currentDialog = null;
					}
				});
				messageDialog.show();
			}
		});
		try {
			synchronized (returnValue) {
				returnValue.wait();
			}
		} catch (InterruptedException e) {
		}
		return returnValue.rc;
	}

	@Override
	public void onSelectAlert(final String message, final Object[] availableItems, final String[] options, final SelectResponseHandler handler) {
		ShowWorldActivity.this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				final SelectItemDialog selectItemDialog = new SelectItemDialog(ShowWorldActivity.this, message, availableItems, options);
				currentDialog = selectItemDialog;
				selectItemDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
					@Override
					public void onDismiss(DialogInterface dialogInterface) {
						handler.handleSelect(selectItemDialog.getItemSelected(), selectItemDialog.getButtonPressed());
						currentDialog = null;
					}
				});
				selectItemDialog.show();
			}
		});
	}

	@Override
	public void onInputAlert(final String title, final String message, final String initialValue, final String[] options, final InputResponseHandler handler) {
		ShowWorldActivity.this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				final InputDialog inputDialog = new InputDialog(ShowWorldActivity.this, title, message, initialValue, options);
				currentDialog = inputDialog;
				inputDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
					@Override
					public void onDismiss(DialogInterface dialogInterface) {
						handler.handleInput(inputDialog.getValue(), inputDialog.getButtonPressed());
						currentDialog = null;
					}
				});
				inputDialog.show();
			}
		});
	}

	@Override
	public void onSelectColor(final String title, final int initialColor, final SelectColorHandler handler) {
		ShowWorldActivity.this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				AmbilWarnaDialog.OnAmbilWarnaListener listener = new AmbilWarnaDialog.OnAmbilWarnaListener() {
					@Override
					public void onOk(AmbilWarnaDialog dialog, int color) {
						handler.handleSelect(new WWColor(color));
						currentDialog = null;
					}

					@Override
					public void onCancel(AmbilWarnaDialog dialog) {
					}
				};
				AmbilWarnaDialog colorPickerDialog = new AmbilWarnaDialog(AndroidClientModel.getClientModel().getContext(), initialColor, listener);
				// currentDialog = colorPickerDialog;
				// colorPickerDialog.setTitle(title);
				colorPickerDialog.show();
			}
		});
	}

	boolean usingDPad;
	float dPadX;
	float dPadY;
	boolean dPadLeftDown;
	boolean dPadRightDown;
	boolean dPadUpDown;
	boolean dPadDownDown;

	class DPadTimerTask extends TimerTask {

		@Override
		public void run() {
			if (usingDPad) {
				if (dPadLeftDown) {
					dPadX = 50;
				} else if (dPadRightDown) {
					dPadX = -50;
				} else {
					dPadX = 0;
				}
				if (dPadUpDown) {
					dPadY = 50;
				} else if (dPadDownDown) {
					dPadY = -50;
				} else {
					dPadY = 0;
				}
				controller(dPadX, dPadY);
			}
		}

	}

	/**
	 * Override to make sure key events go directly to the listeners (and not handled by buttons on interface).
	 */
	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (event.getAction() == KeyEvent.ACTION_DOWN) {
			return onKeyDown(event.getKeyCode(), event);
		} else if (event.getAction() == KeyEvent.ACTION_UP) {
			return onKeyUp(event.getKeyCode(), event);
		} else {
			return false;
		}
	};

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_BUTTON_A || keyCode == KeyEvent.KEYCODE_DPAD_CENTER) { // OK
			clientModel.startAvatarAction(0, 0, 0); // typically start game or fire
		} else if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_BUTTON_B) { // CANCEL/BACK
			if (clientModel != null && clientModel.world != null && clientModel.world.needsSaving()) {
				final MessageDialog dialog;
				if (clientModel.world.supportsSaveAndQuit()) {
					dialog = new MessageDialog(this, null, "Do you want to save and quit?", new String[] { "Save and Quit", "Quit", "Cancel" }, null);
				} else {
					dialog = new MessageDialog(this, null, "Do you want to save?", new String[] { "Save", "Quit (no Save)", "Cancel" }, null);
				}
				currentDialog = dialog;
				dialog.show();
				dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {

					@Override
					public void onDismiss(DialogInterface d) {
						int rc = dialog.getButtonPressed();
						if (rc == 0) {
							clientModel.world.save();
							if (clientModel.world.supportsSaveAndQuit()) {
								MainMenuActivity.showPopupAd = true;
								finish();
							}
						} else if (rc == 1) {
							MainMenuActivity.showPopupAd = true;
							finish();
						} else {
							clientModel.resumeWorld();
						}
						currentDialog = null;
					}
				});
			} else {
				final MessageDialog dialog = new MessageDialog(this, null, "Are you sure you want to quit?", new String[] { "Yes", "No" }, null);
				currentDialog = dialog;
				dialog.show();
				dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {

					@Override
					public void onDismiss(DialogInterface d) {
						int rc = dialog.getButtonPressed();
						if (rc == 0) {
							MainMenuActivity.showPopupAd = true;
							finish();
						} else {
							clientModel.resumeWorld();
						}
						currentDialog = null;
					}
				});
			}
			clientModel.pauseWorld();
			return true; // overriding the standard action handling
		}
		// Keys to support Xperia PLAY and compatible (Gametel)
		else if (keyCode == KeyEvent.KEYCODE_BUTTON_SELECT || keyCode == KeyEvent.KEYCODE_MENU) {
			clientModel.startWorldAction(1); // typically change view
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_BUTTON_START || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
			clientModel.startWorldAction(0); // typically pause
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_W) {
			if (!clientModel.isMogaPocket()) { // ignore as pocket uses dpad and joystick overlapped, and we use the joystick
				usingDPad = true;
				dPadUpDown = true;
			}
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == KeyEvent.KEYCODE_S) {
			if (!clientModel.isMogaPocket()) { // ignore as pocket uses dpad and joystick overlapped, and we use the joystick
				usingDPad = true;
				dPadDownDown = true;
			}
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_A) {
			if (!clientModel.isMogaPocket()) { // ignore as pocket uses dpad and joystick overlapped, and we use the joystick
				usingDPad = true;
				dPadLeftDown = true;
			}
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_D) {
			if (!clientModel.isMogaPocket()) { // ignore as pocket uses dpad and joystick overlapped, and we use the joystick
				usingDPad = true;
				dPadRightDown = true;
			}
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_BUTTON_R1 || keyCode == KeyEvent.KEYCODE_BUTTON_R2) {
			if (clientModel.getAvatarActionLabel(0) != null) {
				clientModel.startAvatarAction(0, 0, 0);
			} else {
				clientModel.startAvatarAction(6, 0, 0); // for pinball
			}
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_BUTTON_L1 || keyCode == KeyEvent.KEYCODE_BUTTON_L2) {
			if (clientModel.getAvatarActionLabel(1) != null) {
				clientModel.startAvatarAction(1, 0, 0);
			} else {
				clientModel.startWorldAction(6); // for pinball
			}
			return true;
		}

		// keys to support simulators on windows and mac
		else if (keyCode == KeyEvent.KEYCODE_1) {
			clientModel.startAvatarAction(0, 0, 0);
		} else if (keyCode == KeyEvent.KEYCODE_2) {
			clientModel.startAvatarAction(1, 0, 0);
		} else if (keyCode == KeyEvent.KEYCODE_3) {
			clientModel.startAvatarAction(2, 0, 0);
		} else if (keyCode == KeyEvent.KEYCODE_4) {
			clientModel.startAvatarAction(3, 0, 0);
		} else if (keyCode == KeyEvent.KEYCODE_5) {
			clientModel.startAvatarAction(4, 0, 0);
		} else if (keyCode == KeyEvent.KEYCODE_6) {
			clientModel.startAvatarAction(5, 0, 0);
		} else if (keyCode == KeyEvent.KEYCODE_7) {
			clientModel.startAvatarAction(6, 0, 0);
		} else if (keyCode == KeyEvent.KEYCODE_F1) {
			clientModel.startWorldAction(0);
		} else if (keyCode == KeyEvent.KEYCODE_F2) {
			clientModel.startWorldAction(1);
		} else if (keyCode == KeyEvent.KEYCODE_F3) {
			clientModel.startWorldAction(2);
		} else if (keyCode == KeyEvent.KEYCODE_F4) {
			clientModel.startWorldAction(3);
		} else if (keyCode == KeyEvent.KEYCODE_F5) {
			clientModel.startWorldAction(4);
		} else if (keyCode == KeyEvent.KEYCODE_F6) {
			clientModel.startWorldAction(5);
		} else if (keyCode == KeyEvent.KEYCODE_F7) {
			clientModel.startWorldAction(6);
		}

		return false; // super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_BUTTON_A || keyCode == KeyEvent.KEYCODE_DPAD_CENTER) { // OK
			clientModel.stopAvatarAction(0);
		}
		// Keys to support Xperia PLAY and compatible (Gametel)
		else if (keyCode == KeyEvent.KEYCODE_BACK && !event.isAltPressed()) {
			return true;
		}
		// Keys to support Xperia PLAY and compatible (Gametel)
		else if (keyCode == KeyEvent.KEYCODE_BUTTON_SELECT || keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_V) { // select key on gamepad, "v" on keyboard
			clientModel.stopWorldAction(1);
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_BUTTON_START || keyCode == KeyEvent.KEYCODE_P) { // start key on gamepad, "p" on keyboard
			clientModel.stopWorldAction(0);
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_W) {
			dPadUpDown = false;
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == KeyEvent.KEYCODE_S) {
			dPadDownDown = false;
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_A) {
			dPadLeftDown = false;
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_D) {
			dPadRightDown = false;
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_BUTTON_R1 || keyCode == KeyEvent.KEYCODE_BUTTON_R2) {
			if (clientModel.getAvatarActionLabel(0) != null) {
				clientModel.stopAvatarAction(0);
			} else {
				clientModel.stopAvatarAction(6); // for pinball
			}
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_BUTTON_L1 || keyCode == KeyEvent.KEYCODE_BUTTON_L2) {
			if (clientModel.getAvatarActionLabel(1) != null) {
				clientModel.stopAvatarAction(1);
			} else {
				clientModel.stopWorldAction(6); // for pinball
			}
			return true;
		}

		// keys to support simulators on windows and mac
		else if (keyCode == KeyEvent.KEYCODE_1) {
			clientModel.stopAvatarAction(0);
		} else if (keyCode == KeyEvent.KEYCODE_2) {
			clientModel.stopAvatarAction(1);
		} else if (keyCode == KeyEvent.KEYCODE_3) {
			clientModel.stopAvatarAction(2);
		} else if (keyCode == KeyEvent.KEYCODE_4) {
			clientModel.stopAvatarAction(3);
		} else if (keyCode == KeyEvent.KEYCODE_5) {
			clientModel.stopAvatarAction(4);
		} else if (keyCode == KeyEvent.KEYCODE_6) {
			clientModel.stopAvatarAction(5);
		} else if (keyCode == KeyEvent.KEYCODE_7) {
			clientModel.stopAvatarAction(6);
		} else if (keyCode == KeyEvent.KEYCODE_F1) {
			clientModel.stopWorldAction(0);
		} else if (keyCode == KeyEvent.KEYCODE_F2) {
			clientModel.stopWorldAction(1);
		} else if (keyCode == KeyEvent.KEYCODE_F3) {
			clientModel.stopWorldAction(2);
		} else if (keyCode == KeyEvent.KEYCODE_F4) {
			clientModel.stopWorldAction(3);
		} else if (keyCode == KeyEvent.KEYCODE_F5) {
			clientModel.stopWorldAction(4);
		} else if (keyCode == KeyEvent.KEYCODE_F6) {
			clientModel.stopWorldAction(5);
		} else if (keyCode == KeyEvent.KEYCODE_F7) {
			clientModel.stopWorldAction(6);
		}

		return false; // super.onKeyUp(keyCode, event);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	float[] m_rotationMatrix = new float[16];
	float[] m_lastMagFields = null;
	float[] m_lastAccels = new float[3];

	boolean doCalibration;
	float initialSensorPitch = 0;
	float initialSensorRoll = -60;
	float lastSensorPitch = 0;
	float lastSensorRoll = -60;

	public void calibrateSensors() {
		doCalibration = true;
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (clientModel.useSensors()) {
			int displayRotation = getWindowManager().getDefaultDisplay().getRotation();
			if (m_lastMagFields == null) {
				m_lastMagFields = new float[3];
				m_lastMagFields[0] = -51.50f;
				m_lastMagFields[1] = 22.0f;
				m_lastMagFields[2] = 15.0f;
			}
			// float sensorAzimuth = event.values[0];
			// float sensorPitch = event.values[1];
			// float sensorRoll = event.values[2];
			if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
				System.arraycopy(event.values, 0, m_lastAccels, 0, 3);
			} else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
				System.arraycopy(event.values, 0, m_lastMagFields, 0, 3);
			} else {
				return;
			}
			if (SensorManager.getRotationMatrix(m_rotationMatrix, null, m_lastAccels, m_lastMagFields)) {
				float[] orientation = new float[4];
				SensorManager.getOrientation(m_rotationMatrix, orientation);
				float sensorAzimuth, sensorPitch, sensorRoll;
				if (displayRotation == Surface.ROTATION_0) { // 10 inch tablets
					sensorAzimuth = orientation[0] * 57.2957795f;
					sensorPitch = -orientation[2] * 57.2957795f;
					sensorRoll = orientation[1] * 57.2957795f;
				} else if (displayRotation == Surface.ROTATION_90) { // most phones and 7 inch tablets
					sensorAzimuth = orientation[0] * 57.2957795f;
					sensorPitch = orientation[1] * 57.2957795f;
					sensorRoll = orientation[2] * 57.2957795f;
				} else if (displayRotation == Surface.ROTATION_180) { // ? some device I've never heard of
					sensorAzimuth = orientation[0] * 57.2957795f;
					sensorPitch = orientation[2] * 57.2957795f;
					sensorRoll = -orientation[1] * 57.2957795f;
				} else { // displayRotation == Surface.ROTATION_270 -- the kindle fire hd and hdx
					sensorAzimuth = orientation[0] * 57.2957795f;
					sensorPitch = -orientation[1] * 57.2957795f;
					sensorRoll = -orientation[2] * 57.2957795f;
				}
				if (doCalibration) {
					initialSensorPitch = sensorPitch;
					initialSensorRoll = sensorRoll;
					lastSensorPitch = sensorPitch;
					lastSensorRoll = sensorRoll;
					doCalibration = false;
					System.out.println("CALIBRATED:  " + sensorPitch + "  " + sensorRoll);
				}
				float avgSensorPitch = (sensorPitch + lastSensorPitch) / 2.0f;
				float avgSensorRoll = (sensorRoll + lastSensorRoll) / 2.0f;
				lastSensorPitch = sensorPitch;
				lastSensorRoll = sensorRoll;
				float deltaX = Math.max(-50, Math.min(50, 3f * (avgSensorPitch - initialSensorPitch)));
				float deltaY = Math.max(-50, Math.min(50, 2f * (avgSensorRoll - initialSensorRoll)));
				controller(deltaX, deltaY);
			}
		}
	}

	public void showBannerAd() {
		// no more ads!!
	}

	public void hideBannerAd() {
		// no more ads!!
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (clientModel.lastTexturePickerDialog != null) {
			clientModel.lastTexturePickerDialog.onActivityResult(requestCode, resultCode, data);
			clientModel.lastTexturePickerDialog = null;
		}
	}

	// Moga support

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

	@Override
	public void onKeyEvent(com.bda.controller.KeyEvent event) {
		if (currentDialog != null) {
			currentDialog.dispatchKeyEvent(new KeyEvent(event.getAction(), event.getKeyCode()));
			if (event.getAction() == com.bda.controller.KeyEvent.ACTION_DOWN) {
				sendKey(event.getKeyCode());
			}
		} else {
			dispatchKeyEvent(new KeyEvent(event.getAction(), event.getKeyCode()));
		}
	}

	@Override
	public void onMotionEvent(com.bda.controller.MotionEvent event) {
		float x = event.getAxisValue(com.bda.controller.MotionEvent.AXIS_X);
		float y = event.getAxisValue(com.bda.controller.MotionEvent.AXIS_Y);
		if (x == 0 && y == 0) {
			x = event.getAxisValue(com.bda.controller.MotionEvent.AXIS_Z);
			y = event.getAxisValue(com.bda.controller.MotionEvent.AXIS_RZ);
		}
		if (Math.abs(x) > 0.25f || Math.abs(y) > 0.25f) { // reset dpad
			usingDPad = false;
			dPadUpDown = false;
			dPadDownDown = false;
			dPadLeftDown = false;
			dPadRightDown = false;
		}
		if (dPadUpDown) {
			y = 1;
		} else if (dPadDownDown) {
			y = -1;
		}
		controller(-x * 50, -y * 50);
	}

	@Override
	public void onStateEvent(com.bda.controller.StateEvent event) {
		System.out.println("Moga state");
	}

	@SuppressLint("NewApi")
	public boolean onGenericMotionEvent(MotionEvent event) {
		if (event.getSource() == InputDevice.SOURCE_JOYSTICK) {
			// get changes in value from left joystick
			float x = event.getAxisValue(MotionEvent.AXIS_X);
			float y = event.getAxisValue(MotionEvent.AXIS_Y);
			float rx = event.getAxisValue(MotionEvent.AXIS_Z);
			float ry = event.getAxisValue(MotionEvent.AXIS_RZ);
			if (Math.abs(x) > Math.abs(rx)) {
				controller(-x * 50, -y * 50);
			} else {
				controller(-rx * 50, -ry * 50);
			}
			return true;
		} else {
			return super.onGenericMotionEvent(event);
		}
	}
}
