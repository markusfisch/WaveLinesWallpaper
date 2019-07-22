package de.markusfisch.android.wavelines.database;

import de.markusfisch.android.wavelines.graphics.WaveLinesRenderer;

import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
	public final int rotation;
	public final int[] colors;
	public final WaveLinesRenderer.WaveLine[] waveLines;

	public Theme() {
		coupled = Math.random() > .5f;
		uniform = Math.random() > .5f;
		shuffle = Math.random() > .5f;
		lines = 2 + (int) Math.round(Math.random() * 8);
		waves = 1 + (int) Math.round(Math.random() * 5);
		amplitude = .02f + Math.round(Math.random() * .13f);
		oscillation = .5f + Math.round(Math.random() * 1.5f);
		rotation = 0;
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
			float oscillation,
			int rotation,
			int[] colors) {
		this.coupled = coupled;
		this.uniform = uniform;
		this.shuffle = shuffle;
		this.lines = lines;
		this.waves = waves;
		this.amplitude = amplitude;
		this.oscillation = oscillation;
		this.rotation = rotation;
		this.colors = colors.clone();
		waveLines = new WaveLinesRenderer.WaveLine[lines];
	}

	public Theme(String json) throws JSONException {
		JSONObject theme = new JSONObject(json);
		coupled = theme.getBoolean("coupled");
		uniform = theme.getBoolean("uniform");
		shuffle = theme.getBoolean("shuffle");
		lines = theme.getInt("lines");
		waves = theme.getInt("waves");
		amplitude = (float) theme.getDouble("amplitude");
		oscillation = (float) theme.getDouble("oscillation");
		rotation = theme.getInt("rotation");
		colors = parseColorArray(theme.getJSONArray("colors"));
		waveLines = new WaveLinesRenderer.WaveLine[lines];
	}

	public String toJson() {
		try {
			JSONObject theme = new JSONObject();
			theme.put("coupled", coupled);
			theme.put("uniform", uniform);
			theme.put("shuffle", shuffle);
			theme.put("lines", lines);
			theme.put("waves", waves);
			theme.put("amplitude", amplitude);
			theme.put("oscillation", oscillation);
			theme.put("rotation", rotation);
			theme.put("colors", getJsonColorArray(colors));
			return theme.toString();
		} catch (JSONException e) {
			return null;
		}
	}

	public static Theme clamp(Theme theme) {
		return new Theme(
			theme.coupled,
			theme.uniform,
			theme.shuffle,
			Math.max(2, Math.min(24, theme.lines)),
			Math.max(1, Math.min(12, theme.waves)),
			Math.max(0f, Math.min(0.15f, theme.amplitude)),
			Math.max(0f, Math.min(3f, theme.oscillation)),
			Math.abs(360 + theme.rotation) % 360,
			theme.colors
		);
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
		rotation = in.readInt();
		colors = new int[in.readInt()];
		in.readIntArray(colors);
		waveLines = new WaveLinesRenderer.WaveLine[lines];
	}

	private static int[] parseColorArray(JSONArray array) {
		int len = array.length();
		int intArray[] = new int[len];
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
