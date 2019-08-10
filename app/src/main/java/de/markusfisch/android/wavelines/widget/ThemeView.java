package de.markusfisch.android.wavelines.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import de.markusfisch.android.wavelines.database.Theme;
import de.markusfisch.android.wavelines.graphics.WaveLinesRenderer;

public class ThemeView extends SurfaceView {
	private final WaveLinesRenderer renderer = new WaveLinesRenderer();
	private final RectF bounds = new RectF();
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

	public ThemeView(Context context) {
		super(context);
		initView();
	}

	public ThemeView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView();
	}

	public ThemeView(
			Context context,
			AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		initView();
	}

	public void setTheme(Theme theme) {
		renderer.setTheme(theme);
	}

	@Override
	protected void onLayout(
			boolean changed,
			int left,
			int top,
			int right,
			int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		if (changed) {
			bounds.set(left, top, right, bottom);
		}
	}

	private void initView() {
		surfaceHolder = getHolder();
		surfaceHolder.addCallback(new SurfaceHolder.Callback() {
			@Override
			public void surfaceChanged(
					SurfaceHolder holder,
					int format,
					int width,
					int height) {
				renderer.setSize(width, height);
				drawing = true;
				postDelayed(drawRunnable, 16L);
			}

			@Override
			public void surfaceCreated(SurfaceHolder holder) {
			}

			@Override
			public void surfaceDestroyed(SurfaceHolder holder) {
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
