package com.gallantrealm.myworld.android;

import java.util.ArrayList;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;

public class AdvancedColorPickerDialog extends Dialog implements android.view.View.OnClickListener {

	public interface OnColorChangedListener {
		void colorChanged(int color);
	}

	private final OnColorChangedListener colorChangedListener;
	int color;
	Activity activity;

	public AdvancedColorPickerDialog(Context context, OnColorChangedListener listener, int initialColor) {
		super(context);
		activity = (Activity) context;
		setContentView(R.layout.color_dialog);
		setTitle("Pick a Color");
		colorChangedListener = listener;
		this.color = initialColor;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		TableLayout colorGrid = (TableLayout) findViewById(R.id.colorGrid);
		ArrayList<View> swatches = colorGrid.getTouchables();
		for (int i = 0; i < swatches.size(); i++) {
			Button swatch = (Button) swatches.get(i);
			swatch.setOnClickListener(this);
		}

		OnColorChangedListener l = new OnColorChangedListener() {
			public void colorChanged(int color) {
				colorChangedListener.colorChanged(color);
				dismiss();
			}
		};

	}

	public void onClick(View v) {
		String tag = (String) v.getTag();
		int color = Color.parseColor(tag);
		colorChangedListener.colorChanged(color);
		dismiss();
	}
}