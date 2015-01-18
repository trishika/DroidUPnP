/**
 * Copyright (C) 2013 Aurélien Chabot <aurelien@chabot.fr>
 *
 * This file is part of DroidUPNP.
 *
 * DroidUPNP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DroidUPNP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DroidUPNP.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.droidupnp.view;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.Switch;
import android.widget.TextView;

import org.droidupnp.R;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends PreferenceActivity {

	public static final String CONTENTDIRECTORY_SERVICE = "pref_contentDirectoryService";
	public static final String CONTENTDIRECTORY_NAME = "pref_contentDirectoryService_name";
	public static final String CONTENTDIRECTORY_SHARE = "pref_contentDirectoryService_share";
	public static final String CONTENTDIRECTORY_VIDEO = "pref_contentDirectoryService_video";
	public static final String CONTENTDIRECTORY_AUDIO = "pref_contentDirectoryService_audio";
	public static final String CONTENTDIRECTORY_IMAGE = "pref_contentDirectoryService_image";

	private Toolbar mActionBar;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		mActionBar.setTitle(getTitle());
//		mActionBar.setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public void setContentView(int layoutResID)
	{
		ViewGroup contentView = (ViewGroup) LayoutInflater.from(this).inflate(
				R.layout.preference_activity, new LinearLayout(this), false);

		mActionBar = (Toolbar) contentView.findViewById(R.id.action_bar);
		mActionBar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});

		ViewGroup contentWrapper = (ViewGroup) contentView.findViewById(R.id.content_wrapper);
		LayoutInflater.from(this).inflate(layoutResID, contentWrapper, true);

		getWindow().setContentView(contentView);
	}

	public static String getSettingContentDirectoryName(Context ctx)
	{
		String value = PreferenceManager.getDefaultSharedPreferences(ctx)
			.getString(SettingsActivity.CONTENTDIRECTORY_NAME, "");
		return (value != "") ? value : android.os.Build.MODEL;
	}

	private static final String TAG = "SettingsActivity";

	private List<Header> mHeaders;

	@Override
	protected void onResume()
	{
		super.onResume();

		if (getListAdapter() instanceof MyPrefsHeaderAdapter)
			((MyPrefsHeaderAdapter) getListAdapter()).resume();
	}

	@Override
	protected void onPause()
	{
		super.onPause();

		if (getListAdapter() instanceof MyPrefsHeaderAdapter)
			((MyPrefsHeaderAdapter) getListAdapter()).pause();
	}

	@Override
	protected boolean isValidFragment (String fragmentName)
	{
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId()) {
			// Respond to the action bar's Up/Home button
			case android.R.id.home:
				NavUtils.navigateUpFromSameTask(this);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onBuildHeaders(List<PreferenceActivity.Header> target)
	{
		loadHeadersFromResource(R.xml.preferences, target);
		mHeaders = target;
	}

	public void setListAdapter(ListAdapter adapter)
	{
		if (mHeaders == null) {
			mHeaders = new ArrayList<>();
			for (int i = 0; i < adapter.getCount(); ++i)
				mHeaders.add((Header) adapter.getItem(i));
		}
		super.setListAdapter(new MyPrefsHeaderAdapter(this, mHeaders));
	}

	public static class ContentDirectorySettingsFragment extends PreferenceFragment
		implements SharedPreferences.OnSharedPreferenceChangeListener
	{
		private ContentDirectoryEnabler enabler;

		@Override
		public void onActivityCreated(Bundle savedInstanceState)
		{
			super.onActivityCreated(savedInstanceState);

			final Activity activity = getActivity();

			PreferenceManager.getDefaultSharedPreferences(activity).registerOnSharedPreferenceChangeListener(this);
			addPreferencesFromResource(R.xml.contentdirectory);

			Switch actionBarSwitch = new Switch(activity);

/*
			if(activity instanceof PreferenceActivity)
			{
				PreferenceActivity preferenceActivity = (PreferenceActivity) activity;
				if(preferenceActivity.onIsHidingHeaders() || !preferenceActivity.onIsMultiPane())
				{
					activity.getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
							ActionBar.DISPLAY_SHOW_CUSTOM);
					activity.getActionBar().setCustomView(actionBarSwitch, new ActionBar.LayoutParams(
						ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT,
						Gravity.CENTER_VERTICAL | Gravity.END));
				}
			}
			*/
			enabler = new ContentDirectoryEnabler(getActivity(), actionBarSwitch);
			updateSettings();

			EditTextPreference editTextPref = (EditTextPreference) findPreference(SettingsActivity.CONTENTDIRECTORY_NAME);
			if(editTextPref != null)
				editTextPref.setSummary(getSettingContentDirectoryName(activity));
		}

		public void onResume()
		{
			super.onResume();
			enabler.resume();
			updateSettings();
		}

		public void onPause()
		{
			super.onPause();
			enabler.pause();
		}

		protected void updateSettings()
		{
			boolean available = enabler.isSwitchOn();
			Log.d("ContentDirectorySettingsFragment", "updateSettings " + (available ? " true" : " false"));

			// Enable or not preference field
			for(int i = 0; i < getPreferenceScreen().getPreferenceCount(); ++i)
				getPreferenceScreen().getPreference(i).setEnabled(available);
		}

		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
		{
			Log.d("ContentDirectorySettingsFragment", "onSharedPreferenceChanged " + key);

			if (key.equals(SettingsActivity.CONTENTDIRECTORY_SERVICE))
			{
				updateSettings();
			}
			else if(key.equals(SettingsActivity.CONTENTDIRECTORY_NAME))
			{
				EditTextPreference editTextPref = (EditTextPreference) findPreference(SettingsActivity.CONTENTDIRECTORY_NAME);
				if(editTextPref != null )
					editTextPref.setSummary(getSettingContentDirectoryName(getActivity()));
			}
		}
	}

	public static class AboutSettingsFragment extends PreferenceFragment
	{
		@Override
		public void onCreate(Bundle savedInstanceState)
		{
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.about);

			// Set version
			try {
				Preference pref = findPreference("version");
				pref.setSummary(getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName);
			} catch (PackageManager.NameNotFoundException e) {
				Log.e(TAG, "exception", e);
			}

			// Dialog for external license
			Preference customPref = findPreference("license_other");
			customPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				public boolean onPreferenceClick(Preference preference) {
					LicenseDialog.showDialog(getActivity());
					return false;
				}
			});

		}
	}

	public static class MyPrefsHeaderAdapter extends ArrayAdapter<Header>
	{
		static final int HEADER_TYPE_CATEGORY = 0;
		static final int HEADER_TYPE_NORMAL = 1;
		static final int HEADER_TYPE_SWITCH = 2;

		private LayoutInflater mInflater;
		private ContentDirectoryEnabler mContentDirectoryEnabler;

		public MyPrefsHeaderAdapter(Context context, List<Header> objects)
		{
			super(context, 0, objects);
			mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			mContentDirectoryEnabler = new ContentDirectoryEnabler(context, new Switch(context));
		}

		public View getView(int position, View convertView, ViewGroup parent)
		{
			Header header = getItem(position);
			int headerType = getHeaderType(header);
			View view = null;

			switch (headerType)
			{
				case HEADER_TYPE_CATEGORY:
					view = mInflater.inflate(android.R.layout.preference_category, parent, false);
					((TextView) view.findViewById(android.R.id.title)).setText(header.getTitle(getContext().getResources()));
					break;

				case HEADER_TYPE_SWITCH:
					view = mInflater.inflate(R.layout.preference_header_switch_item, parent, false);
					((ImageView) view.findViewById(android.R.id.icon)).setImageResource(header.iconRes);
					((TextView) view.findViewById(android.R.id.title)).setText(header.getTitle(getContext().getResources()));
					((TextView) view.findViewById(android.R.id.summary)).setText(header.getSummary(getContext().getResources()));

					if(header.id == R.id.contentdirectory_settings)
						mContentDirectoryEnabler = new ContentDirectoryEnabler(getContext(),
							(Switch) view.findViewById(R.id.switchWidget));
					break;

				case HEADER_TYPE_NORMAL:
					view = mInflater.inflate(R.layout.preference_header_item, parent, false);
					((ImageView) view.findViewById(android.R.id.icon)).setImageResource(header.iconRes);
					((TextView) view.findViewById(android.R.id.title)).setText(header.getTitle(getContext().getResources()));
					((TextView) view.findViewById(android.R.id.summary)).setText(header.getSummary(getContext().getResources()));
					break;
			}

			return view;
		}

		public static int getHeaderType(Header header)
		{
			if ((header.fragment == null) && (header.intent == null)) {
				return HEADER_TYPE_CATEGORY;
			} else if (header.id == R.id.contentdirectory_settings) {
				return HEADER_TYPE_SWITCH;
			} else {
				return HEADER_TYPE_NORMAL;
			}
		}

		public void resume() {
			mContentDirectoryEnabler.resume();
		}

		public void pause() {
			mContentDirectoryEnabler.pause();
		}
	}
}