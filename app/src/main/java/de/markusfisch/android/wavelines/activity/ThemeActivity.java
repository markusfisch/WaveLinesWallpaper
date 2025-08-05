package de.markusfisch.android.wavelines.activity;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.core.view.ViewCompat;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.palette.graphics.Palette;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executors;

import de.markusfisch.android.wavelines.R;
import de.markusfisch.android.wavelines.app.WaveLinesApp;
import de.markusfisch.android.wavelines.database.Theme;
import de.markusfisch.android.wavelines.graphics.BitmapLoader;
import de.markusfisch.android.wavelines.service.WallpaperSetter;
import de.markusfisch.android.wavelines.widget.ThemePagerView;

public class ThemeActivity extends AppCompatActivity {
	private final Handler handler = new Handler(Looper.getMainLooper());
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private static final int FULL_SCREEN_FLAGS =
			View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
					View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
					View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
	private static final int SELECT_LAST = -1;
	private static final String THEME_INDEX = "theme_index";

	private ThemePagerView themesView;
	private MenuItem setThemeMenuItem;
	private View mainLayout;
	private View progressView;
	private View decorView;
	private boolean leanBack = false;

	public static void startWithIndex(Context context, int index) {
		Intent intent = new Intent(context, ThemeActivity.class);
		intent.putExtra(THEME_INDEX, index);
		context.startActivity(intent);
	}

	@Override
	protected void onCreate(Bundle state) {
		super.onCreate(state);
		setContentView(R.layout.activity_theme);

		themesView = (ThemePagerView) findViewById(R.id.themes);
		mainLayout = findViewById(R.id.main_layout);
		progressView = findViewById(R.id.progress_view);
		findViewById(R.id.edit_theme).setOnClickListener(v -> {
			Intent intent = new Intent(ThemeActivity.this,
					EditorActivity.class);
			intent.putExtra(EditorActivity.THEME_ID,
					themesView.getSelectedThemeId());
			startActivity(intent);
		});

		initThemePagerView();
		initWindowInsets();
		initDecorView();

		Intent intent = getIntent();
		if (intent != null) {
			int index = intent.getIntExtra(THEME_INDEX, -1);
			if (index > -1) {
				themesView.setSelectedIndex(index);
			}
			handleSendIntents(intent);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		queryThemesAsync();
		themesView.startDrawing();
	}

	@Override
	public void onPause() {
		super.onPause();
		themesView.closeCursor();
		themesView.stopDrawing();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_theme, menu);
		setThemeMenuItem = menu.findItem(R.id.set_theme);
		updateThemeMenuItem();
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		long id = themesView.getSelectedThemeId();
		int itemId = item.getItemId();
		if (itemId == R.id.set_theme) {
			setAsWallpaper(id, item);
			return true;
		} else if (itemId == R.id.add_theme) {
			addTheme();
			return true;
		} else if (itemId == R.id.delete_theme) {
			askDeleteTheme(id);
			return true;
		} else if (itemId == R.id.duplicate_theme) {
			duplicateTheme(id);
			return true;
		} else if (itemId == R.id.share_theme) {
			shareTheme(id);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void queryThemesAsync() {
		// use previously selected index when this is invoked
		// after a theme has been deleted or when the system is
		// coming back to this activity instance
		queryThemesAsync(themesView.getSelectedIndex());
	}

	private void queryThemesAsync(final int index) {
		if (progressView.getVisibility() == View.VISIBLE) {
			return;
		}
		progressView.setVisibility(View.VISIBLE);
		Executors.newSingleThreadExecutor().execute(() -> {
			Cursor cursor = WaveLinesApp.db.queryThemes();
			handler.post(() -> {
				if (isFinishing()) {
					return;
				}
				progressView.setVisibility(View.GONE);
				if (cursor != null) {
					themesView.setThemes(cursor, index == SELECT_LAST ?
							cursor.getCount() : index);
					updateWallpaper();
				}
			});
		});
	}

	private void updateWallpaper() {
		long wallpaperId = WaveLinesApp.preferences.getTheme();
		if (themesView.getSelectedThemeId() == wallpaperId) {
			// trigger onSharedPreferenceChanged() listener in
			// WaveLinesWallpaperService to update current theme
			WaveLinesApp.preferences.setTheme(0);
			WaveLinesApp.preferences.setTheme(wallpaperId);
		}
	}

	private void updateThemeMenuItem() {
		updateThemeMenuItem(themesView.getSelectedThemeId());
	}

	private void updateThemeMenuItem(long id) {
		if (setThemeMenuItem != null) {
			setThemeMenuItem.setIcon(
					WaveLinesApp.preferences.getTheme() == id ?
							R.drawable.ic_wallpaper_set :
							R.drawable.ic_wallpaper_unset);
		}
	}

	private void addTheme() {
		addTheme(new Theme());
	}

	private void duplicateTheme(long id) {
		addTheme(WaveLinesApp.db.getTheme(id));
	}

	private void addTheme(Theme theme) {
		WaveLinesApp.db.insertTheme(theme);
		queryThemesAsync(SELECT_LAST);
	}

	private void shareTheme(long id) {
		Intent intent = new Intent();
		intent.setType("application/json");
		intent.setAction(Intent.ACTION_SEND);
		intent.putExtra(Intent.EXTRA_TEXT,
				WaveLinesApp.db.getTheme(id).toJson());
		startActivity(Intent.createChooser(intent,
				getString(R.string.share_theme)));
	}

	private void askDeleteTheme(final long id) {
		new AlertDialog.Builder(this)
				.setTitle(R.string.delete_theme)
				.setMessage(R.string.sure_to_delete_theme)
				.setPositiveButton(
						android.R.string.ok,
						(dialog, whichButton) -> deleteTheme(id))
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	private void deleteTheme(long id) {
		if (themesView.getCount() < 2) {
			return;
		}
		WaveLinesApp.db.deleteTheme(id);
		queryThemesAsync();
		if (WaveLinesApp.preferences.getTheme() == id) {
			WaveLinesApp.preferences.setTheme(
					WaveLinesApp.db.getFirstThemeId());
			updateThemeMenuItem();
		}
	}

	private void setAsWallpaper(long id, MenuItem item) {
		WallpaperSetter.setAsWallpaper(this, id);
		item.setIcon(R.drawable.ic_wallpaper_set);
	}

	private void handleSendIntents(Intent intent) {
		String action = intent.getAction();
		if (!Intent.ACTION_SEND.equals(action) &&
				!Intent.ACTION_VIEW.equals(action)) {
			return;
		}
		// consume this intent; this is necessary because
		// a orientation change will start a new activity
		// with the exact same intent
		intent.setAction(null);
		if (!importTheme(intent)) {
			Toast.makeText(this, R.string.error_importing_theme,
					Toast.LENGTH_SHORT).show();
		}
	}

	private boolean importTheme(Intent intent) {
		String type = intent.getType();
		if (type == null) {
			return false;
		} else if (type.startsWith("image/")) {
			addThemeFromImageUriAsync(this,
					(Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM));
			return true;
		} else if ("application/json".equals(type) ||
				"text/plain".equals(type)) {
			String json = intent.getStringExtra(Intent.EXTRA_TEXT);
			if (json == null && (json = getTextFromUri(
					getContentResolver(), intent.getData())) == null) {
				return false;
			}
			try {
				addTheme(new Theme(json));
				return true;
			} catch (JSONException e) {
				return false;
			}
		}
		return false;
	}

	private static String getTextFromUri(ContentResolver resolver, Uri uri) {
		try {
			InputStream in = resolver.openInputStream(uri);
			if (in == null) {
				return null;
			}
			StringBuilder sb = new StringBuilder();
			byte[] buffer = new byte[2048];
			for (int len; (len = in.read(buffer)) > 0; ) {
				sb.append(new String(buffer, 0, len, "UTF-8"));
			}
			in.close();
			return sb.toString();
		} catch (IOException e) {
			return null;
		}
	}

	private void addThemeFromImageUriAsync(final Context context,
			final Uri uri) {
		if (uri == null || progressView.getVisibility() == View.VISIBLE) {
			return;
		}
		progressView.setVisibility(View.VISIBLE);
		Executors.newSingleThreadExecutor().execute(() -> {
			Bitmap bitmap = BitmapLoader.getBitmapFromUri(context, uri, 512);
			handler.post(() -> {
				progressView.setVisibility(View.GONE);
				if (bitmap == null) {
					return;
				}
				addThemeFromBitmap(bitmap);
			});
		});
	}

	private void addThemeFromBitmap(Bitmap bitmap) {
		Palette.from(bitmap).generate(p -> addThemeWithColors(getValidColors(new int[]{
				p.getVibrantColor(0),
				p.getDarkVibrantColor(0),
				p.getLightVibrantColor(0),
				p.getMutedColor(0),
				p.getDarkMutedColor(0),
				p.getLightMutedColor(0)
		})));
	}

	private static int[] getValidColors(int[] colors) {
		int[] valid = new int[colors.length];
		int i = 0;
		for (int color : colors) {
			if (color != 0) {
				valid[i++] = color;
			}
		}
		int[] ret = new int[i];
		System.arraycopy(valid, 0, ret, 0, i);
		return ret;
	}

	private void addThemeWithColors(int[] colors) {
		addTheme(new Theme(colors));
	}

	private void initThemePagerView() {
		themesView.setOnClickListener(v -> {
			// devices with hardware buttons do not automatically
			// leave lean back mode so show the system UI manually
			// if it's hidden
			if (!setSystemUiVisibility(leanBack)) {
				PreviewActivity.show(v.getContext(),
						WaveLinesApp.db.getTheme(
								themesView.getSelectedThemeId()));
			}
		});

		final String title = getString(R.string.themes);
		themesView.setOnChangeListener((index, id) -> {
			setTitle(String.format(title, index + 1,
					themesView.getCount()));
			updateThemeMenuItem(id);
		});
	}

	private void initWindowInsets() {
		ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, insets) -> {
			if (insets.hasSystemWindowInsets()) {
				// restore padding because SYSTEM_UI_FLAG_HIDE_NAVIGATION
				// somehow removes the padding from mainLayout although
				// fitsSytemWindows is set; happens only when
				// windowLayoutInDisplayCutoutMode is set to shortEdges
				v.setPadding(
						insets.getSystemWindowInsetLeft(),
						insets.getSystemWindowInsetTop(),
						insets.getSystemWindowInsetRight(),
						insets.getSystemWindowInsetBottom());
			}
			return insets.consumeSystemWindowInsets();
		});
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private void initDecorView() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			return;
		}
		decorView = getWindow().getDecorView();
		decorView.setOnSystemUiVisibilityChangeListener(visibility -> {
			if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
				decorView.setSystemUiVisibility(FULL_SCREEN_FLAGS);
				setToolBarVisibility(true);
				mainLayout.setVisibility(View.VISIBLE);
				leanBack = false;
			} else {
				setToolBarVisibility(false);
				mainLayout.setVisibility(View.INVISIBLE);
				leanBack = true;
			}
		});
		decorView.setSystemUiVisibility(FULL_SCREEN_FLAGS);
	}

	private void setToolBarVisibility(boolean visible) {
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			if (visible) {
				actionBar.show();
			} else {
				actionBar.hide();
			}
		}
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private boolean setSystemUiVisibility(boolean visible) {
		if (decorView == null ||
				Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			return false;
		}
		int flags = FULL_SCREEN_FLAGS;
		if (!visible) {
			flags |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
					View.SYSTEM_UI_FLAG_FULLSCREEN |
					View.SYSTEM_UI_FLAG_IMMERSIVE;
		}
		decorView.setSystemUiVisibility(flags);
		return true;
	}
}
