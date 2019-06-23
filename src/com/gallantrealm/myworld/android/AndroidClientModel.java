package com.gallantrealm.myworld.android;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Vector;
import com.amazon.device.ads.AdRegistration;
import com.example.android.trivialdrivesample.util.IabHelper;
import com.example.android.trivialdrivesample.util.IabHelper.OnIabPurchaseFinishedListener;
import com.example.android.trivialdrivesample.util.IabResult;
import com.example.android.trivialdrivesample.util.Purchase;
import com.gallantrealm.myworld.android.themes.DefaultTheme;
import com.gallantrealm.myworld.android.themes.Theme;
import com.gallantrealm.myworld.client.model.ClientModel;
import com.gallantrealm.myworld.client.model.ClientModelChangedEvent;
import com.gallantrealm.myworld.model.WWWorld;
import com.gallantrealm.myworld.server.MyWorldServer;
import com.zeemote.zc.Controller;
import com.zeemote.zc.DeviceFactory;
import com.zeemote.zc.IDeviceSearch;
import com.zeemote.zc.IProgressMonitor;
import com.zeemote.zc.IStreamConnector;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ConfigurationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.opengl.GLES20;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.os.PowerManager;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.view.InputDevice;
import uk.co.labbookpages.WavFile;

@SuppressLint("NewApi")
public class AndroidClientModel extends ClientModel {

	public static final int FULLVERSION = 0;
	public static final int GOOGLE = 1;
	public static final int AMAZON = 2;
	public static final int FREEVERSION = 3;

	public static final int market = GOOGLE;

	private static AndroidClientModel clientModel;

	public static AndroidClientModel getClientModel() {
		if (clientModel == null) {
			clientModel = new AndroidClientModel();
		}
		return clientModel;
	}

	private MyWorldServer localServer;
	private int nSongPlayers;
	private MediaPlayer dialogSong;
	private int nDialogSongPlayers;

	private String categoryName;
	private String worldName;
	private String worldClassName;
	private String worldParams;
	private String avatarName;
	private boolean playMusic;
	private boolean playSoundEffects;
	private boolean vibration;
	private boolean useSensors;
	private boolean useZeemote;
	private boolean useScreenControl;
	private boolean controlOnLeft;
	public boolean usingMoga;
	private float controlSensitivity = 1.0f;
	private static final int NSCORES = 24;
	private final int[] scores = new int[NSCORES];
	private final int[] levels = new int[NSCORES];
	private final int[] times = new int[NSCORES];
	private final boolean[] unlocked = new boolean[NSCORES];
	private static final int NAVATARS = 24;
	private final String[] avatarDisplayNames = new String[NAVATARS];
	private String flashMessage;
	private boolean blink;
	private Activity context;
	private boolean alwaysStartAsNew;
	private boolean alwaysStartAsOld;
	private boolean simpleRendering;
	private boolean fullVersion;
	private boolean powerSaver;
	private int playCount;
	private long lastPlayTime;
	private boolean customizeMode;
	private int preferencesVersion; // 0=2013-2014, 1=2015

	public boolean cameraInitiallyFacingAvatar;

	public boolean goggleDogPass;
	
	private String localFolder;  // for worlds and avatars

	IabHelper purchaseHelper;

	public void loadPreferences(Activity context) {
		this.context = context;

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		preferencesVersion = preferences.getInt("preferencesVersion", 1);
		avatarName = preferences.getString("avatarName", context.getString(R.string.defaultAvatarName));
		worldName = preferences.getString("worldName", context.getString(R.string.defaultWorldName));
		worldClassName = preferences.getString("worldClassName", worldName);
		playMusic = preferences.getBoolean("playMusic", true);
		playSoundEffects = preferences.getBoolean("playSoundEffects", true);
		vibration = preferences.getBoolean("vibration", true);
		useSensors = preferences.getBoolean("useSensors", canUseSensors());
		useZeemote = preferences.getBoolean("useZeemote", false);
		useScreenControl = preferences.getBoolean("showScreenControls", !useSensors && !useZeemote);
		controlOnLeft = preferences.getBoolean("controlOnLeft", false);
		usingMoga = false; // this is determined dynamically now, by querying for a moga controller
		controlSensitivity = preferences.getFloat("controlSensitivity", 0.5f);
		stereoscopic = preferences.getBoolean("stereoscopic", false);
		if (preferencesVersion == 0) {
			simpleRendering = !supportsOpenGLES20();
		} else {
			simpleRendering = preferences.getBoolean("simpleRendering", !supportsOpenGLES20());
		}
		fullVersion = preferences.getBoolean("fullVersion", "true".equals(context.getString(R.string.fullVersion)));
		powerSaver = preferences.getBoolean("powerSaver", false);
		playCount = preferences.getInt("playCount", 0);
		lastPlayTime = preferences.getLong("lastPlayTime", 0);
		for (int i = 0; i < NSCORES; i++) {
			scores[i] = preferences.getInt("score" + i, 0);
			levels[i] = preferences.getInt("level" + i, 0);
			times[i] = preferences.getInt("time" + i, 0);
			unlocked[i] = preferences.getBoolean("unlocked" + i, false);
		}
		for (int i = 0; i < NAVATARS; i++) {
			avatarDisplayNames[i] = preferences.getString("avatarDisplayName" + i, null);
		}
		localFolder = preferences.getString("localFolder", Environment.getExternalStorageDirectory().toString() + "/DevWorlds");
	}

	public void setContext(Activity context) {
		// if (this.context != null && purchaseObserver != null) {
		// ResponseHandler.unregister(purchaseObserver);
		// purchaseObserver = null;
		// }
		this.context = context;
		if (purchaseHelper == null) {
			try {
				purchaseHelper = new IabHelper(context, context.getString(R.string.googleappkey));
				purchaseHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
					public void onIabSetupFinished(IabResult result) {
					}
				});
			} catch (Exception e) {
				System.out.println("No in-app purchase, the app isn't setup correctly for it: " + e.getMessage());
			}
		}

	}

	public Activity getContext() {
		return context;
	}

	public void savePreferences(Context context) {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putInt("preferencesVersion", 1);
		editor.putString("avatarName", avatarName);
		editor.putString("worldName", worldName);
		editor.putString("worldClassName", worldClassName);
		editor.putBoolean("playMusic", playMusic);
		editor.putBoolean("playSoundEffects", playSoundEffects);
		editor.putBoolean("vibration", vibration);
		editor.putBoolean("useSensors", useSensors);
		editor.putBoolean("useZeemote", useZeemote);
		editor.putBoolean("showScreenControls", useScreenControl);
		editor.putBoolean("controlOnLeft", controlOnLeft);
		editor.putFloat("controlSensitivity", controlSensitivity);
		editor.putBoolean("stereoscopic", stereoscopic);
		editor.putBoolean("simpleRendering", simpleRendering);
		if (fullVersion) {
			editor.putBoolean("fullVersion", fullVersion);
		}
		editor.putBoolean("powerSaver", powerSaver);
		editor.putInt("playCount", playCount);
		editor.putLong("lastPlayTime", lastPlayTime);
		for (int i = 0; i < NSCORES; i++) {
			editor.putInt("score" + i, scores[i]);
			editor.putInt("level" + i, levels[i]);
			editor.putInt("time" + i, times[i]);
			editor.putBoolean("unlocked" + i, unlocked[i]);
		}
		for (int i = 0; i < NAVATARS; i++) {
			editor.putString("avatarDisplayName" + i, avatarDisplayNames[i]);
		}
		editor.putString("localFolder", localFolder);
		editor.commit();
	}

	boolean testedOpenGL;
	boolean supportsOpenGLES20;

	@Override
	public void setWorld(WWWorld localWorld) {
		testedOpenGL = false; // need to retest if world is changed
		super.setWorld(localWorld);
	}

	public boolean supportsOpenGLES20() {
		if (!testedOpenGL) {

			// check to see if settings indicate it can be supported
			if (context.getString(R.string.supports_new_graphics).equals("false")) {
				System.out.println("supports_new_graphics is false");
				supportsOpenGLES20 = false;
			} else {

				// first, check android
				ActivityManager am = (ActivityManager) getContext().getSystemService(Context.ACTIVITY_SERVICE);
				ConfigurationInfo info = am.getDeviceConfigurationInfo();
				if (info.reqGlEsVersion < 0x20000) {
					System.out.println("deviceConfigurationInfo has reqGLESVersion < 0x20000");
					supportsOpenGLES20 = false;
				} else {

					// Don't trust older android levels (anything below 4.1: Jelly Bean)
					if (Build.VERSION.SDK_INT < 16) {
						System.out.println("Build.VERSION.SDK_INT < 16");
						supportsOpenGLES20 = false;
					} else {

						// Not going to perform well on single cpu devices
						if (Runtime.getRuntime().availableProcessors() < 2) {
							System.out.println("availableProcessors < 2");
							supportsOpenGLES20 = false;
						} else {

							// Likely not going to perform if it can't handle >= 4096 texture sizes
							int[] maxTextureSize = new int[1];
							GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, maxTextureSize, 0);
							if (maxTextureSize[0] > 0 && maxTextureSize[0] < 4096) {
								System.out.println("maxTextureSize < 4096");
								supportsOpenGLES20 = false;
							} else {

								// The world still might not be ready for it
								if (world != null && !world.supportsOpenGLES20()) {
									System.out.println("!world.supportsOpenGLES20");
									supportsOpenGLES20 = false;
								} else {
									supportsOpenGLES20 = true;
								}
							}
						}
					}
				}
			}
			testedOpenGL = true;
		}
		return supportsOpenGLES20;
	}

	public int getPlayCount() {
		return playCount;
	}

	public void updatePlayCount(Context context) {
		playCount = playCount + 1;
		savePreferences(context);
	}

	public long getLastPlayTime() {
		return lastPlayTime;
	}

	public void updateLastPlayTime(Context context) {
		lastPlayTime = System.currentTimeMillis();
		savePreferences(context);
	}

	int songId;
	MediaPlayer songPlayer;

	public void playSong(int songId) {
		if (this.songId != songId) {
			if (songPlayer != null) {
				songPlayer.stop();
				songPlayer.release();
				songPlayer = null;
			}
			this.songId = songId;
		}
		if (songPlayer == null) {
			songPlayer = MediaPlayer.create(getContext(), this.songId);
			if (songPlayer == null) {
				return;
			}
			songPlayer.setLooping(true);
			songPlayer.setVolume(1f, 1f);
		}
		nSongPlayers++;
		if (nSongPlayers > 0) {
			if (isPlayMusic()) {
				songPlayer.start();
			}
		}
	}

	public synchronized void pauseSong() {
		if (songPlayer != null) {
			nSongPlayers--;
			if (nSongPlayers == 0) {
				songPlayer.pause();
			}
		}
	}

	public MyWorldServer getLocalServer() {
		return localServer;
	}

	public void setLocalServer(MyWorldServer server) {
		localServer = server;
	}

	public String getCategoryName() {
		return this.categoryName;
	}

	public void setCategoryName(String categoryName) {
		this.categoryName = categoryName;
		fireClientModelChanged(ClientModelChangedEvent.EVENT_TYPE_SELECTED_CATEGORY_CHANGED);
	}

	public String getWorldName() {
		return this.worldName;
	}

	public void setWorldName(String worldName) {
		this.worldName = worldName;
		this.worldClassName = null;
		fireClientModelChanged(ClientModelChangedEvent.EVENT_TYPE_SELECTED_GAME_CHANGED);
	}

	public void setWorldName(String worldName, String worldClassName) {
		this.worldName = worldName;
		this.worldClassName = worldClassName;
		fireClientModelChanged(ClientModelChangedEvent.EVENT_TYPE_SELECTED_GAME_CHANGED);
	}

	public String getWorldClassName() {
		return this.worldClassName;
	}

	public String getWorldParams() {
		return this.worldParams;
	}

	public void setWorldParams(String params) {
		this.worldParams = params;
	}

	public void setAvatarName(String name) {
		avatarName = name;
		fireClientModelChanged(ClientModelChangedEvent.EVENT_TYPE_SELECTED_AVATAR_CHANGED);
	}

	public String getAvatarName() {
		return avatarName;
	}

	public boolean isPlayMusic() {
		return playMusic && isScreenOn();
	}

	public boolean isScreenOn() {
		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		return pm.isScreenOn();
	}

	public void setPlayMusic(boolean playMusic) {
		this.playMusic = playMusic;
	}

	public boolean isPlaySoundEffects() {
		return playSoundEffects && isScreenOn();
	}

	public void setPlaySoundEffects(boolean playSoundEffects) {
		this.playSoundEffects = playSoundEffects;
	}

	public boolean isVibration() {
		return vibration && isScreenOn();
	}

	public void setVibration(boolean vibration) {
		this.vibration = vibration;
	}

	public boolean canUseSensors() {
		SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		if (sensorManager == null) {
			return false;
		}
		List<Sensor> gameSensors = sensorManager.getSensorList(Sensor.TYPE_GAME_ROTATION_VECTOR);
		if (gameSensors != null && gameSensors.size() > 0) {
			return true;
		}
		return false;
	}

	public boolean useSensors() {
		return useSensors && canUseSensors() && !useMoga(context) && !useGamepad(context);
	}

	public void setUseSensors(boolean useSensors) {
		this.useSensors = useSensors;
	}

	public boolean useZeemote() {
		return useZeemote && !useMoga(context) && !useGamepad(context);
	}

	public boolean useGamepad(Context context) {
		int[] deviceIds = InputDevice.getDeviceIds();
		for (int deviceId : deviceIds) {
			InputDevice dev = InputDevice.getDevice(deviceId);
			int sources = dev.getSources();
			// Verify that the device has gamepad buttons, control sticks, or both.
			if (((sources & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) || ((sources & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK)) {
				// This device is a game controller.
				return true;
			}
		}
		return false;
	}

	public boolean useMoga(Context context) {
		if (usingMoga) { // if ever moga was found, use it
			return true;
		}
		if (context instanceof GallantActivity) {
			com.bda.controller.Controller mogaController = ((GallantActivity) context).mogaController;
			if (mogaController == null) {
				return false;
			}
			usingMoga = mogaController.getState(com.bda.controller.Controller.STATE_CONNECTION) == com.bda.controller.Controller.ACTION_CONNECTED;
			return usingMoga;
		} else if (context instanceof ShowWorldActivity) {
			com.bda.controller.Controller mogaController = ((ShowWorldActivity) context).mogaController;
			if (mogaController == null) {
				return false;
			}
			usingMoga = mogaController.getState(com.bda.controller.Controller.STATE_CONNECTION) == com.bda.controller.Controller.ACTION_CONNECTED;
			return usingMoga;
		} else {
			return false;
		}
	}

	public boolean isMogaPocket() {
		if (!useMoga(context)) {
			return false;
		}
		if (context instanceof GallantActivity) {
			com.bda.controller.Controller mogaController = ((GallantActivity) context).mogaController;
			if (mogaController == null) {
				return false;
			} else {
				return mogaController.getState(com.bda.controller.Controller.STATE_CURRENT_PRODUCT_VERSION) == com.bda.controller.Controller.ACTION_VERSION_MOGA;
			}
		} else if (context instanceof ShowWorldActivity) {
			com.bda.controller.Controller mogaController = ((ShowWorldActivity) context).mogaController;
			if (mogaController == null) {
				return false;
			}
			boolean isMogaPocket = mogaController.getState(com.bda.controller.Controller.STATE_CURRENT_PRODUCT_VERSION) == com.bda.controller.Controller.ACTION_VERSION_MOGA;
			return isMogaPocket;
		} else {
			return false;
		}
	}

	@Override
	public void calibrateSensors() {
		fireClientModelChanged(ClientModelChangedEvent.EVENT_TYPE_CALIBRATE_SENSORS);
	}

	public void setUseZeemote(boolean useZeemote) {
		this.useZeemote = useZeemote;
	}

	public boolean useScreenControl() {
		return useScreenControl && !useMoga(context) && hasTouchScreen();
	}

	public void setUseScreenControl(boolean useScreenControl) {
		this.useScreenControl = useScreenControl;
	}

	public void setControlOnLeft(boolean controlOnLeft) {
		this.controlOnLeft = controlOnLeft;
	}

	public boolean isControlOnLeft() {
		return this.controlOnLeft;
	}

	public void setControlSensitivity(float sensitivity) {
		this.controlSensitivity = sensitivity;
	}

	public float getControlSensitivity() {
		return this.controlSensitivity;
	}

	public boolean useDPad() {
		return !useScreenControl() && !useSensors() && !useZeemote();
	}

	private Controller zeeController;
	private boolean zeeConnected;
	private boolean cancelZee;

	public boolean connectToZeemote(Context context) {
		if (zeeController == null) {
			zeeController = new Controller(1, Controller.TYPE_JS1);
		}
		cancelZee = false;
		try {
			while (!cancelZee) {
				try {
					if (zeeController.isConnected()) {
						return true;
					}
					System.out.println("getting devices");
					IDeviceSearch deviceSearch = DeviceFactory.getDeviceSearch();
					deviceSearch.findDevices(new ZeeProgressMonitor());
					Vector<IStreamConnector> deviceStreams = deviceSearch.getStreamConnectorList();
					System.out.println("connecting to device");
					zeeController.connect(deviceStreams.get(0));
					System.out.println("connected");
					zeeConnected = true;
					if (zeeController.isConnected()) {
						return true;
					}
				} catch (Exception e) {
					System.err.println("Exception accessing zeeMote: " + e.getMessage());
				}
				Thread.sleep(1000);
			}
		} catch (InterruptedException e) {
		}
		return false;
	}

	public void cancelZeemoteConnect() {
		cancelZee = true;
	}

	private class ZeeProgressMonitor implements IProgressMonitor {
		@Override
		public void setMessage(String arg0) {
			System.out.println(arg0);
		}
	}

	public Controller getZeeController() {
		return zeeController;
	}

	public void setScore(int game, int score) {
		this.scores[game - 1] = score;
	}

	public int getScore(int game) {
		return this.scores[game - 1];
	}

	public void setLevel(int game, int level) {
		this.levels[game - 1] = level;
	}

	public int getLevel(int game) {
		return this.levels[game - 1];
	}

	public void setTime(int game, int time) {
		this.times[game - 1] = time;
	}

	public int getTime(int game) {
		return this.times[game - 1];
	}

	public void flashMessage(String message, boolean blink) {
		this.flashMessage = message;
		this.blink = blink;
		fireClientModelChanged(ClientModelChangedEvent.EVENT_TYPE_MESSAGE_FLASHED);
	}

	public String getFlashMessage() {
		return flashMessage;
	}

	public boolean isFlashMessageBlink() {
		return blink;
	}

	public void setAlwaysStartAsNew(boolean alwaysStartAsNew) {
		this.alwaysStartAsNew = alwaysStartAsNew;
	}

	public boolean getAlwaysStartAsNew() {
		return alwaysStartAsNew;
	}

	public void setAlwaysStartAsOld(boolean alwaysStartAsOld) {
		this.alwaysStartAsOld = alwaysStartAsOld;
	}

	public boolean getAlwaysStartAsOld() {
		return alwaysStartAsOld;
	}

	public void setSimpleRendering(boolean simpleRendering) {
		this.simpleRendering = simpleRendering;
	}

	public boolean isSimpleRendering() {
		return simpleRendering;
	}

	public boolean isFullVersion() {
		try {
			return fullVersion // actually marked a full version
					|| (!isGoogle() && !isAmazon()) // is not a market that supports in-app purchase
			;
		} catch (Exception e) { // a problem figuring out above
			return true; // free!
		}
	}

	static final int RC_REQUEST = 10001;
	static final String SKU_FULL_VERSION = "fullversion";
	// static final String SKU_FULL_VERSION = "android.test.purchase";
	static final String PAYLOAD = "DontSteal.IWorkedHardToMakeThisApp";

	public void buyFullVersion() {
		try {
			purchaseHelper.flagEndAsync();
			purchaseHelper.launchPurchaseFlow(context, SKU_FULL_VERSION, IabHelper.ITEM_TYPE_SUBS, RC_REQUEST, new OnIabPurchaseFinishedListener() {
				public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
					if (purchaseHelper == null) { // shutdown
						return;
					}
					if (result.isFailure()) {
						System.err.println("Purchase Failed: " + result.getMessage());
						// new MessageDialog(context, "Purchase Failed", "There was an error purchasing. Check your internet connection and try again later.", null).show();
						// Note: No need to show an error as Google Play's error dialog is better.
						return;
					}
					setFullVersion(true);
					savePreferences(context);
					new MessageDialog(context, "Purchase Success", "Thanks for purchasing!  Restart the app to enable all features and remove ads.", null).show();
				}
			}, PAYLOAD);
		} catch (Exception e) {
			e.printStackTrace();
			new MessageDialog(context, "Purchase Failed", "There was an error launching Google Play for purchasing.  Please make sure Google Play is installed and working.", null).show();
		}
	}

	public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
		if (purchaseHelper != null) {
			try {
				purchaseHelper.handleActivityResult(requestCode, resultCode, data);
			} catch (Exception e) {
			}
		}
		return false;
	}

	public boolean isGoggleDogPass() {
		return goggleDogPass;
	}

	public void setGoggleDogPass(boolean pass) {
		goggleDogPass = pass;
	}

	public boolean canShowAds() {
		if (isChromebook()) {
			return false;
		}
		return ((isAmazon() || isGoogle()) && !context.getString(R.string.amazonappkey).equals("")) //
				|| (isGoogle() && !context.getString(R.string.admobid).equals(""));
	}

	public void initAds() {
		if (!isFullVersion()) {
			try {
				if (context.getString(R.string.amazonappkey).length() > 0) { // prepare for amazon ads
					System.out.println("Registering for Amazon ads.");
					if (BuildConfig.DEBUG) {
						AdRegistration.enableTesting(true);
						AdRegistration.enableLogging(true);
					}
					AdRegistration.setAppKey(context.getString(R.string.amazonappkey));
					AdRegistration.registerApp(context);
				}
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}

	public boolean isGoogle() {
		return market == GOOGLE;
	}

	public boolean isAmazon() {
		return market == AMAZON;
	}

	public boolean isFree() {
		return market == FREEVERSION;
	}

	public boolean isChromebook() {
		return context.getPackageManager().hasSystemFeature("org.chromium.arc.device_management");
	}

	public boolean isWorldUnlocked(int i) {
		if (isFullVersion()) {
			return true;
		}
		if (isGoggleDogPass()) {
			return true;
		}
		return unlocked[i - 1];
	}

	public void setWorldUnlocked(int i) {
		unlocked[i - 1] = true;
		savePreferences(context);
	}

	/**
	 * Sets only the indicated world unlocked and locks all others (that have zero scores).
	 * 
	 * @param i
	 */
	public void setOnlyWorldUnlocked(int i) {
		for (int j = 0; j < unlocked.length; j++) {
			if (getScore(j + 1) == 0) {
				unlocked[j] = false;
			}
		}
		unlocked[i - 1] = true;
		savePreferences(context);
	}

	public void setAllWorldsUnlocked() {
		for (int j = 0; j < unlocked.length; j++) {
			if (getScore(j + 1) == 0) {
				unlocked[j] = true;
			}
		}
		savePreferences(context);
	}

	public void setFullVersion(boolean fullVersion) {
		this.fullVersion = fullVersion;
		fireClientModelChanged(ClientModelChangedEvent.EVENT_TYPE_FULLVERSION_CHANGED);
	}

	public boolean isPowerSaver() {
		return false; // no longer an option
	}

	public void setPowerSaver(boolean powerSaver) {
		this.powerSaver = powerSaver;
	}

	public void showBannerAds() {
		if (context instanceof ShowWorldActivity && context.getString(R.string.showBannerAds).equals("true")) {
			((ShowWorldActivity) context).showBannerAd();
		}
	}

	public void hideBannerAds() {
		if (context instanceof ShowWorldActivity) {
			((ShowWorldActivity) context).hideBannerAd();
		}
	}

	public void showPopupAd() {
		if (context.getString(R.string.showPopupAds).equals("true") && !isFullVersion() && !useMoga(context)) {
			System.out.println("POPUP AD!!!!!");
			new AdDialog(context).show();
		}
	}

	private String themeName = "com.gallantrealm.myworld.android.themes.DefaultTheme";
	private Theme theme = new DefaultTheme();

	public void setThemeName(String themeName) {
		this.themeName = themeName;
		try {
			this.theme = (Theme) this.getClass().getClassLoader().loadClass(themeName).newInstance();
			typeface = null;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String getThemeName() {
		return themeName;
	}

	public Theme getTheme() {
		return theme;
	}

	Typeface typeface;

	public Typeface getTypeface(Context context) {
		if (typeface == null) {
			try {
				String font = getTheme().font;
				typeface = Typeface.createFromAsset(context.getAssets(), font);
			} catch (Throwable e) {
				System.err.println("Could not create typeface for app.");
			}
		}
		return typeface;
	}

	public TexturePickerDialog lastTexturePickerDialog;

	/**
	 * Loads a bitmap for editing, such as in the eggworld decorator
	 * 
	 * @param fileName
	 * @return
	 */
	public Bitmap loadBitmap(String fileName) {
		Bitmap bitmap = null;
		try {
			File file = new File(getContext().getFilesDir(), fileName);
			if (!file.exists()) {
				// create a new bitmap, set to white
				bitmap = Bitmap.createBitmap(512, 512, Config.RGB_565);
				for (int i = 0; i < 512; i++) {
					for (int j = 0; j < 512; j++) {
						bitmap.setPixel(i, j, 0xFFFFFFFF);
					}
				}
				saveBitmap(bitmap, fileName);
			}
			InputStream inStream = new BufferedInputStream(new FileInputStream(file), 65536);
			Bitmap tbitmap = BitmapFactory.decodeStream(inStream);
			inStream.close();
			bitmap = tbitmap.copy(tbitmap.getConfig(), true);
			tbitmap.recycle();
			// bitmap = tbitmap;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return bitmap;
	}

	/**
	 * Saves a bitmap to app-local files.
	 * 
	 * @param bitmap
	 * @param fileName
	 */
	public void saveBitmap(Bitmap bitmap, String fileName) {
		try {
			File file = new File(getContext().getFilesDir(), fileName);
			if (file.exists()) {
				file.delete();
			}
			OutputStream outStream = new BufferedOutputStream(new FileOutputStream(file), 65536);
			bitmap.compress(CompressFormat.PNG, 100, outStream);
			outStream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Loads a RAW wave file into a wave table
	 * 
	 * @param fileName
	 * @return
	 */
	public double[] loadWave(String fileName, boolean external) throws Exception {
		System.out.println("Loading wav file: " + fileName);
		File file;
		if (fileName.startsWith("file:")) { // via an external url
			file = new File(fileName.substring(7));
		} else if (fileName.startsWith("/")) { // full path file
			file = new File(fileName);
		} else { // within the application
			if (external) {
				try {
					file = new File(getContext().getExternalFilesDir(null), fileName);
				} catch (Exception e) {
					file = new File(getContext().getFilesDir(), fileName);
				}
			} else {
				file = new File(getContext().getFilesDir(), fileName);
			}
		}
		InputStream is;
		long len;
		if (file.exists()) {
			is = new FileInputStream(file);
			len = file.length();
		} else {
			// if file not found, perhaps it has a built-in replacement, so try assets
			System.out.println("Looking for wav in assets");
			file = null;
			fileName = trimName(fileName);
			is = context.getAssets().open(fileName);
			len = context.getAssets().openFd(fileName).getLength();
		}
		WavFile wavFile = WavFile.openWavFile(is, len);
		int waveLength = Math.min((int) wavFile.getNumFrames(), 1000000);
		double[] wave = new double[waveLength];
		wavFile.readFramesMono(wave, waveLength);
		return wave;
	}

	private String trimName(String sampleName) {
		if (sampleName.lastIndexOf("/") >= 0) {
			return sampleName.substring(sampleName.lastIndexOf("/") + 1);
		} else {
			return sampleName;
		}
	}

	public Object loadObject(String fileName) {
		return loadObject(fileName, false);
	}

	public InputStream loadFile(String fileName, boolean external) {
		InputStream inStream = null;
		if (fileName.startsWith("file:")) { // via an external url
			try {
				File file = new File(fileName.substring(7));
				inStream = new FileInputStream(file);
			} catch (Exception e) {
			}
		} else { // within the application
			if (external && getContext().getExternalFilesDir(null) != null) { // external file
				try {
					File file = new File(getContext().getExternalFilesDir(null), fileName);
					inStream = new FileInputStream(file);
				} catch (Exception e) {
				}
			}
			if (inStream == null) { // internal file
				try {
					File file = new File(getContext().getFilesDir(), fileName);
					inStream = new FileInputStream(file);
				} catch (Exception e) {
				}
			}
			// if file not found, it is a built-in. so try asset
			if (inStream == null) {
				try {
					inStream = context.getAssets().open(fileName.trim());
				} catch (Exception e) {
				}
			}
		}
		return inStream;
	}

	/**
	 * Loads a serializable object from a file.
	 * 
	 * @param fileName
	 * @return
	 */
	public Object loadObject(String fileName, boolean external) {
		Object object = null;
		if (fileName.startsWith("file:")) { // via an external url
			try {
				File file = new File(fileName.substring(7));
				ObjectInputStream inStream = new ObjectInputStream(new FileInputStream(file));
				object = inStream.readObject();
				inStream.close();
			} catch (Exception e) {
			}
		} else { // within the application
			if (external && getContext().getExternalFilesDir(null) != null) { // external file
				try {
					File file = new File(getContext().getExternalFilesDir(null), fileName);
					ObjectInputStream inStream = new ObjectInputStream(new FileInputStream(file));
					object = inStream.readObject();
					inStream.close();
				} catch (Exception e) {
				}
			}
			if (object == null) { // internal file
				try {
					File file = new File(getContext().getFilesDir(), fileName);
					ObjectInputStream inStream = new ObjectInputStream(new FileInputStream(file));
					object = inStream.readObject();
					inStream.close();
				} catch (Exception e) {
				}
			}
			// if file not found, it is a built-in. so try asset
			if (object == null) {
				try {
					InputStream is = context.getAssets().open(fileName.trim());
					ObjectInputStream inStream = new ObjectInputStream(is);
					object = inStream.readObject();
					inStream.close();
				} catch (Exception e) {
				}
			}
		}
		return object;
	}

	public void deleteObject(String fileName) {
		deleteObject(fileName, false);
	}

	public void deleteObject(String fileName, boolean external) {
		try {
			File file;
			if (fileName.startsWith("file:")) { // an external file
				file = new File(fileName.substring(7));
			} else { // within the application
				if (external && getContext().getExternalFilesDir(null) != null) {
					file = new File(getContext().getExternalFilesDir(null), fileName);
				} else {
					file = new File(getContext().getFilesDir(), fileName);
				}
			}
			file.delete();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void saveObject(Object object, String fileName) {
		saveObject(object, fileName, false);
	}

	/**
	 * Saves an object to app-local files. The object needs to be serializable
	 * 
	 * @param bitmap
	 * @param fileName
	 */
	public void saveObject(Object object, String fileName, boolean external) {
		boolean saved = false;
		if (external && getContext().getExternalFilesDir(null) != null) {
			try {
				File file = new File(getContext().getExternalFilesDir(null), fileName);
				if (file.exists()) {
					file.delete();
				}
				ObjectOutputStream outStream = new ObjectOutputStream(new FileOutputStream(file));
				outStream.writeObject(object);
				outStream.close();
				saved = true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (!saved) {
			try {
				File file = new File(getContext().getFilesDir(), fileName);
				if (file.exists()) {
					file.delete();
				}
				ObjectOutputStream outStream = new ObjectOutputStream(new FileOutputStream(file));
				outStream.writeObject(object);
				outStream.close();
				saved = true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Exports an object to sdcard storage.
	 */
	public File exportObject(Object object, String fileName) {
		File file;
		try {
			File appDir = new File(Environment.getExternalStorageDirectory() + "/Android/data/" + context.getApplicationInfo().packageName);
			if (!appDir.exists()) {
				appDir.mkdir();
			}
			file = new File(appDir, fileName);
			if (file.exists()) {
				file.delete();
			}
			ObjectOutputStream outStream = new ObjectOutputStream(new FileOutputStream(file));
			outStream.writeObject(object);
			outStream.close();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return file;
	}

	@Override
	public void startAvatarAction(int actionId, float x, float y) {
		if (world != null && actionId < world.getAvatarActions().length && world.getAvatarActions()[actionId] instanceof PauseAction) {
			world.getAvatarActions()[actionId].start();
		} else {
			super.startAvatarAction(actionId, x, y);
		}
	}

	@Override
	public void startWorldAction(int actionId) {
		if (world != null && actionId < world.getWorldActions().length && world.getWorldActions()[actionId] instanceof PauseAction) {
			world.getWorldActions()[actionId].start();
		} else {
			super.startWorldAction(actionId);
		}
	}

	public boolean isCustomizeMode() {
		return customizeMode;
	}

	public void setCustomizeMode(boolean mode) {
		customizeMode = mode;
	}

	public boolean isCustomizable(int worldNum) {
		if (!isWorldUnlocked(worldNum)) {
			return false;
		}
		int id = 0;
		if (worldNum == 1) {
			id = R.string.world1customizable;
		} else if (worldNum == 2) {
			id = R.string.world2customizable;
		} else if (worldNum == 3) {
			id = R.string.world3customizable;
		} else if (worldNum == 4) {
			id = R.string.world4customizable;
		} else if (worldNum == 5) {
			id = R.string.world5customizable;
		} else if (worldNum == 6) {
			id = R.string.world6customizable;
		} else if (worldNum == 7) {
			id = R.string.world7customizable;
		} else if (worldNum == 8) {
			id = R.string.world8customizable;
		} else if (worldNum == 9) {
			id = R.string.world9customizable;
		} else if (worldNum == 10) {
			id = R.string.world10customizable;
		} else if (worldNum == 11) {
			id = R.string.world11customizable;
		} else if (worldNum == 12) {
			id = R.string.world12customizable;
		} else if (worldNum == 13) {
			id = R.string.world13customizable;
		} else if (worldNum == 14) {
			id = R.string.world14customizable;
		} else if (worldNum == 15) {
			id = R.string.world15customizable;
		} else if (worldNum == 16) {
			id = R.string.world16customizable;
		} else if (worldNum == 17) {
			id = R.string.world17customizable;
		} else if (worldNum == 18) {
			id = R.string.world18customizable;
		} else if (worldNum == 19) {
			id = R.string.world19customizable;
		} else if (worldNum == 20) {
			id = R.string.world20customizable;
		} else if (worldNum == 21) {
			id = R.string.world21customizable;
		} else if (worldNum == 22) {
			id = R.string.world22customizable;
		} else if (worldNum == 23) {
			id = R.string.world23customizable;
		} else if (worldNum == 24) {
			id = R.string.world24customizable;
		} else {
			return false;
		}
		if (context == null) {
			return false;
		}
		return "true".equals(context.getString(id));
	}

	public String getAvatarDisplayName(int avatarNum, String name) {
		if (avatarDisplayNames[avatarNum - 1] != null) {
			return avatarDisplayNames[avatarNum - 1];
		}
		return name;
	}

	public void setAvatarDisplayName(int avatarNum, String displayName) {
		avatarDisplayNames[avatarNum - 1] = displayName;
	}

	public void vibrate(int milliseconds) {
		if (isVibration()) {
			Vibrator vibrator = (Vibrator) context.getSystemService(Activity.VIBRATOR_SERVICE);
			if (vibrator != null) {
				vibrator.vibrate(milliseconds);
			}
		}
	}

	public boolean hasTouchScreen() {
		PackageManager pm = context.getPackageManager();
		if (pm.hasSystemFeature(PackageManager.FEATURE_FAKETOUCH)) {
			return true;
		}
		return false;
	}

	private class ReturnValue {
		public int rc;
	}

	@Override
	public int alert(final String title, final String message, final String[] options, final String checkinMessage) {
		if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
			throw new IllegalThreadStateException("This can't be called on the looper thread");
		}
		final ReturnValue returnValue = new ReturnValue();
		getContext().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				final MessageDialog messageDialog = new MessageDialog(getContext(), title, message, options, checkinMessage);
//				currentDialog = messageDialog;
				messageDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
					@Override
					public void onDismiss(DialogInterface dialogInterface) {
						synchronized (returnValue) {
							returnValue.rc = messageDialog.getButtonPressed();
							returnValue.notify();
						}
//						currentDialog = null;
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
	
	public void setLocalFolder(String localFolder) {
		this.localFolder = localFolder;
		savePreferences(this.context);
	}

	public String getLocalFolder() {
		return localFolder;
	}
}
