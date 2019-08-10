package de.markusfisch.android.wavelines.app;

import android.app.Application;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import de.markusfisch.android.wavelines.R;
import de.markusfisch.android.wavelines.database.Database;
import de.markusfisch.android.wavelines.preference.Preferences;

public class WaveLinesApp extends Application {
	public static final Database db = new Database();
	public static final Preferences preferences = new Preferences();

	@Override
	public void onCreate() {
		super.onCreate();
		db.open(this);
		preferences.init(this);
	}

	public static void initToolbar(AppCompatActivity activity) {
		Toolbar toolbar = (Toolbar) activity.findViewById(R.id.toolbar);
		if (toolbar != null) {
			activity.setSupportActionBar(toolbar);
		}
	}
}
