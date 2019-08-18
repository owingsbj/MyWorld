package com.gallantrealm.myworld.android;

import com.gallantrealm.android.Translator;
import com.gallantrealm.myworld.android.renderer.AndroidRenderer;
import com.gallantrealm.myworld.client.model.ClientModel;
import com.zeemote.zc.event.ButtonEvent;
import com.zeemote.zc.event.IButtonListener;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

public class Select3dObjectDialog extends Dialog implements IButtonListener {
	ClientModel clientModel = AndroidClientModel.getClientModel();

	TextView messageText;
	GridView itemsView;
	Button option1Button;
	Button option2Button;
	Button option3Button;
	int buttonPressed;
	String title;
	String message;
	String[] options;
	String checkinMessage;
	String leaderboardId;
	float score;
	String scoreMsg;
	Activity activity;
	Object[] availableItems;
	Object selectedItem;

	public Select3dObjectDialog(Context context, String message, Object[] availableItems, String[] options) {
		super(context, R.style.Theme_Dialog);
		activity = (Activity) context;
		this.availableItems = availableItems;
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.title = null;
		this.message = message;
		this.checkinMessage = null;
		this.leaderboardId = null;
		this.options = options;
		setContentView(R.layout.select_3dobject_dialog);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		messageText = (TextView) findViewById(R.id.messageText);
		itemsView = (GridView) findViewById(R.id.itemsView);
		option1Button = (Button) findViewById(R.id.option1Button);
		option2Button = (Button) findViewById(R.id.option2Button);
		option3Button = (Button) findViewById(R.id.option3Button);

		Typeface typeface = clientModel.getTypeface(getContext());
		if (typeface != null) {
			messageText.setTypeface(typeface);
			option1Button.setTypeface(typeface);
			option2Button.setTypeface(typeface);
			option3Button.setTypeface(typeface);
		}

		int styleId = clientModel.getTheme().buttonStyleId;
		if (styleId != 0) {
			option1Button.setBackgroundResource(styleId);
			option2Button.setBackgroundResource(styleId);
			option3Button.setBackgroundResource(styleId);
		}

		if (message != null) {
			messageText.setText(message);
		}
		itemsView.setAdapter(new ArrayAdapter<Object>(activity, R.layout.select_3dobject_row, availableItems) {
			@SuppressLint("NewApi")
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				View row;
				if (convertView != null) {
					row = convertView;
				} else {
					row = LayoutInflater.from(getContext()).inflate(R.layout.select_3dobject_row, parent, false);
				}
//				if (itemsView.isItemChecked(position)) {
//					row.setBackgroundColor(0xff80c0ff);
//				} else {
//					row.setBackgroundColor(0x00ffffff);
//				}
				TextView label = (TextView) row.findViewById(R.id.item_row_label);
				final Object item = availableItems[position];
				final Class itemClass = item instanceof Class ? (Class) item : item.getClass();
				try {
					String name = (String) (itemClass.getMethod("getName").invoke(item));
					label.setText(Translator.getTranslator().translate(name));
				} catch (Exception e) {
					try {
						String name = (String) itemClass.getMethod("getStaticName").invoke(null);
						label.setText(Translator.getTranslator().translate(name));
					} catch (Exception e2) {
						label.setText(Translator.getTranslator().translate(itemClass.getSimpleName()));
					}
				}
				final ImageView image = (ImageView) row.findViewById(R.id.item_row_image);
				try {
					final String imageFileName = (String) itemClass.getMethod("getImageFileName").invoke(item);
					if (imageFileName != null) {
						AsyncTask.execute(new Runnable() {
							   @Override
							   public void run() {
									final Bitmap bitmap = AndroidRenderer.readImageTexture(Uri.parse(imageFileName));
									Select3dObjectDialog.this.activity.runOnUiThread(new Runnable() {
										public void run() {
											image.setImageDrawable(new BitmapDrawable(bitmap));
										}
									});
							   }
							});
					}
				} catch (Exception e) {
					try {
						int imageResource = (Integer) itemClass.getMethod("getStaticImageResource").invoke(null);
						if (imageResource == 0) {
							image.setVisibility(View.INVISIBLE);
						}
						image.setImageResource(imageResource);
					} catch (Exception e2) {
						try {
							Thread thread = new Thread() {
								public void run() {
									try {
										Bitmap bitmap = AndroidRenderer.readImageTexture(Uri.parse((String) itemClass.getMethod("getStaticImageFileName").invoke(null)));
										image.setImageDrawable(new BitmapDrawable(bitmap));
									} catch (Exception e) {
										e.printStackTrace();
									}
								}
							};
							thread.start();
						} catch (Exception e3) {
							image.setImageResource(0);
						}
					}
				}
				TextView description = (TextView) row.findViewById(R.id.item_row_description);
				try {
					String text = (String) itemClass.getMethod("getDescription").invoke(item);
					description.setText(Translator.getTranslator().translate(text));
				} catch (Exception e) {
					try {
						String text = (String) itemClass.getMethod("getStaticDescription").invoke(null);
						description.setText(Translator.getTranslator().translate(text));
					} catch (Exception e2) {
						description.setText("");
					}
				}
				return row;
			}
		});
		itemsView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View view, int position, long arg3) {
				view.setSelected(true);
				selectedItem = availableItems[position];
				if (options == null) {
					buttonPressed = 0;
					Select3dObjectDialog.this.dismiss();
					Select3dObjectDialog.this.cancel();
				}
			}
		});

		option1Button.setVisibility(View.GONE);
		option2Button.setVisibility(View.GONE);
		option3Button.setVisibility(View.GONE);
		if (options == null) {
//			option1Button.setText("OK");
//			option1Button.setVisibility(View.VISIBLE);
		} else {
			if (options.length > 0) {
				option1Button.setText(options[0]);
				option1Button.setVisibility(View.VISIBLE);
				if (options.length > 1) {
					option2Button.setText(options[1]);
					option2Button.setVisibility(View.VISIBLE);
					if (options.length > 2) {
						option3Button.setText(options[2]);
						option3Button.setVisibility(View.VISIBLE);
					}
				}
			}
		}
		option1Button.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				buttonPressed = 0;
				Select3dObjectDialog.this.dismiss();
				Select3dObjectDialog.this.cancel();
				return true;
			}

		});
		option2Button.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				buttonPressed = 1;
				Select3dObjectDialog.this.dismiss();
				Select3dObjectDialog.this.cancel();
				return true;
			}

		});
		option3Button.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				buttonPressed = 2;
				Select3dObjectDialog.this.dismiss();
				Select3dObjectDialog.this.cancel();
				return true;
			}

		});

		if (clientModel.useZeemote() && clientModel.getZeeController() != null) {
			clientModel.getZeeController().addButtonListener(this);
		}

		Translator.getTranslator().translate(this.getWindow().getDecorView());

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

	public int getButtonPressed() {
		return buttonPressed;
	}

	public Object getItemSelected() {
		return selectedItem;
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
				buttonPressed = 0;
				Select3dObjectDialog.this.dismiss();
				Select3dObjectDialog.this.cancel();
			} else if (buttonEvent.getButtonGameAction() == ButtonEvent.BUTTON_B) {
				buttonPressed = options.length - 1;
				Select3dObjectDialog.this.dismiss();
				Select3dObjectDialog.this.cancel();
			}
		}
	}

}
