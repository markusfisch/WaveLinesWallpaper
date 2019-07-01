package de.markusfisch.android.wavelines.fragment;

import de.markusfisch.android.wavelines.activity.AbstractActivity;
import de.markusfisch.android.wavelines.app.WaveLinesApp;
import de.markusfisch.android.wavelines.database.Theme;
import de.markusfisch.android.wavelines.widget.ThemesView;
import de.markusfisch.android.wavelines.R;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

public class ThemeListFragment extends Fragment {
	private ThemesView themesView;
	private MenuItem setThemeMenuItem;
	private View progressView;

	@Override
	public void onCreate(Bundle state) {
		super.onCreate(state);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle state) {
		View view = inflater.inflate(R.layout.fragment_theme_list,
				container, false);

		themesView = view.findViewById(R.id.themes);

		final String title = getString(R.string.themes);
		themesView.setOnChangeListener(new ThemesView.OnChangeListener() {
			@Override
			public void onChange(int index, long id) {
				Activity activity = getActivity();
				if (activity == null) {
					return;
				}
				activity.setTitle(String.format(title, index + 1,
						themesView.getCount()));
				updateThemeMenuItem(id);
			}
		});

		view.findViewById(R.id.edit_theme).setOnClickListener(
				new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				AbstractActivity.addFragment(getFragmentManager(),
						ThemeEditorFragment.newInstance(
								themesView.getSelectedThemeId()));
			}
		});

		progressView = view.findViewById(R.id.progress_view);

		return view;
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
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.fragment_theme_list, menu);
		setThemeMenuItem = menu.findItem(R.id.set_theme);
		updateThemeMenuItem();
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
				if (!isAdded()) {
					return;
				}
				progressView.setVisibility(View.GONE);
				if (cursor != null) {
					themesView.setThemes(cursor, index);
				}
			}
		}.execute();
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
		WaveLinesApp.db.insertTheme(new Theme());
		queryThemesAsync(themesView.getCount());
	}

	private void duplicateTheme(long id) {
		WaveLinesApp.db.insertTheme(WaveLinesApp.db.getTheme(id));
		queryThemesAsync(themesView.getCount());
	}

	private void askDeleteTheme(final long id) {
		Activity activity = getActivity();
		if (activity == null) {
			return;
		}
		new AlertDialog.Builder(activity)
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
		Activity activity = getActivity();
		if (activity == null) {
			return;
		}
		item.setIcon(R.drawable.ic_wallpaper_set);
		Toast.makeText(activity, R.string.set_as_wallpaper,
				Toast.LENGTH_SHORT).show();
	}
}
