package de.markusfisch.android.wavelines.activity;

import de.markusfisch.android.wavelines.database.Theme;
import de.markusfisch.android.wavelines.widget.ThemeView;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;

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
		ThemeView view = new ThemeView(this);
		view.setTheme((Theme) getIntent().getExtras().getParcelable(THEME));
		view.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		setContentView(view);
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private static void hideSystemUi(Window window) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			return;
		}
		window.getDecorView().setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
						View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
						View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
						View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
						View.SYSTEM_UI_FLAG_FULLSCREEN);
		window.setStatusBarColor(0);
		window.setNavigationBarColor(0);
	}
}
