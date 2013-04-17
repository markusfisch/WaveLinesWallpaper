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

import de.markusfisch.android.colorcompositor.ColorCompositor;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

public class Compositor extends ColorCompositor
{
	private SharedPreferences preferences = null;

	static public int[] getCustomColors( final SharedPreferences p )
	{
		final int l = p.getInt( "custom_colors", 0 );

		if( l < 1 )
			return null;

		final int colors[] = new int[l];

		for( int n = 0; n < l; ++n )
			colors[n] = p.getInt( "custom_color"+n, 0 );

		return colors;
	}

	@Override
	public void onCreate( Bundle state )
	{
		super.onCreate( state );

		if( (preferences = getSharedPreferences(
			WaveLinesWallpaper.SHARED_PREFERENCES_NAME,
			0 )) == null )
			return;

		loadColors();
	}

	@Override
	public void onPause()
	{
		super.onPause();

		saveColors();
	}

	private void loadColors()
	{
		int c[] = getCustomColors( preferences );

		if( c == null )
			c = WaveLinesWallpaper.getThemeColors(
				getApplicationContext(),
				preferences );

		if( c == null )
			return;

		for( int n = 0, l = c.length; n < l; ++n )
			addColor( c[n] );
	}

	private void saveColors()
	{
		final int count = colorList.getChildCount();
		final SharedPreferences.Editor e = preferences.edit();

		e.putInt( "custom_colors", count );

		for( int n = 0; n < count; ++n )
		{
			View v = colorList.getChildAt( n );

			if( v instanceof ColorLayout )
			{
				e.remove( "custom_color"+n );
				e.putInt( "custom_color"+n, ((ColorLayout) v).color );
			}
		}

		e.putString( "theme", count > 0 ? "custom" : "blue" );
		e.commit();
	}
}
