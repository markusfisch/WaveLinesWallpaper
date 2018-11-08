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

		final double elapsed = delta / 1000.0;
		float r = 0;

		for (int i = theme.lines; i-- > 0; ) {
			flow(theme.waveLines[i], elapsed);

			if (r > height) {
				continue;
			}

			// build path
			{
				final float l = theme.waveLines[i].length;
				final float h = l / 2;
				float lx = theme.waveLines[i].shift;
				float y = r;
				float ly = y;
				float x = lx + l;

				path.reset();
				path.moveTo(lx, ly);

				if (y == 0) {
					x = width;
					path.lineTo(x, y);
				} else {
					final float a = theme.waveLines[i].amplitude;

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

						if (x > width) {
							break;
						}
					}
				}

				r += theme.waveLines[i].thickness;
				ly = r + amplitudeMax * 2;
				path.lineTo(x, ly);
				path.lineTo(0, ly);
			}

			paint.setColor(theme.waveLines[i].color);
			canvas.drawPath(path, paint);
		}
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
		final float maxSize = Math.max(width, height);

		thicknessMax = (float) Math.ceil(
				(maxSize / theme.lines) * 2f);
		thicknessMin = Math.max(2, .01f * maxSize);

		amplitudeMax = theme.amplitude * maxSize;
		amplitudeMin = -amplitudeMax;

		if (!sizeUpdate && theme.waveLines[0] != null) {
			return true;
		}
		sizeUpdate = false;

		int hl = 0;
		float growths[] = null;
		int indices[] = null;

		if (!theme.uniform) {
			hl = (int) Math.ceil(theme.lines / 2f);
			growths = new float[hl];
			indices = new int[hl];

			// calculate growth of master rows
			{
				final float min = maxSize * .0001f;
				final float max = maxSize * .0020f;

				for (int i = hl; i-- > 0; ) {
					growths[i] = (Math.random() > .5 ? -1 : 1) *
							(min + (float) Math.random() * max);
					indices[i] = i;
				}
			}

			// mix indices to have random partners
			for (int i = hl; i-- > 0; ) {
				int p = (int) Math.round(Math.random() * (hl - 1));

				if (p == i) {
					continue;
				}

				int tmp = indices[p];
				indices[p] = indices[i];
				indices[i] = tmp;
			}
		}

		// create wave lines
		{
			int c = !theme.shuffle ? 0 : (int) Math.round(Math.random() *
					(theme.colors.length - 1));
			final float thickness = maxSize / theme.lines;

			final int length = (int) Math.ceil((float) maxSize / theme.waves);
			final float v = length * .1f;
			final float hv = v / 2f;
			WaveLine last = null;

			for (int i = theme.lines; i-- > 0; ++c) {
				float growth = !theme.uniform && i < hl ? growths[i] : 0;
				int color = theme.colors[c % theme.colors.length];
				int yang = !theme.uniform && i >= hl ? indices[i - hl] : -1;
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
					last = new WaveLine(
						length + ((float) Math.random() * v - hv),
						thickness,
						growth,
						amplitudeMin + (float) Math.ceil(Math.random() *
								(amplitudeMax - amplitudeMin)),
						.1f + (theme.coupled ?
								amplitudeMax * .37f :
								(float) Math.random() * amplitudeMax * .75f),
						-(float) Math.random() * length * 2,
						width * .01f + (float) Math.random() *
								(width * .03125f),
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

		if (wl.amplitude > amplitudeMax ||
				wl.amplitude < amplitudeMin) {
			if (wl.amplitude > amplitudeMax) {
				wl.amplitude = amplitudeMax - (wl.amplitude - amplitudeMax);
			} else {
				wl.amplitude = amplitudeMin + (amplitudeMin - wl.amplitude);
			}

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

			if (wl.thickness > thicknessMax ||
					wl.thickness < thicknessMin) {
				if (wl.thickness > thicknessMax) {
					wl.thickness = thicknessMax -
							(wl.thickness - thicknessMax);
				} else {
					wl.thickness = thicknessMin +
							(thicknessMin - wl.thickness);
				}

				wl.growth = -wl.growth;
			}
		}
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
