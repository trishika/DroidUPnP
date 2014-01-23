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

import org.droidupnp.model.upnp.IUpnpDevice;

import android.util.Log;
import android.view.View;
import android.widget.ListView;

public class ServiceDiscoveryFragment extends UpnpDeviceListFragment {

	protected static final String TAG = "ServiceDiscoveryFragment";

	private IUpnpDevice selectedDevice;

	public ServiceDiscoveryFragment()
	{
		super(true);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		super.onListItemClick(l, v, position, id);
		selectedDevice = list.getItem(position).getDevice();

		Log.w(TAG, "Device : " + selectedDevice.toString());
		selectedDevice.printService();
	}

	@Override
	protected boolean isSelected(IUpnpDevice device)
	{
		return false;
	}

	@Override
	protected void select(IUpnpDevice device)
	{
	}

	@Override
	protected void select(IUpnpDevice device, boolean force)
	{
	}
}