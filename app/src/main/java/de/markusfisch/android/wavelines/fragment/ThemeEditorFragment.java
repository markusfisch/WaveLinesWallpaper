package de.markusfisch.android.wavelines.fragment;

import de.markusfisch.android.wavelines.app.WaveLinesApp;
import de.markusfisch.android.wavelines.database.Theme;
import de.markusfisch.android.wavelines.R;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ThemeEditorFragment extends Fragment {
	private static final String ID = "id";
	private static final Pattern HEX_PATTERN = Pattern.compile(
			"^[0-9a-f]{1,6}$");

	private final ArrayList<Integer> colors = new ArrayList<>();
	private final SeekBar.OnSeekBarChangeListener updateColorFromBarsListener =
			new SeekBar.OnSeekBarChangeListener() {
		@Override
		public void onProgressChanged(SeekBar seekBar, int progressValue,
				boolean fromUser) {
			setColorFromHSVBars();
			updateHSVLabels();
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {}
	};
	private final SeekBar.OnSeekBarChangeListener updateLabelsListener =
			new SeekBar.OnSeekBarChangeListener() {
		@Override
		public void onProgressChanged(SeekBar seekBar, int progressValue,
				boolean fromUser) {
			linesLabel.setText(String.format(linesTemplate,
					linesBar.getProgress()));
			wavesLabel.setText(String.format(wavesTemplate,
					wavesBar.getProgress()));
			amplitudeLabel.setText(String.format(amplitudeTemplate,
					amplitudeBar.getProgress() / 100f));
			rotationLabel.setText(String.format(rotationTemplate,
					rotationBar.getProgress()));
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {}
	};

	private long themeId;
	private SwitchCompat coupledSwitch;
	private SwitchCompat uniformSwitch;
	private SwitchCompat shuffleSwitch;
	private TextView linesLabel;
	private String linesTemplate;
	private SeekBar linesBar;
	private TextView wavesLabel;
	private String wavesTemplate;
	private SeekBar wavesBar;
	private TextView amplitudeLabel;
	private String amplitudeTemplate;
	private SeekBar amplitudeBar;
	private TextView rotationLabel;
	private String rotationTemplate;
	private SeekBar rotationBar;
	private HorizontalScrollView colorsScroll;
	private LinearLayout colorsList;
	private TextView hueLabel;
	private String hueTemplate;
	private SeekBar hueBar;
	private TextView satLabel;
	private String satTemplate;
	private SeekBar satBar;
	private TextView valLabel;
	private String valTemplate;
	private SeekBar valBar;
	private int selectedColor;

	public static ThemeEditorFragment newInstance(long id) {
		Bundle args = new Bundle();
		args.putLong(ID, id);

		ThemeEditorFragment fragment = new ThemeEditorFragment();
		fragment.setArguments(args);

		return fragment;
	}

	@Override
	public void onCreate(Bundle state) {
		super.onCreate(state);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle state) {
		getActivity().setTitle(R.string.edit_theme);

		View view = inflater.inflate(
				R.layout.fragment_theme_editor,
				container,
				false);

		initViews(view);

		Bundle args = getArguments();
		Theme theme;
		if (args != null &&
				(themeId = args.getLong(ID)) > 0 &&
				(theme = WaveLinesApp.db.getTheme(themeId)) != null) {
			setTheme(inflater, theme);
		}

		return view;
	}

	@Override
	public void onPause() {
		super.onPause();
		if (themeId > -1) {
			WaveLinesApp.db.updateTheme(themeId, getTheme());
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.fragment_theme_editor, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.cancel:
				themeId = -1;
				getFragmentManager().popBackStack();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private Theme getTheme() {
		return new Theme(
			coupledSwitch.isChecked(),
			uniformSwitch.isChecked(),
			shuffleSwitch.isChecked(),
			linesBar.getProgress(),
			wavesBar.getProgress(),
			amplitudeBar.getProgress() / 100f,
			rotationBar.getProgress(),
			toArray(colors)
		);
	}

	private void initViews(View view) {
		coupledSwitch = view.findViewById(R.id.coupled);
		uniformSwitch = view.findViewById(R.id.uniform);
		shuffleSwitch = view.findViewById(R.id.shuffle);
		linesLabel = view.findViewById(R.id.lines_label);
		linesTemplate = getString(R.string.lines);
		linesBar = view.findViewById(R.id.lines);
		linesBar.setOnSeekBarChangeListener(updateLabelsListener);
		wavesLabel = view.findViewById(R.id.waves_label);
		wavesTemplate = getString(R.string.waves);
		wavesBar = view.findViewById(R.id.waves);
		wavesBar.setOnSeekBarChangeListener(updateLabelsListener);
		amplitudeLabel = view.findViewById(R.id.amplitude_label);
		amplitudeTemplate = getString(R.string.amplitude);
		amplitudeBar = view.findViewById(R.id.amplitude);
		amplitudeBar.setOnSeekBarChangeListener(updateLabelsListener);
		rotationLabel = view.findViewById(R.id.rotation_label);
		rotationTemplate = getString(R.string.rotation);
		rotationBar = view.findViewById(R.id.rotation);
		rotationBar.setOnSeekBarChangeListener(updateLabelsListener);
		colorsScroll = view.findViewById(R.id.colors_scroll);
		colorsList = view.findViewById(R.id.colors);
		hueLabel = view.findViewById(R.id.hue_label);
		hueTemplate = getString(R.string.hue);
		hueBar = view.findViewById(R.id.hue);
		satLabel = view.findViewById(R.id.saturation_label);
		satTemplate = getString(R.string.saturation);
		satBar = view.findViewById(R.id.saturation);
		valLabel = view.findViewById(R.id.value_label);
		valTemplate = getString(R.string.value);
		valBar = view.findViewById(R.id.value);

		setHSVBarListener(updateColorFromBarsListener);
		initColorButtons(view);
	}

	private void setHSVBarListener(SeekBar.OnSeekBarChangeListener listener) {
		hueBar.setOnSeekBarChangeListener(listener);
		satBar.setOnSeekBarChangeListener(listener);
		valBar.setOnSeekBarChangeListener(listener);
	}

	private void initColorButtons(View view) {
		view.findViewById(R.id.add_color).setOnClickListener(
				new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				addNewColor();
			}
		});
		view.findViewById(R.id.remove_color).setOnClickListener(
				new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				removeColor();
			}
		});
		view.findViewById(R.id.duplicate_color).setOnClickListener(
				new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				duplicateColor();
			}
		});
		view.findViewById(R.id.shift_left).setOnClickListener(
				new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				shiftLeft();
			}
		});
		view.findViewById(R.id.shift_right).setOnClickListener(
				new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				shiftRight();
			}
		});
	}

	private void setTheme(LayoutInflater inflater, Theme theme) {
		coupledSwitch.setChecked(theme.coupled);
		uniformSwitch.setChecked(theme.uniform);
		shuffleSwitch.setChecked(theme.shuffle);
		linesBar.setProgress(theme.lines);
		wavesBar.setProgress(theme.waves);
		amplitudeBar.setProgress((int) Math.round(theme.amplitude * 100f));
		rotationBar.setProgress(theme.rotation);
		toList(colors, theme.colors);
		colorsList.removeAllViews();
		for (int color : colors) {
			addColorView(inflater, color);
		}
		selectedColor = 0;
		updateColorControls();
	}

	private void addColorView(LayoutInflater inflater, int color) {
		View view = (ImageView) inflater.inflate(R.layout.item_color,
				colorsList, false);
		view.setBackgroundColor(color);
		view.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				selectedColor = colorsList.indexOfChild(v);
				updateColorControls();
			}
		});
		colorsList.addView(view);
	}

	private void addNewColor() {
		addNewColor(Theme.getSimilarColor(colors.get(selectedColor)));
	}

	private void addNewColor(int color) {
		Activity activity = getActivity();
		if (activity == null) {
			return;
		}
		LayoutInflater inflater = activity.getLayoutInflater();
		if (inflater == null) {
			return;
		}
		int count = colorsList.getChildCount();
		addColorView(inflater, color);
		colors.add(color);
		selectedColor = count;
		updateColorControls();
		colorsScroll.postDelayed(new Runnable() {
			@Override
			public void run() {
				colorsScroll.fullScroll(HorizontalScrollView.FOCUS_RIGHT);
			}
		}, 100L);
	}

	private void removeColor() {
		if (colorsList.getChildCount() > 1) {
			colorsList.removeViewAt(selectedColor);
			colors.remove(selectedColor);
			if (selectedColor > 0) {
				--selectedColor;
			}
			updateColorControls();
		}
	}

	private void duplicateColor() {
		addNewColor(colors.get(selectedColor));
	}

	private void shiftLeft() {
		if (selectedColor > 0) {
			swapColors(selectedColor, selectedColor - 1);
			--selectedColor;
			updateSelectionMarker();
		}
	}

	private void shiftRight() {
		if (selectedColor < colorsList.getChildCount() - 1) {
			swapColors(selectedColor, selectedColor + 1);
			++selectedColor;
			updateSelectionMarker();
		}
	}

	private void swapColors(int a, int b) {
		int color = colors.get(a);
		colors.set(a, colors.get(b));
		colors.set(b, color);
		colorsList.getChildAt(a).setBackgroundColor(colors.get(a));
		colorsList.getChildAt(b).setBackgroundColor(colors.get(b));
	}

	private void setColorFromHSVBars() {
		int color = Color.HSVToColor(new float[]{
			hueBar.getProgress(),
			satBar.getProgress() / 100f,
			valBar.getProgress() / 100f
		});
		setSelectedColor(color);
	}

	private void setSelectedColor(int color) {
		colors.set(selectedColor, color);
		colorsList.getChildAt(selectedColor).setBackgroundColor(color);
		updateColorControls();
	}

	private void updateColorControls() {
		int color = colors.get(selectedColor);
		float hsv[] = new float[3];
		Color.RGBToHSV(
			(color >> 16) & 0xff,
			(color >> 8) & 0xff,
			color & 0xff,
			hsv
		);
		setHSVBarListener(null);
		hueBar.setProgress((int) Math.round(hsv[0]));
		satBar.setProgress((int) Math.round(hsv[1] * 100f));
		valBar.setProgress((int) Math.round(hsv[2] * 100f));
		updateHSVLabels();
		setHSVBarListener(updateColorFromBarsListener);
		updateSelectionMarker();
	}

	private void updateSelectionMarker() {
		for (int i = 0, len = colorsList.getChildCount(); i < len; ++i) {
			ImageView view = (ImageView) colorsList.getChildAt(i);
			if (i == selectedColor) {
				view.setImageResource(R.drawable.ic_selected);
			} else {
				view.setImageResource(0);
			}
		}
	}

	private void updateHSVLabels() {
		hueLabel.setText(String.format(hueTemplate, hueBar.getProgress()));
		satLabel.setText(String.format(satTemplate,
				satBar.getProgress() / 100f));
		valLabel.setText(String.format(valTemplate,
				valBar.getProgress() / 100f));
	}

	private static int[] toArray(List<Integer> list) {
		int size = list.size();
		int a[] = new int[size];
		for (int i = 0; i < size; ++i) {
			a[i] = list.get(i);
		}
		return a;
	}

	private static void toList(ArrayList<Integer> list, int a[]) {
		for (int i = 0, l = a.length; i < l; ++i) {
			list.add(a[i]);
		}
	}
}
