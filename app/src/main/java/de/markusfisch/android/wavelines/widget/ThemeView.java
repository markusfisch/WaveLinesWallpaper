package de.markusfisch.android.wavelines.widget;

import de.markusfisch.android.wavelines.graphics.WaveLinesRenderer;
import de.markusfisch.android.wavelines.database.Theme;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.SurfaceView;
import android.view.SurfaceHolder;

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
			long now = System.currentTimeMillis();
			drawView(now - lastDraw);
			lastDraw = now;
			postDelayed(drawRunnable, 16l);
		}
	};

	private SurfaceHolder surfaceHolder;
	private long lastDraw = 0;
	private boolean drawing = false;

	public ThemeView(Context context) {
		super(context);
		initView(context);
	}

	public ThemeView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView(context);
	}

	public ThemeView(
			Context context,
			AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		initView(context);
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

	private void initView(Context context) {
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
				lastDraw = System.currentTimeMillis();
				postDelayed(drawRunnable, 16l);
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

	private void drawView(long delta) {
		Canvas canvas = surfaceHolder.lockCanvas();
		if (canvas == null) {
			return;
		}
		renderer.draw(canvas, delta);
		surfaceHolder.unlockCanvasAndPost(canvas);
	}
}
