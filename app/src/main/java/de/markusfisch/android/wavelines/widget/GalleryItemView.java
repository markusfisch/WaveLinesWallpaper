package de.markusfisch.android.wavelines.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import de.markusfisch.android.wavelines.database.Theme;
import de.markusfisch.android.wavelines.graphics.WaveLinesRenderer;

public class GalleryItemView extends View {
	private final Path selectedMarkerPath = new Path();
	private final Paint selectedMarkerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final WaveLinesRenderer renderer = new WaveLinesRenderer();

	private Bitmap bitmap;
	private Theme theme;
	private int width;
	private int height;

	public GalleryItemView(Context context) {
		super(context);
		initMarker(context);
	}

	public GalleryItemView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initMarker(context);
	}

	public GalleryItemView(
			Context context,
			AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		initMarker(context);
	}

	public void setTheme(Theme theme) {
		this.theme = theme;
		recycle();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		width = getMeasuredWidth();
		height = width; // make item square for now
		setMeasuredDimension(width, height);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if (theme == null || width < 1 || height < 1) {
			return;
		}
		if (bitmap == null) {
			bitmap = Bitmap.createBitmap(width, height,
					Bitmap.Config.ARGB_8888);
			Canvas c = new Canvas(bitmap);
			renderer.setSize(width, height);
			renderer.setTheme(theme);
			renderer.draw(c);
		}
		canvas.drawBitmap(bitmap, 0, 0, null);
		if (isSelected()) {
			canvas.drawPath(selectedMarkerPath, selectedMarkerPaint);
		}
	}

	private void initMarker(Context context) {
		float dp = context.getResources().getDisplayMetrics().density;
		float side = 32f * dp;
		selectedMarkerPaint.setColor(0xffffffff);
		selectedMarkerPath.moveTo(0, 0);
		selectedMarkerPath.lineTo(side, 0);
		selectedMarkerPath.lineTo(0, side);
		selectedMarkerPath.lineTo(0, 0);
	}

	private void recycle() {
		if (bitmap != null) {
			bitmap.recycle();
			bitmap = null;
		}
	}
}
