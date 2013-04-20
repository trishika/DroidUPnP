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

import org.droidupnp.Main;
import org.droidupnp.R;
import org.droidupnp.model.upnp.IRegistryListener;
import org.droidupnp.model.upnp.IUpnpDevice;

import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public abstract class UpnpDeviceListFragment extends ListFragment {

	protected static final String TAG = "UpnpDeviceListFragment";

	protected ArrayAdapter<DeviceDisplay> list;

	private final BrowsingRegistryListener browsingRegistryListener;

	protected boolean extendedInformation;

	public UpnpDeviceListFragment(boolean extendedInformation)
	{
		browsingRegistryListener = new BrowsingRegistryListener();
		this.extendedInformation = extendedInformation;
	}

	public UpnpDeviceListFragment()
	{
		this(false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		list = new ArrayAdapter<DeviceDisplay>(this.getView().getContext(),
				android.R.layout.simple_list_item_single_choice);
		setListAdapter(list);

		Log.d(TAG, "Activity created");
		Main.upnpServiceController.getServiceListener().addListener(browsingRegistryListener);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// Inflate the layout for this fragment
		return inflater.inflate(R.layout.sub_device_fragment, container, false);
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreated");
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState)
	{
		this.getListView().setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
			{
				showInfoDialog(position);
				return true;
			}
		});
	}

	private void showInfoDialog(int position)
	{
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		DialogFragment newFragment = DeviceInfoDialog.newInstance(list.getItem(position));
		newFragment.show(ft, "dialog");
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		Main.upnpServiceController.getServiceListener().removeListener(browsingRegistryListener);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		super.onListItemClick(l, v, position, id);
	}

	public class BrowsingRegistryListener implements IRegistryListener {

		@Override
		public void deviceAdded(final IUpnpDevice device)
		{
			Log.i(TAG, "New device detected : " + device.getDisplayString());

			if (device.isFullyHydrated() && filter(device) && getActivity() != null) // Visible
				getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run()
					{
						DeviceDisplay d = new DeviceDisplay(device, extendedInformation);
						int position = list.getPosition(d);
						if (position >= 0)
						{
							// Device already in the list, re-set new value at same position
							list.remove(d);
							list.insert(d, position);
						}
						else
						{
							list.add(d);
						}
						if (isSelected(d.getDevice()))
						{
							position = list.getPosition(d);
							getListView().setItemChecked(position, true);

							Log.d(TAG, "Reselect device to refresh it");
							select(d.getDevice(), true);

							Log.i(TAG, d.toString() + " is selected at position " + position);
						}
					}
				});
		}

		@Override
		public void deviceRemoved(final IUpnpDevice device)
		{
			Log.i(TAG, "Device removed : " + device.getFriendlyName());

			// Callback device remove
			removed(device);

			if (getActivity() != null) // Visible
				getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run()
					{
						// Remove device from list
						list.remove(new DeviceDisplay(device));
					}
				});
		}
	}

	/**
	 * Filter device you want to add to this device list fragment
	 * 
	 * @param device
	 *            the device to test
	 * @return add it or not
	 */
	protected abstract boolean filter(IUpnpDevice device);

	/**
	 * Filter to know if device is selected
	 * 
	 * @param d
	 * @return
	 */
	protected abstract boolean isSelected(IUpnpDevice d);

	/**
	 * Select a device
	 * 
	 * @param device
	 */
	protected abstract void select(IUpnpDevice device);

	/**
	 * Select a device
	 * 
	 * @param device
	 * @param force
	 */
	protected abstract void select(IUpnpDevice device, boolean force);

	/**
	 * Callback when device removed
	 * 
	 * @param d
	 */
	protected abstract void removed(IUpnpDevice d);

}