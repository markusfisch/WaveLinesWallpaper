package de.markusfisch.android.wavelines.activity;

import de.markusfisch.android.wavelines.database.Theme;
import de.markusfisch.android.wavelines.widget.ThemeView;

import android.annotation.TargetApi;
import android.os.Bundle;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;

public class PreviewActivity extends AppCompatActivity {
	public static final String THEME = "theme";

	// instead of going through all the hassles of building a proper
	// parcelable, I'm just going to take it easy and use this fine
	// static variable
	public static Theme previewTheme = null;

	@Override
	protected void onCreate(Bundle state) {
		super.onCreate(state);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		hideSystemUi(getWindow());
		ThemeView view = new ThemeView(this);
		view.setTheme(previewTheme);
		setContentView(view);
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public static void hideSystemUi(Window window) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			return;
		}
		window.getDecorView().setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
						View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
						View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
		window.setStatusBarColor(0);
		window.setNavigationBarColor(0);
	}
}
