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

package org.droidupnp.controller.cling;

import org.droidupnp.model.cling.UpnpService;
import org.droidupnp.model.cling.UpnpServiceController;
import org.droidupnp.model.upnp.IDeviceDiscoveryObserver;
import org.droidupnp.model.upnp.IUpnpDevice;
import org.fourthline.cling.model.meta.LocalDevice;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class ServiceController extends UpnpServiceController implements IDeviceDiscoveryObserver
{
	private static final String TAG = "Cling.ServiceController";
	protected static final String LAST_RENDERER_DEVICE = "last_renderer_device_uid";
	protected static final String LAST_CONTENT_DEVICE = "last_content_device_uid";

	private final ServiceListener upnpServiceListener;
	private Activity activity = null;
	protected SharedPreferences sharedPref;

	public ServiceController(Context ctx)
	{
		super();
		upnpServiceListener = new ServiceListener(ctx);
		sharedPref = PreferenceManager.getDefaultSharedPreferences(ctx);
		getRendererDiscovery().addObserver(this);
		getContentDirectoryDiscovery().addObserver(this);
	}

	@Override
	protected void finalize()
	{
		pause();
	}

	@Override
	public ServiceListener getServiceListener()
	{
		return upnpServiceListener;
	}

	@Override
	public void pause()
	{
		super.pause();
		activity.unbindService(upnpServiceListener.getServiceConnexion());
		activity = null;
	}

	@Override
	public void resume(Activity activity)
	{
		super.resume(activity);
		this.activity = activity;

		// This will start the UPnP service if it wasn't already started
		Log.d(TAG, "Start upnp service");
		activity.bindService(new Intent(activity, UpnpService.class), upnpServiceListener.getServiceConnexion(),
				Context.BIND_AUTO_CREATE);
	}

	@Override
	public void addDevice(LocalDevice localDevice) {
		upnpServiceListener.getUpnpService().getRegistry().addDevice(localDevice);
	}

	@Override
	public void removeDevice(LocalDevice localDevice) {
		upnpServiceListener.getUpnpService().getRegistry().removeDevice(localDevice);
	}

	@Override
	public void setSelectedRenderer(IUpnpDevice renderer, boolean force) {
		super.setSelectedRenderer(renderer, force);
		if (renderer != null) {
			sharedPref.edit().putString(LAST_RENDERER_DEVICE, renderer.getUID()).apply();
		}
	}

	@Override
	public void setSelectedContentDirectory(IUpnpDevice contentDirectory, boolean force) {
		super.setSelectedContentDirectory(contentDirectory, force);
		if (contentDirectory != null) {
			sharedPref.edit().putString(LAST_CONTENT_DEVICE, contentDirectory.getUID()).apply();
		}
	}

	@Override
	public void addedDevice(IUpnpDevice device) {
		if (device.getUID().equals(sharedPref.getString(LAST_RENDERER_DEVICE, ""))) {
			setSelectedRenderer(device, false);
		} else if (device.getUID().equals(sharedPref.getString(LAST_CONTENT_DEVICE, ""))) {
			setSelectedContentDirectory(device, false);
		}
	}

	@Override
	public void removedDevice(IUpnpDevice device) {}
}
