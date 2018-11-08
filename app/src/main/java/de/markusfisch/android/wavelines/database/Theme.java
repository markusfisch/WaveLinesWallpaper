package de.markusfisch.android.wavelines.database;

import de.markusfisch.android.wavelines.graphics.WaveLinesRenderer;

import android.graphics.Color;

public class Theme {
	public final boolean coupled;
	public final boolean uniform;
	public final boolean shuffle;
	public final int lines;
	public final int waves;
	public final float amplitude;
	public final int colors[];
	public final WaveLinesRenderer.WaveLine waveLines[];

	public Theme() {
		coupled = Math.random() > .5f ? true : false;
		uniform = Math.random() > .5f ? true : false;
		shuffle = Math.random() > .5f ? true : false;
		lines = 2 + (int) Math.round(Math.random() * 8);
		waves = 1 + (int) Math.round(Math.random() * 5);
		amplitude = .02f + Math.round(Math.random() * .13f);
		int ncolors = 2 + (int) Math.round(Math.random() * 4);
		colors = new int[ncolors];
		colors[0] = 0xff000000 | (int) Math.round(Math.random() * 0xffffff);
		for (int i = 1; i < ncolors; ++i) {
			colors[i] = getSimilarColor(colors[i - 1]);
		}
		waveLines = new WaveLinesRenderer.WaveLine[lines];
	}

	public Theme(
			boolean coupled,
			boolean uniform,
			boolean shuffle,
			int lines,
			int waves,
			float amplitude,
			int colors[]) {
		this.coupled = coupled;
		this.uniform = uniform;
		this.shuffle = shuffle;
		this.lines = lines;
		this.waves = waves;
		this.amplitude = amplitude;
		this.colors = colors.clone();
		waveLines = new WaveLinesRenderer.WaveLine[lines];
	}

	public static int getSimilarColor(int color) {
		float hsv[] = new float[3];
		Color.RGBToHSV(
			(color >> 16) & 0xff,
			(color >> 8) & 0xff,
			color & 0xff,
			hsv
		);
		float mod = .1f + ((float) Math.random() * .15f);
		float value = hsv[2] + mod;
		if (value > 1f) {
			value = Math.max(0f, hsv[2] - mod);
		}
		hsv[2] = value;
		return Color.HSVToColor(hsv);
	}
}
