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
	// instead of going through all the hassles of building a proper
	// parcelable, I'm just going to take it easy and use this fine
	// static variable
	private static Theme previewTheme = null;

	public static void show(Context context, Theme theme) {
		previewTheme = theme;
		context.startActivity(new Intent(context, PreviewActivity.class));
	}

	@Override
	protected void onCreate(Bundle state) {
		super.onCreate(state);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		hideSystemUi(getWindow());
		ThemeView view = new ThemeView(this);
		view.setTheme(previewTheme);
		view.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		setContentView(view);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		clear();
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private static void hideSystemUi(Window window) {
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

	private static void clear() {
		previewTheme = null;
	}
}
