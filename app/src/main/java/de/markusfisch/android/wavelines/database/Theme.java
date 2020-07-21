package de.markusfisch.android.wavelines.database;

import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.markusfisch.android.wavelines.graphics.WaveLinesRenderer;

public class Theme implements Parcelable {
	public static final Creator<Theme> CREATOR = new Creator<Theme>() {
		@Override
		public Theme createFromParcel(Parcel in) {
			return new Theme(in);
		}

		@Override
		public Theme[] newArray(int size) {
			return new Theme[size];
		}
	};

	public final boolean coupled;
	public final boolean uniform;
	public final boolean shuffle;
	public final int lines;
	public final int waves;
	public final float amplitude;
	public final float oscillation;
	public final float shift;
	public final float speed;
	public final float growth;
	public final int rotation;
	public final int[] colors;
	public final int[] strokeWidths;
	public final WaveLinesRenderer.WaveLine[] waveLines;

	public Theme(
			boolean coupled,
			boolean uniform,
			boolean shuffle,
			int lines,
			int waves,
			float amplitude,
			float oscillation,
			float shift,
			float speed,
			float growth,
			int rotation,
			int[] colors,
			int[] strokeWidths) {
		this.coupled = coupled;
		this.uniform = uniform;
		this.shuffle = shuffle;
		this.lines = Math.max(2, Math.min(24, lines));
		this.waves = Math.max(1, Math.min(12, waves));
		this.amplitude = Math.max(0f, Math.min(0.15f, amplitude));
		this.oscillation = Math.max(0f, Math.min(3f, oscillation));
		this.shift = Math.max(0f, Math.min(1f, shift));
		this.speed = Math.max(0f, Math.min(.3f, speed));
		this.growth = Math.max(0f, Math.min(.004f, growth));
		this.rotation = Math.abs(360 + rotation) % 360;
		this.colors = colors.clone();
		this.strokeWidths = initStrokeWidths(strokeWidths, colors.length);
		waveLines = new WaveLinesRenderer.WaveLine[this.lines];
	}

	public Theme() {
		this(getRandomColors());
	}

	public Theme(int[] colors) {
		this(
				Math.random() > .5f,
				Math.random() > .8f,
				false,
				2 + (int) Math.round(Math.random() * 8),
				1 + (int) Math.round(Math.random() * 5),
				.02f + (float) Math.random() * .03f,
				.5f + (float) Math.random() * 1.5f,
				(float) Math.random(),
				.005f + (float) Math.random() * .02f,
				0f,
				Math.random() > .5f
						? 0
						: (((int) Math.round(
						Math.random() * 90f - 45f) + 360) % 360),
				colors,
				getDefaultStrokeWidths(colors.length)
		);
	}

	public Theme(String json) throws JSONException {
		this(new JSONObject(json));
	}

	public Theme(JSONObject theme) throws JSONException {
		this(
				theme.getBoolean("coupled"),
				theme.getBoolean("uniform"),
				theme.getBoolean("shuffle"),
				theme.getInt("lines"),
				theme.getInt("waves"),
				(float) theme.getDouble("amplitude"),
				(float) theme.getDouble("oscillation"),
				theme.getInt("version") > 4
						? (float) theme.getDouble("shift")
						: 0f,
				theme.getInt("version") > 5
						? (float) theme.getDouble("speed")
						: .01f,
				theme.getInt("version") > 6
						? (float) theme.getDouble("growth")
						: 0f,
				theme.getInt("rotation"),
				parseColorArray(theme.getJSONArray("colors")),
				theme.getInt("version") > 7
						? parseIntArray(theme.getJSONArray("strokeWidths"))
						: getDefaultStrokeWidths(
						theme.getJSONArray("colors").length())
		);
	}

	public String toJson() {
		try {
			JSONObject theme = new JSONObject();
			theme.put("version", Database.VERSION);
			theme.put("coupled", coupled);
			theme.put("uniform", uniform);
			theme.put("shuffle", shuffle);
			theme.put("lines", lines);
			theme.put("waves", waves);
			theme.put("amplitude", amplitude);
			theme.put("oscillation", oscillation);
			theme.put("shift", shift);
			theme.put("speed", speed);
			theme.put("growth", growth);
			theme.put("rotation", rotation);
			theme.put("colors", getJsonColorArray(colors));
			theme.put("strokeWidths", getJsonIntArray(strokeWidths));
			return theme.toString();
		} catch (JSONException e) {
			return null;
		}
	}

	public static int getSimilarColor(int color) {
		float[] hsv = new float[3];
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

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeInt(coupled ? 1 : 0);
		out.writeInt(uniform ? 1 : 0);
		out.writeInt(shuffle ? 1 : 0);
		out.writeInt(lines);
		out.writeInt(waves);
		out.writeFloat(amplitude);
		out.writeFloat(oscillation);
		out.writeFloat(shift);
		out.writeFloat(speed);
		out.writeFloat(growth);
		out.writeInt(rotation);
		out.writeInt(colors.length);
		out.writeIntArray(colors);
		out.writeInt(strokeWidths.length);
		out.writeIntArray(strokeWidths);
	}

	private Theme(Parcel in) {
		coupled = in.readInt() > 0;
		uniform = in.readInt() > 0;
		shuffle = in.readInt() > 0;
		lines = in.readInt();
		waves = in.readInt();
		amplitude = in.readFloat();
		oscillation = in.readFloat();
		shift = in.readFloat();
		speed = in.readFloat();
		growth = in.readFloat();
		rotation = in.readInt();
		colors = new int[in.readInt()];
		in.readIntArray(colors);
		strokeWidths = new int[in.readInt()];
		in.readIntArray(strokeWidths);
		waveLines = new WaveLinesRenderer.WaveLine[lines];
	}

	private static int[] getRandomColors() {
		int ncolors = 2 + (int) Math.round(Math.random() * 4);
		int[] colors = new int[ncolors];
		// dice for every channel separately to increase diversity
		colors[0] = 0xff000000 |
				((int) Math.round(Math.random() * 0xff) << 16) |
				((int) Math.round(Math.random() * 0xff) << 8) |
				(int) Math.round(Math.random() * 0xff);
		for (int i = 1; i < ncolors; ++i) {
			colors[i] = getSimilarColor(colors[i - 1]);
		}
		return colors;
	}

	private static int[] initStrokeWidths(int[] src, int length) {
		if (src == null) {
			return getDefaultStrokeWidths(length);
		} else if (src.length == length) {
			return src.clone();
		}
		int[] copy = new int[length];
		System.arraycopy(src, 0, copy, 0, Math.min(src.length, length));
		for (int i = src.length; i < length; ++i) {
			copy[i] = 0;
		}
		return copy;
	}

	private static int[] getDefaultStrokeWidths(int length) {
		int[] array = new int[length];
		for (int i = 0; i < length; ++i) {
			array[i] = 0;
		}
		return array;
	}

	private static int[] parseColorArray(JSONArray array) {
		int len = array.length();
		int[] intArray = new int[len];
		for (int i = 0; i < len; ++i) {
			intArray[i] = Color.parseColor(array.optString(i));
		}
		return intArray;
	}

	private static JSONArray getJsonColorArray(int[] intArray) {
		JSONArray array = new JSONArray();
		for (int i = 0, l = intArray.length; i < l; ++i) {
			array.put(String.format("#%08X", intArray[i]));
		}
		return array;
	}

	private static int[] parseIntArray(JSONArray array) {
		int len = array.length();
		int[] intArray = new int[len];
		for (int i = 0; i < len; ++i) {
			intArray[i] = array.optInt(i);
		}
		return intArray;
	}

	private static JSONArray getJsonIntArray(int[] intArray) {
		JSONArray array = new JSONArray();
		for (int i = 0, l = intArray.length; i < l; ++i) {
			array.put(intArray[i]);
		}
		return array;
	}
}
