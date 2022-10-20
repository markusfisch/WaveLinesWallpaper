package de.markusfisch.android.wavelines.service;

import android.graphics.Canvas;
import android.view.SurfaceHolder;

import de.markusfisch.android.wavelines.app.WaveLinesApp;
import de.markusfisch.android.wavelines.graphics.WaveLinesRenderer;

public class WaveLinesWallpaperService extends CanvasWallpaperService {
	private static boolean isRunning = false;

	public static boolean isRunning() {
		return isRunning;
	}

	@Override
	public Engine onCreateEngine() {
		return new WaveLinesEngine();
	}

	private class WaveLinesEngine extends CanvasWallpaperEngine {
		private final WaveLinesRenderer renderer = new WaveLinesRenderer();

		WaveLinesEngine() {
			super();
			WaveLinesApp.preferences.getPreferences()
					.registerOnSharedPreferenceChangeListener(
							(preferences, key) -> update());
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
}
