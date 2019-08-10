package com.gallantrealm.myworld.client.model;

public interface AlertListener {
	public int onAlert(String title, String message, String[] options, String checkinMessage);

	public int onAlert(String title, String message, String[] options, String leaderboardId, long score, String scoreMsg);

	public void onSelectAlert(final String message, final Object[] availableItems, final String[] options, SelectResponseHandler handler);

	public void onInputAlert(final String title, final String message, final String initialValue, final String[] options, InputResponseHandler handler);

	public void onSelectColor(final String title, final int initialColor, final SelectColorHandler handler);

}
