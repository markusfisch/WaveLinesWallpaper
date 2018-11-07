package de.markusfisch.android.wavelines.activity;

import de.markusfisch.android.wavelines.R;

import android.annotation.SuppressLint;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

abstract public class AbstractActivity extends AppCompatActivity {
	// because lint doesn't like the unchained commit()
	@SuppressLint("CommitTransaction")
	public static void addFragment(FragmentManager fm, Fragment fragment) {
		FragmentTransaction fa = fm.beginTransaction()
				.replace(R.id.content_frame, fragment);
		if (fm.findFragmentById(R.id.content_frame) != null) {
			fa.addToBackStack(null);
		}
		fa.commit();
	}

	@Override
	public boolean onSupportNavigateUp() {
		FragmentManager fm = getSupportFragmentManager();
		if (fm != null && fm.getBackStackEntryCount() > 0) {
			fm.popBackStack();
		} else {
			finish();
		}
		return true;
	}

	protected void addFragment(Fragment fragment) {
		addFragment(getSupportFragmentManager(), fragment);
	}

	protected void showUpOnlyForSubsequentFragments() {
		getSupportFragmentManager().addOnBackStackChangedListener(
				new FragmentManager.OnBackStackChangedListener() {
			@Override
			public void onBackStackChanged() {
				canBack();
			}
		});
		canBack();
	}

	protected void canBack() {
		setUpEnabled(getSupportFragmentManager().getBackStackEntryCount() > 0);
	}

	protected void setUpEnabled(boolean enabled) {
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(enabled);
		}
	}
}
