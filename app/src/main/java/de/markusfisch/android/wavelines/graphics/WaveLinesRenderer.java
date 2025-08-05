package de.markusfisch.android.wavelines.graphics;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.SystemClock;

import java.util.Random;

import de.markusfisch.android.wavelines.app.WaveLinesApp;
import de.markusfisch.android.wavelines.database.Theme;

public class WaveLinesRenderer {
	private static final float MIN_THICKNESS = .33f;
	private static final long SEED = System.currentTimeMillis();

	private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Path path = new Path();

	private Theme theme;
	private long lastTime;
	private float thicknessMax;
	private float thicknessMin;
	private float thicknessCombined;
	private float amplitude;
	private float width;
	private float height;
	private float maxSize;
	private float density = WaveLinesApp.dp;
	private boolean themeUpdate = true;
	private boolean sizeUpdate = true;

	public void setTheme(Theme theme) {
		if (this.theme != theme) {
			this.theme = theme;
			themeUpdate = true;
		}
	}

	public void setSize(int width, int height) {
		this.width = width;
		this.height = height;
		sizeUpdate = true;
		themeUpdate = true;
	}

	public void setDensity(float density) {
		this.density = density;
	}

	public void draw(Canvas canvas) {
		draw(canvas, SystemClock.elapsedRealtime());
	}

	public void draw(Canvas canvas, long now) {
		if (themeUpdate) {
			if (!initWaves()) {
				return;
			}
			themeUpdate = false;
			lastTime = now - 16L;
		}

		// scale for shorter time deltas only
		float factor = Math.min(.032f, (now - lastTime) / 1000.0f);
		lastTime = now;

		canvas.save();

		// always translate view rectangle to be the middle of the
		// maxSize*maxSize square to avoid jumping if a rotation
		// is applied (and centering becomes necessary)
		//
		//     +-----------+ square to draw
		//     |  +-----+  |
		//     |  |view |  |
		//     |  |  *  |  |
		//     |  |     |  |
		//     |  +-----+  |
		//     +-----------+
		//
		// * = pivot
		{
			float cx = width * .5f;
			float cy = height * .5f;
			// move the pivot of the rotation to the middle of the view
			canvas.translate(cx, cy);
			canvas.rotate(theme.rotation);
			// move back and a little bit more to position the view
			// rectangle in the middle of the maxSize*maxSize square
			canvas.translate(
					Math.round((maxSize - width) * -.5f - cx),
					Math.round((maxSize - height) * -.5f - cy)
			);
		}

		float y = 0f;
		for (int i = theme.lines; i-- > 0; ) {
			WaveLine wl = theme.waveLines[i];
			flow(wl, factor);

			if (y > maxSize) {
				continue;
			}

			float x;
			if (y == 0) {
				canvas.drawColor(wl.color);
				y += wl.thickness;
				continue;
			} else {
				float waveLength = wl.length;
				float halfWaveLength = waveLength / 2;
				float amp = (float) Math.sin(wl.amplitude) * amplitude;
				float lastX = wl.shift;
				x = lastX + waveLength;

				path.reset();
				path.moveTo(lastX, y);
				for (; ; lastX = x, x += waveLength) {
					float center = lastX + halfWaveLength;
					path.cubicTo(
							center,
							y - amp,
							center,
							y + amp,
							x,
							y
					);

					if (x > maxSize) {
						break;
					}
				}
			}

			y += wl.thickness;

			if (wl.strokeWidth > 0) {
				paint.setStrokeWidth(wl.strokeWidth);
				paint.setStyle(Paint.Style.STROKE);
			} else {
				float bottom = i > 0 && theme.waveLines[i - 1].strokeWidth == 0
						? y + amplitude * 2f // faster than maxSize
						: maxSize;
				path.lineTo(x, bottom);
				path.lineTo(0, bottom);
				paint.setStyle(Paint.Style.FILL);
			}

			paint.setColor(wl.color);
			canvas.drawPath(path, paint);
		}

		canvas.restore();
	}

	private boolean initWaves() {
		if (width < 1 || height < 1 || theme == null ||
				theme.lines < 2 ||
				theme.colors == null ||
				theme.colors.length < 2) {
			return false;
		}

		// calculate sizes relative to screen size
		maxSize = (float) Math.ceil(Math.sqrt(
				width * width + height * height));

		float thicknessPerLine = maxSize / theme.lines;
		thicknessMax = (float) Math.ceil(thicknessPerLine * 2f);
		thicknessMin = thicknessPerLine * MIN_THICKNESS;

		// maximum thickness of master plus partner line
		thicknessCombined = thicknessMax + thicknessMin * .5f;

		amplitude = theme.amplitude * maxSize;

		if (!sizeUpdate && theme.waveLines[0] != null) {
			return true;
		}
		sizeUpdate = false;

		int firstHalf = 0;
		float[] thicknesses = null;
		float[] growths = null;
		int[] indices = null;

		if (!theme.uniform) {
			firstHalf = (int) Math.ceil(theme.lines * .5f);
			thicknesses = new float[firstHalf];
			growths = new float[firstHalf];
			indices = new int[firstHalf];

			Random r = new Random(SEED);

			// calculate thickness of master rows
			{
				float range = thicknessMax - thicknessMin;
				for (int i = 0; i < firstHalf; ++i) {
					thicknesses[i] = thicknessMin + r.nextFloat() * range;
				}
			}

			// calculate growth of master rows
			{
				float min = maxSize * (.0002f + theme.growth);
				float range = maxSize * .002f;
				for (int i = 0; i < firstHalf; ++i) {
					growths[i] = (r.nextBoolean() ? -1 : 1) *
							(min + r.nextFloat() * range);
					indices[i] = i;
				}
			}

			// mix indices to have random partners using a
			// Fisher–Yates shuffle
			for (int i = firstHalf; i > 1; ) {
				int j = r.nextInt(i--);
				int tmp = indices[j];
				indices[j] = indices[i];
				indices[i] = tmp;
			}
		}

		// calculate wave lines
		{
			int colors = theme.colors.length;
			int strokeWidths = theme.strokeWidths.length;
			int colorIndex = !theme.shuffle
					? 0
					: (int) Math.round(Math.random() * colors);
			int waveLength = (int) Math.ceil(maxSize / theme.waves);
			float averageThickness = maxSize / theme.lines;
			float shift = waveLength * -2;
			float shiftPlus = theme.shift * (waveLength * 2f / theme.lines);
			float speed = maxSize * theme.speed;
			WaveLine lastWave = null;

			for (int i = theme.lines; i-- > 0; ++colorIndex) {
				float thickness = averageThickness;
				float growth = 0;
				int color = theme.colors[colorIndex % colors];
				float strokeWidth = theme.strokeWidths[
						colorIndex % strokeWidths] * density;
				int yang = -1;
				if (!theme.uniform && thicknesses != null) {
					if (i < firstHalf) {
						thickness = thicknesses[i];
						growth = growths[i];
					} else {
						yang = indices[i - firstHalf];
					}
				}
				if (theme.coupled && lastWave != null) {
					lastWave = new WaveLine(
							lastWave.length,
							thickness,
							growth,
							lastWave.amplitude,
							lastWave.oscillation,
							shift,
							lastWave.speed,
							strokeWidth,
							color,
							yang
					);
				} else {
					lastWave = new WaveLine(
							waveLength,
							thickness,
							growth,
							1.57f,
							theme.oscillation,
							shift * (!theme.coupled && shiftPlus == 0
									? (float) Math.random()
									: 1f),
							speed,
							strokeWidth,
							color,
							yang
					);
				}
				shift += shiftPlus;
				theme.waveLines[i] = lastWave;
			}
		}

		return true;
	}

	private void flow(WaveLine wl, float factor) {
		wl.amplitude += wl.oscillation * factor;

		wl.shift += wl.speed * factor;
		if (wl.shift > 0) {
			wl.shift -= wl.length * 2;
		}

		if (wl.yang > -1) {
			wl.thickness = Math.max(thicknessMin, thicknessCombined -
					theme.waveLines[wl.yang].thickness);
		} else if (wl.growth != 0) {
			wl.thickness += wl.growth * factor;
			if (wl.thickness > thicknessMax || wl.thickness < thicknessMin) {
				wl.thickness = bounce(wl.thickness,
						thicknessMin, thicknessMax);
				wl.growth = -wl.growth;
			}
		}
	}

	private static float bounce(float v, float min, float max) {
		if (v > max) {
			return max - (v - max);
		} else if (v < min) {
			return min + (min - v);
		}
		return v;
	}

	public static class WaveLine {
		private final float length;
		private float thickness;
		private float growth;
		private float amplitude;
		private final float oscillation;
		private float shift;
		private final float speed;
		private final float strokeWidth;
		private final int color;
		private final int yang;

		private WaveLine(
				float length,
				float thickness,
				float growth,
				float amplitude,
				float oscillation,
				float shift,
				float speed,
				float strokeWidth,
				int color,
				int yang) {
			this.length = length;
			this.thickness = thickness;
			this.growth = growth;
			this.amplitude = amplitude;
			this.oscillation = oscillation;
			this.shift = shift;
			this.speed = speed;
			this.strokeWidth = strokeWidth;
			this.color = color;
			this.yang = yang;
		}
	}
}
