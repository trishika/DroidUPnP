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

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v4.widget.DrawerLayout;

import org.droidupnp.controller.upnp.IUpnpServiceController;
import org.droidupnp.model.upnp.IFactory;
import org.droidupnp.view.ContentDirectoryFragment;
import org.droidupnp.view.SettingsActivity;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;

import java.util.Enumeration;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class Main extends AppCompatActivity
{
	private static final String TAG = "Main";
	private static final int REQUEST_READ_EXT_STORAGE = 12345;

	// Controller
	public static IUpnpServiceController upnpServiceController = null;
	public static IFactory factory = null;

	private static Menu actionBarMenu = null;

	private DrawerFragment mDrawerFragment;
	private CharSequence mTitle;

	private static ContentDirectoryFragment mContentDirectoryFragment;

	public static void setContentDirectoryFragment(ContentDirectoryFragment f) {
		mContentDirectoryFragment = f;
	}

	public static ContentDirectoryFragment getContentDirectoryFragment() {
		return mContentDirectoryFragment;
	}

	public static void setSearchVisibility(boolean visibility)
	{
		if(actionBarMenu == null)
			return;

		MenuItem item = actionBarMenu.findItem(R.id.action_search);

		if(item != null)
			item.setVisible(visibility);
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

		if(getFragmentManager().findFragmentById(R.id.navigation_drawer) instanceof DrawerFragment)
		{
			mDrawerFragment = (DrawerFragment)
					getFragmentManager().findFragmentById(R.id.navigation_drawer);
			mTitle = getTitle();

			// Set up the drawer.
			mDrawerFragment.setUp(
				R.id.navigation_drawer,
				(DrawerLayout) findViewById(R.id.drawer_layout));
		}

		if (Build.VERSION.SDK_INT >= 23) {
			if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
				ActivityCompat.requestPermissions(this,	new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_EXT_STORAGE);
			}
		}
	}

	@Override
	public void onResume()
	{
		Log.v(TAG, "Resume activity");
		upnpServiceController.resume(this);
		super.onResume();
	}

	@Override
	public void onPause()
	{
		Log.v(TAG, "Pause activity");
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
		ActionBar actionBar = getSupportActionBar();
		if(actionBar!=null) {
			actionBar.setDisplayShowTitleEnabled(true);
			actionBar.setTitle(mTitle);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		actionBarMenu = menu;
		restoreActionBar();
		return super.onCreateOptionsMenu(menu);
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
		if (cd!=null && !cd.goBack()) {
			return;
		}
		super.onBackPressed();
	}

	public static InetAddress formatInetAddress(int ipAddress) throws UnknownHostException
	{

		return InetAddress.getByName
				(
						String.format
								(
										"%d.%d.%d.%d",
										(ipAddress & 0xff), (ipAddress >> 8 & 0xff),
										(ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff)
								)
				);
	}

	private static InetAddress getLocalIpAdressFromIntf(String intfName)
	{
		try
		{
			NetworkInterface intf = NetworkInterface.getByName(intfName);
			if(intf.isUp())
			{
				for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();)
				{
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address)
						return inetAddress;
				}
			}
		} catch (Exception e) {
			Log.w(TAG, "Unable to get ip adress for interface " + intfName);
		}
		return null;
	}

	public static InetAddress getLocalIpAddress(Context ctx) throws UnknownHostException
	{
		WifiManager wifiManager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		int ipAddress = wifiInfo.getIpAddress();
		if(ipAddress!=0)
			return InetAddress.getByName(String.format("%d.%d.%d.%d",
				(ipAddress & 0xff), (ipAddress >> 8 & 0xff),
				(ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff)));

		Log.d(TAG, "No ip adress available throught wifi manager, try to get it manually");

		InetAddress inetAddress;

		inetAddress = getLocalIpAdressFromIntf("wlan0");
		if(inetAddress!=null)
		{
			Log.d(TAG, "Got an ip for interfarce wlan0");
			return inetAddress;
		}

		inetAddress = getLocalIpAdressFromIntf("usb0");
		if(inetAddress!=null)
		{
			Log.d(TAG, "Got an ip for interfarce usb0");
			return inetAddress;
		}

		// Quick patch below should be refactored ("un-duplicate" code for getting gateway adress for a net iface by moving this code to a function)
		ConnectivityManager connectivityManager = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connectivityManager != null)
		{
			try
			{
				// getMethod and getField may throw exception (No such Method and no such field exceptions)
				Method getTetheredIfaces = connectivityManager.getClass().getMethod("getTetheredIfaces");
				String[] tetheredIfaces = (String[]) getTetheredIfaces.invoke(connectivityManager);
				//if (tetheredIfaces.size() == 1)
				//{
				for (String netIfaceName : tetheredIfaces)
				{
					NetworkInterface netIface = NetworkInterface.getByName(netIfaceName);
					Field gatewayField =
						netIface != null
						? netIface.getClass().getField("gateway")
						: null;
					if (gatewayField != null)
					{
						int gateway = gatewayField.getInt(netIface);
						if (netIface.isUp() && gateway != 0)
						{
							Log.d(TAG, "Using Gateway ip adress because device is tethering.");
							return formatInetAddress(gateway);
						}
					}
					else
						Log.d(TAG, "Could not retrieve tethered interface named " + netIfaceName);
				}
                //}
            }
            catch(Exception e)
            {
				Log.d(TAG, e.getMessage());
            }
            try
            {
                Method isWifiApEnabledMethod = wifiManager.getClass().getMethod("isWifiApEnabled");
                boolean isWifiApEnabled = (boolean) isWifiApEnabledMethod.invoke(wifiManager);
                if (isWifiApEnabled)
                {
                    NetworkInterface netIface = NetworkInterface.getByName("wlan0");
                    Field gatewayField = null;
                    try
                    {
                        gatewayField =
                                netIface != null
                                        ? netIface.getClass().getField("gateway")
                                        : null;
                    }
                    catch (Exception e)
                    {
                    }
                    if (gatewayField != null)
                    {
                        int gateway = gatewayField.getInt(netIface);
                        if (netIface.isUp() && gateway != 0)
                        {
							Log.d(TAG, "Using Gateway ip adress because device is tethering.");
                            return formatInetAddress(gateway);
                        }
                    }

					DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
					if (dhcpInfo != null && dhcpInfo.gateway != 0)
					{
						Log.d(TAG, "Using Gateway ip adress because device is tethering.");
						return formatInetAddress(dhcpInfo.gateway);
					}

					Log.d(TAG, "Tethering is activated, and ip address could not be processed, default value will be used");
                    return InetAddress.getByName("192.168.43.1");
                }
			}
			catch (Exception e)
			{
				Log.d(TAG, e.getMessage());
			}
		}

		return InetAddress.getByName("0.0.0.0");
	}
}
