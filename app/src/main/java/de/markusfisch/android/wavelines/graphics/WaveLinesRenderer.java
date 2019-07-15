package de.markusfisch.android.wavelines.graphics;

import de.markusfisch.android.wavelines.database.Theme;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

public class WaveLinesRenderer {
	private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Path path = new Path();

	private Theme theme;
	private float thicknessMax;
	private float thicknessMin;
	private float amplitudeMax;
	private float amplitudeMin;
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

	public void draw(Canvas canvas, long delta) {
		if (themeUpdate) {
			if (create()) {
				themeUpdate = false;
			} else {
				return;
			}
		}

		canvas.save();
		{
			float cx = width * .5f;
			float cy = height * .5f;
			canvas.translate(cx, cy);
			canvas.rotate(theme.rotation);
			canvas.translate(
					(maxSize - width) * -.5f - cx,
					(maxSize - height) * -.5f - cy);
		}

		final double elapsed = delta / 1000.0;
		float r = 0;

		for (int i = theme.lines; i-- > 0; ) {
			WaveLine wl = theme.waveLines[i];
			flow(wl, elapsed);

			if (r > maxSize) {
				continue;
			}

			// build path
			{
				final float l = wl.length;
				final float h = l / 2;
				float lx = wl.shift;
				float y = r;
				float ly = y;
				float x = lx + l;

				path.reset();
				path.moveTo(lx, ly);

				if (y == 0) {
					x = maxSize;
					path.lineTo(x, y);
				} else {
					final float a = wl.amplitude;

					for (; ; lx = x, x += l) {
						final float m = lx + h;

						path.cubicTo(
								m,
								y - a,
								m,
								y + a,
								x,
								y
						);

						if (x > maxSize) {
							break;
						}
					}
				}

				r += wl.thickness;
				ly = r + amplitudeMax * 2;
				path.lineTo(x, ly);
				path.lineTo(0, ly);
			}

			paint.setColor(wl.color);
			canvas.drawPath(path, paint);
		}

		canvas.restore();
	}

	private boolean create() {
		if (theme == null ||
				theme.lines < 1 ||
				width < 1 ||
				height < 1 ||
				theme.colors == null ||
				theme.colors.length < 2) {
			return false;
		}

		// calculate sizes relative to screen size
		maxSize = (float) Math.sqrt(width*width + height*height);

		thicknessMax = (float) Math.ceil((maxSize / theme.lines) * 2f);
		thicknessMin = Math.max(2, .01f * maxSize);

		amplitudeMax = theme.amplitude * maxSize;
		amplitudeMin = -amplitudeMax;

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
				final float min = maxSize * .0001f;
				final float max = maxSize * .0020f;

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

		// create wave lines
		{
			int colorIndex = !theme.shuffle ? 0 : (int) Math.round(
					Math.random() * (theme.colors.length - 1));
			final float thickness = maxSize / theme.lines;

			final int waveLength = (int) Math.ceil(maxSize / theme.waves);
			final float lengthVariation = waveLength * .1f;
			WaveLine last = null;

			for (int i = theme.lines; i-- > 0; ++colorIndex) {
				float growth = !theme.uniform && i < firstHalf ?
						growths[i] : 0;
				int color = theme.colors[colorIndex % theme.colors.length];
				int yang = !theme.uniform && i >= firstHalf ?
						indices[i - firstHalf] : -1;
				if (theme.coupled && last != null) {
					last = new WaveLine(
						last.length,
						last.thickness,
						growth,
						last.amplitude,
						last.power,
						last.shift,
						last.speed,
						color,
						yang
					);
				} else {
					float length = waveLength +
							((float) Math.random() * lengthVariation -
									lengthVariation * .5f);
					float amplitude = amplitudeMin + (float) Math.ceil(
							Math.random() * (amplitudeMax - amplitudeMin));
					float power = .1f + (theme.coupled ?
							amplitudeMax * .37f :
							(float) Math.random() * amplitudeMax * .75f);
					float shift = (float) Math.random() * waveLength * -2;
					float speed = maxSize * .01f +
							(float) Math.random() * maxSize * .03125f;
					last = new WaveLine(
						length,
						thickness,
						growth,
						amplitude,
						power,
						shift,
						speed,
						color,
						yang
					);
				}
				theme.waveLines[i] = last;
			}
		}

		return true;
	}

	private void flow(WaveLine wl, double delta) {
		// raise the power if the wave gets shallow to avoid
		// having straight lines for too long
		float p = amplitudeMax - Math.abs(wl.amplitude);

		if (Math.abs(wl.power) > p) {
			p = wl.power;
		} else if (wl.power < 0) {
			p = -p;
		}

		wl.amplitude += p * delta;

		if (wl.amplitude > amplitudeMax || wl.amplitude < amplitudeMin) {
			wl.amplitude = clampMirror(wl.amplitude, amplitudeMin,
					amplitudeMax);

			wl.power = -wl.power;
		}

		wl.shift += wl.speed * delta;

		if (wl.shift > 0) {
			wl.shift -= wl.length * 2;
		}

		if (wl.yang > -1) {
			wl.thickness = (thicknessMax + thicknessMin) -
					theme.waveLines[wl.yang].thickness;
		} else if (wl.growth != 0) {
			wl.thickness += wl.growth * delta;

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
		private float power;
		private float shift;
		private float speed;
		private int color;
		private int yang;

		private WaveLine(
				float length,
				float thickness,
				float growth,
				float amplitude,
				float power,
				float shift,
				float speed,
				int color,
				int yang) {
			this.length = length;
			this.thickness = thickness;
			this.growth = growth;
			this.amplitude = amplitude;
			this.power = power;
			this.shift = shift;
			this.speed = speed;
			this.color = color;
			this.yang = yang;
		}
	}
}
