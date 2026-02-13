package de.markusfisch.android.wavelines.app;

import android.app.Application;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import de.markusfisch.android.wavelines.R;
import de.markusfisch.android.wavelines.database.Database;
import de.markusfisch.android.wavelines.preference.Preferences;
import de.markusfisch.android.wavelines.receiver.BatteryLevelReceiver;

public class WaveLinesApp extends Application {
	public static final Database db = new Database();
	public static final Preferences preferences = new Preferences();
	private static final BatteryLevelReceiver batteryLevelReceiver =
			new BatteryLevelReceiver();

	public static float dp = 1f;

	@Override
	public void onCreate() {
		super.onCreate();
		db.open(this);
		preferences.init(this);
		dp = getResources().getDisplayMetrics().density;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			registerBatteryReceiver();
		}
	}

	private void registerBatteryReceiver() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_BATTERY_LOW);
		filter.addAction(Intent.ACTION_BATTERY_OKAY);
		filter.addAction(Intent.ACTION_BATTERY_CHANGED);
		registerReceiver(batteryLevelReceiver, filter);
		// Note it's not required to unregister the receiver because it
		// needs to be there as long as this application is running.
	}

	public static Toolbar initToolbar(AppCompatActivity activity) {
		Toolbar toolbar = activity.findViewById(R.id.toolbar);
		if (toolbar != null) {
			activity.setSupportActionBar(toolbar);
		}
		return toolbar;
	}
}
