package com.gallantrealm.myworld.android;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.method.KeyListener;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class TexturePickerDialog extends Dialog {

	TextView textureURL;
	Activity context;
	OnTextureChangedListener listener;

	public interface OnTextureChangedListener {
		void textureChanged(String texture);
	}

	public TexturePickerDialog(final Activity context, final OnTextureChangedListener listener) {
		super(context);
		this.context = context;
		this.listener = listener;

		setContentView(R.layout.texture_picker);
		setTitle("Pick a Texture");

		OnTouchListener onTouchListener = new OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent arg1) {
				if (view instanceof ImageButton) {
					String texture = (String) ((ImageButton) view).getTag();
					System.out.println("TEXTURE: " + texture);
					listener.textureChanged(texture);
					TexturePickerDialog.this.dismiss();
				}
				return true;
			}
		};

		TableLayout textureTable = (TableLayout) findViewById(R.id.textureTable);
		for (int i = 0; i < textureTable.getChildCount(); i++) {
			TableRow textureRow = (TableRow) textureTable.getChildAt(i);
			for (int j = 0; j < textureRow.getChildCount(); j++) {
				ImageButton textureButton = (ImageButton) textureRow.getChildAt(j);
				textureButton.setOnTouchListener(onTouchListener);
			}
		}

		textureURL = (TextView) findViewById(R.id.textureURL);

		final KeyListener okeylistener = textureURL.getKeyListener();
		textureURL.setKeyListener(new KeyListener() {

			@Override
			public boolean onKeyDown(View view, Editable text, int keyCode, KeyEvent event) {
				if (keyCode == 23) { //KeyEvent.KEYCODE_BREAK) {
					if (textureURL.getText() != null && textureURL.getText().length() > 0) {
						String texture = textureURL.getText().toString();
						try {
							Uri uri = Uri.parse(texture);
							Bitmap bitmap = MediaStore.Images.Media.getBitmap(TexturePickerDialog.this.context.getContentResolver(), uri);
							System.out.println("TEXTURE: " + texture);
							listener.textureChanged(texture);
							TexturePickerDialog.this.dismiss();
						} catch (Exception e) {
							e.printStackTrace();
							MessageDialog messageDialog = new MessageDialog(TexturePickerDialog.this.context, null, "The image cannot be opened.  Is the URL correct?", new String[] { "OK" });
							messageDialog.show();
						}
					}
				}
				return okeylistener.onKeyDown(view, text, keyCode, event);
			}

			@Override
			public boolean onKeyOther(View view, Editable text, KeyEvent event) {
				return okeylistener.onKeyOther(view, text, event);
			}

			@Override
			public boolean onKeyUp(View view, Editable text, int keyCode, KeyEvent event) {
				return okeylistener.onKeyUp(view, text, keyCode, event);
			}

			@Override
			public void clearMetaKeyState(View arg0, Editable arg1, int arg2) {
				okeylistener.clearMetaKeyState(arg0, arg1, arg2);
			}

			@Override
			public int getInputType() {
				return okeylistener.getInputType();
			}
		});

		((Button) findViewById(R.id.selectImageButton)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.setType("image/*");
				intent.setAction(Intent.ACTION_GET_CONTENT);
				context.startActivityForResult(Intent.createChooser(intent, "Complete action using"), 0);

				AndroidClientModel clientModel = AndroidClientModel.getClientModel();
				clientModel.lastTexturePickerDialog = TexturePickerDialog.this;
			}
		});

	}

	//@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != Activity.RESULT_OK) {
			TexturePickerDialog.this.dismiss();
			return;
		}
		try {
			String path = "";
			Uri mImageCaptureUri = data.getData();
			path = getRealPathFromURI(mImageCaptureUri); //from Gallery
			if (path == null) {
				path = mImageCaptureUri.getPath(); //from File Manager
			}
//			if (path != null) {
//				Bitmap bitmap = BitmapFactory.decodeFile(path);
//			}
			System.out.println("TEXTURE: " + path);
			listener.textureChanged("file:/" + path);
			TexturePickerDialog.this.dismiss();
		} catch (Throwable e) {
			e.printStackTrace();
			MessageDialog messageDialog = new MessageDialog(TexturePickerDialog.this.context, null, "The image cannot be opened.  Is the file an image file?", new String[] { "OK" });
			messageDialog.show();
		}
	}

	public String getRealPathFromURI(Uri contentUri) {
		String[] proj = { MediaStore.Images.Media.DATA };
		Cursor cursor = context.managedQuery(contentUri, proj, null, null, null);

		if (cursor == null)
			return null;

		int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

		cursor.moveToFirst();

		return cursor.getString(column_index);
	}

}
