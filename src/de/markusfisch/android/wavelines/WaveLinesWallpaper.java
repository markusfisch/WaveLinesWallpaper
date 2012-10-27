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

import android.graphics.Canvas;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.preference.PreferenceManager;

public class WaveLinesWallpaper extends Wallpaper
{
	static final public String SHARED_PREFERENCES_NAME = "WaveLinesSettings";

	@Override
	public Engine onCreateEngine()
	{
		return new WaveLinesEngine();
	}

	class WaveLinesEngine
		extends WallpaperEngine
		implements SharedPreferences.OnSharedPreferenceChangeListener
	{
		final private WaveLines w = new WaveLines();

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

		@Override
		protected void drawFrame( final Canvas c, final long e )
		{
			c.save();
			w.draw( c, e );
			c.restore();
		}

		public void onSharedPreferenceChanged( SharedPreferences p, String k )
		{
			delay = Integer.parseInt( p.getString( "delay", "100" ) );

			w.coupled = p.getBoolean( "coupled", true );
			w.uniform = p.getBoolean( "uniform", false );
			w.lines = Integer.parseInt(
				p.getString( "lines", "24" ) );
			w.waves = Integer.parseInt(
				p.getString( "waves", "3" ) );
			w.relativeAmplitude = Float.parseFloat(
				p.getString( "amplitude", ".02" ) );

			// load color theme
			{
				String theme = p.getString( "theme", "blue" );
				int themeId = getResources().getIdentifier(
					theme+"_colors",
					"array",
					getPackageName() );

				TypedArray a = getResources().obtainTypedArray( themeId );
				int colors[] = new int[a.length()];
				for( int n = 0, l = a.length();
					n < l;
					++n )
					colors[n] = a.getColor( n, 0 );
				a.recycle();

				w.colors = colors;
			}

			w.reset();
		}
	}
}
