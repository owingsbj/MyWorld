package com.gallantrealm.myworld.android;

import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.gallantrealm.myworld.client.model.ClientModel;
import com.gallantrealm.myworld.client.model.ClientModelChangedEvent;
import com.gallantrealm.myworld.client.model.ClientModelChangedListener;

public class SelectWorldActivity extends GallantActivity implements View.OnClickListener, ClientModelChangedListener {

	private final ClientModel clientModel = AndroidClientModel.getClientModel();

	View mainLayout;
	TextView titleText;
	View world1Select;
	View world2Select;
	View world3Select;
	View world4Select;
	View world5Select;
	View world6Select;
	View world7Select;
	View world8Select;
	View world9Select;
	View world10Select;
	View world11Select;
	View world12Select;
	Button okButton;
	Button resetButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.select_world_old);

		mainLayout = findViewById(R.id.mainLayout);
		titleText = (TextView) findViewById(R.id.titleText);
		world1Select = findViewById(R.id.world1Select);
		world2Select = findViewById(R.id.world2Select);
		world3Select = findViewById(R.id.world3Select);
		world4Select = findViewById(R.id.world4Select);
		world5Select = findViewById(R.id.world5Select);
		world6Select = findViewById(R.id.world6Select);
		world7Select = findViewById(R.id.world7Select);
		world8Select = findViewById(R.id.world8Select);
		world9Select = findViewById(R.id.world9Select);
		world10Select = findViewById(R.id.world10Select);
		world11Select = findViewById(R.id.world11Select);
		world12Select = findViewById(R.id.world12Select);
		okButton = (Button) findViewById(R.id.okButton);
		resetButton = (Button) findViewById(R.id.resetScoreButton);

		mainLayout.setBackgroundResource(clientModel.getTheme().themeBackgroundId);
		songId = clientModel.getTheme().themeSongId;

		Typeface typeface = clientModel.getTypeface(this);
		if (typeface != null) {
			titleText.setTypeface(typeface);
			okButton.setTypeface(typeface);
			resetButton.setTypeface(typeface);
		}

		int styleId = clientModel.getTheme().buttonStyleId;
		if (styleId != 0) {
			okButton.setBackgroundResource(styleId);
			resetButton.setBackgroundResource(styleId);
		}
		
		int nworlds = Integer.parseInt(getString(R.string.nworlds));
		if (nworlds < 1)
			world1Select.setVisibility(View.GONE);
		if (nworlds < 2)
			world2Select.setVisibility(View.GONE);
		if (nworlds < 3)
			world3Select.setVisibility(View.GONE);
		if (nworlds < 4)
			world4Select.setVisibility(View.GONE);
		if (nworlds < 5)
			world5Select.setVisibility(View.GONE);
		if (nworlds < 6)
			world6Select.setVisibility(View.GONE);
		if (nworlds < 7)
			world7Select.setVisibility(View.GONE);
		if (nworlds < 8)
			world8Select.setVisibility(View.GONE);
		if (nworlds < 9)
			world9Select.setVisibility(View.GONE);
		if (nworlds < 10)
			world10Select.setVisibility(View.GONE);
		if (nworlds < 11)
			world11Select.setVisibility(View.GONE);
		if (nworlds < 12)
			world12Select.setVisibility(View.GONE);

		world1Select.setOnClickListener(this);
		world2Select.setOnClickListener(this);
		world3Select.setOnClickListener(this);
		world4Select.setOnClickListener(this);
		world5Select.setOnClickListener(this);
		world6Select.setOnClickListener(this);
		world7Select.setOnClickListener(this);
		world8Select.setOnClickListener(this);
		world9Select.setOnClickListener(this);
		world10Select.setOnClickListener(this);
		world11Select.setOnClickListener(this);
		world12Select.setOnClickListener(this);
		okButton.setOnClickListener(this);
		resetButton.setOnClickListener(this);

//		if (true) {
//			AlertDialog.Builder builder = new AlertDialog.Builder(this);
//			builder.setMessage("Querying to see if you have full version access.  Please make sure you have internet connection.");
//			builder.setCancelable(false);
//			builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
//				public void onClick(DialogInterface dialog, int id) {
//					billingService.restoreTransactions();
//					dialog.cancel();
//				}
//			});
//			AlertDialog alert = builder.create();
//			alert.show();
//		}

		updateSelectedWorld();
		clientModel.addClientModelChangedListener(this);

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onClick(View v) {
		if (v.equals(world1Select) && clientModel.isWorldUnlocked(1)) {
			clientModel.setWorldName(getString(R.string.world1ClassName));
		} else if (v.equals(world2Select) && clientModel.isWorldUnlocked(2)) {
			clientModel.setWorldName(getString(R.string.world2ClassName));
		} else if (v.equals(world3Select) && clientModel.isWorldUnlocked(3)) {
			clientModel.setWorldName(getString(R.string.world3ClassName));
		} else if (v.equals(world4Select) && clientModel.isWorldUnlocked(4)) {
			clientModel.setWorldName(getString(R.string.world4ClassName));
		} else if (v.equals(world5Select) && clientModel.isWorldUnlocked(5)) {
			clientModel.setWorldName(getString(R.string.world5ClassName));
		} else if (v.equals(world6Select) && clientModel.isWorldUnlocked(6)) {
			clientModel.setWorldName(getString(R.string.world6ClassName));
		} else if (v.equals(world7Select) && clientModel.isWorldUnlocked(7)) {
			clientModel.setWorldName(getString(R.string.world7ClassName));
		} else if (v.equals(world8Select) && clientModel.isWorldUnlocked(8)) {
			clientModel.setWorldName(getString(R.string.world8ClassName));
		} else if (v.equals(world9Select) && clientModel.isWorldUnlocked(9)) {
			clientModel.setWorldName(getString(R.string.world9ClassName));
		} else if (v.equals(world10Select) && clientModel.isWorldUnlocked(10)) {
			clientModel.setWorldName(getString(R.string.world10ClassName));
		} else if (v.equals(world11Select) && clientModel.isWorldUnlocked(11)) {
			clientModel.setWorldName(getString(R.string.world11ClassName));
		} else if (v.equals(world12Select) && clientModel.isWorldUnlocked(12)) {
			clientModel.setWorldName(getString(R.string.world12ClassName));
		} else if (v.equals(okButton)) {
			this.finish();
		} else if (v.equals(resetButton)) {
			String worldName = clientModel.getWorldName();
			if (worldName != null) {
				this.resetScore();
			}
		}
		clientModel.savePreferences(this);
	}

	void resetScore() {
		final int world = getSelectedWorld();
		if (clientModel.getScore(world) > 0) {
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
			} else {
				name = "Selected world";
			}
			final MessageDialog messageDialog = new MessageDialog(this, null, "Reset level for " + name + "?", new String[] { "Yes", "No" }, null);
			messageDialog.show();
			messageDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface dialog) {
					if (messageDialog.getButtonPressed() == 0) {
						clientModel.setScore(world, 0);
						SelectWorldActivity.this.updateScores();
					}
				}
			});
		}
	}

	public void updateScores() {
		((TextView) findViewById(R.id.score1)).setText(getScoreString(1));
		((TextView) findViewById(R.id.score2)).setText(getScoreString(2));
		((TextView) findViewById(R.id.score3)).setText(getScoreString(3));
		((TextView) findViewById(R.id.score4)).setText(getScoreString(4));
		((TextView) findViewById(R.id.score5)).setText(getScoreString(5));
		((TextView) findViewById(R.id.score6)).setText(getScoreString(6));
		((TextView) findViewById(R.id.score7)).setText(getScoreString(7));
		((TextView) findViewById(R.id.score8)).setText(getScoreString(8));
		((TextView) findViewById(R.id.score9)).setText(getScoreString(9));
		((TextView) findViewById(R.id.score10)).setText(getScoreString(10));
		((TextView) findViewById(R.id.score11)).setText(getScoreString(11));
		((TextView) findViewById(R.id.score12)).setText(getScoreString(12));
	}

	public String getScoreString(int i) {
		if (clientModel.isWorldUnlocked(i)) {
			int score = clientModel.getScore(i);
			if (score == 0) {
				return "";
			} else {
				return getString(R.string.scoreLabel) + " " + clientModel.getScore(i);
			}
		} else {
			return "Locked";
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		updateScores();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	public void clientModelChanged(ClientModelChangedEvent event) {
		if (event.getEventType() == ClientModelChangedEvent.EVENT_TYPE_SELECTED_GAME_CHANGED) {
			updateSelectedWorld();
		} else if (event.getEventType() == ClientModelChangedEvent.EVENT_TYPE_FULLVERSION_CHANGED) {
			SelectWorldActivity.this.updateScores();
		}
	}

	public void updateSelectedWorld() {
		String worldName = clientModel.getWorldName();
		if (worldName == null) { // avoid null pointers
			worldName = getString(R.string.world1ClassName);
		}
		int nworlds = Integer.parseInt(getString(R.string.nworlds));
		if (nworlds >= 1)
			world1Select.setBackgroundColor((worldName.equals(getString(R.string.world1ClassName))) ? 0x8040f040 : 0x80404040);
		if (nworlds >= 2)
			world2Select.setBackgroundColor((worldName.equals(getString(R.string.world2ClassName))) ? 0x8040f040 : 0x80404040);
		if (nworlds >= 3)
			world3Select.setBackgroundColor((worldName.equals(getString(R.string.world3ClassName))) ? 0x8040f040 : 0x80404040);
		if (nworlds >= 4)
			world4Select.setBackgroundColor((worldName.equals(getString(R.string.world4ClassName))) ? 0x8040f040 : 0x80404040);
		if (nworlds >= 5)
			world5Select.setBackgroundColor((worldName.equals(getString(R.string.world5ClassName))) ? 0x8040f040 : 0x80404040);
		if (nworlds >= 6)
			world6Select.setBackgroundColor((worldName.equals(getString(R.string.world6ClassName))) ? 0x8040f040 : 0x80404040);
		if (nworlds >= 7)
			world7Select.setBackgroundColor((worldName.equals(getString(R.string.world7ClassName))) ? 0x8040f040 : 0x80404040);
		if (nworlds >= 8)
			world8Select.setBackgroundColor((worldName.equals(getString(R.string.world8ClassName))) ? 0x8040f040 : 0x80404040);
		if (nworlds >= 9)
			world9Select.setBackgroundColor((worldName.equals(getString(R.string.world9ClassName))) ? 0x8040f040 : 0x80404040);
		if (nworlds >= 10)
			world10Select.setBackgroundColor((worldName.equals(getString(R.string.world10ClassName))) ? 0x8040f040 : 0x80404040);
		if (nworlds >= 11)
			world11Select.setBackgroundColor((worldName.equals(getString(R.string.world11ClassName))) ? 0x8040f040 : 0x80404040);
		if (nworlds >= 12)
			world12Select.setBackgroundColor((worldName.equals(getString(R.string.world12ClassName))) ? 0x8040f040 : 0x80404040);
	}

	public int getSelectedWorld() {
		String worldName = clientModel.getWorldName();
		if (worldName == null) { // avoid null pointers
			worldName = getString(R.string.world1ClassName);
		}
		if (worldName.equals(getString(R.string.world1ClassName)))
			return 1;
		if (worldName.equals(getString(R.string.world2ClassName)))
			return 2;
		if (worldName.equals(getString(R.string.world3ClassName)))
			return 3;
		if (worldName.equals(getString(R.string.world4ClassName)))
			return 4;
		if (worldName.equals(getString(R.string.world5ClassName)))
			return 5;
		if (worldName.equals(getString(R.string.world6ClassName)))
			return 6;
		if (worldName.equals(getString(R.string.world7ClassName)))
			return 7;
		if (worldName.equals(getString(R.string.world8ClassName)))
			return 8;
		if (worldName.equals(getString(R.string.world9ClassName)))
			return 9;
		if (worldName.equals(getString(R.string.world10ClassName)))
			return 10;
		if (worldName.equals(getString(R.string.world11ClassName)))
			return 11;
		if (worldName.equals(getString(R.string.world12ClassName)))
			return 12;
		return 1;
	}
}
