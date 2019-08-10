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
	public final int rotation;
	public final int[] colors;
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
			int rotation,
			int[] colors) {
		this.coupled = coupled;
		this.uniform = uniform;
		this.shuffle = shuffle;
		this.lines = Math.max(2, Math.min(64, lines));
		this.waves = Math.max(1, Math.min(12, waves));
		this.amplitude = Math.max(0f, Math.min(0.15f, amplitude));
		this.oscillation = Math.max(0f, Math.min(3f, oscillation));
		this.shift = Math.max(0f, Math.min(1f, shift));
		this.speed = Math.max(0f, Math.min(.3f, speed));
		this.rotation = Math.abs(360 + rotation) % 360;
		this.colors = colors.clone();
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
				Math.random() > .5f ? 0 : (((int) Math.round(
						Math.random() * 90f - 45f) + 360) % 360),
				colors
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
				theme.getInt("version") > 4 ?
						(float) theme.getDouble("shift") : 0f,
				theme.getInt("version") > 5 ?
						(float) theme.getDouble("speed") : .01f,
				theme.getInt("rotation"),
				parseColorArray(theme.getJSONArray("colors"))
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
			theme.put("rotation", rotation);
			theme.put("colors", getJsonColorArray(colors));
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
		out.writeInt(rotation);
		out.writeInt(colors.length);
		out.writeIntArray(colors);
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
		rotation = in.readInt();
		colors = new int[in.readInt()];
		in.readIntArray(colors);
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
}
