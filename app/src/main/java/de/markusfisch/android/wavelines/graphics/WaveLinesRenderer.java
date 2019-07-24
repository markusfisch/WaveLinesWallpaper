package de.markusfisch.android.wavelines.graphics;

import de.markusfisch.android.wavelines.database.Theme;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.SystemClock;

public class WaveLinesRenderer {
	private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Path path = new Path();

	private Theme theme;
	private long lastTime;
	private float thicknessMax;
	private float thicknessMin;
	private float amplitude;
	private float width;
	private float height;
	private float maxSize;
	private boolean themeUpdate = true;
	private boolean sizeUpdate = true;

	public void setTheme(Theme theme) {
		this.theme = theme;
		themeUpdate = true;
	}

	public void setSize(int width, int height) {
		this.width = width;
		this.height = height;
		sizeUpdate = true;
	}

	public void draw(Canvas canvas) {
		long now = SystemClock.elapsedRealtime();

		if (themeUpdate) {
			if (init()) {
				themeUpdate = false;
				lastTime = now - 16L;
			} else {
				return;
			}
		}

		double factor = (now - lastTime) / 1000.0;
		lastTime = now;

		canvas.save();
		{
			float cx = width * .5f;
			float cy = height * .5f;
			canvas.translate(cx, cy);
			canvas.rotate(theme.rotation);
			canvas.translate(
					(maxSize - width) * -.5f - cx,
					(maxSize - height) * -.5f - cy
			);
		}

		float y = 0f;

		for (int i = theme.lines; i-- > 0; ) {
			WaveLine wl = theme.waveLines[i];
			flow(wl, factor);

			if (y > maxSize) {
				continue;
			}

			// build path
			{
				float waveLength = wl.length;
				float halfWaveLength = waveLength / 2;
				float amp = (float) Math.sin(wl.amplitude) * amplitude;
				float lastX = wl.shift;
				float lastY = y;
				float x = lastX + waveLength;

				path.reset();
				path.moveTo(lastX, lastY);

				if (y == 0) {
					x = maxSize;
					path.lineTo(x, y);
				} else {
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
				lastY = y + amplitude * 2;
				path.lineTo(x, lastY);
				path.lineTo(0, lastY);
			}

			paint.setColor(wl.color);
			canvas.drawPath(path, paint);
		}

		canvas.restore();
	}

	private boolean init() {
		if (theme == null ||
				theme.lines < 1 ||
				width < 1 ||
				height < 1 ||
				theme.colors == null ||
				theme.colors.length < 2) {
			return false;
		}

		// calculate sizes relative to screen size
		maxSize = (float) Math.sqrt(width * width + height * height);

		thicknessMax = (float) Math.ceil((maxSize / theme.lines) * 2f);
		thicknessMin = Math.max(2, .01f * maxSize);

		amplitude = theme.amplitude * maxSize;

		if (!sizeUpdate && theme.waveLines[0] != null) {
			return true;
		}
		sizeUpdate = false;

		int firstHalf = 0;
		float[] growths = null;
		int[] indices = null;

		if (!theme.uniform) {
			firstHalf = (int) Math.ceil(theme.lines * .5f);
			growths = new float[firstHalf];
			indices = new int[firstHalf];

			// calculate growth of master rows
			{
				float min = maxSize * .0001f;
				float max = maxSize * .0020f;

				for (int i = firstHalf; i-- > 0; ) {
					growths[i] = (Math.random() > .5 ? -1 : 1) *
							(min + (float) Math.random() * max);
					indices[i] = i;
				}
			}

			// mix indices to have random partners
			for (int i = firstHalf; i-- > 0; ) {
				int j = (int) Math.round(Math.random() * (firstHalf - 1));

				if (j == i) {
					continue;
				}

				int tmp = indices[j];
				indices[j] = indices[i];
				indices[i] = tmp;
			}
		}

		// calculate wave lines
		{
			int colorIndex = !theme.shuffle ? 0 : (int) Math.round(
					Math.random() * (theme.colors.length - 1));
			int waveLength = (int) Math.ceil(maxSize / theme.waves);
			float thickness = maxSize / theme.lines;
			float shift = waveLength * -2;
			float shiftPlus = theme.shift * (waveLength * 2f / theme.lines);
			float speed = maxSize * theme.speed;
			WaveLine lastWave = null;

			for (int i = theme.lines; i-- > 0; ++colorIndex) {
				float growth = !theme.uniform && i < firstHalf ?
						growths[i] : 0;
				int color = theme.colors[colorIndex % theme.colors.length];
				int yang = !theme.uniform && i >= firstHalf ?
						indices[i - firstHalf] : -1;
				if (theme.coupled && lastWave != null) {
					lastWave = new WaveLine(
							lastWave.length,
							lastWave.thickness,
							growth,
							lastWave.amplitude,
							lastWave.oscillation,
							shift,
							lastWave.speed,
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
							shift * (!theme.coupled && shiftPlus == 0 ?
									(float) Math.random() : 1f),
							speed,
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

	private void flow(WaveLine wl, double factor) {
		wl.amplitude += wl.oscillation * factor;

		wl.shift += wl.speed * factor;
		if (wl.shift > 0) {
			wl.shift -= wl.length * 2;
		}

		if (wl.yang > -1) {
			wl.thickness = (thicknessMax + thicknessMin) -
					theme.waveLines[wl.yang].thickness;
		} else if (wl.growth != 0) {
			wl.thickness += wl.growth * factor;

			if (wl.thickness > thicknessMax || wl.thickness < thicknessMin) {
				wl.thickness = clampMirror(wl.thickness, thicknessMin,
						thicknessMax);

				wl.growth = -wl.growth;
			}
		}
	}

	private static float clampMirror(float v, float min, float max) {
		if (v > max) {
			return max - (v - max);
		} else if (v < min) {
			return min + (min - v);
		}
		return v;
	}

	public static class WaveLine {
		private float length;
		private float thickness;
		private float growth;
		private float amplitude;
		private float oscillation;
		private float shift;
		private float speed;
		private int color;
		private int yang;

		private WaveLine(
				float length,
				float thickness,
				float growth,
				float amplitude,
				float oscillation,
				float shift,
				float speed,
				int color,
				int yang) {
			this.length = length;
			this.thickness = thickness;
			this.growth = growth;
			this.amplitude = amplitude;
			this.oscillation = oscillation;
			this.shift = shift;
			this.speed = speed;
			this.color = color;
			this.yang = yang;
		}
	}
}
