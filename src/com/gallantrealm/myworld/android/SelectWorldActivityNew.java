package com.gallantrealm.myworld.android;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.gallantrealm.myworld.FastMath;
import com.gallantrealm.myworld.client.model.ClientModelChangedEvent;
import com.gallantrealm.myworld.client.model.ClientModelChangedListener;

public class SelectWorldActivityNew extends GallantActivity implements View.OnClickListener, ClientModelChangedListener, OnGestureListener {

	int nworlds;
	int currentWorldNum;

	View mainLayout;
	TextView titleText;
	TextView countText;
	Button previousButton;
	Button nextButton;
	View selectedView;
	TextView worldNameText;
	ImageView worldImage;
	TextView worldDescriptionText;
	TextView worldScoreText;
	Button okButton;
	Button resetButton;
	Button customizeButton;

	GestureDetector gestureDetector;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.select_world);

		mainLayout = findViewById(R.id.mainLayout);
		titleText = (TextView) findViewById(R.id.titleText);
		countText = (TextView) findViewById(R.id.countText);
		selectedView = findViewById(R.id.selectedView);
		worldNameText = (TextView) findViewById(R.id.avatarNameText);
		worldImage = (ImageView) findViewById(R.id.avatarImage);
		worldDescriptionText = (TextView) findViewById(R.id.avatarDescriptionText);
		worldScoreText = (TextView) findViewById(R.id.worldScoreText);
		previousButton = (Button) findViewById(R.id.previousButton);
		nextButton = (Button) findViewById(R.id.nextButton);
		okButton = (Button) findViewById(R.id.okButton);
		resetButton = (Button) findViewById(R.id.resetScoreButton);
		customizeButton = (Button) findViewById(R.id.customizeButton);

		mainLayout.setBackgroundResource(clientModel.getTheme().themeBackgroundId);
		songId = clientModel.getTheme().themeSongId;

		Typeface typeface = clientModel.getTypeface(this);
		if (typeface != null) {
			titleText.setTypeface(typeface);
			countText.setTypeface(typeface);
			worldNameText.setTypeface(typeface);
			okButton.setTypeface(typeface);
			resetButton.setTypeface(typeface);
			customizeButton.setTypeface(typeface);
		}

		int styleId = clientModel.getTheme().buttonStyleId;
		if (styleId != 0) {
			okButton.setBackgroundResource(styleId);
			resetButton.setBackgroundResource(styleId);
			customizeButton.setBackgroundResource(styleId);
		}

		previousButton.setOnClickListener(this);
		nextButton.setOnClickListener(this);
		okButton.setOnClickListener(this);
		resetButton.setOnClickListener(this);
		customizeButton.setOnClickListener(this);

		nworlds = Integer.parseInt(getString(R.string.nworlds));
		currentWorldNum = getSelectedWorldNum();
		updateUI();
		clientModel.addClientModelChangedListener(this);

		gestureDetector = new GestureDetector(this, this);
	}

	@Override
	public void onClick(View v) {
		if (v.equals(nextButton)) {
			onNext();
		} else if (v.equals(previousButton)) {
			onPrevious();
		} else if (v.equals(okButton)) {
			if (!clientModel.isWorldUnlocked(currentWorldNum)) {
				(new MessageDialog(this, null, "Sorry, can't select because it is locked.", new String[] { "OK"})).show();
			} else {
				this.finish();
			}
		} else if (v.equals(resetButton)) {
			String worldName = clientModel.getWorldName();
			if (worldName != null) {
				this.resetScore();
			}
		} else if (v.equals(customizeButton)) {
			System.out.println("launching world for customization");
			clientModel.setCustomizeMode(true);
			Intent intent = new Intent(SelectWorldActivityNew.this, StartWorldActivity.class);
			startActivity(intent);
		}
		clientModel.savePreferences(this);
		updateUI();
	}

	@Override
	public void onNext() {
		if (currentWorldNum < nworlds) {
			Animation animation = new TranslateAnimation(selectedView.getWidth() / 4, 0, 0, 0);
			animation.setDuration(250);
			selectedView.startAnimation(animation);
			currentWorldNum += 1;
			if (clientModel.isWorldUnlocked(currentWorldNum)) {
				setSelectedWorldNum(currentWorldNum);
			}
		}
	}

	@Override
	public void onPrevious() {
		if (currentWorldNum > 1) {
			Animation animation = new TranslateAnimation(-selectedView.getWidth() / 4, 0, 0, 0);
			animation.setDuration(250);
			selectedView.startAnimation(animation);
			currentWorldNum -= 1;
			if (clientModel.isWorldUnlocked(currentWorldNum)) {
				setSelectedWorldNum(currentWorldNum);
			}
		}
	}

	void resetScore() {
		if (!clientModel.isWorldUnlocked(currentWorldNum)) {
			return;
		}
		final int world = getSelectedWorldNum();
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
			final MessageDialog messageDialog = new MessageDialog(this, null, "Reset level or score for " + name + "?", new String[] { "Yes", "No" }, null);
			messageDialog.show();
			messageDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface dialog) {
					if (messageDialog.getButtonPressed() == 0) {
						clientModel.setScore(world, 0);
						clientModel.setLevel(world, 0);
						SelectWorldActivityNew.this.updateUI();
					}
				}
			});
		}
	}

	@SuppressLint("NewApi")
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
		if (event.getEventType() == ClientModelChangedEvent.EVENT_TYPE_SELECTED_GAME_CHANGED || event.getEventType() == ClientModelChangedEvent.EVENT_TYPE_FULLVERSION_CHANGED) {
			updateUI();
		}
	}

	public int getSelectedWorldNum() {
		String worldName = clientModel.getWorldName();
		if (getString(R.string.world1ClassName).equals(worldName)) {
			return 1;
		}
		if (getString(R.string.world2ClassName).equals(worldName)) {
			return 2;
		}
		if (getString(R.string.world3ClassName).equals(worldName)) {
			return 3;
		}
		if (getString(R.string.world4ClassName).equals(worldName)) {
			return 4;
		}
		if (getString(R.string.world5ClassName).equals(worldName)) {
			return 5;
		}
		if (getString(R.string.world6ClassName).equals(worldName)) {
			return 6;
		}
		if (getString(R.string.world7ClassName).equals(worldName)) {
			return 7;
		}
		if (getString(R.string.world8ClassName).equals(worldName)) {
			return 8;
		}
		if (getString(R.string.world9ClassName).equals(worldName)) {
			return 9;
		}
		if (getString(R.string.world10ClassName).equals(worldName)) {
			return 10;
		}
		if (getString(R.string.world11ClassName).equals(worldName)) {
			return 11;
		}
		if (getString(R.string.world12ClassName).equals(worldName)) {
			return 12;
		}
		if (getString(R.string.world13ClassName).equals(worldName)) {
			return 13;
		}
		if (getString(R.string.world14ClassName).equals(worldName)) {
			return 14;
		}
		if (getString(R.string.world15ClassName).equals(worldName)) {
			return 15;
		}
		if (getString(R.string.world16ClassName).equals(worldName)) {
			return 16;
		}
		if (getString(R.string.world17ClassName).equals(worldName)) {
			return 17;
		}
		if (getString(R.string.world18ClassName).equals(worldName)) {
			return 18;
		}
		if (getString(R.string.world19ClassName).equals(worldName)) {
			return 19;
		}
		if (getString(R.string.world20ClassName).equals(worldName)) {
			return 20;
		}
		if (getString(R.string.world21ClassName).equals(worldName)) {
			return 21;
		}
		if (getString(R.string.world22ClassName).equals(worldName)) {
			return 22;
		}
		if (getString(R.string.world23ClassName).equals(worldName)) {
			return 23;
		}
		if (getString(R.string.world24ClassName).equals(worldName)) {
			return 24;
		}
		return 1;
	}

	public void setSelectedWorldNum(int n) {
		String worldName = "";
		if (n == 1) {
			worldName = getString(R.string.world1ClassName);
		} else if (n == 2) {
			worldName = getString(R.string.world2ClassName);
		} else if (n == 3) {
			worldName = getString(R.string.world3ClassName);
		} else if (n == 4) {
			worldName = getString(R.string.world4ClassName);
		} else if (n == 5) {
			worldName = getString(R.string.world5ClassName);
		} else if (n == 6) {
			worldName = getString(R.string.world6ClassName);
		} else if (n == 7) {
			worldName = getString(R.string.world7ClassName);
		} else if (n == 8) {
			worldName = getString(R.string.world8ClassName);
		} else if (n == 9) {
			worldName = getString(R.string.world9ClassName);
		} else if (n == 10) {
			worldName = getString(R.string.world10ClassName);
		} else if (n == 11) {
			worldName = getString(R.string.world11ClassName);
		} else if (n == 12) {
			worldName = getString(R.string.world12ClassName);
		} else if (n == 13) {
			worldName = getString(R.string.world13ClassName);
		} else if (n == 14) {
			worldName = getString(R.string.world14ClassName);
		} else if (n == 15) {
			worldName = getString(R.string.world15ClassName);
		} else if (n == 16) {
			worldName = getString(R.string.world16ClassName);
		} else if (n == 17) {
			worldName = getString(R.string.world17ClassName);
		} else if (n == 18) {
			worldName = getString(R.string.world18ClassName);
		} else if (n == 19) {
			worldName = getString(R.string.world19ClassName);
		} else if (n == 20) {
			worldName = getString(R.string.world20ClassName);
		} else if (n == 21) {
			worldName = getString(R.string.world21ClassName);
		} else if (n == 22) {
			worldName = getString(R.string.world22ClassName);
		} else if (n == 23) {
			worldName = getString(R.string.world23ClassName);
		} else if (n == 24) {
			worldName = getString(R.string.world24ClassName);
		}
		clientModel.setWorldName(worldName);
	}

	Bitmap oldBitmap;

	public void updateUI() {
		int n = currentWorldNum;
		int worldNameR;
		int worldImageR;
		int worldDescriptionR;
		if (n == 1) {
			worldNameR = R.string.world1Name;
			worldImageR = R.raw.world1;
			worldDescriptionR = R.string.world1description;
		} else if (n == 2) {
			worldNameR = R.string.world2Name;
			worldImageR = R.raw.world2;
			worldDescriptionR = R.string.world2description;
		} else if (n == 3) {
			worldNameR = R.string.world3Name;
			worldImageR = R.raw.world3;
			worldDescriptionR = R.string.world3description;
		} else if (n == 4) {
			worldNameR = R.string.world4Name;
			worldImageR = R.raw.world4;
			worldDescriptionR = R.string.world4description;
		} else if (n == 5) {
			worldNameR = R.string.world5Name;
			worldImageR = R.raw.world5;
			worldDescriptionR = R.string.world5description;
		} else if (n == 6) {
			worldNameR = R.string.world6Name;
			worldImageR = R.raw.world6;
			worldDescriptionR = R.string.world6description;
		} else if (n == 7) {
			worldNameR = R.string.world7Name;
			worldImageR = R.raw.world7;
			worldDescriptionR = R.string.world7description;
		} else if (n == 8) {
			worldNameR = R.string.world8Name;
			worldImageR = R.raw.world8;
			worldDescriptionR = R.string.world8description;
		} else if (n == 9) {
			worldNameR = R.string.world9Name;
			worldImageR = R.raw.world9;
			worldDescriptionR = R.string.world9description;
		} else if (n == 10) {
			worldNameR = R.string.world10Name;
			worldImageR = R.raw.world10;
			worldDescriptionR = R.string.world10description;
		} else if (n == 11) {
			worldNameR = R.string.world11Name;
			worldImageR = R.raw.world11;
			worldDescriptionR = R.string.world11description;
		} else if (n == 12) {
			worldNameR = R.string.world12Name;
			worldImageR = R.raw.world12;
			worldDescriptionR = R.string.world12description;
		} else if (n == 13) {
			worldNameR = R.string.world13Name;
			worldImageR = R.raw.world13;
			worldDescriptionR = R.string.world13description;
		} else if (n == 14) {
			worldNameR = R.string.world14Name;
			worldImageR = R.raw.world14;
			worldDescriptionR = R.string.world14description;
		} else if (n == 15) {
			worldNameR = R.string.world15Name;
			worldImageR = R.raw.world15;
			worldDescriptionR = R.string.world15description;
		} else if (n == 16) {
			worldNameR = R.string.world16Name;
			worldImageR = R.raw.world16;
			worldDescriptionR = R.string.world16description;
		} else if (n == 17) {
			worldNameR = R.string.world17Name;
			worldImageR = R.raw.world17;
			worldDescriptionR = R.string.world17description;
		} else if (n == 18) {
			worldNameR = R.string.world18Name;
			worldImageR = R.raw.world18;
			worldDescriptionR = R.string.world18description;
		} else if (n == 19) {
			worldNameR = R.string.world19Name;
			worldImageR = R.raw.world19;
			worldDescriptionR = R.string.world19description;
		} else if (n == 20) {
			worldNameR = R.string.world20Name;
			worldImageR = R.raw.world20;
			worldDescriptionR = R.string.world20description;
		} else if (n == 21) {
			worldNameR = R.string.world21Name;
			worldImageR = R.raw.world21;
			worldDescriptionR = R.string.world21description;
		} else if (n == 22) {
			worldNameR = R.string.world22Name;
			worldImageR = R.raw.world22;
			worldDescriptionR = R.string.world22description;
		} else if (n == 23) {
			worldNameR = R.string.world23Name;
			worldImageR = R.raw.world23;
			worldDescriptionR = R.string.world23description;
		} else { //24
			worldNameR = R.string.world24Name;
			worldImageR = R.raw.world24;
			worldDescriptionR = R.string.world24description;
		}
		worldNameText.setText(getString(worldNameR));
		worldImage.setImageBitmap(null);
		if (oldBitmap != null) {
			oldBitmap.recycle();
			oldBitmap = null;
		}
		try {
			Bitmap bitmap = BitmapFactory.decodeResource(getResources(), worldImageR);
			worldImage.setImageBitmap(bitmap);
			oldBitmap = bitmap;
		} catch (Exception e) {
			e.printStackTrace();
		}
		worldDescriptionText.setText(getString(worldDescriptionR));
		worldScoreText.setText(getScoreString(n));
		if (n <= 1) {
			previousButton.setVisibility(View.INVISIBLE);
		} else {
			previousButton.setVisibility(View.VISIBLE);
		}
		if (n >= nworlds) {
			nextButton.setVisibility(View.INVISIBLE);
		} else {
			nextButton.setVisibility(View.VISIBLE);
		}
		countText.setText("" + n + " of " + nworlds);
		if (clientModel.isCustomizable(n)) {
			customizeButton.setVisibility(View.VISIBLE);
		} else {
			customizeButton.setVisibility(View.GONE);
		}
	}

	public String getScoreString(int i) {
		if (clientModel.isWorldUnlocked(i)) {
			String scoreString = "";
			int level = clientModel.getLevel(i);
			if (level > 0) {
				scoreString += "Level: " + level + "  ";
			}
			int time = clientModel.getTime(i);
			if (time > 0) {
				scoreString += "Time: " + formatTime(time) + "  ";
			}
			int score = clientModel.getScore(i);
			if (score > 0) {
				scoreString += getString(R.string.scoreLabel) + " " + score;
			}
			return scoreString;
		} else {
			return "Locked";
		}
	}

	public static String formatTime(long millis) {
		Date time = new Date(millis);
		SimpleDateFormat sdf = new SimpleDateFormat("mm:ss.S");
		String timeString = sdf.format(time);
		if (timeString.endsWith(".")) {
			timeString += "0";
		}
		timeString = timeString.substring(0, 7);
		return timeString;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		gestureDetector.onTouchEvent(event);
		return super.onTouchEvent(event);
	}

	@Override
	public boolean onDown(MotionEvent e) {
		return false;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		if (Math.abs(velocityX) > Math.abs(velocityY)) {
			if (velocityX < 0) {
				onNext();
			} else if (velocityX > 0) {
				onPrevious();
			}
		}
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		return false;
	}

}
