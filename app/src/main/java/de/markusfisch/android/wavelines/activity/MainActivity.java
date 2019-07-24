package de.markusfisch.android.wavelines.activity;

import de.markusfisch.android.wavelines.app.WaveLinesApp;
import de.markusfisch.android.wavelines.database.Theme;
import de.markusfisch.android.wavelines.graphics.BitmapLoader;
import de.markusfisch.android.wavelines.widget.ThemesView;
import de.markusfisch.android.wavelines.R;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.OnApplyWindowInsetsListener;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.WindowInsetsCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private static final int FULL_SCREEN_FLAGS =
			View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
					View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
					View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
	private static final int SELECT_LAST = -1;

	private final Rect windowInsets = new Rect();

	private ThemesView themesView;
	private MenuItem setThemeMenuItem;
	private View mainLayout;
	private View progressView;
	private View decorView;
	private boolean leanBack = false;

	@Override
	protected void onCreate(Bundle state) {
		super.onCreate(state);
		setContentView(R.layout.activity_main);

		themesView = (ThemesView) findViewById(R.id.themes);
		mainLayout = findViewById(R.id.main_layout);
		progressView = findViewById(R.id.progress_view);
		findViewById(R.id.edit_theme).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this,
						EditorActivity.class);
				intent.putExtra(EditorActivity.THEME_ID,
						themesView.getSelectedThemeId());
				startActivity(intent);
			}
		});

		initThemesView();
		initWindowInsets();
		initDecorView();

		handleSendIntents(getIntent());
	}

	@Override
	public void onResume() {
		super.onResume();
		queryThemesAsync();
	}

	@Override
	public void onPause() {
		super.onPause();
		themesView.closeCursor();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		setThemeMenuItem = menu.findItem(R.id.set_theme);
		updateThemeMenuItem();
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		long id = themesView.getSelectedThemeId();
		switch (item.getItemId()) {
			case R.id.set_theme:
				setAsWallpaper(id, item);
				return true;
			case R.id.add_theme:
				addTheme();
				return true;
			case R.id.delete_theme:
				askDeleteTheme(id);
				return true;
			case R.id.duplicate_theme:
				duplicateTheme(id);
				return true;
			case R.id.share_theme:
				shareTheme(id);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private void queryThemesAsync() {
		queryThemesAsync(themesView.getSelectedIndex());
	}

	// this AsyncTask is running for a short and finite time only
	// and it's perfectly okay to delay garbage collection of the
	// parent instance until this task has ended
	@SuppressLint("StaticFieldLeak")
	private void queryThemesAsync(final int index) {
		if (progressView.getVisibility() == View.VISIBLE) {
			return;
		}
		progressView.setVisibility(View.VISIBLE);
		new AsyncTask<Void, Void, Cursor>() {
			@Override
			protected Cursor doInBackground(Void... nothings) {
				return WaveLinesApp.db.queryThemes();
			}

			@Override
			protected void onPostExecute(Cursor cursor) {
				if (isFinishing()) {
					return;
				}
				progressView.setVisibility(View.GONE);
				if (cursor != null) {
					themesView.setThemes(cursor, index == SELECT_LAST ?
							cursor.getCount() : index);
					updateWallpaper();
				}
			}
		}.execute();
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
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(
									DialogInterface dialog,
									int whichButton) {
								deleteTheme(id);
							}
						})
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
		WaveLinesApp.preferences.setTheme(id);
		item.setIcon(R.drawable.ic_wallpaper_set);
		Toast.makeText(this, R.string.set_as_wallpaper,
				Toast.LENGTH_SHORT).show();
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

		String type = intent.getType();
		if (type == null) {
			return;
		} else if (type.startsWith("image/")) {
			addThemeFromImageUriAsync(this,
				(Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM));
		} else if ("application/json".equals(type)) {
			String json = intent.getStringExtra(Intent.EXTRA_TEXT);
			if (json == null && (json = getTextFromUri(
					getContentResolver(), intent.getData())) == null) {
				Toast.makeText(this, R.string.error_invalid_json,
						Toast.LENGTH_SHORT).show();
				return;
			}
			try {
				addTheme(Theme.clamp(new Theme(json)));
			} catch (JSONException e) {
				Toast.makeText(this, R.string.error_invalid_json,
						Toast.LENGTH_SHORT).show();
			}
		}
	}

	private static String getTextFromUri(ContentResolver resolver, Uri uri) {
		try {
			InputStream in = resolver.openInputStream(uri);
			StringBuilder sb = new StringBuilder();
			byte[] buffer = new byte[2048];
			for (int len; (len = in.read(buffer)) > 0; ) {
				sb.append(new String(buffer, 0, len, "UTF-8"));
			}
			return sb.toString();
		} catch (IOException e) {
			return null;
		}
	}

	// this AsyncTask is running for a short and finite time only
	// and it's perfectly okay to delay garbage collection of the
	// parent instance until this task has ended
	@SuppressLint("StaticFieldLeak")
	private void addThemeFromImageUriAsync(final Context context,
			final Uri uri) {
		if (uri == null || progressView.getVisibility() == View.VISIBLE) {
			return;
		}
		progressView.setVisibility(View.VISIBLE);
		new AsyncTask<Void, Void, Bitmap>() {
			@Override
			protected Bitmap doInBackground(Void... nothings) {
				return BitmapLoader.getBitmapFromUri(context, uri, 512);
			}

			@Override
			protected void onPostExecute(Bitmap bitmap) {
				progressView.setVisibility(View.GONE);
				if (bitmap == null) {
					return;
				}
				addThemeFromBitmap(bitmap);
			}
		}.execute();
	}

	private void addThemeFromBitmap(Bitmap bitmap) {
		Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
			@Override
			public void onGenerated(Palette p) {
				addThemeWithColors(getValidColors(new int[]{
						p.getVibrantColor(0),
						p.getDarkVibrantColor(0),
						p.getLightVibrantColor(0),
						p.getMutedColor(0),
						p.getDarkMutedColor(0),
						p.getLightMutedColor(0)
				}));
			}
		});
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
		Theme random = new Theme();
		addTheme(new Theme(
				random.coupled,
				random.uniform,
				random.shuffle,
				colors.length,
				random.waves,
				random.amplitude,
				random.oscillation,
				random.shift,
				random.rotation,
				colors
		));
	}

	private void initThemesView() {
		themesView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// devices with hardware buttons do not automatically
				// leave lean back mode so show the system UI manually
				// if it's hidden
				if (!setSystemUiVisibility(leanBack)) {
					PreviewActivity.show(v.getContext(),
							WaveLinesApp.db.getTheme(
									themesView.getSelectedThemeId()));
				}
			}
		});

		final String title = getString(R.string.themes);
		themesView.setOnChangeListener(new ThemesView.OnChangeListener() {
			@Override
			public void onChange(int index, long id) {
				setTitle(String.format(title, index + 1,
						themesView.getCount()));
				updateThemeMenuItem(id);
			}
		});
	}

	private void initWindowInsets() {
		ViewCompat.setOnApplyWindowInsetsListener(mainLayout, new OnApplyWindowInsetsListener() {
			@Override
			public WindowInsetsCompat onApplyWindowInsets(View v,
					WindowInsetsCompat insets) {
				if (insets.hasSystemWindowInsets()) {
					// restore padding because SYSTEM_UI_FLAG_HIDE_NAVIGATION
					// somehow removes the padding from mainLayout although
					// fitsSytemWindows is set; happens only when
					// windowLayoutInDisplayCutoutMode is set to shortEdges
					mainLayout.setPadding(
							insets.getSystemWindowInsetLeft(),
							insets.getSystemWindowInsetTop(),
							insets.getSystemWindowInsetRight(),
							insets.getSystemWindowInsetBottom());
				}
				return insets.consumeSystemWindowInsets();
			}
		});
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private void initDecorView() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			return;
		}
		decorView = getWindow().getDecorView();
		decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
			@Override
			public void onSystemUiVisibilityChange(int visibility) {
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
