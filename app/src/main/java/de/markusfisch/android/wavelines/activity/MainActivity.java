package de.markusfisch.android.wavelines.activity;

import de.markusfisch.android.wavelines.fragment.ThemeListFragment;
import de.markusfisch.android.wavelines.R;

import android.os.Bundle;

public class MainActivity extends AbstractActivity {
	@Override
	protected void onCreate(Bundle state) {
		super.onCreate(state);
		setContentView(R.layout.activity_main);
		showUpOnlyForSubsequentFragments();
		if (state == null) {
			addFragment(new ThemeListFragment());
		}
	}
}
