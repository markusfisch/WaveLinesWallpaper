package de.markusfisch.android.wavelines.preference;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import de.markusfisch.android.wavelines.app.WaveLinesApp;

public class Preferences {
	private static final String THEME_ID = "_theme_id";
	private static final String GALLERY_COLUMNS = "_gallery_columns";

	private SharedPreferences preferences;
	private long themeId = 0;
	private int galleryColumns = 2;

	public void init(Context context) {
		preferences = PreferenceManager.getDefaultSharedPreferences(context);
		update();
	}

	public SharedPreferences getPreferences() {
		return preferences;
	}

	public void update() {
		themeId = preferences.getLong(THEME_ID, getLegacyThemeId(
				WaveLinesApp.db.getFirstThemeId()));
		galleryColumns = preferences.getInt(GALLERY_COLUMNS, galleryColumns);
	}

	public long getTheme() {
		return themeId;
	}

	public void setTheme(long id) {
		themeId = id;
		// commit() instead of apply to force listeners to take
		// notice immediately.
		commitLong(THEME_ID, themeId);
	}

	public int getGalleryColumns() {
		return galleryColumns;
	}

	public void setGalleryColumns(int columns) {
		galleryColumns = columns;
		applyInt(GALLERY_COLUMNS, galleryColumns);
	}

	private void applyInt(String key, int value) {
		SharedPreferences.Editor editor = preferences.edit();
		editor.putInt(key, value);
		editor.apply();
	}

	private void commitLong(String key, long value) {
		SharedPreferences.Editor editor = preferences.edit();
		editor.putLong(key, value);
		editor.commit();
	}

	// this method will be removed with the next version
	private long getLegacyThemeId(long preset) {
		try {
			String s = preferences.getString("theme_id", null);
			if (s != null && s.length() > 0) {
				return Long.parseLong(s);
			}
		} catch (NumberFormatException e) {
			// use preset
		}
		return preset;
	}
}
