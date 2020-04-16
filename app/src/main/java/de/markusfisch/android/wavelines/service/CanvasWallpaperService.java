package de.markusfisch.android.wavelines.service;

import android.graphics.Canvas;
import android.os.Handler;
import android.os.SystemClock;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;

public abstract class CanvasWallpaperService extends WallpaperService {
	protected abstract class CanvasWallpaperEngine extends Engine {
		private final Handler handler = new Handler();
		private final Runnable runnable = new Runnable() {
			public void run() {
				nextFrame();
			}
		};

		private boolean visible = false;
		private long delay;

		@Override
		public void onDestroy() {
			super.onDestroy();
			stopRunnable();
		}

		@Override
		public void onVisibilityChanged(boolean visible) {
			this.visible = visible;
			if (visible) {
				resetDelay();
				nextFrame();
			} else {
				stopRunnable();
			}
		}

		@Override
		public void onSurfaceChanged(
				SurfaceHolder holder,
				int format,
				int width,
				int height) {
			super.onSurfaceChanged(holder, format, width, height);
			resetDelay();
			nextFrame();
		}

		@Override
		public void onSurfaceDestroyed(SurfaceHolder holder) {
			visible = false;
			stopRunnable();
			super.onSurfaceDestroyed(holder);
		}

		@Override
		public void onOffsetsChanged(
				float xOffset,
				float yOffset,
				float xOffsetStep,
				float yOffsetStep,
				int xPixelOffset,
				int yPixelOffset) {
		}

		protected abstract void drawFrame(Canvas canvas, long now);

		protected void nextFrame() {
			stopRunnable();

			if (!visible) {
				return;
			}

			handler.postDelayed(runnable, delay);

			long now = SystemClock.elapsedRealtime();
			SurfaceHolder holder = getSurfaceHolder();
			Canvas canvas = null;

			try {
				if ((canvas = holder.lockCanvas()) != null) {
					drawFrame(canvas, now);
				}
			} finally {
				if (canvas != null) {
					try {
						holder.unlockCanvasAndPost(canvas);
					} catch (IllegalArgumentException e) {
						// ignore, can't do anything about it;
						// spotted in crash logging, reason unknown
					}
				}
			}

			long t = SystemClock.elapsedRealtime() - now;
			if (t > delay) {
				delay = t + t;
			}
		}

		protected void resetDelay() {
			delay = 32L;
		}

		private void stopRunnable() {
			handler.removeCallbacks(runnable);
		}
	}
}
