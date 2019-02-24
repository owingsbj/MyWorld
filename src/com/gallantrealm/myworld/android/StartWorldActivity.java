package com.gallantrealm.myworld.android;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.lang.reflect.Constructor;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Timer;
import java.util.TimerTask;
import com.gallantrealm.myworld.client.model.ClientModelChangedEvent;
import com.gallantrealm.myworld.client.model.ClientModelChangedListener;
import com.gallantrealm.myworld.communication.Communications;
import com.gallantrealm.myworld.communication.TCPCommunications;
import com.gallantrealm.myworld.model.WWWorld;
import com.gallantrealm.myworld.server.MyWorldServer;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class StartWorldActivity extends Activity {

	private final AndroidClientModel clientModel = AndroidClientModel.getClientModel();
	private TextView startMessage;
	private TextView startupHintText;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.start_world);

		final String worldName;
		if (getIntent().getAction() != null) {
			worldName = getIntent().getAction();
		} else {
			worldName = clientModel.getWorldName();
		}

		startMessage = (TextView) findViewById(R.id.startMessage);
		startupHintText = (TextView) findViewById(R.id.startupHintText);
		startupHintText.setVisibility(View.INVISIBLE);

		Typeface typeface = clientModel.getTypeface(this);
		if (typeface != null) {
			startMessage.setTypeface(typeface);
		}

		(new AsyncTask<String, Void, String>() {

			@Override
			protected String doInBackground(String... params) {
				clientModel.paused = false;
				clientModel.setLocalPhysicsThread(false); // TODO set to true when no local server
				clientModel.addClientModelChangedListener(new ClientModelChangedListener() {
					@Override
					public void clientModelChanged(ClientModelChangedEvent event) {
						if (event.getEventType() == ClientModelChangedEvent.EVENT_TYPE_MESSAGE_RECEIVED) {
							// thread restrictions in android.. need to switch threads. messagesText.setText(clientModel.getLastMessageReceived());
						} else if (event.getEventType() == ClientModelChangedEvent.EVENT_TYPE_CONNECTED) {
							Intent intent = new Intent(StartWorldActivity.this, ShowWorldActivity.class);
							startActivity(intent);
						}
					}
				});

				try {
					clientModel.setUserNameField(clientModel.getAvatarName());
					clientModel.setWorldAddressField("localhost:8880");

					if (clientModel.getAlwaysStartAsNew()) {
						newWorld(worldName);
					} else if (clientModel.getAlwaysStartAsOld()) {
						File tfile;
						if (worldName.startsWith("file://")) {
							tfile = new File(worldName.substring(7));
						} else {
							tfile = new File(getFilesDir(), worldName);
						}
						final File worldFile = tfile;
						System.out.println(tfile.toString() + " length: " + tfile.length());
						restoreWorld(worldName, worldFile);
					} else {
						startupTheWorld(worldName, false);
					}

					// Tell gallantrealm.com that the world was started
					if (!BuildConfig.DEBUG) {
						try {
							String shortWorldName = worldName.substring(worldName.lastIndexOf('.') + 1);
							String urlString = "http://gallantrealm.com/insights/recordEvent.jsp?app=" + URLEncoder.encode(getString(R.string.app_name)) + "&event=startWorld" + "&world=" + shortWorldName;
							System.out.println(urlString);
							URL grUrl = new URL(urlString);
							HttpURLConnection connection = (HttpURLConnection) grUrl.openConnection();
							connection.setConnectTimeout(2000);
							System.out.println(connection.getResponseMessage());
							connection.disconnect();
						} catch (Throwable t) {
							t.printStackTrace(); // otherwise ignore
						}
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
				return "Executed";
			}

			@Override
			protected void onPostExecute(String result) {
			}

			@Override
			protected void onPreExecute() {
			}

			@Override
			protected void onProgressUpdate(Void... values) {
			}
		}).execute();

	}

	public void startupTheWorld(final String worldName, boolean reset) {
		try {

			clientModel.initializeCameraPosition(); // doing it here so world can override initial camera position

			// Create or restore the world
			final File worldFile = new File(getFilesDir(), worldName);
			if (worldFile.exists() && !reset) {

				this.runOnUiThread(new Runnable() {
					@Override
					public void run() {

						final MessageDialog messageDialog = new MessageDialog(StartWorldActivity.this, null, "Restore from last time?", new String[] { "Restore", "New" }, null);
						messageDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
							@Override
							public void onDismiss(DialogInterface dialogInterface) {
								int rc = messageDialog.getButtonPressed();
								if (rc == 0) {
									restoreWorld(worldName, worldFile);
								} else {
									newWorld(worldName);
								}
							}
						});
						messageDialog.show();
					}
				});

			} else {
				newWorld(worldName);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void restoreWorld(final String worldName, File worldFile) {
		clientModel.initializeCameraPosition(); // doing it here so world can override initial camera position
		try {
			FileInputStream worldInputStream = new FileInputStream(worldFile);
			ObjectInputStream worldObjectStream = new ObjectInputStream(worldInputStream);
			WWWorld world = (WWWorld) worldObjectStream.readObject();
			worldObjectStream.close();
			System.out.println("saved world read in");
			clientModel.setLocalWorld(world);
			world.restored();
			if (world.usesAccelerometer() && clientModel.useSensors()) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						startupHintText.setText(getString(R.string.accelerometerHint));
						startupHintText.setVisibility(View.VISIBLE);
					}
				});
			}
			// serveLocalWorld(world);
			Timer t = new Timer();
			t.schedule(new TimerTask() {
				@Override
				public void run() {
					if (clientModel.useZeemote()) {
						clientModel.connectToZeemote(StartWorldActivity.this);
					}
					Intent intent = new Intent(StartWorldActivity.this, ShowWorldActivity.class);
					startActivity(intent);
					StartWorldActivity.this.finish();
				}
			}, 2500l);
		} catch (Exception e) {
			e.printStackTrace();
			if (worldName.startsWith("file:")) {
				final MessageDialog messageDialog = new MessageDialog(StartWorldActivity.this, null, "Sorry, the world could not be opened.", new String[] { "OK" }, null);
				messageDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
					@Override
					public void onDismiss(DialogInterface dialogInterface) {
						StartWorldActivity.this.finish();
					}
				});
				messageDialog.show();
			} else {
				final MessageDialog messageDialog = new MessageDialog(StartWorldActivity.this, null, "The state could not be restored. Starting new.", new String[] { "OK" }, null);
				messageDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
					@Override
					public void onDismiss(DialogInterface dialogInterface) {
						newWorld(worldName);
					}
				});
				messageDialog.show();
			}
		}
	}

	public void newWorld(String worldName) {
		clientModel.initializeCameraPosition(); // doing it here so world can override initial camera position
		try {
			String saveWorldFileName = (new File(getFilesDir(), worldName)).getAbsolutePath();
			Class<WWWorld> worldClass = (Class<WWWorld>) this.getClass().getClassLoader().loadClass(worldName);
			Constructor<WWWorld> constructor = worldClass.getConstructor(String.class, String.class);
			WWWorld world = constructor.newInstance(saveWorldFileName, clientModel.getAvatarName());
			clientModel.setLocalWorld(world);
			if (world.usesAccelerometer() && clientModel.useSensors()) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						startupHintText.setText(getString(R.string.accelerometerHint));
						startupHintText.setVisibility(View.VISIBLE);
					}
				});
			}
			// serveLocalWorld(world);
			Timer t = new Timer();
			t.schedule(new TimerTask() {
				@Override
				public void run() {
					if (clientModel.useZeemote()) {
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								startupHintText.setText("Trying to connect to Zeemote controller.  Please make sure it is turned on.");
								startupHintText.setVisibility(View.VISIBLE);
							}
						});
						boolean connected = clientModel.connectToZeemote(StartWorldActivity.this);
						if (connected) {
							Intent intent = new Intent(StartWorldActivity.this, ShowWorldActivity.class);
							startActivity(intent);
							StartWorldActivity.this.finish();
						}
					} else {
						Intent intent = new Intent(StartWorldActivity.this, ShowWorldActivity.class);
						startActivity(intent);
						StartWorldActivity.this.finish();
					}
				}
			}, 2500l);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onStop() {
		if (clientModel.useZeemote()) {
			clientModel.cancelZeemoteConnect();
		}
		super.onStop();
	}

	public void servelocalWorld(WWWorld world, String worldFileName) {
		final int port = 8880;
		final int clientLimit = 10;

		Communications communications = new TCPCommunications();

		// Start serving the world
		if (clientModel.getLocalServer() != null) {
			clientModel.getLocalServer().stopServer();
		}
		MyWorldServer server = new MyWorldServer(world, communications, port, clientLimit);
		clientModel.setLocalServer(server);
		server.startServer(true);
		// clientModel.connect();
	}

}
