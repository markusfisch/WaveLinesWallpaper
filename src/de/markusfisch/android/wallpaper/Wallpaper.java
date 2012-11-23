/*
 *   O         ,-
 *  ° o    . -´  '     ,-
 *   °  .´        ` . ´,´
 *     ( °   ))     . (
 *      `-;_    . -´ `.`.
 *          `._'       ´
 *
 * 2012 Markus Fisch <mf@markusfisch.de>
 * Public Domain
 */
package de.markusfisch.android.wallpaper;

import android.os.SystemClock;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;
import android.graphics.Canvas;

public abstract class Wallpaper extends WallpaperService
{
	protected abstract class WallpaperEngine extends Engine
	{
		protected int delay = 40;

		final private Handler handler = new Handler();
		final private Runnable runnable = new Runnable()
		{
			public void run()
			{
				nextFrame();
			}
		};

		private boolean visible = false;
		private long time = 0;

		@Override
		public void onDestroy()
		{
			super.onDestroy();

			stopRunnable();
		}

		@Override
		public void onVisibilityChanged( boolean v )
		{
			visible = v;

			if( visible )
			{
				time = SystemClock.elapsedRealtime();
				nextFrame();
			}
			else
				stopRunnable();
		}

		@Override
		public void onSurfaceChanged(
			SurfaceHolder holder,
			int format,
			int width,
			int height )
		{
			super.onSurfaceChanged( holder, format, width, height );

			nextFrame();
		}

		@Override
		public void onSurfaceDestroyed( SurfaceHolder holder )
		{
			visible = false;
			stopRunnable();

			super.onSurfaceDestroyed( holder );
		}

		@Override
		public void onOffsetsChanged(
			float xOffset,
			float yOffset,
			float xOffsetStep,
			float yOffsetStep,
			int xPixelOffset,
			int yPixelOffset )
		{
			nextFrame();
		}

		protected abstract void drawFrame( final Canvas c, final long e );

		protected void nextFrame()
		{
			stopRunnable();

			if( !visible )
				return;

			handler.postDelayed( runnable, delay );

			final SurfaceHolder h = getSurfaceHolder();
			Canvas c = null;

			try
			{
				if( (c = h.lockCanvas()) != null )
				{
					final long now = SystemClock.elapsedRealtime();
					drawFrame( c, now-time );
					time = now;
				}
			}
			finally
			{
				if( c != null )
					h.unlockCanvasAndPost( c );
			}
		}

		private void stopRunnable()
		{
			handler.removeCallbacks( runnable );
		}
	}
}
