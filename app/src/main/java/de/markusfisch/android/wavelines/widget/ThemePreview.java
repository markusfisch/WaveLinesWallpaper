package de.markusfisch.android.wavelines.widget;

import android.content.Context;
import android.graphics.Canvas;
import androidx.annotation.NonNull;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import de.markusfisch.android.wavelines.database.Theme;
import de.markusfisch.android.wavelines.graphics.WaveLinesRenderer;

public class ThemePreview extends SurfaceView {
	private final WaveLinesRenderer renderer = new WaveLinesRenderer();
	private final Runnable drawRunnable = new Runnable() {
		@Override
		public void run() {
			removeCallbacks(drawRunnable);
			if (!drawing) {
				return;
			}
			drawView();
			postDelayed(drawRunnable, 16L);
		}
	};

	private SurfaceHolder surfaceHolder;
	private boolean drawing = false;

	public ThemePreview(Context context) {
		super(context);
		initView();
	}

	public ThemePreview(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView();
	}

	public ThemePreview(
			Context context,
			AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		initView();
	}

	public void setDensity(float density) {
		renderer.setDensity(density);
	}

	public void setTheme(Theme theme) {
		renderer.setTheme(theme);
	}

	private void initView() {
		surfaceHolder = getHolder();
		surfaceHolder.addCallback(new SurfaceHolder.Callback() {
			@Override
			public void surfaceChanged(
					@NonNull SurfaceHolder holder,
					int format,
					int width,
					int height) {
				renderer.setSize(width, height);
				drawing = true;
				postDelayed(drawRunnable, 16L);
			}

			@Override
			public void surfaceCreated(@NonNull SurfaceHolder holder) {
			}

			@Override
			public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
				drawing = false;
				removeCallbacks(drawRunnable);
			}
		});
	}

	private void drawView() {
		Canvas canvas = surfaceHolder.lockCanvas();
		if (canvas == null) {
			return;
		}
		renderer.draw(canvas);
		surfaceHolder.unlockCanvasAndPost(canvas);
	}
}
