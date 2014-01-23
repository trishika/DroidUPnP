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

import org.droidupnp.R;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class DeviceFragment extends Fragment {

	private static final String TAG = "DeviceFragment";

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		return inflater.inflate(R.layout.device_fragment, container, false);
	}

	@Override
	public void onDestroyView()
	{
		super.onDestroyView();

		// Destroy containing stuff
		FragmentManager fm = getActivity().getFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
		Fragment f1 = fm.findFragmentById(R.id.RendererDeviceFragment);
		if (f1 != null)
		{
			Log.i(TAG, "Remove RendererDeviceFragment");
			ft.remove(f1);
		}
		Fragment f2 = fm.findFragmentById(R.id.ContentDirectoryDeviceFragment);
		if (f2 != null)
		{
			Log.i(TAG, "Remove RendererDeviceFragment");
			ft.remove(f2);
		}

		ft.commit();
	}

}
