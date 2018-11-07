package de.markusfisch.android.wavelines.app;

import de.markusfisch.android.wavelines.database.Database;
import de.markusfisch.android.wavelines.preference.Preferences;

import android.app.Application;

public class WaveLinesApp extends Application {
	public static final Database db = new Database();
	public static final Preferences preferences = new Preferences();

	@Override
	public void onCreate() {
		super.onCreate();
		db.open(this);
		preferences.init(this);
	}
}
