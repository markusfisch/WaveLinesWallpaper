package de.markusfisch.android.wavelines.widget;

import de.markusfisch.android.wavelines.database.Theme;
import de.markusfisch.android.wavelines.graphics.WaveLinesRenderer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

public class GalleryItemView extends View {
	private final WaveLinesRenderer renderer = new WaveLinesRenderer();

	private Bitmap bitmap;
	private Theme theme;
	private int width;
	private int height;

	public GalleryItemView(Context context) {
		super(context);
	}

	public GalleryItemView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public GalleryItemView(
			Context context,
			AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);
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
	}

	private void recycle() {
		if (bitmap != null) {
			bitmap.recycle();
			bitmap = null;
		}
	}
}
