package de.markusfisch.android.wavelines.widget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;

import de.markusfisch.android.wavelines.R;
import de.markusfisch.android.wavelines.app.WaveLinesApp;
import de.markusfisch.android.wavelines.database.Theme;
import de.markusfisch.android.wavelines.graphics.WaveLinesRenderer;

public class GalleryItemView extends View {
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
		// Make item a square for now.
		width = height = getMeasuredWidth();
		setMeasuredDimension(width, height);
	}

	@Override
	protected void onDraw(@NonNull Canvas canvas) {
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
			canvas.drawRect(0, 0, width, height, selectedMarkerPaint);
		}
	}

	private void initMarker(Context context) {
		Resources res = context.getResources();
		selectedMarkerPaint.setColor(res.getColor(R.color.accent));
		selectedMarkerPaint.setStyle(Paint.Style.STROKE);
		selectedMarkerPaint.setStrokeWidth(8f * WaveLinesApp.dp);
	}

	private void recycle() {
		if (bitmap != null) {
			bitmap.recycle();
			bitmap = null;
		}
	}
}
