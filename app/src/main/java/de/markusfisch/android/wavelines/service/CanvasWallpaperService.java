package de.markusfisch.android.wavelines.service;

import android.os.Handler;
import android.graphics.Canvas;
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

		@Override
		public void onDestroy() {
			super.onDestroy();
			stopRunnable();
		}

		@Override
		public void onVisibilityChanged(boolean v) {
			visible = v;

			if (visible) {
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
			super.onSurfaceChanged(
					holder,
					format,
					width,
					height);

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

		protected abstract void drawFrame(Canvas canvas);

		protected void nextFrame() {
			stopRunnable();

			if (!visible) {
				return;
			}

			handler.postDelayed(runnable, 32L);

			SurfaceHolder holder = getSurfaceHolder();
			Canvas canvas = null;

			try {
				if ((canvas = holder.lockCanvas()) != null) {
					drawFrame(canvas);
				}
			} finally {
				if (canvas != null) {
					holder.unlockCanvasAndPost(canvas);
				}
			}
		}

		private void stopRunnable() {
			handler.removeCallbacks(runnable);
		}
	}
}
