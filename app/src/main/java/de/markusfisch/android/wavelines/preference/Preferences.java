package de.markusfisch.android.wavelines.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.preference.PreferenceManager;

import de.markusfisch.android.wavelines.app.WaveLinesApp;

public class Preferences {
	private static final String THEME_ID = "theme_id";
	private static final String GALLERY_COLUMNS = "gallery_columns";

	private SharedPreferences preferences;
	private long themeId = 0;
	private int galleryColumns = 2;

	private static int parseInt(String s, int preset) {
		try {
			if (s != null && s.length() > 0) {
				return Integer.parseInt(s);
			}
		} catch (NumberFormatException e) {
			// use preset
		}
		return preset;
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

	public void init(Context context) {
		preferences = PreferenceManager.getDefaultSharedPreferences(context);
		update();
	}

	public SharedPreferences getPreferences() {
		return preferences;
	}

	public void update() {
		themeId = parseLong(
				preferences.getString(THEME_ID, null),
				WaveLinesApp.db.getFirstThemeId());
		galleryColumns = parseInt(
				preferences.getString(GALLERY_COLUMNS, null),
				galleryColumns);
	}

	public long getTheme() {
		return themeId;
	}

	public void setTheme(long id) {
		themeId = id;
		putString(THEME_ID, String.valueOf(themeId));
	}

	public int getGalleryColumns() {
		return galleryColumns;
	}

	public void setGalleryColumns(int columns) {
		galleryColumns = columns;
		putString(GALLERY_COLUMNS, String.valueOf(galleryColumns));
	}

	private void putString(String key, String value) {
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString(key, value);
		editor.apply();
	}
}
