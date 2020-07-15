package de.markusfisch.android.wavelines.service;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import de.markusfisch.android.wavelines.R;
import de.markusfisch.android.wavelines.app.WaveLinesApp;

public class WallpaperSetter {
	public static void setAsWallpaper(Context context, long id) {
		WaveLinesApp.preferences.setTheme(id);
		int message;
		if (WaveLinesWallpaperService.isRunning()) {
			message = R.string.wallpaper_set;
		} else if (startLiveWallpaperPicker(context)) {
			message = R.string.pick_live_wallpaper;
		} else {
			message = R.string.pick_live_wallpaper_manually;
		}
		Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
	}

	private static boolean startLiveWallpaperPicker(Context context) {
		Intent intent = new Intent(Intent.ACTION_SET_WALLPAPER);
		intent.setClassName(
				"com.android.wallpaper.livepicker",
				"com.android.wallpaper.livepicker.LiveWallpaperActivity");
		if (intent.resolveActivity(context.getPackageManager()) != null) {
			context.startActivity(intent);
			return true;
		}
		return false;
	}
}
