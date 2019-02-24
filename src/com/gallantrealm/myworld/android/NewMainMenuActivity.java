package com.gallantrealm.myworld.android;

import android.content.Intent;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class NewMainMenuActivity extends GallantActivity {

	public View mainLayout;
	public TextView titleText;
	public Button chooseAvatarButton;
	public Button chooseWorldButton;
	public Button playButton;
	public Button settingsButton;
	public Button quitButton;
	public Button helpButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getString(R.string.navatars).equals("0")) {
			setContentView(R.layout.new_main_menu_no_avatar);
		} else {
			setContentView(R.layout.new_main_menu);
		}

		clientModel.loadPreferences(this);

		this.setVolumeControlStream(AudioManager.STREAM_MUSIC);

		mainLayout = findViewById(R.id.mainLayout);
		titleText = (TextView) findViewById(R.id.titleText);
// plusOneButton = (PlusOneButton) findViewById(R.id.plus_one_button);
		chooseAvatarButton = (Button) findViewById(R.id.chooseAvatarButton);
		chooseWorldButton = (Button) findViewById(R.id.chooseWorldButton);
		playButton = (Button) findViewById(R.id.playButton);
		settingsButton = (Button) findViewById(R.id.settingsButton);
		quitButton = (Button) findViewById(R.id.quitButton);
		helpButton = (Button) findViewById(R.id.helpButton);

		mainLayout.setBackgroundResource(clientModel.getTheme().themeBackgroundId);
		songId = clientModel.getTheme().themeSongId;

		Typeface typeface = clientModel.getTypeface(this);
		if (typeface != null) {
			titleText.setTypeface(typeface);
			chooseAvatarButton.setTypeface(typeface);
			chooseWorldButton.setTypeface(typeface);
			playButton.setTypeface(typeface);
			settingsButton.setTypeface(typeface);
			quitButton.setTypeface(typeface);
			helpButton.setTypeface(typeface);
		}

		int styleId = clientModel.getTheme().buttonStyleId;
		if (styleId != 0) {
			chooseAvatarButton.setBackgroundResource(styleId);
			chooseWorldButton.setBackgroundResource(styleId);
			playButton.setBackgroundResource(styleId);
			settingsButton.setBackgroundResource(styleId);
			quitButton.setBackgroundResource(styleId);
			helpButton.setBackgroundResource(styleId);
		}

		chooseAvatarButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					Class activityClass = NewMainMenuActivity.this.getClassLoader().loadClass(getString(R.string.selectAvatarClassName));
					Intent intent = new Intent(NewMainMenuActivity.this, activityClass);
					startActivity(intent);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		chooseWorldButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					Class activityClass = NewMainMenuActivity.this.getClassLoader().loadClass(getString(R.string.selectWorldClassName));
					Intent intent = new Intent(NewMainMenuActivity.this, activityClass);
					startActivity(intent);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		playButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				clientModel.setCustomizeMode(false);
				Intent intent = new Intent(NewMainMenuActivity.this, StartWorldActivity.class);
				startActivity(intent);
			}
		});
		settingsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(NewMainMenuActivity.this, SettingsActivity.class);
				startActivity(intent);
			}
		});
		helpButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(NewMainMenuActivity.this, HelpActivity.class);
				startActivity(intent);
			}
		});
		quitButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				NewMainMenuActivity.this.finish();
			}
		});

		if (!clientModel.isFullVersion()) {
			unlockNextWorld();
		}
	}

	protected void unlockNextWorld() {
		if (System.currentTimeMillis() > clientModel.getLastPlayTime() + 1000 * 60 * 60) {
			int nworlds = Integer.parseInt(getString(R.string.nworlds));
			for (int i = 0; i < nworlds; i++) {
				int worldToUnlock = i + 1;
				if (!clientModel.isWorldUnlocked(worldToUnlock)) {
					clientModel.setWorldUnlocked(worldToUnlock);
					String worldName = getWorldName(worldToUnlock);
					clientModel.setWorldName(getWorldClassName(worldToUnlock));
					final MessageDialog messageDialog = new MessageDialog(NewMainMenuActivity.this, null, //
							"Thanks for playing " + getString(R.string.app_name) + ".  We've unlocked " + worldName + " for you to play this time.  Enjoy!", new String[] { "OK" }, null);
					messageDialog.show();
					break;
				}
			}
			clientModel.updateLastPlayTime(this);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	public static boolean showPopupAd;

	@Override
	protected void onStart() {
		super.onStart();
		if (showPopupAd) {
			clientModel.showPopupAd();
			showPopupAd = false;
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
// plusOneButton.initialize("https://play.google.com/store/apps/details?id=" + getPackageName(), 1);
	}

	public String getWorldName(int world) {
		String name;
		if (world == 1) {
			name = getString(R.string.world1Name);
		} else if (world == 2) {
			name = getString(R.string.world2Name);
		} else if (world == 3) {
			name = getString(R.string.world3Name);
		} else if (world == 4) {
			name = getString(R.string.world4Name);
		} else if (world == 5) {
			name = getString(R.string.world5Name);
		} else if (world == 6) {
			name = getString(R.string.world6Name);
		} else if (world == 7) {
			name = getString(R.string.world7Name);
		} else if (world == 8) {
			name = getString(R.string.world8Name);
		} else if (world == 9) {
			name = getString(R.string.world9Name);
		} else if (world == 10) {
			name = getString(R.string.world10Name);
		} else if (world == 11) {
			name = getString(R.string.world11Name);
		} else if (world == 12) {
			name = getString(R.string.world12Name);
		} else if (world == 13) {
			name = getString(R.string.world13Name);
		} else if (world == 14) {
			name = getString(R.string.world14Name);
		} else if (world == 15) {
			name = getString(R.string.world15Name);
		} else if (world == 16) {
			name = getString(R.string.world16Name);
		} else if (world == 17) {
			name = getString(R.string.world17Name);
		} else if (world == 18) {
			name = getString(R.string.world18Name);
		} else if (world == 19) {
			name = getString(R.string.world19Name);
		} else if (world == 20) {
			name = getString(R.string.world20Name);
		} else if (world == 21) {
			name = getString(R.string.world21Name);
		} else if (world == 22) {
			name = getString(R.string.world22Name);
		} else if (world == 23) {
			name = getString(R.string.world23Name);
		} else if (world == 24) {
			name = getString(R.string.world24Name);
		} else {
			name = "Selected world";
		}
		return name;
	}

	public String getWorldClassName(int world) {
		String name;
		if (world == 1) {
			name = getString(R.string.world1ClassName);
		} else if (world == 2) {
			name = getString(R.string.world2ClassName);
		} else if (world == 3) {
			name = getString(R.string.world3ClassName);
		} else if (world == 4) {
			name = getString(R.string.world4ClassName);
		} else if (world == 5) {
			name = getString(R.string.world5ClassName);
		} else if (world == 6) {
			name = getString(R.string.world6ClassName);
		} else if (world == 7) {
			name = getString(R.string.world7ClassName);
		} else if (world == 8) {
			name = getString(R.string.world8ClassName);
		} else if (world == 9) {
			name = getString(R.string.world9ClassName);
		} else if (world == 10) {
			name = getString(R.string.world10ClassName);
		} else if (world == 11) {
			name = getString(R.string.world11ClassName);
		} else if (world == 12) {
			name = getString(R.string.world12ClassName);
		} else if (world == 13) {
			name = getString(R.string.world13ClassName);
		} else if (world == 14) {
			name = getString(R.string.world14ClassName);
		} else if (world == 15) {
			name = getString(R.string.world15ClassName);
		} else if (world == 16) {
			name = getString(R.string.world16ClassName);
		} else if (world == 17) {
			name = getString(R.string.world17ClassName);
		} else if (world == 18) {
			name = getString(R.string.world18ClassName);
		} else if (world == 19) {
			name = getString(R.string.world19ClassName);
		} else if (world == 20) {
			name = getString(R.string.world20ClassName);
		} else if (world == 21) {
			name = getString(R.string.world21ClassName);
		} else if (world == 22) {
			name = getString(R.string.world22ClassName);
		} else if (world == 23) {
			name = getString(R.string.world23ClassName);
		} else if (world == 24) {
			name = getString(R.string.world24ClassName);
		} else {
			name = "Selected world";
		}
		return name;
	}

}
