package de.markusfisch.android.wavelines.preference;

import de.markusfisch.android.wavelines.app.WaveLinesApp;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.preference.PreferenceManager;

import java.lang.NumberFormatException;

public class Preferences {
	private static final String THEME_ID = "theme_id";

	private SharedPreferences preferences;
	private long themeId = 0;

	public void init(Context context) {
		preferences = PreferenceManager.getDefaultSharedPreferences(context);
		update();
	}

	public SharedPreferences getPreferences() {
		return preferences;
	}

	public void update() {
		themeId = parseLong(preferences.getString(THEME_ID, null),
				WaveLinesApp.db.getFirstThemeId());
	}

	public long getTheme() {
		return themeId;
	}

	public void setTheme(long id) {
		themeId = id;
		putString(THEME_ID, String.valueOf(themeId));
	}

	private void putString(String key, String value) {
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString(key, value);
		editor.apply();
	}

	private static long parseLong(String s, long preset) {
		try {
			if (s != null && s.length() > 0) {
				return Long.parseLong(s);
			}
		} catch (NumberFormatException e) {
			// use preset
		}
		return preset;
	}
}
