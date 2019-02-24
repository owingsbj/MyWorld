package com.gallantrealm.myworld.model;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

/**
 * Saves the world every minute.
 */
public class SaveWorldThread extends Thread {

	private final WWWorld world;
	private final String worldFileName;
	public boolean safeStop;

	public SaveWorldThread(WWWorld world, String worldFileName) {
		this.world = world;
		this.worldFileName = worldFileName;
		setName("SaveWorldThread");
		setDaemon(true);
		setPriority(2);
	}

	@Override
	public void run() {
		try {
			Thread.sleep(5000);
			File worldFile = new File(worldFileName);
			String tempWorldFileName = worldFileName + ".tmp";
			File tempWorldFile = new File(tempWorldFileName);
			while (!safeStop) {
				try {
					if (tempWorldFile.exists()) {
						tempWorldFile.delete();
					}
					OutputStream worldOutputStream = new BufferedOutputStream(new FileOutputStream(tempWorldFileName), 65536);
					ObjectOutputStream worldObjectStream = new ObjectOutputStream(worldOutputStream);
					worldObjectStream.writeObject(world);
					worldObjectStream.close();
					if (worldFile.exists()) {
						worldFile.delete();
					}
					tempWorldFile.renameTo(worldFile);
				} catch (Exception e) {
					e.printStackTrace();
				}
				Thread.sleep(5000);
			}
		} catch (InterruptedException e) {
			return;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
