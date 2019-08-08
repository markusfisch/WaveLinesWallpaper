package de.markusfisch.android.wavelines.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import de.markusfisch.android.wavelines.R;
import de.markusfisch.android.wavelines.app.WaveLinesApp;
import de.markusfisch.android.wavelines.database.Theme;
import de.markusfisch.android.wavelines.widget.ThemeView;

public class EditorActivity extends AppCompatActivity {
	public static final String THEME_ID = "id";

	private static final String THEME = "theme";

	private final ArrayList<Integer> colors = new ArrayList<>();
	private long themeId;
	private ThemeView preview;
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
	private TextView oscillationLabel;
	private String oscillationTemplate;
	private SeekBar oscillationBar;
	private TextView shiftLabel;
	private String shiftLabelRandom;
	private String shiftTemplate;
	private SeekBar shiftBar;
	private TextView speedLabel;
	private String speedTemplate;
	private SeekBar speedBar;
	private TextView rotationLabel;
	private String rotationTemplate;
	private SeekBar rotationBar;
	private final SeekBar.OnSeekBarChangeListener updateLabelsListener = new SeekBar.OnSeekBarChangeListener() {
		@Override
		public void onProgressChanged(SeekBar seekBar, int progressValue,
									  boolean fromUser) {
			linesLabel.setText(String.format(linesTemplate,
					linesBar.getProgress()));
			wavesLabel.setText(String.format(wavesTemplate,
					wavesBar.getProgress()));
			amplitudeLabel.setText(String.format(amplitudeTemplate,
					amplitudeBar.getProgress() / 100f));
			oscillationLabel.setText(String.format(oscillationTemplate,
					oscillationBar.getProgress() / 10f));
			updateShiftLabel();
			speedLabel.setText(String.format(speedTemplate,
					speedBar.getProgress() / 100f));
			rotationLabel.setText(String.format(rotationTemplate,
					rotationBar.getProgress()));
			updatePreview();
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
		}
	};
	private final CompoundButton.OnCheckedChangeListener switchListener = new CompoundButton.OnCheckedChangeListener() {
		public void onCheckedChanged(CompoundButton buttonView,
									 boolean isChecked) {
			updatePreview();
			updateShiftLabel();
		}
	};
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
	private final SeekBar.OnSeekBarChangeListener updateColorFromBarsListener = new SeekBar.OnSeekBarChangeListener() {
		@Override
		public void onProgressChanged(SeekBar seekBar, int progressValue,
									  boolean fromUser) {
			setColorFromHSVBars();
			updateHSVLabels();
			updatePreview();
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
		}
	};

	private static int[] toArray(List<Integer> list) {
		int size = list.size();
		int[] a = new int[size];
		for (int i = 0; i < size; ++i) {
			a[i] = list.get(i);
		}
		return a;
	}

	private static void toList(ArrayList<Integer> list, int[] a) {
		for (int i1 : a) {
			list.add(i1);
		}
	}

	@Override
	public void onCreate(Bundle state) {
		super.onCreate(state);
		setContentView(R.layout.activity_editor);

		WaveLinesApp.initToolbar(this);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		initViews();

		Theme theme = null;
		if (state != null) {
			theme = state.getParcelable(THEME);
			themeId = state.getLong(THEME_ID);
		}

		if (theme == null || themeId < 1) {
			Intent intent = getIntent();
			if (intent != null &&
					(themeId = intent.getLongExtra(THEME_ID, -1)) > 0) {
				theme = WaveLinesApp.db.getTheme(themeId);
			}
		}

		if (theme != null) {
			setTheme(theme);
		} else {
			finish();
		}
	}

	@Override
	public void onBackPressed() {
		saveTheme();
		super.onBackPressed();
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		savedInstanceState.putParcelable(THEME, getNewTheme());
		savedInstanceState.putLong(THEME_ID, themeId);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_editor, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				saveTheme();
				break;
			case R.id.cancel:
				askDiscardChanges();
				return true;
			default:
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void askDiscardChanges() {
		new AlertDialog.Builder(this)
				.setTitle(R.string.discard_changes)
				.setMessage(R.string.sure_to_discard_changes)
				.setPositiveButton(
						android.R.string.ok,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(
									DialogInterface dialog,
									int whichButton) {
								themeId = -1;
								finish();
							}
						})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	private void saveTheme() {
		if (themeId > 0) {
			WaveLinesApp.db.updateTheme(themeId, getNewTheme());
		}
	}

	private Theme getNewTheme() {
		return new Theme(
				coupledSwitch.isChecked(),
				uniformSwitch.isChecked(),
				shuffleSwitch.isChecked(),
				linesBar.getProgress(),
				wavesBar.getProgress(),
				amplitudeBar.getProgress() / 100f,
				oscillationBar.getProgress() / 10f,
				shiftBar.getProgress() / 100f,
				speedBar.getProgress() / 100f,
				rotationBar.getProgress(),
				toArray(colors)
		);
	}

	private void initViews() {
		preview = (ThemeView) findViewById(R.id.preview);
		preview.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				PreviewActivity.show(v.getContext(), getNewTheme());
			}
		});
		coupledSwitch = (SwitchCompat) findViewById(R.id.coupled);
		coupledSwitch.setOnCheckedChangeListener(switchListener);
		uniformSwitch = (SwitchCompat) findViewById(R.id.uniform);
		uniformSwitch.setOnCheckedChangeListener(switchListener);
		shuffleSwitch = (SwitchCompat) findViewById(R.id.shuffle);
		shuffleSwitch.setOnCheckedChangeListener(switchListener);
		linesLabel = (TextView) findViewById(R.id.lines_label);
		linesTemplate = getString(R.string.lines);
		linesBar = (SeekBar) findViewById(R.id.lines);
		linesBar.setOnSeekBarChangeListener(updateLabelsListener);
		wavesLabel = (TextView) findViewById(R.id.waves_label);
		wavesTemplate = getString(R.string.waves);
		wavesBar = (SeekBar) findViewById(R.id.waves);
		wavesBar.setOnSeekBarChangeListener(updateLabelsListener);
		amplitudeLabel = (TextView) findViewById(R.id.amplitude_label);
		amplitudeTemplate = getString(R.string.amplitude);
		amplitudeBar = (SeekBar) findViewById(R.id.amplitude);
		amplitudeBar.setOnSeekBarChangeListener(updateLabelsListener);
		oscillationLabel = (TextView) findViewById(R.id.oscillation_label);
		oscillationTemplate = getString(R.string.oscillation);
		oscillationBar = (SeekBar) findViewById(R.id.oscillation);
		oscillationBar.setOnSeekBarChangeListener(updateLabelsListener);
		shiftLabel = (TextView) findViewById(R.id.shift_label);
		shiftLabelRandom = getString(R.string.shift_random);
		shiftTemplate = getString(R.string.shift);
		shiftBar = (SeekBar) findViewById(R.id.shift);
		shiftBar.setOnSeekBarChangeListener(updateLabelsListener);
		speedLabel = (TextView) findViewById(R.id.speed_label);
		speedTemplate = getString(R.string.speed);
		speedBar = (SeekBar) findViewById(R.id.speed);
		speedBar.setOnSeekBarChangeListener(updateLabelsListener);
		rotationLabel = (TextView) findViewById(R.id.rotation_label);
		rotationTemplate = getString(R.string.rotation);
		rotationBar = (SeekBar) findViewById(R.id.rotation);
		rotationBar.setOnSeekBarChangeListener(updateLabelsListener);
		colorsScroll = (HorizontalScrollView) findViewById(R.id.colors_scroll);
		colorsList = (LinearLayout) findViewById(R.id.colors);
		hueLabel = (TextView) findViewById(R.id.hue_label);
		hueTemplate = getString(R.string.hue);
		hueBar = (SeekBar) findViewById(R.id.hue);
		satLabel = (TextView) findViewById(R.id.saturation_label);
		satTemplate = getString(R.string.saturation);
		satBar = (SeekBar) findViewById(R.id.saturation);
		valLabel = (TextView) findViewById(R.id.value_label);
		valTemplate = getString(R.string.value);
		valBar = (SeekBar) findViewById(R.id.value);

		setHSVBarListener(updateColorFromBarsListener);
		initColorButtons();
	}

	private void setHSVBarListener(SeekBar.OnSeekBarChangeListener listener) {
		hueBar.setOnSeekBarChangeListener(listener);
		satBar.setOnSeekBarChangeListener(listener);
		valBar.setOnSeekBarChangeListener(listener);
	}

	private void initColorButtons() {
		findViewById(R.id.add_color).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				addNewColor();
				updatePreview();
			}
		});
		findViewById(R.id.remove_color).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				removeColor();
				updatePreview();
			}
		});
		findViewById(R.id.duplicate_color).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				duplicateColor();
				updatePreview();
			}
		});
		findViewById(R.id.shift_left).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				shiftLeft();
				updatePreview();
			}
		});
		findViewById(R.id.shift_right).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				shiftRight();
				updatePreview();
			}
		});
		findViewById(R.id.enter_color).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				askForManualColorInput();
			}
		});
	}

	private void setTheme(Theme theme) {
		coupledSwitch.setChecked(theme.coupled);
		uniformSwitch.setChecked(theme.uniform);
		shuffleSwitch.setChecked(theme.shuffle);
		linesBar.setProgress(theme.lines);
		wavesBar.setProgress(theme.waves);
		amplitudeBar.setProgress(Math.round(theme.amplitude * 100f));
		oscillationBar.setProgress(Math.round(theme.oscillation * 10f));
		shiftBar.setProgress(Math.round(theme.shift * 100f));
		speedBar.setProgress(Math.round(theme.speed * 100f));
		rotationBar.setProgress(theme.rotation);
		toList(colors, theme.colors);
		colorsList.removeAllViews();
		LayoutInflater inflater = getLayoutInflater();
		for (int color : colors) {
			addColorView(inflater, color);
		}
		selectedColor = 0;
		updateColorControls();
		updatePreview();
	}

	private void addColorView(LayoutInflater inflater, int color) {
		View view = inflater.inflate(R.layout.item_color,
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
		LayoutInflater inflater = getLayoutInflater();
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

	@SuppressLint("InflateParams")
	private void askForManualColorInput() {
		View view = getLayoutInflater().inflate(
				R.layout.dialog_enter_color,
				// a dialog does not have a parent view group
				// so InflateParams must be suppressed
				null);
		final EditText editText = view.findViewById(R.id.color);
		String hex = String.format("#%06X",
				0xffffff & colors.get(selectedColor));
		editText.setText(hex);
		new AlertDialog.Builder(this)
				.setTitle(R.string.enter_color)
				.setView(view)
				.setPositiveButton(
						android.R.string.ok,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(
									DialogInterface dialog,
									int whichButton) {
								setColorFromHexName(
										editText.getText().toString());
							}
						})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	private void setColorFromHexName(String hex) {
		try {
			setSelectedColor(Color.parseColor(hex) | 0xff000000);
		} catch (IllegalArgumentException e) {
			Toast.makeText(this, e.getLocalizedMessage(),
					Toast.LENGTH_SHORT).show();
		}
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
		float[] hsv = new float[3];
		Color.RGBToHSV(
				(color >> 16) & 0xff,
				(color >> 8) & 0xff,
				color & 0xff,
				hsv
		);
		setHSVBarListener(null);
		hueBar.setProgress(Math.round(hsv[0]));
		satBar.setProgress(Math.round(hsv[1] * 100f));
		valBar.setProgress(Math.round(hsv[2] * 100f));
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

	private void updatePreview() {
		preview.setTheme(getNewTheme());
	}

	private void updateShiftLabel() {
		float value = shiftBar.getProgress() / 100f;
		String label;
		if (value == 0 && !coupledSwitch.isChecked()) {
			label = shiftLabelRandom;
		} else {
			label = String.format(shiftTemplate, value);
		}
		shiftLabel.setText(label);
	}
}
