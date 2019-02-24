package com.gallantrealm.myworld.android;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.gallantrealm.android.Translator;

public class GallantSplashActivity extends GallantActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.gallant_splash);

		clientModel.loadPreferences(this); // to prepare model for use later

		clientModel.setGoggleDogPass(false);
		View goggleDog = findViewById(R.id.goggleDog);
		if (goggleDog != null) {
			goggleDog.setClickable(true);
			goggleDog.setOnLongClickListener(new View.OnLongClickListener() {
				public boolean onLongClick(View v) {
					clientModel.setGoggleDogPass(true);
					return false;
				}
			});
		}

// HeyzapLib.setFlags(1 << 23); // turn off Heyzap notification

		clientModel.initAds(); // prepare ads (if any)

		Translator.getTranslator().translate(this.getWindow().getDecorView());
	}

	public void showMainMenu() {
		try {
			Intent intent = new Intent(GallantSplashActivity.this, GallantSplashActivity.this.getClassLoader().loadClass(getString(R.string.mainMenuClassName)));
			intent.setData(getIntent().getData()); // pass along invokation params
			startActivity(intent);
			GallantSplashActivity.this.finish();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	Timer t;

	@Override
	protected void onStart() {
		super.onStart();
		t = new Timer();
		t.schedule(new TimerTask() {
			@Override
			public void run() {
				clientModel.updatePlayCount(GallantSplashActivity.this);
// if (!clientModel.isFullVersion() && clientModel.getPlayCount() % 3 == 0) {
// GallantSplashActivity.this.runOnUiThread(new Runnable() {
// @Override
// public void run() {
// try {
// final MessageDialog messageDialog = new MessageDialog(GallantSplashActivity.this, null, //
// getString(R.string.buyMessage), //
// new String[] { "Buy", /* "Rate", */"Skip" }, null);
// messageDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
// @Override
// public void onDismiss(DialogInterface dialogInterface) {
// int rc = messageDialog.getButtonPressed();
// if (rc == 0) { // buy
// boolean launched = clientModel.buyFullVersion();
// if (!launched) {
// final MessageDialog messageDialog = new MessageDialog(GallantSplashActivity.this, null, "There is a problem with accessing the store.\nCheck your network connection and try again.", new String[] { "OK" }, null);
// messageDialog.show();
// messageDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
// @Override
// public void onDismiss(DialogInterface dialog) {
// showMainMenu();
// }
// });
// } else {
// if (clientModel.isAmazon()) {
// showMainMenu();
// } else {
// // main will be shown once billing is done
// }
// }
// // } else if (rc == 1) { // rate
// // Uri uri;
// // if (clientModel.isGoogle()) {
// // uri = Uri.parse("market://details?id=" + GallantSplashActivity.this.getPackageName());
// // } else if (clientModel.isAmazon()) {
// // uri = Uri.parse("amzn://apps/android?p=" + GallantSplashActivity.this.getPackageName());
// // } else {
// // uri = Uri.parse("market://details?id=" + GallantSplashActivity.this.getPackageName());
// // }
// // Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
// // try {
// // System.out.println("Going to market");
// // startActivity(goToMarket);
// // } catch (ActivityNotFoundException e) {
// // e.printStackTrace();
// // Toast.makeText(GallantSplashActivity.this, "Couldn't launch store.", Toast.LENGTH_LONG).show();
// // }
// } else { // skip
// showMainMenu();
// }
// }
// });
// messageDialog.show();
// } catch (Exception e) {
// showMainMenu();
// }
// }
// });
// } else {
				showMainMenu();
// }
			}
		}, 2000l);
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (t != null) {
			t.cancel();
			t = null;
		}
	}

}
