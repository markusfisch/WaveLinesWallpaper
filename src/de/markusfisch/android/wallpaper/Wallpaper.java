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

		private Handler handler = new Handler();
		private Runnable runnable = new Runnable()
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
			handler.removeCallbacks( runnable );
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
			nextFrame();
		}

		@Override
		public void onSurfaceDestroyed( SurfaceHolder holder )
		{
			super.onSurfaceDestroyed( holder );
			visible = false;
			stopRunnable();
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

		protected abstract void drawFrame( final Canvas c, final float t );

		protected void nextFrame()
		{
			stopRunnable();

			if( visible )
				handler.postDelayed( runnable, delay );

			final SurfaceHolder holder = getSurfaceHolder();
			Canvas c = null;

			try
			{
				if( (c = holder.lockCanvas()) != null )
				{
					final long now = SystemClock.elapsedRealtime();
					drawFrame( c, (float)delay/(now-time) );
					time = now;
				}
			}
			finally
			{
				if( c != null )
					holder.unlockCanvasAndPost( c );
			}
		}

		private void stopRunnable()
		{
			handler.removeCallbacks( runnable );
		}
	}
}
