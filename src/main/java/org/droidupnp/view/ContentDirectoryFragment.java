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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.Callable;

import org.droidupnp.Main;
import org.droidupnp.R;
import org.droidupnp.model.cling.didl.ClingAudioItem;
import org.droidupnp.model.cling.didl.ClingImageItem;
import org.droidupnp.model.cling.didl.ClingVideoItem;
import org.droidupnp.model.upnp.IDeviceDiscoveryObserver;
import org.droidupnp.model.upnp.didl.DIDLDevice;
import org.droidupnp.model.upnp.CallableContentDirectoryFilter;
import org.droidupnp.model.upnp.IContentDirectoryCommand;
import org.droidupnp.model.upnp.IRendererCommand;
import org.droidupnp.model.upnp.IUpnpDevice;
import org.droidupnp.model.upnp.didl.IDIDLContainer;
import org.droidupnp.model.upnp.didl.IDIDLItem;
import org.droidupnp.model.upnp.didl.IDIDLObject;
import org.droidupnp.model.upnp.didl.IDIDLParentContainer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListFragment;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;

public class ContentDirectoryFragment extends ListFragment implements Observer
{
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
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// Inflate the layout for this fragment
		return inflater.inflate(R.layout.browsing_list_fragment, container, false);
	}

	/** This update the search visibility depending on current content directory capabilities */
	public void updateSearchVisibility()
	{
		final Activity a = getActivity();
		if(a!=null) {
			a.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					try {
						Main.setSearchVisibility(contentDirectoryCommand!=null && contentDirectoryCommand.isSearchAvailable());
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
		}
	}

	private DeviceObserver deviceObserver;

	public class DeviceObserver implements IDeviceDiscoveryObserver
	{
		ContentDirectoryFragment cdf;

		public DeviceObserver(ContentDirectoryFragment cdf){
			this.cdf = cdf;
		}

		@Override
		public void addedDevice(IUpnpDevice device) {
			if(Main.upnpServiceController.getSelectedContentDirectory() == null)
				cdf.update();
		}

		@Override
		public void removedDevice(IUpnpDevice device) {
			if(Main.upnpServiceController.getSelectedContentDirectory() == null)
				cdf.update();
		}
	}

	public class CustomAdapter extends ArrayAdapter<DIDLObjectDisplay>
	{
		private final int layout;
		private LayoutInflater inflater;

		public CustomAdapter(Context context) {
			super(context, 0);
			this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			this.layout = R.layout.browsing_list_item;
		}

//		@ViewById
//		TextView text1;

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			if (convertView == null)
				convertView = inflater.inflate(layout, null);

			// Item
			final DIDLObjectDisplay entry = getItem(position);

			ImageView imageView = (ImageView) convertView.findViewById(R.id.icon);
			imageView.setImageResource(entry.getIcon());

			TextView text1 = (TextView) convertView.findViewById(R.id.text1);
			text1.setText(entry.getTitle());

			TextView text2 = (TextView) convertView.findViewById(R.id.text2);
			text2.setText((entry.getDescription()!=null) ? entry.getDescription() : "");

			TextView text3 = (TextView) convertView.findViewById(R.id.text3);
			text3.setText(entry.getCount());

			return convertView;
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		contentList = new CustomAdapter(this.getView().getContext());

		setListAdapter(contentList);

		deviceObserver = new DeviceObserver(this);
		Main.upnpServiceController.getContentDirectoryDiscovery().addObserver(deviceObserver);

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

        getListView().setOnItemLongClickListener( new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View v, int position, long id) {
                Log.v(TAG, "On long-click event");

                IDIDLObject didl = contentList.getItem(position).getDIDLObject();

                if (didl instanceof IDIDLItem)
                {
                    IDIDLItem ididlItem = (IDIDLItem) didl;
                    final Activity a = getActivity();
                    final Intent intent = new Intent(Intent.ACTION_VIEW);

                    Uri uri = Uri.parse(ididlItem.getURI());

                    if (didl instanceof ClingAudioItem)
                        intent.setDataAndType(uri, "audio/*");

                    if (didl instanceof ClingVideoItem)
                        intent.setDataAndType(uri, "video/*");

                    // A image from the network can't be viewed using any of android's stock apps.
                    // See:
                    // https://stackoverflow.com/questions/7734432/use-android-intent-to-display-an-image-from-internet
                    // This is only here in case it works in the future.
                    if (didl instanceof ClingImageItem)
                        intent.setDataAndType(uri, "image/*");

                    AlertDialog.Builder builder = new AlertDialog.Builder(a);
                    CharSequence[] list = new CharSequence[1];
                    list[0] = "Launch local viewer";
                    builder.setItems(list, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            try {
                                a.startActivity(intent);
                            }
                            catch (ActivityNotFoundException ex) {
                                Toast.makeText(getActivity(), "Nothing on your device can view/open this item", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.show();

                    return true;
                }
                else
                {
                    Toast.makeText(getActivity(), "No action available", Toast.LENGTH_SHORT).show();
                }

                return true;
            }
        });

		Log.d(TAG, "Force refresh");
		refresh();
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		Main.upnpServiceController.delSelectedContentDirectoryObserver(this);
		Main.upnpServiceController.getContentDirectoryDiscovery().removeObserver(deviceObserver);
	}

	private PullToRefreshLayout mPullToRefreshLayout;

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);

		// This is the View which is created by ListFragment
		ViewGroup viewGroup = (ViewGroup) view;

		view.setBackgroundColor(getResources().getColor(R.color.grey));

		// We need to create a PullToRefreshLayout manually
		mPullToRefreshLayout = new PullToRefreshLayout(viewGroup.getContext());

		// We can now setup the PullToRefreshLayout
		ActionBarPullToRefresh.from(getActivity())
			.insertLayoutInto(viewGroup)
			.theseChildrenArePullable(getListView(), getListView().getEmptyView())
			.listener(new uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener() {
				@Override
				public void onRefreshStarted(View view) {
					refresh();
				}
			})
			.setup(mPullToRefreshLayout);
	}

	@Override
	public void onDestroyView()
	{
		mPullToRefreshLayout.setRefreshComplete();
		super.onDestroyView();
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
		super.onResume();
		contentList.clear();
		refresh();
	}

	public Boolean goBack()
	{
		if(tree == null || tree.isEmpty())
		{
			if(Main.upnpServiceController.getSelectedContentDirectory() != null)
			{
				// Back on device root, unselect device
				Main.upnpServiceController.setSelectedContentDirectory(null);
				return false;
			}
			else
			{
				// Already at the upper level
				return true;
			}
		}
		else
		{
			// Go back in browsing
			currentID = tree.pop();
			update();
			return false;
		}
	}

	public void printCurrentContentDirectoryInfo()
	{
		Log.i(TAG, "Device : " + Main.upnpServiceController.getSelectedContentDirectory().getDisplayString());
		Main.upnpServiceController.getSelectedContentDirectory().printService();
	}

	public class RefreshCallback implements Callable<Void> {
		public Void call() throws java.lang.Exception {
			final Activity a = getActivity();
			if(a!=null) {
				a.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						try {
							setListShown(true);
							mPullToRefreshLayout.setRefreshComplete();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
			}
			return null;
		}
	}

	public class ContentCallback extends RefreshCallback
	{
		private ArrayAdapter<DIDLObjectDisplay> contentList;
		private ArrayList<DIDLObjectDisplay> content;

		public ContentCallback(ArrayAdapter<DIDLObjectDisplay> contentList)
		{
			this.contentList = contentList;
			this.content = new ArrayList<DIDLObjectDisplay>();
		}

		public void setContent(ArrayList<DIDLObjectDisplay> content)
		{
			this.content = content;
		}

		public Void call() throws java.lang.Exception
		{
			final Activity a = getActivity();
			if(a!=null) {
				a.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						try {
							// Empty the list
							contentList.clear();
							// Fill the list
							contentList.addAll(content);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
			}
			// Refresh
			return super.call();
		}
	}


//	@UiThread
	public synchronized void refresh()
	{
		Log.d(TAG, "refresh");

		final Activity a = getActivity();
		if(a!=null) {
			a.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					try {
						setListShown(false);
						mPullToRefreshLayout.setRefreshComplete();
						mPullToRefreshLayout.setRefreshing(true);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
		}

		// Update search visibility
		updateSearchVisibility();

		if (Main.upnpServiceController.getSelectedContentDirectory() == null)
		{
			if (device != null)
			{
				Log.i(TAG, "Current content directory have been removed");
				device = null;
				tree = null;
			}

			// Fill with the content directory list
			final Collection<IUpnpDevice> upnpDevices = Main.upnpServiceController.getServiceListener()
				.getFilteredDeviceList(new CallableContentDirectoryFilter());

			ArrayList<DIDLObjectDisplay> list = new ArrayList<DIDLObjectDisplay>();
			for (IUpnpDevice upnpDevice : upnpDevices)
				list.add(new DIDLObjectDisplay(new DIDLDevice(upnpDevice)));

			try {
				ContentCallback cc = new ContentCallback(contentList);
				cc.setContent(list);
				cc.call();
			} catch (Exception e){e.printStackTrace();}

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

			Log.i(TAG, "Browse root of a new device");
			contentDirectoryCommand.browse("0", null, new ContentCallback(contentList));
		}
		else
		{
			if (tree != null && tree.size() > 0)
			{
				String parentID = (tree.size() > 0) ? tree.getLast() : null;
				Log.i(TAG, "Browse, currentID : " + currentID + ", parentID : " + parentID);
				contentDirectoryCommand.browse(currentID, parentID, new ContentCallback(contentList));
			}
			else
			{
				Log.i(TAG, "Browse root");
				contentDirectoryCommand.browse("0", null, new ContentCallback(contentList));
			}
		}
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		super.onListItemClick(l, v, position, id);

		IDIDLObject didl = contentList.getItem(position).getDIDLObject();

		try {
			if(didl instanceof DIDLDevice)
			{
				Main.upnpServiceController.setSelectedContentDirectory( ((DIDLDevice)didl).getDevice(), false );

				// Refresh display
				refresh();
			}
			else if (didl instanceof IDIDLContainer)
			{
				// Update position
				if (didl instanceof IDIDLParentContainer)
				{
					currentID = tree.pop();
				}
				else
				{
					currentID = didl.getId();
					String parentID = didl.getParentID();
					tree.push(parentID);
				}

				// Refresh display
				refresh();
			}
			else if (didl instanceof IDIDLItem)
			{
				// Launch item
				launchURI((IDIDLItem) didl);
			}
		} catch (Exception e) {
			Log.e(TAG, "Unable to finish action after item click");
			e.printStackTrace();
		}
	}

	private void launchURI(final IDIDLItem uri)
	{
		if (Main.upnpServiceController.getSelectedRenderer() == null)
		{
			// No renderer selected yet, open a popup to select one
			final Activity a = getActivity();
			if(a!=null) {
				a.runOnUiThread(new Runnable(){
					@Override
					public void run() {
						try {
							RendererDialog rendererDialog = new RendererDialog();
							rendererDialog.setCallback(new Callable<Void>() {
								@Override
								public Void call() throws Exception {
									launchURIRenderer(uri);
									return null;
								}
							});
							rendererDialog.show(getActivity().getFragmentManager(), "RendererDialog");
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
			}
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
		update();
	}

	public void update()
	{
		final Activity a = getActivity();
		if(a!=null) {
			a.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					refresh();
				}
			});
		}
	}
}
