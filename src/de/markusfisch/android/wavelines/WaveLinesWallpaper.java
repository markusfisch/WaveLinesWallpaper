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
package de.markusfisch.android.wavelines;

import de.markusfisch.android.wallpaper.Wallpaper;

import android.content.Context;
import android.content.res.TypedArray;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.preference.PreferenceManager;
import android.view.SurfaceHolder;

public class WaveLinesWallpaper extends Wallpaper
{
	static public final String SHARED_PREFERENCES_NAME = "WaveLinesSettings";

	@Override
	public Engine onCreateEngine()
	{
		return new WaveLinesEngine();
	}

	private class WaveLinesEngine
		extends WallpaperEngine
		implements SharedPreferences.OnSharedPreferenceChangeListener
	{
		private final WaveLines w = new WaveLines();

		public WaveLinesEngine()
		{
			super();

			PreferenceManager.setDefaultValues(
				WaveLinesWallpaper.this,
				R.xml.settings,
				false );
			SharedPreferences p = WaveLinesWallpaper.this.getSharedPreferences(
				SHARED_PREFERENCES_NAME, 0 );
			p.registerOnSharedPreferenceChangeListener( this );
			onSharedPreferenceChanged( p, null );
		}

		public void onSharedPreferenceChanged(
			final SharedPreferences p,
			final String key )
		{
			delay = Integer.parseInt( p.getString( "delay", "100" ) );

			w.uniform = p.getBoolean( "uniform", false );
			w.coupled = p.getBoolean( "coupled", true );
			w.uniform = p.getBoolean( "uniform", false );
			w.lines = Integer.parseInt(
				p.getString( "lines", "24" ) );
			w.waves = Integer.parseInt(
				p.getString( "waves", "3" ) );
			w.relativeAmplitude = Float.parseFloat(
				p.getString( "amplitude", ".02" ) );
			w.colors = WaveLinesWallpaper.getThemeColors(
				getApplicationContext(),
				p );

			w.reset();
		}

		@Override
		public void onSurfaceChanged(
			final SurfaceHolder holder,
			final int format,
			final int width,
			final int height )
		{
			super.onSurfaceChanged( holder, format, width, height );

			w.setup( width, height );
		}

		@Override
		protected void drawFrame( final Canvas c, final long e )
		{
			c.save();
			w.draw( c, e );
			c.restore();
		}
	}

	static public int[] getThemeColors(
		final Context context,
		final SharedPreferences preferences )
	{
		final String theme = preferences.getString( "theme", "blue" );

		if( theme.equals( "custom" ) )
			return Compositor.getCustomColors( preferences );

		final int themeId = context.getResources().getIdentifier(
			theme+"_colors",
			"array",
			context.getPackageName() );

		if( themeId < 1 )
			return null;

		final TypedArray a = context.getResources().obtainTypedArray(
			themeId );
		final int colors[] = new int[a.length()];

		for( int n = 0, l = a.length();
			n < l;
			++n )
			colors[n] = a.getColor( n, 0 );

		a.recycle();

		return colors;
	}
}
