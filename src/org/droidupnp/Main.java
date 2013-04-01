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

package org.droidupnp;

import java.util.Observer;

import org.droidupnp.controller.upnp.IUpnpServiceController;
import org.droidupnp.model.upnp.IFactory;
import org.droidupnp.view.ContentDirectoryFragment;
import org.droidupnp.view.DeviceFragment;
import org.droidupnp.view.RendererFragment;
import org.droidupnp.view.ServiceDiscoveryFragment;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

@SuppressLint("DefaultLocale")
public class Main extends Activity {

	private static final String TAG = "Main";
	private static final boolean DISCOVERY_TAB = false;
	private static final String STATE_SELECTEDTAB = "selectedTab";
	private int tab = 0;

	// Controller
	public static IUpnpServiceController upnpServiceController;
	public static IFactory factory = null;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		Log.d(TAG, "onCreated");

		if (savedInstanceState == null)
		{
			// Cling factory init, controller
			factory = new org.droidupnp.controller.cling.Factory();
			upnpServiceController = factory.createUpnpServiceController(this);
		}

		// Attach listener
		Fragment contentDirectoryFragment = getFragmentManager().findFragmentById(R.id.ContentDirectoryFragment);
		if (contentDirectoryFragment != null && contentDirectoryFragment instanceof Observer)
			upnpServiceController.addSelectedContentDirectoryObserver((Observer) contentDirectoryFragment);
		else
			Log.w(TAG, "No contentDirectoryFragment yet !");

		Fragment rendererFragment = getFragmentManager().findFragmentById(R.id.RendererFragment);
		if (rendererFragment != null && rendererFragment instanceof Observer)
			upnpServiceController.addSelectedRendererObserver((Observer) rendererFragment);
		else
			Log.w(TAG, "No rendererFragment yet !");

		// View
		final ActionBar bar = getActionBar();

		bar.setDisplayShowHomeEnabled(false);
		bar.setDisplayShowTitleEnabled(false);

		if (savedInstanceState != null)
			tab = savedInstanceState.getInt(STATE_SELECTEDTAB);
		else
			tab = 0;

	}

	@Override
	protected void onSaveInstanceState(Bundle savedInstanceState)
	{
		Log.i(TAG, "Save instance");
		savedInstanceState.putInt(STATE_SELECTEDTAB, tab);
		super.onSaveInstanceState(savedInstanceState);
	}

	@Override
	public void onPause()
	{
		Log.i(TAG, "onPause");
		tab = getActionBar().getSelectedNavigationIndex();
		getActionBar().removeAllTabs(); // Clear tab onPause, to avoid bug due to use of nested fragment
		super.onPause();
	}

	@Override
	public void onResume()
	{
		Log.i(TAG, "onResume");

		if ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_NORMAL)
		{
			final ActionBar bar = getActionBar();

			bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
			bar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);

			bar.addTab(bar.newTab().setText("Renderer")
					.setTabListener(new TabListener<RendererFragment>(this, "Renderer", RendererFragment.class)));
			bar.addTab(bar
					.newTab()
					.setText("Content")
					.setTabListener(
							new TabListener<ContentDirectoryFragment>(this, "Content", ContentDirectoryFragment.class)));
			bar.addTab(bar.newTab().setText("Device")
					.setTabListener(new TabListener<DeviceFragment>(this, "Device", DeviceFragment.class)));

			if (DISCOVERY_TAB)
				bar.addTab(bar
						.newTab()
						.setText("Discovery")
						.setTabListener(
								new TabListener<ServiceDiscoveryFragment>(this, "Discovery",
										ServiceDiscoveryFragment.class)));

			bar.setSelectedNavigationItem(tab);
		}

		super.onResume();
	}

	@Override
	protected void onDestroy()
	{
		Log.i(TAG, "Destroy");
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle item selection
		switch (item.getItemId())
		{
		// case R.id.menu_settings:
		// return true;
			case R.id.menu_quit:
				finish();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, v, menuInfo);
		@SuppressWarnings("unused")
		MenuInflater inflater = getMenuInflater();
		// inflater.inflate(R.menu.context_menu, menu);
	}

	public static class TabListener<T extends Fragment> implements ActionBar.TabListener {

		private final Activity mActivity;
		private final String mTag;
		private final Class<T> mClass;
		private final Bundle mArgs;
		private Fragment mFragment;

		public TabListener(Activity activity, String tag, Class<T> clz)
		{
			this(activity, tag, clz, null);
		}

		public TabListener(Activity activity, String tag, Class<T> clz, Bundle args)
		{
			mActivity = activity;
			mTag = tag;
			mClass = clz;
			mArgs = args;

			// Check to see if we already have a fragment for this tab, probably
			// from a previously saved state. If so, deactivate it, because our
			// initial state is that a tab isn't shown.
			mFragment = mActivity.getFragmentManager().findFragmentByTag(mTag);
			if (mFragment != null && !mFragment.isDetached())
			{
				FragmentTransaction ft = mActivity.getFragmentManager().beginTransaction();
				ft.detach(mFragment);
				ft.commit();
			}
		}

		@Override
		public void onTabSelected(Tab tab, FragmentTransaction ft)
		{
			if (mFragment == null)
			{
				mFragment = Fragment.instantiate(mActivity, mClass.getName(), mArgs);
				ft.add(android.R.id.content, mFragment, mTag);
			}
			else
			{
				ft.attach(mFragment);
			}
		}

		@Override
		public void onTabUnselected(Tab tab, FragmentTransaction ft)
		{
			if (mFragment != null)
			{
				ft.detach(mFragment);
			}
		}

		@Override
		public void onTabReselected(Tab tab, FragmentTransaction ft)
		{
			// Toast.makeText(mActivity, "Reselected!", Toast.LENGTH_SHORT).show();
		}
	}
}
