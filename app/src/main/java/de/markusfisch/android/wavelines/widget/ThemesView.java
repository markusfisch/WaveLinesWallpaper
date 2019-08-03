package de.markusfisch.android.wavelines.widget;

import de.markusfisch.android.wavelines.app.WaveLinesApp;
import de.markusfisch.android.wavelines.graphics.WaveLinesRenderer;
import de.markusfisch.android.wavelines.database.Database;
import de.markusfisch.android.wavelines.database.Theme;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.support.v4.widget.EdgeEffectCompat;
import android.os.Build;
import android.os.Parcelable;
import android.os.Parcel;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.SurfaceHolder;
import android.view.View;

import java.util.ArrayList;

public class ThemesView extends SurfaceView {
	public interface OnChangeListener {
		void onChange(int index, long id);
	}

	private static final int RADIUS = 1;

	private final WaveLinesRenderer renderer = new WaveLinesRenderer();
	private final ArrayList<ThemePreview> themePreviews = new ArrayList<>();
	private final RectF bounds = new RectF();
	private final Runnable completeSwipeRunnable = new Runnable() {
		@Override
		public void run() {
			long now = System.currentTimeMillis();
			double factor = (now - completeSwipeLast) / 16.0;
			completeSwipeLast = now;

			deltaX += stepX * factor;

			if (stepX > 0 ? deltaX > finalX : deltaX < finalX) {
				stopSwipe();
			} else {
				post(completeSwipeRunnable);
			}
		}
	};
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
	private EdgeEffectCompat edgeEffectLeft;
	private EdgeEffectCompat edgeEffectRight;
	private Cursor cursor;
	private int themeCount;
	private int currentIndex;
	private int idColumn;
	private int pointerId;
	private boolean swiping = false;
	private boolean drawing = false;
	private float swipeThreshold;
	private float initialX;
	private float deltaX;
	private float stepX;
	private float finalX;
	private long initialTime;
	private long completeSwipeLast;
	private OnChangeListener onChangeListener;

	public ThemesView(Context context) {
		super(context);
		initView(context);
	}

	public ThemesView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView(context);
	}

	public ThemesView(
			Context context,
			AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		initView(context);
	}

	public void closeCursor() {
		if (cursor != null) {
			cursor.close();
			cursor = null;
		}
		themeCount = 0;
	}

	public void setThemes(Cursor cursor) {
		setThemes(cursor, 0);
	}

	public void setThemes(Cursor cursor, int index) {
		closeCursor();
		if (cursor == null || (themeCount = cursor.getCount()) < 1) {
			return;
		}
		this.cursor = cursor;
		idColumn = cursor.getColumnIndex(Database.THEMES_ID);
		currentIndex = Math.max(0, Math.min(index, themeCount - 1));
		if (bounds.width() > 0) {
			generatePreviews();
		}
		propagateChange();
	}

	public void setOnChangeListener(OnChangeListener listener) {
		onChangeListener = listener;
	}

	public void setSelectedIndex(int index) {
		currentIndex = index;
	}

	public int getSelectedIndex() {
		return currentIndex;
	}

	public long getSelectedThemeId() {
		return getThemeId(currentIndex);
	}

	public int getCount() {
		return themeCount;
	}

	@Override
	public Parcelable onSaveInstanceState() {
		Parcelable superState = super.onSaveInstanceState();
		SavedState savedState = new SavedState(superState);
		savedState.index = currentIndex;
		return savedState;
	}

	@Override
	public void onRestoreInstanceState(Parcelable state) {
		SavedState savedState = (SavedState) state;
		super.onRestoreInstanceState(savedState.getSuperState());
		currentIndex = savedState.index;
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
			generatePreviews();
		}
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// ignore all input while animating
		if (completeSwipeLast > 0) {
			return true;
		}
		switch (event.getActionMasked()) {
			default:
				break;
			case MotionEvent.ACTION_DOWN:
				initSwipe(event, -1);
				return true;
			case MotionEvent.ACTION_POINTER_UP:
				if (swiping) {
					initSwipe(event, event.getActionIndex());
				}
				return true;
			case MotionEvent.ACTION_MOVE:
				int pointerCount = event.getPointerCount();
				if (swiping) {
					swipe(event);
				} else if (initialX > -1 &&
						pointerCount == 1 &&
						Math.abs(getSwipeDistance(event)) > swipeThreshold) {
					startSwipe();
				}
				return true;
			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_UP:
				if (swiping) {
					completeSwipe(event);
					return true;
				} else {
					performClick();
				}
				break;
		}
		return super.onTouchEvent(event);
	}

	private void initView(Context context) {
		float dp = context.getResources().getDisplayMetrics().density;
		swipeThreshold = 16f * dp;

		edgeEffectLeft = new EdgeEffectCompat(context);
		edgeEffectRight = new EdgeEffectCompat(context);

		for (int i = RADIUS * 2 + 1; i-- > 0; ) {
			themePreviews.add(new ThemePreview());
		}

		initSurfaceHolder();
	}

	private void initSurfaceHolder() {
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
		drawView(canvas);
		surfaceHolder.unlockCanvasAndPost(canvas);
	}

	private void drawView(Canvas canvas) {
		if (swiping) {
			float boundsWidth = bounds.width();
			for (int i = Math.max(0, currentIndex - 1),
					l = Math.min(themeCount - 1, currentIndex + 1);
					i <= l; ++i) {
				ThemePreview preview = getThemePreview(i);
				if (preview != null && preview.bitmap != null) {
					canvas.drawBitmap(preview.bitmap,
							deltaX + (i - currentIndex) * boundsWidth,
							0,
							null);
				}
			}
		} else {
			ThemePreview preview = getThemePreview(currentIndex);
			if (preview != null && preview.theme != null) {
				renderer.setTheme(preview.theme);
				renderer.draw(canvas);
			}
		}
		drawEdgeEffects(canvas);
	}

	private void drawEdgeEffects(Canvas canvas) {
		drawEdgeEffect(canvas, edgeEffectLeft, 90);
		drawEdgeEffect(canvas, edgeEffectRight, 270);
	}

	private void drawEdgeEffect(
			Canvas canvas,
			EdgeEffectCompat edgeEffect,
			int degrees) {
		if (canvas == null || edgeEffect == null ||
				edgeEffect.isFinished()) {
			return;
		}

		int restoreCount = canvas.getSaveCount();
		int width = getWidth();
		int height = getHeight() - getPaddingTop() - getPaddingBottom();

		canvas.rotate(degrees);

		if (degrees == 270) {
			canvas.translate(
					(float) -height + getPaddingTop(),
					0);
		} else {
			canvas.translate(-getPaddingTop(), -width);
		}

		edgeEffect.setSize(height, width);

		if (edgeEffect.draw(canvas)) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
				postInvalidateOnAnimation();
			} else {
				postInvalidate();
			}
		}

		canvas.restoreToCount(restoreCount);
	}

	private void initSwipe(MotionEvent event, int ignore) {
		if (ignore < 0) {
			initialTime = event.getEventTime();
		}
		for (int i = 0, l = event.getPointerCount(); i < l; ++i) {
			if (i != ignore) {
				pointerId = event.getPointerId(i);
				initialX = event.getX(i);
				if (ignore > -1) {
					initialX -= deltaX;
				}
				break;
			}
		}
	}

	private void startSwipe() {
		deltaX = 0;
		swiping = true;
		refreshPreview(currentIndex);
	}

	private void refreshPreview(int index) {
		ThemePreview preview = getThemePreview(index);
		if (preview != null && preview.bitmap != null) {
			Canvas canvas = new Canvas(preview.bitmap);
			renderer.setTheme(preview.theme);
			renderer.draw(canvas);
		}
	}

	private void swipe(MotionEvent event) {
		deltaX = trim(getSwipeDistance(event));

		float width = bounds.width();
		if (Math.abs(deltaX) >= width) {
			if (deltaX > 0) {
				--currentIndex;
				initialX += width;
				deltaX -= width;
			} else {
				++currentIndex;
				initialX -= width;
				deltaX += width;
			}
			shiftPreviews(deltaX);
		}

		drawRunnable.run();
	}

	private void completeSwipe(MotionEvent event) {
		deltaX = trim(getSwipeDistance(event));

		final float swipeDistance = Math.abs(deltaX);
		final float swipeTime = (float) event.getEventTime() - initialTime;
		final float width = bounds.width();
		final float speed = Math.min(width * .1f, Math.max(
				width * .06f,
				swipeDistance * 16f / swipeTime));

		if (deltaX == 0) {
			swiping = false;
			return;
		} else if (swipeDistance > width * .5f || swipeTime < 300) {
			if (deltaX > 0) {
				finalX = width;
				stepX = speed;
			} else {
				finalX = -width;
				stepX = -speed;
			}
		} else {
			finalX = 0;
			stepX = deltaX > 0 ? -speed : speed;
		}

		completeSwipeLast = System.currentTimeMillis() - 16;
		removeCallbacks(completeSwipeRunnable);
		post(completeSwipeRunnable);
	}

	private void stopSwipe() {
		if (finalX != 0) {
			currentIndex += finalX < 0 ? 1 : -1;
			shiftPreviews(finalX);
			propagateChange();
		}
		deltaX = 0;
		completeSwipeLast = 0;
		swiping = false;
	}

	private void propagateChange() {
		if (onChangeListener != null) {
			onChangeListener.onChange(currentIndex, getThemeId(currentIndex));
		}
	}

	private float trim(float d) {
		if (d > 0 ?
				currentIndex == 0 :
				currentIndex == themeCount - 1) {
			if (!edgeEffectLeft.isFinished()) {
				edgeEffectLeft.onRelease();
			}
			if (!edgeEffectRight.isFinished()) {
				edgeEffectRight.onRelease();
			}
			float nd = d / getWidth();
			if (nd < 0) {
				// onPull(float) is deprecated but required
				// because of support for SDK 9
				edgeEffectLeft.onPull(nd);
			} else if (nd > 0) {
				edgeEffectRight.onPull(nd);
			}
			return 0;
		}
		return d;
	}

	private void shiftPreviews(float d) {
		ThemePreview preview = new ThemePreview();
		if (d < 0) {
			themePreviews.get(0).recycle();
			themePreviews.remove(0);
			themePreviews.add(preview);
			generatePreviewAt(currentIndex + RADIUS, preview);
		} else if (d > 0) {
			int lastItem = themePreviews.size() - 1;
			themePreviews.get(lastItem).recycle();
			themePreviews.remove(lastItem);
			themePreviews.add(0, preview);
			generatePreviewAt(currentIndex - RADIUS, preview);
		}
	}

	private float getSwipeDistance(MotionEvent event) {
		for (int i = 0, l = event.getPointerCount(); i < l; ++i) {
			if (event.getPointerId(i) == pointerId) {
				return event.getX(i) - initialX;
			}
		}
		return 0;
	}

	private void generatePreviews() {
		for (int i = Math.max(0, currentIndex - RADIUS);
				i < currentIndex; ++i) {
			generatePreviewAt(i);
		}
		generatePreviewAt(currentIndex);
		for (int i = currentIndex + 1, l = Math.min(themeCount, i + RADIUS);
				i < l; ++i) {
			generatePreviewAt(i);
		}
	}

	private void generatePreviewAt(int index) {
		generatePreviewAt(index, getThemePreview(index));
	}

	private void generatePreviewAt(int index, ThemePreview preview) {
		Theme theme = getThemeAt(index);
		if (preview == null || theme == null) {
			return;
		}
		int width = Math.round(bounds.width());
		int height = Math.round(bounds.height());
		Bitmap bitmap = Bitmap.createBitmap(width, height,
				Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		renderer.setSize(width, height);
		renderer.setTheme(theme);
		renderer.draw(canvas);
		preview.set(bitmap, theme);
	}

	private Theme getThemeAt(int index) {
		return cursor != null && !cursor.isClosed() &&
				cursor.moveToPosition(index) ?
				WaveLinesApp.db.themeFromCursor(cursor) :
				null;
	}

	private long getThemeId(int index) {
		return cursor != null && !cursor.isClosed() &&
				cursor.moveToPosition(index) ?
				cursor.getLong(idColumn) :
				0;
	}

	private ThemePreview getThemePreview(int index) {
		int previewIndex = index - currentIndex + RADIUS;
		if (previewIndex < 0 || previewIndex >= themePreviews.size()) {
			return null;
		}
		return themePreviews.get(previewIndex);
	}

	private static class ThemePreview {
		private Bitmap bitmap;
		private Theme theme;

		private void set(Bitmap bitmap, Theme theme) {
			recycle();
			this.bitmap = bitmap;
			this.theme = theme;
		}

		private void recycle() {
			if (bitmap != null) {
				bitmap.recycle();
				bitmap = null;
			}
		}
	}

	private static final class SavedState extends View.BaseSavedState {
		private int index;

		SavedState(Parcelable superState) {
			super(superState);
		}

		private SavedState(Parcel in) {
			super(in);
			index = in.readInt();
		}

		@Override
		public void writeToParcel(Parcel out, int flags) {
			super.writeToParcel(out, flags);
			out.writeInt(index);
		}

		public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
			public SavedState createFromParcel(Parcel in) {
				return new SavedState(in);
			}

			public SavedState[] newArray(int size) {
				return new SavedState[size];
			}
		};
	}
}
