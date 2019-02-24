package com.gallantrealm.myworld.android;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;

import android.app.Dialog;
import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.gallantrealm.myworld.FastMath;
import com.gallantrealm.myworld.android.renderer.AndroidRenderer;
import com.gallantrealm.myworld.model.WWBox;
import com.gallantrealm.myworld.model.WWObject;
import com.gallantrealm.myworld.model.WWVector;
import com.gallantrealm.myworld.model.WWWorld;

public class SoundPickerDialog extends Dialog {

	WWWorld world;
	ArrayAdapter<String> adapter;
	String selectedItem;

	public SoundPickerDialog(Context context, final WWWorld world) {
		super(context);
		this.world = world;
		setContentView(R.layout.sound_picker);
		setTitle("Import");

		Button okButton = (Button) findViewById(R.id.okButton);
		Button cancelButton = (Button) findViewById(R.id.cancelButton);
		final ListView assemblyListView = (ListView) findViewById(R.id.soundListView);
		assemblyListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		AndroidClientModel clientModel = AndroidClientModel.getClientModel();
		adapter = new ArrayAdapter<String>(context, R.layout.sound_list_item, R.id.name) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				View v = super.getView(position, convertView, parent);
				if (assemblyListView.isItemChecked(position)) {
					v.setBackgroundColor(0xff80c0ff);
				} else {
					v.setBackgroundColor(0x00ffffff);
				}
				return v;
			}
		};
		assemblyListView.setAdapter(adapter);
		assemblyListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				selectedItem = adapter.getItem(position);
			}

		});

		cancelButton.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				SoundPickerDialog.this.dismiss();
				return true;
			}
		});

		okButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				AndroidClientModel clientModel = AndroidClientModel.getClientModel();
				WWObject selectedObject = clientModel.getSelectedObject();
				WWVector selectedPosition;
				if (selectedObject != null) {
					selectedPosition = selectedObject.getPosition().add(0, 0, selectedObject.getSize().z);
				} else {
					selectedPosition = new WWVector(0, 0, 0);
				}
				try {
					File file = new File(clientModel.getContext().getFilesDir(), selectedItem + ".g3d");
					InputStream worldInputStream = new BufferedInputStream(new FileInputStream(file), 65536);
					ObjectInputStream worldObjectStream = new ObjectInputStream(worldInputStream);
					WWWorld assemblyWorld = (WWWorld) worldObjectStream.readObject();
					WWObject[] objects = assemblyWorld.getObjects();
					System.out.println("Adding " + (objects.length - 2) + " objects");
					// create a keystone that is the parent of all the objects added
					WWObject keystone = new WWBox();
					keystone.setPhantom(true);
					keystone.setTransparency(WWObject.SIDE_ALL, 1);
					world.addObject(keystone);
					// figure out a good location for the keystone
					float minx = 1000;
					float maxx = -1000;
					float miny = 1000;
					float maxy = -1000;
					float minz = 1000;
					float maxz = -1000;
					for (int i = 0; i < objects.length; i++) {
						WWObject object = objects[i];
						if (object != null && !object.deleted) {
							if ("sky".equals(object.getName())) { // skip sky
							} else if ("ground".equals(object.getName())) { // skip ground
							} else if ("water".equals(object.getName())) { // skip water
							} else if ("avatar".equals(object.getName())) { // skip avatar
							} else {
								WWVector position = object.getPosition();
								minx = FastMath.min(minx, position.x);
								maxx = FastMath.max(maxx, position.x);
								miny = FastMath.min(miny, position.y);
								maxy = FastMath.max(maxy, position.y);
								minz = FastMath.min(minz, position.z);
								maxz = FastMath.max(maxz, position.z);
							}
						}
					}
					float keyx = FastMath.avg(minx, maxx);
					float keyy = FastMath.avg(miny, maxy);
					float keyz = FastMath.avg(minz, maxz);
					keystone.setPosition(keyx, keyy, keyz);
					for (int i = 0; i < objects.length; i++) {
						WWObject object = objects[i];
						if (object != null && !object.deleted) {
							if ("sky".equals(object.getName())) { // skip sky
							} else if ("ground".equals(object.getName())) { // skip ground
							} else if ("water".equals(object.getName())) { // skip water
							} else if ("avatar".equals(object.getName())) { // skip avatar
							} else {
								object.setPosition(object.getPosition().add(selectedPosition).subtract(keyx, keyy, keyz));
								world.addObject(object);
								if (object.getParent() == 0) {
									object.setParent(keystone);
								}
							}
						}
					}
					worldObjectStream.close();
					SoundPickerDialog.this.dismiss();
					AndroidRenderer.clearRenderings();
				} catch (Exception e) {
					e.printStackTrace();
					// TODO add some kind of error feedback
				}
			}
		});
	}

	@Override
	public void show() {
		AndroidClientModel clientModel = AndroidClientModel.getClientModel();
		adapter.clear();
		File filesDir = clientModel.getContext().getFilesDir();
		File[] files = filesDir.listFiles();
		for (int i = 0; i < files.length; i++) {
			String name = files[i].getName();
			if (name.endsWith(".g3d")) {
				adapter.add(name.substring(0, name.length() - 4));
			}
		}
		super.show();
	}
}
