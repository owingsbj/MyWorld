package com.gallantrealm.myworld.android;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.amazon.device.ads.Ad;
import com.amazon.device.ads.AdError;
import com.amazon.device.ads.AdLayout;
import com.amazon.device.ads.AdListener;
import com.amazon.device.ads.AdProperties;
import com.amazon.device.ads.AdRegistration;
import com.amazon.device.ads.AdTargetingOptions;
import com.gallantrealm.myworld.client.model.ClientModel;
import com.zeemote.zc.event.ButtonEvent;
import com.zeemote.zc.event.IButtonListener;

public class AdDialog extends GallantDialog implements IButtonListener {
	ClientModel clientModel = AndroidClientModel.getClientModel();

	TextView titleText;
	FrameLayout adFrame;
	Activity activity;
	Button okButton;

	private AdLayout adLayout;
	private AdTargetingOptions adTarget;

	public AdDialog(Context context) {
		super(context, R.style.Theme_Dialog);
		activity = (Activity) context;
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.ad_dialog);
		setCancelable(false);
		setCanceledOnTouchOutside(false);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		titleText = (TextView) findViewById(R.id.titleText);
		adFrame = (FrameLayout) findViewById(R.id.adFrame);
		okButton = (Button) findViewById(R.id.okButton);

		okButton.setVisibility(View.INVISIBLE);  // until ad is loaded

		Typeface typeface = clientModel.getTypeface(getContext());
		if (typeface != null) {
			titleText.setTypeface(typeface);
			okButton.setTypeface(typeface);
		}

		DisplayMetrics metrics = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
		int adWidth = (int) (Math.min(900, metrics.widthPixels * 0.5f));
		int adHeight = (int) (adWidth * 250 / 300);
		adLayout = new AdLayout(activity, com.amazon.device.ads.AdSize.SIZE_300x250);
		adTarget = new AdTargetingOptions();
		FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(adWidth, adHeight); // otherwise buttons dont work
		adFrame.addView(adLayout, lp);
		adLayout.setListener(new AdListener() {

			@Override
			public void onAdLoaded(Ad arg0, AdProperties arg1) {
				System.out.println("Ad loaded!!!");
				AdDialog.this.adFrame.requestLayout();
				okButton.setVisibility(View.VISIBLE);
			}

			@Override
			public void onAdFailedToLoad(Ad ad, AdError error) {
				System.out.println("Ad failed to load!!");
				AdDialog.this.dismiss();
				AdDialog.this.cancel();
//					com.amazon.device.ads.InterstitialAd iad = new com.amazon.device.ads.InterstitialAd(AdDialog.this.activity);
//					iad.loadAd();
//					iad.showAd();
			}

			@Override
			public void onAdExpanded(Ad arg0) {
				AdDialog.this.dismiss();
				AdDialog.this.cancel();
			}

			@Override
			public void onAdDismissed(Ad arg0) {
			}

			@Override
			public void onAdCollapsed(Ad arg0) {
			}
		});
		adLayout.loadAd();

		okButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				AdDialog.this.dismiss();
				AdDialog.this.cancel();
			}
		});
		if (clientModel.useZeemote() && clientModel.getZeeController() != null) {
			clientModel.getZeeController().addButtonListener(this);
		}
	}
	
	@Override
	public void show() {
		super.show();
	}

	@Override
	public void dismiss() {
		super.dismiss();
		if (clientModel.useZeemote() && clientModel.getZeeController() != null) {
			clientModel.getZeeController().removeButtonListener(this);
		}
	}

	boolean controllerWasPressed;

	@Override
	public void buttonPressed(ButtonEvent buttonEvent) {
		controllerWasPressed = true;
	}

	@Override
	public void buttonReleased(ButtonEvent buttonEvent) {
		if (controllerWasPressed) {
			controllerWasPressed = false;
			if (buttonEvent.getButtonGameAction() == ButtonEvent.BUTTON_A) {
				// TODO visit ad somehow
				AdDialog.this.dismiss();
				AdDialog.this.cancel();
			} else if (buttonEvent.getButtonGameAction() == ButtonEvent.BUTTON_B) {
				AdDialog.this.dismiss();
				AdDialog.this.cancel();
			}
		}
	}

	@Override
	public void onOkay() {
		// TODO visit somehow
		super.onOkay();
	}

	@Override
	public void onCancel() {
		super.onCancel();
	}

}
