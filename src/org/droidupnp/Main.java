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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Observer;

import org.droidupnp.controller.upnp.IUpnpServiceController;
import org.droidupnp.model.upnp.IFactory;
import org.droidupnp.view.ContentDirectoryFragment;
import org.droidupnp.view.DeviceFragment;
import org.droidupnp.view.RendererFragment;
import org.droidupnp.view.ServiceDiscoveryFragment;
import org.droidupnp.view.SettingsActivity;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
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
	public static IUpnpServiceController upnpServiceController = null;
	public static IFactory factory = null;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		Log.d(TAG, "onCreated : " + savedInstanceState + factory + upnpServiceController);

		// Use cling factory
		if (factory == null)
			factory = new org.droidupnp.controller.cling.Factory();

		// Upnp service
		if (upnpServiceController == null)
			upnpServiceController = factory.createUpnpServiceController(this);

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
		upnpServiceController.pause();
		upnpServiceController.getServiceListener().getServiceConnexion().onServiceDisconnected(null);
		super.onPause();
	}

	@Override
	public void onResume()
	{
		Log.i(TAG, "onResume");

		upnpServiceController.resume(this);

		if ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_NORMAL)
		{
			final ActionBar bar = getActionBar();

			bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
			bar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);
//			bar.setDisplayHomeAsUpEnabled(true);
			bar.addTab(bar
					.newTab()
					.setText(getString(R.string.renderer))
					.setTabListener(
							new TabListener<RendererFragment>(this, getString(R.string.renderer),
									RendererFragment.class)));

			bar.addTab(bar
					.newTab()
					.setText(getString(R.string.browser))
					.setTabListener(
							new TabListener<ContentDirectoryFragment>(this, getString(R.string.browser),
									ContentDirectoryFragment.class)));

			bar.addTab(bar
					.newTab()
					.setText(getString(R.string.device))
					.setTabListener(
							new TabListener<DeviceFragment>(this, getString(R.string.device), DeviceFragment.class)));

			if (DISCOVERY_TAB)
				bar.addTab(bar
						.newTab()
						.setText(getString(R.string.discovery))
						.setTabListener(
								new TabListener<ServiceDiscoveryFragment>(this, getString(R.string.discovery),
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
			case R.id.menu_refresh:
				refresh();
				break;
			case R.id.menu_settings:
				startActivity(new Intent(this, SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP));
				break;
			//case R.id.menu_about:
			//	AboutDialog.showDialog(this);
			//	break;
			case R.id.menu_quit:
				finish();
				break;
			default:
				return super.onOptionsItemSelected(item);
		}
		return false;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, v, menuInfo);
		@SuppressWarnings("unused")
		MenuInflater inflater = getMenuInflater();
		// inflater.inflate(R.menu.context_menu, menu);
	}

	public void refresh()
	{
		upnpServiceController.getServiceListener().refresh();
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

	public static InetAddress getLocalIpAddress(Context ctx) throws UnknownHostException
	{
		WifiManager wifiManager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		int ipAddress = wifiInfo.getIpAddress();
		return InetAddress.getByName(String.format("%d.%d.%d.%d",
				(ipAddress & 0xff), (ipAddress >> 8 & 0xff),
				(ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff)));
	}
}
