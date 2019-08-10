package de.markusfisch.android.wavelines.activity;

import de.markusfisch.android.wavelines.app.WaveLinesApp;
import de.markusfisch.android.wavelines.adapter.GalleryAdapter;
import de.markusfisch.android.wavelines.database.Theme;
import de.markusfisch.android.wavelines.R;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

public class GalleryActivity extends AppCompatActivity {
	private GridLayoutManager manager;
	private GalleryAdapter adapter;
	private View progressView;

	@Override
	protected void onCreate(Bundle state) {
		super.onCreate(state);
		setContentView(R.layout.activity_gallery);
		WaveLinesApp.initToolbar(this);
		setTitle(R.string.gallery);

		// clamp because previous versions allowed more columns
		manager = new GridLayoutManager(this, clampSpanCount(
				WaveLinesApp.preferences.getGalleryColumns()));

		RecyclerView recyclerView = (RecyclerView) findViewById(R.id.themes);
		recyclerView.setHasFixedSize(true);
		recyclerView.setLayoutManager(manager);

		adapter = new GalleryAdapter();
		adapter.setClickListener(new GalleryAdapter.ItemClickListener() {
			@Override
			public void onItemClick(View view, long id, int position) {
				ThemeActivity.startWithIndex(view.getContext(), position);
			}
		});
		recyclerView.setAdapter(adapter);

		addScaleGestureDetector(recyclerView);

		progressView = findViewById(R.id.progress_view);
	}

	@Override
	protected void onResume() {
		super.onResume();
		queryThemesAsync(false);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		adapter.swapCursor(null);
		WaveLinesApp.preferences.setGalleryColumns(manager.getSpanCount());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_gallery, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.add_theme:
				addTheme();
				return true;
			case R.id.change_layout:
				chooseLayout();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	// ignore warning about missing View.performClick() because this is
	// handling gestures, not clicks
	@SuppressLint("ClickableViewAccessibility")
	private void addScaleGestureDetector(final RecyclerView recyclerView) {
		final float dp = getResources().getDisplayMetrics().density;
		final int scaleThreshold = Math.round(dp * 16f);
		final ScaleGestureDetector detector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
			@Override
			public boolean onScale(ScaleGestureDetector detector) {
				if (detector.getTimeDelta() > 200) {
					float diff = detector.getCurrentSpan() -
							detector.getPreviousSpan();
					if (Math.abs(diff) > scaleThreshold) {
						int current = manager.getSpanCount();
						int target = clampSpanCount(
								current + (diff < 0f ? 1 : -1));
						if (target != current) {
							setSpanCount(target);
							return true;
						}
					}
				}
				return false;
			}
		});
		recyclerView.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				detector.onTouchEvent(event);
				return false;
			}
		});
	}

	private static int clampSpanCount(int count) {
		return Math.min(3, Math.max(2, count));
	}

	private void setSpanCount(int count) {
		manager.setSpanCount(count);
		adapter.notifyDataSetChanged();
	}

	// this AsyncTask is running for a short and finite time only
	// and it's perfectly okay to delay garbage collection of the
	// parent instance until this task has ended
	@SuppressLint("StaticFieldLeak")
	private void queryThemesAsync(final boolean scrollToLast) {
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
					adapter.swapCursor(cursor);
					if (scrollToLast) {
						manager.scrollToPosition(cursor.getCount() - 1);
					}
				}
			}
		}.execute();
	}

	private void addTheme() {
		WaveLinesApp.db.insertTheme(new Theme());
		queryThemesAsync(true);
	}

	private void chooseLayout() {
		final int[] values = getResources().getIntArray(
				R.array.span_count_values);
		new AlertDialog.Builder(this)
				.setTitle(R.string.how_many_columns)
				.setItems(R.array.span_count_names, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						setSpanCount(values[which]);
					}
				})
				.show();
	}
}
