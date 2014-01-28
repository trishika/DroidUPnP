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

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v4.widget.DrawerLayout;

import org.droidupnp.controller.upnp.IUpnpServiceController;
import org.droidupnp.model.upnp.IFactory;
import org.droidupnp.view.ContentDirectoryFragment;
import org.droidupnp.view.RendererFragment;
import org.droidupnp.view.SettingsActivity;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Observer;

public class Main extends Activity
{
	private static final String TAG = "Main";

	// Controller
	public static IUpnpServiceController upnpServiceController = null;
	public static IFactory factory = null;

	private DrawerFragment mDrawerFragment;
	private CharSequence mTitle;

	public ContentDirectoryFragment getContentDirectoryFragment()
	{
		Fragment f = getFragmentManager().findFragmentById(R.id.ContentDirectoryFragment);
		if(f != null)
			return (ContentDirectoryFragment) f;
		return null;
	}

	public RendererFragment getRenderer()
	{
		Fragment f = getFragmentManager().findFragmentById(R.id.RendererFragment);
		if(f != null)
			return (RendererFragment) f;
		return null;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

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

		mDrawerFragment = (DrawerFragment)
				getFragmentManager().findFragmentById(R.id.navigation_drawer);
		mTitle = getTitle();

		// Set up the drawer.
		mDrawerFragment.setUp(
				R.id.navigation_drawer,
				(DrawerLayout) findViewById(R.id.drawer_layout));
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
	}

	@Override
	public void onResume()
	{
		Log.i(TAG, "onResume");
		upnpServiceController.resume(this);
		super.onResume();
	}

	@Override
	public void onPause()
	{
		Log.i(TAG, "onPause");
		upnpServiceController.pause();
		upnpServiceController.getServiceListener().getServiceConnexion().onServiceDisconnected(null);
		super.onPause();
	}

	public void refresh()
	{
		upnpServiceController.getServiceListener().refresh();
		ContentDirectoryFragment cd = getContentDirectoryFragment();
		if(cd!=null)
			cd.refresh();
	}

	public void restoreActionBar() {
		ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setTitle(mTitle);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		restoreActionBar();
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.

		// Handle item selection
		switch (item.getItemId())
		{
			case R.id.menu_refresh:
				refresh();
				break;
			case R.id.menu_settings:
				startActivity(new Intent(this, SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP));
				break;
			case R.id.menu_quit:
				finish();
				break;
			default:
				return super.onOptionsItemSelected(item);
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onBackPressed()
	{
		ContentDirectoryFragment cd = getContentDirectoryFragment();
		if(cd!=null)
			if (cd.goBack())
				super.onBackPressed();
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
