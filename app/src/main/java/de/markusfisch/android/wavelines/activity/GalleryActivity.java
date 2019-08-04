package de.markusfisch.android.wavelines.activity;

import de.markusfisch.android.wavelines.app.WaveLinesApp;
import de.markusfisch.android.wavelines.adapter.GalleryAdapter;
import de.markusfisch.android.wavelines.R;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public class GalleryActivity extends AppCompatActivity {
	private GalleryAdapter adapter;
	private View progressView;

	@Override
	protected void onCreate(Bundle state) {
		super.onCreate(state);
		setContentView(R.layout.activity_gallery);
		WaveLinesApp.initToolbar(this);
		setTitle(R.string.gallery);

		RecyclerView recyclerView = (RecyclerView) findViewById(R.id.themes);
		recyclerView.setHasFixedSize(true);
		recyclerView.setLayoutManager(new GridLayoutManager(this, 2));

		adapter = new GalleryAdapter();
		adapter.setClickListener(new GalleryAdapter.ItemClickListener() {
			@Override
			public void onItemClick(View view, long id, int position) {
				ThemeActivity.startWithIndex(view.getContext(), position);
			}
		});
		recyclerView.setAdapter(adapter);

		progressView = findViewById(R.id.progress_view);
	}

	@Override
	protected void onResume() {
		super.onResume();
		queryThemesAsync();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		adapter.swapCursor(null);
	}

	// this AsyncTask is running for a short and finite time only
	// and it's perfectly okay to delay garbage collection of the
	// parent instance until this task has ended
	@SuppressLint("StaticFieldLeak")
	private void queryThemesAsync() {
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
				}
			}
		}.execute();
	}
}
