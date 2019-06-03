package com.gallantrealm.myworld.android;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.lang.reflect.Constructor;
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
		System.out.println(">StartWorldActivity.onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.start_world);

		clientModel.setContext(this);
		
		final String worldClassName;
		if (getIntent().getAction() != null) {
			worldClassName = getIntent().getAction();
		} else if (clientModel.getWorldClassName() != null) {
			worldClassName = clientModel.getWorldClassName();
		} else {
			worldClassName = clientModel.getWorldName();
		}

		startMessage = (TextView) findViewById(R.id.startMessage);
		startupHintText = (TextView) findViewById(R.id.startupHintText);
		startupHintText.setVisibility(View.INVISIBLE);

		Typeface typeface = clientModel.getTypeface(this);
		if (typeface != null) {
			startMessage.setTypeface(typeface);
		}

		AsyncTask.execute(new Runnable() {
			public void run() {
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
						newWorld(worldClassName);
					} else if (clientModel.getAlwaysStartAsOld()) {
						File tfile;
						if (worldClassName.startsWith("file://")) {
							tfile = new File(worldClassName.substring(7));
						} else {
							tfile = new File(getFilesDir(), worldClassName);
						}
						final File worldFile = tfile;
						System.out.println(tfile.toString() + " length: " + tfile.length());
						restoreWorld(worldClassName, worldFile);
					} else {
						startupTheWorld(worldClassName, false);
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		System.out.println("<StartWorldActivity.onCreate");
	}

	public void startupTheWorld(final String worldClassName, boolean reset) {
		System.out.println(">StartWorldActivity.startupTheWorld");
		try {

			clientModel.initializeCameraPosition(); // doing it here so world can override initial camera position

			// Create or restore the world
			final File worldFile = new File(getFilesDir(), worldClassName);
			if (worldFile.exists() && !reset) {

				this.runOnUiThread(new Runnable() {
					@Override
					public void run() {

						final MessageDialog messageDialog = new MessageDialog(StartWorldActivity.this, null, "Restore from last time?", new String[] { "Restore", "New" }, null);
						messageDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
							@Override
							public void onDismiss(DialogInterface dialogInterface) {
								AsyncTask.execute(new Runnable() {
									public void run() {
										int rc = messageDialog.getButtonPressed();
										if (rc == 0) {
											restoreWorld(worldClassName, worldFile);
										} else {
											newWorld(worldClassName);
										}
									}
								});
							}
						});
						messageDialog.show();
					}
				});

			} else {
				newWorld(worldClassName);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("<StartWorldActivity.startupTheWorld");
	}

	public void restoreWorld(final String worldClassName, File worldFile) {
		System.out.println(">StartWorldActivity.restoreWorld");
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
			if (worldClassName.startsWith("file:")) {
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
						newWorld(worldClassName);
					}
				});
				messageDialog.show();
			}
		}
		System.out.println("<StartWorldActivity.restoreWorld");
	}

	public void newWorld(String worldClassName) {
		System.out.println(">StartWorldActivity.newWorld");
		clientModel.initializeCameraPosition(); // doing it here so world can override initial camera position
		try {
			String saveWorldFileName = (new File(getFilesDir(), worldClassName)).getAbsolutePath();
			Class<WWWorld> worldClass = (Class<WWWorld>) this.getClass().getClassLoader().loadClass(worldClassName);
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
			runOnUiThread(new Runnable() {
				public void run() {
					final MessageDialog messageDialog = new MessageDialog(StartWorldActivity.this, null, "Couldn't initialize the world.  Try choosing the world again and replaying.", new String[] { "OK" }, null);
					messageDialog.show();
				}
			});
		}
		System.out.println("<StartWorldActivity.newWorld");
	}

	@Override
	protected void onStart() {
		System.out.println(">StartWorldActivity.onStart");
		super.onStart();
		System.out.println("<StartWorldActivity.onStart");
	}

	@Override
	protected void onStop() {
		System.out.println(">StartWorldActivity.onStop");
		if (clientModel.useZeemote()) {
			clientModel.cancelZeemoteConnect();
		}
		super.onStop();
		System.out.println("<StartWorldActivity.onStop");
	}

	@Override
	protected void onDestroy() {
		System.out.println(">StartWorldActivity.onDestroy");
		super.onDestroy();
		System.out.println("<StartWorldActivity.onDestroy");
	}

	public void servelocalWorld(WWWorld world, String worldFileName) {
		System.out.println(">ConnectToWorldActivity.servelocalWorld");

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
		System.out.println("<ConnectToWorldActivity.servelocalWorld");
	}

}
