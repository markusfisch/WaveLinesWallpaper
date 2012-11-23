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

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class Settings
	extends PreferenceActivity
{
	@Override
	public void onCreate( final Bundle state )
	{
		super.onCreate( state );

		getPreferenceManager().setSharedPreferencesName(
			WaveLinesWallpaper.SHARED_PREFERENCES_NAME );

		addPreferencesFromResource( R.xml.settings );
	}
}
