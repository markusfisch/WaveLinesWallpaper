package de.markusfisch.android.wavelines.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import androidx.appcompat.app.AppCompatActivity;

import de.markusfisch.android.wavelines.R;
import de.markusfisch.android.wavelines.database.Theme;
import de.markusfisch.android.wavelines.widget.ThemePreview;

public class PreviewActivity extends AppCompatActivity {
	public static final String THEME = "theme";

	public static void show(Context context, Theme theme) {
		Intent intent = new Intent(context, PreviewActivity.class);
		intent.putExtra(THEME, theme);
		context.startActivity(intent);
	}

	@Override
	protected void onCreate(Bundle state) {
		super.onCreate(state);
		hideSystemUi(getWindow());
		setContentView(R.layout.activity_preview);
		ThemePreview view = findViewById(R.id.theme_view);
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			view.setTheme(extras.getParcelable(THEME));
		}
		view.setOnClickListener(v -> finish());
	}

	private static void hideSystemUi(Window window) {
		window.getDecorView().setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
						View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
						View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
		window.setStatusBarColor(0);
		window.setNavigationBarColor(0);
	}
}
