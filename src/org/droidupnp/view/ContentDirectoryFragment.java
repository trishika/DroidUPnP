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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.Callable;

import org.droidupnp.Main;
import org.droidupnp.model.upnp.IContentDirectoryCommand;
import org.droidupnp.model.upnp.IRendererCommand;
import org.droidupnp.model.upnp.IUpnpDevice;
import org.droidupnp.model.upnp.didl.IDIDLContainer;
import org.droidupnp.model.upnp.didl.IDIDLItem;
import org.droidupnp.model.upnp.didl.IDIDLObject;
import org.droidupnp.model.upnp.didl.IDIDLParentContainer;

import android.app.ListFragment;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class ContentDirectoryFragment extends ListFragment implements Observer {

	private static final String TAG = "ContentDirectoryFragment";

	private ArrayAdapter<DIDLObjectDisplay> contentList;
	private LinkedList<String> tree = null;
	private String currentID = null;
	private IUpnpDevice device;

	private IContentDirectoryCommand contentDirectoryCommand;

	static final String STATE_CONTENTDIRECTORY = "contentDirectory";
	static final String STATE_TREE = "tree";
	static final String STATE_CURRENT = "current";

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		contentList = new ArrayAdapter<DIDLObjectDisplay>(this.getView().getContext(),
				android.R.layout.simple_list_item_1);
		setListAdapter(contentList);

		// Listen to content directory change
		if (Main.upnpServiceController != null)
			Main.upnpServiceController.addSelectedContentDirectoryObserver(this);
		else
			Log.w(TAG, "upnpServiceController was not ready !!!");

		if (savedInstanceState != null
				&& savedInstanceState.getStringArray(STATE_TREE) != null
				&& Main.upnpServiceController.getSelectedContentDirectory() != null
				&& 0 == Main.upnpServiceController.getSelectedContentDirectory().getUID()
						.compareTo(savedInstanceState.getString(STATE_CONTENTDIRECTORY)))
		{
			Log.i(TAG, "Restore previews state");

			// Content directory is still the same => reload context
			tree = new LinkedList<String>(Arrays.asList(savedInstanceState.getStringArray(STATE_TREE)));
			currentID = savedInstanceState.getString(STATE_CURRENT);

			device = Main.upnpServiceController.getSelectedContentDirectory();
			contentDirectoryCommand = Main.factory.createContentDirectoryCommand();
		}

		Log.i(TAG, "Force refresh");
		refresh(true);

		Log.d(TAG, "Activity created");
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState)
	{
		Log.i(TAG, "Save instance state");

		if (Main.upnpServiceController.getSelectedContentDirectory() == null)
			return;

		savedInstanceState.putString(STATE_CONTENTDIRECTORY, Main.upnpServiceController.getSelectedContentDirectory()
				.getUID());

		if (tree != null)
		{
			String[] arrayTree = new String[tree.size()];
			int i = 0;
			for (String s : tree)
				arrayTree[i++] = s;

			savedInstanceState.putStringArray(STATE_TREE, arrayTree);
			savedInstanceState.putString(STATE_CURRENT, currentID);
		}

		super.onSaveInstanceState(savedInstanceState);
	}

	@Override
	public void onResume()
	{
		Log.i(TAG, "onResume");
		super.onResume();
		contentList.clear();
		refresh();
	}

	@Override
	public void onPause()
	{
		Log.i(TAG, "onPause");
		super.onPause();
	}

	@Override
	public void onDestroy()
	{
		Log.i(TAG, "onDestroy");
		Main.upnpServiceController.delSelectedContentDirectoryObserver(this);
		super.onDestroy();
	}

	public void printCurrentContentDirectoryInfo()
	{
		Log.i(TAG, "Device : " + Main.upnpServiceController.getSelectedContentDirectory().getDisplayString());
		Main.upnpServiceController.getSelectedContentDirectory().printService();
	}

	public void refresh()
	{
		refresh(false);
	}

	public void refresh(boolean force)
	{
		Log.d(TAG, "refresh " + force);

		if (Main.upnpServiceController.getSelectedContentDirectory() == null)
		{
			if (device != null)
			{
				Log.i(TAG, "Current content directory have been removed");
				device = null;

				if (getActivity() != null) // Visible
					getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run()
						{
							// Clear display list
							contentList.clear();
						}
					});

				tree = null;
			}
			return;
		}

		Log.i(TAG, "device " + device + " device " + ((device != null) ? device.getDisplayString() : ""));
		Log.i(TAG, "contentDirectoryCommand : " + contentDirectoryCommand);

		contentDirectoryCommand = Main.factory.createContentDirectoryCommand();
		if (contentDirectoryCommand == null)
			return; // Can't do anything if upnp not ready

		if (device == null || !device.equals(Main.upnpServiceController.getSelectedContentDirectory()))
		{
			device = Main.upnpServiceController.getSelectedContentDirectory();

			Log.i(TAG, "Content directory changed !!! "
					+ Main.upnpServiceController.getSelectedContentDirectory().getDisplayString());

			tree = new LinkedList<String>();

			contentDirectoryCommand.browse(getActivity(), contentList, "0"); // browse root
		}
		else if (force || contentList.isEmpty())
		{
			if (tree.size() > 0)
			{
				String parentID = (tree.size() > 0) ? tree.getLast() : null;
				contentDirectoryCommand.browse(getActivity(), contentList, currentID, parentID);
			}
			else
				contentDirectoryCommand.browse(getActivity(), contentList, "0"); // browse root
		}
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		super.onListItemClick(l, v, position, id);

		IDIDLObject didl = contentList.getItem(position).getDIDLObject();
		String parentID = null;

		try
		{
			if (didl instanceof IDIDLContainer)
			{
				// Browsing
				if (didl instanceof IDIDLParentContainer)
				{
					currentID = tree.pop();
					parentID = (tree.size() > 0) ? tree.getLast() : null;
				}
				else
				{
					currentID = didl.getId();
					parentID = didl.getParentID();
					tree.push(parentID);
				}

				Log.d(TAG, "Browse, currentID : " + currentID + ", parentID : " + parentID);
				contentDirectoryCommand.browse(getActivity(), contentList, currentID, parentID);
			}
			else if (didl instanceof IDIDLItem)
			{
				// Launch item
				launchURI((IDIDLItem) didl);
			}
		}
		catch (Exception e)
		{
			Log.e(TAG, "Unable to finish action after item click");
			e.printStackTrace();
		}
	}

	private void launchURI(final IDIDLItem uri)
	{
		if (Main.upnpServiceController.getSelectedRenderer() == null)
		{
			// No renderer selected yet, open a popup to select one
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run()
				{
					RendererDialog rendererDialog = new RendererDialog();
					rendererDialog.setCallback(new Callable<Void>() {

						@Override
						public Void call() throws Exception
						{
							launchURIRenderer(uri);
							return null;
						}
					});

					rendererDialog.show(getActivity().getFragmentManager(), "RendererDialog");
				}
			});

		}
		else
		{
			// Renderer available, go for it
			launchURIRenderer(uri);
		}

	}

	private void launchURIRenderer(IDIDLItem uri)
	{
		IRendererCommand rendererCommand = Main.factory.createRendererCommand(Main.factory.createRendererState());
		rendererCommand.launchItem(uri);
	}

	@Override
	public void update(Observable observable, Object data)
	{
		Log.i(TAG, "ContentDirectory have changed");
		refresh();
	}
}
