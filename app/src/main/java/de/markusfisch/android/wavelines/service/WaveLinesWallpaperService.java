package de.markusfisch.android.wavelines.service;

import de.markusfisch.android.wavelines.app.WaveLinesApp;
import de.markusfisch.android.wavelines.graphics.WaveLinesRenderer;

import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.view.SurfaceHolder;

public class WaveLinesWallpaperService extends CanvasWallpaperService {
	@Override
	public Engine onCreateEngine() {
		return new WaveLinesEngine();
	}

	private class WaveLinesEngine extends CanvasWallpaperEngine {
		private final WaveLinesRenderer renderer = new WaveLinesRenderer();
		private final SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
			@Override
			public void onSharedPreferenceChanged(
					SharedPreferences preferences,
					String key) {
				update();
			}
		};

		public WaveLinesEngine() {
			super();
			WaveLinesApp.preferences.getPreferences()
					.registerOnSharedPreferenceChangeListener(listener);
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
		}

		@Override
		protected void drawFrame(Canvas canvas) {
			renderer.draw(canvas);
		}

		private void update() {
			renderer.setTheme(WaveLinesApp.db.getTheme(
					WaveLinesApp.preferences.getTheme()));
		}
	}
}
