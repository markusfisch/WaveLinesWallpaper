package de.markusfisch.android.wavelines.service;

import android.app.WallpaperManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;

import de.markusfisch.android.wavelines.R;
import de.markusfisch.android.wavelines.app.WaveLinesApp;

public class WallpaperSetter {
	public static void setAsWallpaper(Context context, long id) {
		// The onSharedPreferenceChanged() listener in
		// WaveLinesWallpaperService is only ever triggered
		// if the value has changed so force this.
		WaveLinesApp.preferences.setTheme(0);
		WaveLinesApp.preferences.setTheme(id);

		int message;
		if (!canSetWallpaper(context)) {
			message = R.string.cannot_set_wallpaper;
		} else if (WaveLinesWallpaperService.isRunning()) {
			message = R.string.wallpaper_set;
		} else if (startChangeLiveWallpaper(context)) {
			return;
		} else if (startLiveWallpaperPicker(context)) {
			message = R.string.pick_live_wallpaper;
		} else {
			message = R.string.pick_live_wallpaper_manually;
		}
		Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
	}

	private static boolean canSetWallpaper(Context context) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
			return true;
		}
		WallpaperManager wm = WallpaperManager.getInstance(context);
		return wm.isWallpaperSupported() &&
				(Build.VERSION.SDK_INT < Build.VERSION_CODES.N ||
						wm.isSetWallpaperAllowed());
	}

	private static boolean startChangeLiveWallpaper(Context context) {
		Intent intent = new Intent(
				WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
		intent.putExtra(
				WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
				new ComponentName(context,
						WaveLinesWallpaperService.class));
		return startActivity(context, intent);
	}

	private static boolean startLiveWallpaperPicker(Context context) {
		Intent intent = new Intent(Intent.ACTION_SET_WALLPAPER);
		intent.setClassName(
				"com.android.wallpaper.livepicker",
				"com.android.wallpaper.livepicker.LiveWallpaperActivity");
		return startActivity(context, intent);
	}

	private static boolean startActivity(Context context, Intent intent) {
		try {
			// Avoid using `intent.resolveActivity()` at API level 30+ due
			// to the new package visibility restrictions. In order for
			// `resolveActivity()` to "see" another package, we would need
			// to list that package/intent in a `<queries>` block in the
			// Manifest. But since we used `resolveActivity()` only to avoid
			// an exception if the Intent cannot be resolved, it's much easier
			// and more robust to just try and catch that exception if
			// necessary.
			context.startActivity(intent);
			return true;
		} catch (ActivityNotFoundException e) {
			return false;
		}
	}
}
