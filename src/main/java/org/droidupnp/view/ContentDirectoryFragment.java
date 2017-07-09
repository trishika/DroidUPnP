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

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.Callable;

import org.droidupnp.Main;
import org.droidupnp.R;
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
import android.app.ListFragment;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ContentDirectoryFragment extends ListFragment implements Observer
{
	private static final String TAG = ContentDirectoryFragment.class.getSimpleName();

	private ArrayAdapter<DIDLObjectDisplay> contentList;
	private LinkedList<String> tree = null;
	private String currentID = null;
	private IUpnpDevice device;

	private LruCache<String, Bitmap> mMemoryCache;

	private IContentDirectoryCommand contentDirectoryCommand;

	private SwipeRefreshLayout swipeContainer;

	static final String STATE_CONTENTDIRECTORY = "contentDirectory";
	static final String STATE_TREE = "tree";
	static final String STATE_CURRENT = "current";

	static final int IMAGE_FADE_ANIMATION_DURATION = 400;
	static final float MAX_CACHE_SIZE = 1.0f/8.0f;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
	   Main.setContentDirectoryFragment(this);
	   super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		initCache();
		// Inflate the layout for this fragment
		return inflater.inflate(R.layout.browsing_list_fragment, container, false);
	}

	private void initCache() {
		final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

		// Use 1/8th of the available memory for this memory cache.
		final int cacheSize = (int) (maxMemory * MAX_CACHE_SIZE);

		mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
			@Override
			protected int sizeOf(String key, Bitmap bitmap) {
				// The cache size will be measured in kilobytes rather than
				// number of items.
				return bitmap.getByteCount() / 1024;
			}
		};
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

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			if (convertView == null)
				convertView = inflater.inflate(layout, null);

			// Item
			final DIDLObjectDisplay entry = getItem(position);

			ImageView imageView = (ImageView) convertView.findViewById(R.id.icon);
			if(entry.getIcon() instanceof  Integer)
				imageView.setImageResource((Integer) entry.getIcon());
			else if(entry.getIcon() instanceof URI) {
				imageView.setTag(entry.getIcon().toString());
				new DownloadImageTask(imageView, entry.getIcon().toString()).execute();
			}
			else
				imageView.setImageResource(android.R.color.transparent);

			TextView text1 = (TextView) convertView.findViewById(R.id.text1);
			text1.setText(entry.getTitle());

			TextView text2 = (TextView) convertView.findViewById(R.id.text2);
			text2.setText((entry.getDescription()!=null) ? entry.getDescription() : "");

			TextView text3 = (TextView) convertView.findViewById(R.id.text3);
			text3.setText(entry.getCount());

			return convertView;
		}
	}

	public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
		if (getBitmapFromMemCache(key) == null) {
			mMemoryCache.put(key, bitmap);
		}
	}

	public Bitmap getBitmapFromMemCache(String key) {
		return mMemoryCache.get(key);
	}

	private class DownloadImageTask extends AsyncTask<Void, Void, Bitmap> {
		ImageView imageView;
		String url;

		public DownloadImageTask(ImageView imageView, String url) {
			this.imageView = imageView;
			this.url = url;
			imageView.setImageResource(android.R.color.transparent);
		}

		@Override
		protected Bitmap doInBackground(Void... voids) {
			try {
				Bitmap b = getBitmapFromMemCache(url);

				if (b != null) {
					return b;
				}

				b = BitmapFactory.decodeStream(new java.net.URL(url).openStream());
				addBitmapToMemoryCache(url, b);
				return b;
			} catch (IOException e) {
				Log.e(TAG, "IO Error during image fetch: "+e.getMessage());
				return null;
			}
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			if (result != null && imageView.getTag().equals(url)) {
				imageView.setImageBitmap(result);

				Animation a = new AlphaAnimation(0.00f, 1.00f);
				a.setDuration(IMAGE_FADE_ANIMATION_DURATION);
				a.setAnimationListener(new Animation.AnimationListener() {

					public void onAnimationStart(Animation animation) {
					}

					public void onAnimationRepeat(Animation animation) {
					}

					public void onAnimationEnd(Animation animation) {
						imageView.setVisibility(View.VISIBLE);
					}
				});

				imageView.startAnimation(a);
			}
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
			tree = new LinkedList<>(Arrays.asList(savedInstanceState.getStringArray(STATE_TREE)));
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
					intent.setDataAndType(uri, didl.getDataType());

					try {
						a.startActivity(intent);
					} catch (ActivityNotFoundException ex) {
						Toast.makeText(getActivity(), R.string.failed_action, Toast.LENGTH_SHORT).show();
					}
				}
				else
				{
					Toast.makeText(getActivity(), R.string.no_action_available, Toast.LENGTH_SHORT).show();
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


	@Override
	public void onViewCreated(View view, Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);

		view.setBackgroundColor(getResources().getColor(R.color.grey));

		swipeContainer = (SwipeRefreshLayout) view.findViewById(R.id.swipeContainer);

		// Setup refresh listener which triggers new data loading
		swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				refresh();
			}
		});
	}

	@Override
	public void onDestroyView()
	{
		swipeContainer.setRefreshing(false);
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
			Log.d(TAG, "Go back in browsing");
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
			Log.d(TAG, "Stop refresh");
			final Activity a = getActivity();
			if(a!=null) {
				a.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						try {
							swipeContainer.setRefreshing(false);
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
			this.content = new ArrayList<>();
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

	public void setEmptyText(CharSequence text) {
		((TextView)getListView().getEmptyView()).setText(text);
	}

	public synchronized void refresh()
	{
		Log.d(TAG, "refresh");

		setEmptyText(getString(R.string.loading));

		swipeContainer.setRefreshing(true);

		// Update search visibility
		updateSearchVisibility();

		if (Main.upnpServiceController.getSelectedContentDirectory() == null)
		{
			// List here the content directory devices
			setEmptyText(getString(R.string.device_list_empty));

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
