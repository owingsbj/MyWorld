package com.gallantrealm.myworld.android;

import com.gallantrealm.myworld.client.model.ClientModelChangedEvent;
import com.gallantrealm.myworld.client.model.ClientModelChangedListener;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;

public class SettingsActivity extends GallantActivity implements View.OnClickListener, ClientModelChangedListener, AdapterView.OnItemSelectedListener {

	View mainLayout;
	TextView titleText;
	CheckBox playMusicCheckBox;
	CheckBox playSoundEffectsCheckBox;
	CheckBox vibrationCheckBox;
	TextView controlTypeLabel;
	Spinner controlType;
	SeekBar controlSensitivity;
	CheckBox viewIn3dCheckBox;
	CheckBox simpleRenderingCheckBox;
	CheckBox powerSaverCheckBox;
	Button okButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.settings);

		mainLayout = findViewById(R.id.mainLayout);
		titleText = (TextView) findViewById(R.id.titleText);
		simpleRenderingCheckBox = (CheckBox) findViewById(R.id.simpleRenderingCheckBox);
		playMusicCheckBox = (CheckBox) findViewById(R.id.playMusicCheckBox);
		playSoundEffectsCheckBox = (CheckBox) findViewById(R.id.playSoundEffectsCheckBox);
		vibrationCheckBox = (CheckBox) findViewById(R.id.vibrationCheckBox);
		controlTypeLabel = (TextView) findViewById(R.id.controlTypeLabel);
		controlType = (Spinner) findViewById(R.id.controlType);
		controlSensitivity = (SeekBar) findViewById(R.id.controlSensitivity);
		viewIn3dCheckBox = (CheckBox) findViewById(R.id.viewIn3dCheckbox);
		simpleRenderingCheckBox = (CheckBox) findViewById(R.id.simpleRenderingCheckBox);
		powerSaverCheckBox = (CheckBox) findViewById(R.id.powerSaverCheckBox);
		okButton = (Button) findViewById(R.id.okButton);

		// shadows aren't working yet.
		// showShadowsCheckBox.setVisibility(View.GONE);

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
		
		// only need to show option for simple rendering when there is GLES20
		clientModel.setWorld(null);
		if (!clientModel.supportsOpenGLES20()) {
			simpleRenderingCheckBox.setVisibility(View.GONE);
		}
		
		// power saver no longer featured
		powerSaverCheckBox.setVisibility(View.GONE);

		if (clientModel.useMoga(this) || clientModel.useGamepad(this) ||  !getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER)) {
			controlTypeLabel.setVisibility(View.GONE);
			controlType.setVisibility(View.GONE);
		} else {
			ArrayAdapter<CharSequence> controlTypeAdapter;
			if (getString(R.string.supportsScreenController).equals("true") && getPackageManager().hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)) {
				controlTypeAdapter = new ArrayAdapter(this, R.layout.spinner_item, new String[] { "Tilt", "Screen Left", "Screen Right", "Gamepad", "Zeemote" });
			} else {
				controlTypeAdapter = new ArrayAdapter(this, R.layout.spinner_item, new String[] { "Tilt", "Gamepad", "Zeemote" });
			}
			controlTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			controlType.setAdapter(controlTypeAdapter);
			controlType.setOnItemSelectedListener(this);
		}

		controlSensitivity.setProgress((int) (clientModel.getControlSensitivity() * 100));
		controlSensitivity.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				clientModel.setControlSensitivity(progress / 100.0f);
				clientModel.savePreferences(SettingsActivity.this);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}

		});

		simpleRenderingCheckBox.setOnClickListener(this);
		playMusicCheckBox.setOnClickListener(this);
		playSoundEffectsCheckBox.setOnClickListener(this);
		vibrationCheckBox.setOnClickListener(this);
		viewIn3dCheckBox.setOnClickListener(this);
		simpleRenderingCheckBox.setOnClickListener(this);
		powerSaverCheckBox.setOnClickListener(this);
		okButton.setOnClickListener(this);

		clientModel.loadPreferences(this);

		updateSettings();
		clientModel.addClientModelChangedListener(this);
	}

	@Override
	public void onClick(View v) {
		if (v.equals(simpleRenderingCheckBox)) {
			clientModel.setSimpleRendering(simpleRenderingCheckBox.isChecked());
			clientModel.savePreferences(this);
			updateSettings();
		} else if (v.equals(playMusicCheckBox)) {
			clientModel.setPlayMusic(playMusicCheckBox.isChecked());
			clientModel.savePreferences(this);
			updateSettings();
			if (clientModel.isPlayMusic()) {
				clientModel.playSong(clientModel.getTheme().themeSongId);
			} else {
				clientModel.pauseSong();
			}
		} else if (v.equals(playSoundEffectsCheckBox)) {
			clientModel.setPlaySoundEffects(playSoundEffectsCheckBox.isChecked());
			clientModel.savePreferences(this);
			updateSettings();
		} else if (v.equals(vibrationCheckBox)) {
			clientModel.setVibration(vibrationCheckBox.isChecked());
			clientModel.savePreferences(this);
			updateSettings();
		} else if (v.equals(viewIn3dCheckBox)) {
			clientModel.setStereoscopic(viewIn3dCheckBox.isChecked());
			clientModel.savePreferences(this);
			updateSettings();
		} else if (v.equals(simpleRenderingCheckBox)) {
			clientModel.setSimpleRendering(simpleRenderingCheckBox.isChecked());
			clientModel.savePreferences(this);
			updateSettings();
		} else if (v.equals(powerSaverCheckBox)) {
			clientModel.setPowerSaver(powerSaverCheckBox.isChecked());
			clientModel.savePreferences(this);
			updateSettings();
		} else if (v.equals(okButton)) {
			this.finish();
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	public void clientModelChanged(ClientModelChangedEvent event) {
	}

	public void updateSettings() {
		playMusicCheckBox.setChecked(clientModel.isPlayMusic());
		playSoundEffectsCheckBox.setChecked(clientModel.isPlaySoundEffects());
		vibrationCheckBox.setChecked(clientModel.isVibration());
// SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
// if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) == null) {
		if (clientModel.useSensors()) {
			controlType.setSelection(0);
		} else if (clientModel.useScreenControl()) {
			if (clientModel.isControlOnLeft()) {
				controlType.setSelection(1);
			} else {
				controlType.setSelection(2);
			}
		} else if (clientModel.useZeemote()) {
			controlType.setSelection(4);
		} else {
			controlType.setSelection(3);
		}
// }
		viewIn3dCheckBox.setChecked(clientModel.isStereoscopic());
		simpleRenderingCheckBox.setChecked(clientModel.isSimpleRendering());
		powerSaverCheckBox.setChecked(clientModel.isPowerSaver());
	}

	@Override
	public void onItemSelected(AdapterView av, View view, int arg1, long arg2) {
		String type = (String)controlType.getSelectedItem();
		// "Tilt", "Screen Left", "Screen Right", "Zeemote", "Gamepad"
		if (type == null) {
			return;
		} else if  (type.equals("Tilt")) { // tilt
			clientModel.setUseScreenControl(false);
			clientModel.setControlOnLeft(false);
			clientModel.setUseSensors(true);
			clientModel.setUseZeemote(false);
			clientModel.savePreferences(this);
		} else if (type.equals("Screen Left")) { // left screen
			clientModel.setUseScreenControl(true);
			clientModel.setControlOnLeft(true);
			clientModel.setUseSensors(false);
			clientModel.setUseZeemote(false);
			clientModel.savePreferences(this);
		} else if (type.equals("Screen Right")) { // right screen
			clientModel.setUseScreenControl(true);
			clientModel.setControlOnLeft(false);
			clientModel.setUseSensors(false);
			clientModel.setUseZeemote(false);
			clientModel.savePreferences(this);
		} else if (type.equals("Zeemote")) { // zeemote
			clientModel.setUseScreenControl(false);
			clientModel.setControlOnLeft(false);
			clientModel.setUseSensors(false);
			clientModel.setUseZeemote(true);
			clientModel.savePreferences(this);
		} else if (type.equals("Gamepad")) { // gamepad
			clientModel.setUseScreenControl(false);
			clientModel.setControlOnLeft(false);
			clientModel.setUseSensors(false);
			clientModel.setUseZeemote(false);
			clientModel.savePreferences(this);
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub

	}

}
