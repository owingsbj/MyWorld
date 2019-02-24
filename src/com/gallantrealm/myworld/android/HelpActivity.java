package com.gallantrealm.myworld.android;

import android.annotation.SuppressLint;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;

public class HelpActivity extends GallantActivity {

	AndroidClientModel clientModel = AndroidClientModel.getClientModel();

	View mainLayout;
	TextView titleText;
	WebView helpView;
	Button okButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.help);

		mainLayout = findViewById(R.id.mainLayout);
		titleText = (TextView) findViewById(R.id.titleText);
		helpView = (WebView) findViewById(R.id.helpView);
		okButton = (Button) findViewById(R.id.okButton);

		mainLayout.setBackgroundResource(clientModel.getTheme().themeBackgroundId);
		songId = clientModel.getTheme().themeSongId;

		Typeface typeface = clientModel.getTypeface(this);
		if (typeface != null) {
			titleText.setTypeface(typeface);
			okButton.setTypeface(typeface);
		}

		int styleId = clientModel.getTheme().buttonStyleId;
		if (styleId != 0) {
			okButton.setBackgroundResource(styleId);
		}

		helpView.setOnKeyListener(new View.OnKeyListener() {

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				// For some reason html view sucks up the keys so handling them here as well. 
//				System.out.println("Keycode: " + keyCode);
				if (event.getAction() == KeyEvent.ACTION_DOWN) {
					if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_BUTTON_A || keyCode == KeyEvent.KEYCODE_BUTTON_SELECT) {
						onOkay();
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
				}
				return false;
			}
		});

		okButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				HelpActivity.this.finish();
			}
		});
	}

	@SuppressLint("NewApi")
	@Override
	protected void onStart() {
		super.onStart();
		if (clientModel.useMoga(this)) {
			helpView.loadUrl("file:///android_asset/mogahelp.html");
		} else {
			helpView.loadUrl("file:///android_asset/help.html");
		}
		// use a transparent background for help backgrounds that are semi-transarent to show through
		helpView.setBackgroundColor(0x80404040);
		if (Build.VERSION.SDK_INT >= 11) {
			helpView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

}
