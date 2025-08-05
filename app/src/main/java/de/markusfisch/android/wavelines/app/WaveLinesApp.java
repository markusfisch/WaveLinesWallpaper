package de.markusfisch.android.wavelines.app;

import android.app.Application;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import de.markusfisch.android.wavelines.R;
import de.markusfisch.android.wavelines.database.Database;
import de.markusfisch.android.wavelines.preference.Preferences;

public class WaveLinesApp extends Application {
	public static final Database db = new Database();
	public static final Preferences preferences = new Preferences();

	public static float dp = 1f;

	@Override
	public void onCreate() {
		super.onCreate();
		db.open(this);
		preferences.init(this);
		dp = getResources().getDisplayMetrics().density;
	}

	public static Toolbar initToolbar(AppCompatActivity activity) {
		Toolbar toolbar = activity.findViewById(R.id.toolbar);
		if (toolbar != null) {
			activity.setSupportActionBar(toolbar);
		}
		return toolbar;
	}
}
