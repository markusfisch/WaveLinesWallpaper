package de.markusfisch.android.wavelines.service;

import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.view.SurfaceHolder;

import de.markusfisch.android.wavelines.app.WaveLinesApp;
import de.markusfisch.android.wavelines.graphics.WaveLinesRenderer;
import de.markusfisch.android.wavelines.receiver.BatteryLevelReceiver;

public class WaveLinesWallpaperService extends CanvasWallpaperService {
	private static boolean isRunning = false;
	private static boolean batteryLow = false;
	private static boolean powerConnected = false;
	private static WaveLinesEngine engine;

	private ComponentName batteryLevelComponent;

	public static boolean isRunning() {
		return isRunning;
	}

	public static void setPaused(boolean paused) {
		batteryLow = paused;
		if (engine != null) {
			engine.setPaused(paused);
		}
	}

	public static void setPowerConnected(boolean connected) {
		powerConnected = connected;
	}

	@Override
	public Engine onCreateEngine() {
		engine = new WaveLinesEngine();
		engine.setPaused(batteryLow && !powerConnected);
		return engine;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		batteryLevelComponent = new ComponentName(this,
				BatteryLevelReceiver.class);
		enableComponent(batteryLevelComponent, true);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		enableComponent(batteryLevelComponent, false);
		engine = null;
	}

	private class WaveLinesEngine
			extends CanvasWallpaperEngine
			implements SharedPreferences.OnSharedPreferenceChangeListener {
		private final WaveLinesRenderer renderer = new WaveLinesRenderer();

		private WaveLinesEngine() {
			super();
			WaveLinesApp.preferences.getPreferences()
					.registerOnSharedPreferenceChangeListener(this);
		}

		@Override
		public void onCreate(SurfaceHolder holder) {
			super.onCreate(holder);
			update();
		}

		@Override
		public void onSharedPreferenceChanged(
				SharedPreferences preferences,
				String key) {
			update();
		}

		@Override
		public void onSurfaceChanged(
				SurfaceHolder holder,
				int format,
				int width,
				int height) {
			super.onSurfaceChanged(holder, format, width, height);
			renderer.setSize(width, height);
			isRunning = true;
		}

		@Override
		protected void drawFrame(Canvas canvas, long now) {
			renderer.draw(canvas, now);
		}

		private void update() {
			resetDelay();
			renderer.setTheme(WaveLinesApp.db.getTheme(
					WaveLinesApp.preferences.getTheme()));
		}
	}

	private void enableComponent(ComponentName name, boolean enable) {
		getPackageManager().setComponentEnabledSetting(name,
				enable ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
						PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
				PackageManager.DONT_KILL_APP);
	}
}
