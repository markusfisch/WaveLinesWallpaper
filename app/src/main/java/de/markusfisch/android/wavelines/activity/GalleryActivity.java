package de.markusfisch.android.wavelines.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.concurrent.Executors;

import de.markusfisch.android.wavelines.R;
import de.markusfisch.android.wavelines.adapter.GalleryAdapter;
import de.markusfisch.android.wavelines.app.WaveLinesApp;
import de.markusfisch.android.wavelines.database.Theme;
import de.markusfisch.android.wavelines.service.WallpaperSetter;

public class GalleryActivity extends AppCompatActivity {
	private final Handler handler = new Handler(Looper.getMainLooper());

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

			@Override
			public boolean onItemLongClick(View view, long id, int position) {
				WallpaperSetter.setAsWallpaper(GalleryActivity.this, id);
				adapter.notifyDataSetChanged();
				return true;
			}
		});
		recyclerView.setAdapter(adapter);

		addScaleGestureDetector(recyclerView);

		findViewById(R.id.add_theme).setOnClickListener(v -> addTheme());

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
		if (item.getItemId() == R.id.change_layout) {
			chooseLayout();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	// ignore warning about missing View.performClick() because this is
	// handling gestures, not clicks
	@SuppressLint("ClickableViewAccessibility")
	private void addScaleGestureDetector(final RecyclerView recyclerView) {
		final int scaleThreshold = Math.round(16f * WaveLinesApp.dp);
		final ScaleGestureDetector detector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
			@Override
			public boolean onScale(@NonNull ScaleGestureDetector detector) {
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
		recyclerView.setOnTouchListener((v, event) -> {
			detector.onTouchEvent(event);
			return false;
		});
	}

	private static int clampSpanCount(int count) {
		return Math.min(3, Math.max(2, count));
	}

	private void setSpanCount(int count) {
		manager.setSpanCount(count);
		adapter.notifyDataSetChanged();
	}

	private void queryThemesAsync(final boolean scrollToLast) {
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
					adapter.swapCursor(cursor);
					if (scrollToLast) {
						manager.scrollToPosition(cursor.getCount() - 1);
					}
				}
			});
		});
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
				.setItems(R.array.span_count_names,
						(dialog, which) -> setSpanCount(values[which]))
				.show();
	}
}
