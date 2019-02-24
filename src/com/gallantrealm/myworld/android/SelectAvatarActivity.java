package com.gallantrealm.myworld.android;

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

public class SelectAvatarActivity extends GallantActivity implements View.OnClickListener, ClientModelChangedListener, OnGestureListener {

	int navatars;

	View mainLayout;
	TextView titleText;
	Button okButton;
	Button previousButton;
	Button nextButton;
	View selectedView;
	TextView avatarNameText;
	ImageView avatarImage;
	TextView avatarDescriptionText;

	GestureDetector gestureDetector;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.select_avatar);

		mainLayout = findViewById(R.id.mainLayout);
		titleText = (TextView) findViewById(R.id.titleText);
		selectedView = findViewById(R.id.selectedView);
		avatarNameText = (TextView) findViewById(R.id.avatarNameText);
		avatarImage = (ImageView) findViewById(R.id.avatarImage);
		avatarDescriptionText = (TextView) findViewById(R.id.avatarDescriptionText);
		previousButton = (Button) findViewById(R.id.previousButton);
		nextButton = (Button) findViewById(R.id.nextButton);
		okButton = (Button) findViewById(R.id.okButton);

		mainLayout.setBackgroundResource(clientModel.getTheme().themeBackgroundId);
		songId = clientModel.getTheme().themeSongId;

		Typeface typeface = clientModel.getTypeface(this);
		if (typeface != null) {
			titleText.setTypeface(typeface);
			avatarNameText.setTypeface(typeface);
			okButton.setTypeface(typeface);
		}

		int styleId = clientModel.getTheme().buttonStyleId;
		if (styleId != 0) {
			okButton.setBackgroundResource(styleId);
		}

		previousButton.setOnClickListener(this);
		nextButton.setOnClickListener(this);
		okButton.setOnClickListener(this);

		navatars = Integer.parseInt(getString(R.string.navatars));
		updateSelectedAvatar();
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
			this.finish();
		}
		clientModel.savePreferences(this);
	}

	@Override
	public void onNext() {
		int n = getSelectedAvatarNum();
		if (n < navatars) {
			Animation animation = new TranslateAnimation(selectedView.getWidth() / 4, 0, 0, 0);
			animation.setDuration(250);
			selectedView.startAnimation(animation);
			setSelectedAvatarNum(n + 1);
		}
	}

	@Override
	public void onPrevious() {
		int n = getSelectedAvatarNum();
		if (n > 1) {
			Animation animation = new TranslateAnimation(-selectedView.getWidth() / 4, 0, 0, 0);
			animation.setDuration(250);
			selectedView.startAnimation(animation);
			setSelectedAvatarNum(n - 1);
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
		if (event.getEventType() == ClientModelChangedEvent.EVENT_TYPE_SELECTED_AVATAR_CHANGED) {
			updateSelectedAvatar();
		}
	}

	public int getSelectedAvatarNum() {
		String avatarName = clientModel.getAvatarName();
		if (getString(R.string.avatar1name).equals(avatarName)) {
			return 1;
		}
		if (getString(R.string.avatar2name).equals(avatarName)) {
			return 2;
		}
		if (getString(R.string.avatar3name).equals(avatarName)) {
			return 3;
		}
		if (getString(R.string.avatar4name).equals(avatarName)) {
			return 4;
		}
		if (getString(R.string.avatar5name).equals(avatarName)) {
			return 5;
		}
		if (getString(R.string.avatar6name).equals(avatarName)) {
			return 6;
		}
		return 1;
	}

	public void setSelectedAvatarNum(int n) {
		if (n == 1) {
			clientModel.setAvatarName(getString(R.string.avatar1name));
		} else if (n == 2) {
			clientModel.setAvatarName(getString(R.string.avatar2name));
		} else if (n == 3) {
			clientModel.setAvatarName(getString(R.string.avatar3name));
		} else if (n == 4) {
			clientModel.setAvatarName(getString(R.string.avatar4name));
		} else if (n == 5) {
			clientModel.setAvatarName(getString(R.string.avatar5name));
		} else if (n == 6) {
			clientModel.setAvatarName(getString(R.string.avatar6name));
		}
		updateSelectedAvatar();
	}

	Bitmap oldBitmap;

	public void updateSelectedAvatar() {
		int n = getSelectedAvatarNum();
		int avatarNameR;
		int avatarImageR;
		int avatarDescriptionR;
		if (n == 1) {
			avatarNameR = R.string.avatar1name;
			avatarImageR = R.raw.avatar1;
			avatarDescriptionR = R.string.avatar1description;
		} else if (n == 2) {
			avatarNameR = R.string.avatar2name;
			avatarImageR = R.raw.avatar2;
			avatarDescriptionR = R.string.avatar2description;
		} else if (n == 3) {
			avatarNameR = R.string.avatar3name;
			avatarImageR = R.raw.avatar3;
			avatarDescriptionR = R.string.avatar3description;
		} else if (n == 4) {
			avatarNameR = R.string.avatar4name;
			avatarImageR = R.raw.avatar4;
			avatarDescriptionR = R.string.avatar4description;
		} else if (n == 5) {
			avatarNameR = R.string.avatar5name;
			avatarImageR = R.raw.avatar5;
			avatarDescriptionR = R.string.avatar5description;
		} else {
			avatarNameR = R.string.avatar6name;
			avatarImageR = R.raw.avatar6;
			avatarDescriptionR = R.string.avatar6description;
		}
		avatarNameText.setText(getString(avatarNameR));
		avatarImage.setImageBitmap(null);
		if (oldBitmap != null) {
			oldBitmap.recycle();
			oldBitmap = null;
		}
		try {
			Bitmap bitmap = BitmapFactory.decodeResource(getResources(), avatarImageR);
			avatarImage.setImageBitmap(bitmap);
			oldBitmap = bitmap;
		} catch (Exception e) {
			e.printStackTrace();
		}
		avatarDescriptionText.setText(getString(avatarDescriptionR));
		if (n <= 1) {
			previousButton.setVisibility(View.INVISIBLE);
		} else {
			previousButton.setVisibility(View.VISIBLE);
		}
		if (n >= navatars) {
			nextButton.setVisibility(View.INVISIBLE);
		} else {
			nextButton.setVisibility(View.VISIBLE);
		}
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
