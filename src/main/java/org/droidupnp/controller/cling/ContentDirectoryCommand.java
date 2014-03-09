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

import org.droidupnp.Main;
import org.droidupnp.model.cling.CDevice;
import org.droidupnp.model.cling.didl.ClingAudioItem;
import org.droidupnp.model.cling.didl.ClingDIDLContainer;
import org.droidupnp.model.cling.didl.ClingDIDLItem;
import org.droidupnp.model.cling.didl.ClingDIDLParentContainer;
import org.droidupnp.model.cling.didl.ClingImageItem;
import org.droidupnp.model.cling.didl.ClingVideoItem;
import org.droidupnp.model.upnp.IContentDirectoryCommand;
import org.droidupnp.view.ContentDirectoryFragment;
import org.droidupnp.view.DIDLObjectDisplay;
import org.fourthline.cling.controlpoint.ControlPoint;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.types.UDAServiceType;
import org.fourthline.cling.support.contentdirectory.callback.Browse;
import org.fourthline.cling.support.contentdirectory.callback.Search;
import org.fourthline.cling.support.model.BrowseFlag;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.DIDLObject;
import org.fourthline.cling.support.model.SortCriterion;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.item.AudioItem;
import org.fourthline.cling.support.model.item.ImageItem;
import org.fourthline.cling.support.model.item.Item;
import org.fourthline.cling.support.model.item.VideoItem;

import android.util.Log;

import java.util.ArrayList;

@SuppressWarnings("rawtypes")
public class ContentDirectoryCommand implements IContentDirectoryCommand
{
	private static final String TAG = "ContentDirectoryCommand";

	private final ControlPoint controlPoint;

	public ContentDirectoryCommand(ControlPoint controlPoint)
	{
		this.controlPoint = controlPoint;
	}

	@SuppressWarnings("unused")
	private Service getMediaReceiverRegistarService()
	{
		if (Main.upnpServiceController.getSelectedContentDirectory() == null)
			return null;

		return ((CDevice) Main.upnpServiceController.getSelectedContentDirectory()).getDevice().findService(
				new UDAServiceType("X_MS_MediaReceiverRegistar"));
	}

	private Service getContentDirectoryService()
	{
		if (Main.upnpServiceController.getSelectedContentDirectory() == null)
			return null;

		return ((CDevice) Main.upnpServiceController.getSelectedContentDirectory()).getDevice().findService(
				new UDAServiceType("ContentDirectory"));
	}

	private ArrayList<DIDLObjectDisplay> buildContentList(String parent, DIDLContent didl)
	{
		ArrayList<DIDLObjectDisplay> list = new ArrayList<DIDLObjectDisplay>();

		if (parent != null)
			list.add(new DIDLObjectDisplay(new ClingDIDLParentContainer(parent)));

		for (Container item : didl.getContainers())
		{
			list.add(new DIDLObjectDisplay(new ClingDIDLContainer(item)));
			Log.v(TAG, "Add container : " + item.getTitle());
		}

		for (Item item : didl.getItems())
		{
			ClingDIDLItem clingItem = null;
			if(item instanceof VideoItem)
				clingItem = new ClingVideoItem((VideoItem)item);
			else if(item instanceof AudioItem)
				clingItem = new ClingAudioItem((AudioItem)item);
			else if(item instanceof ImageItem)
				clingItem = new ClingImageItem((ImageItem)item);
			else
				clingItem = new ClingDIDLItem(item);

			list.add(new DIDLObjectDisplay(clingItem));
			Log.v(TAG, "Add item : " + item.getTitle());

			for (DIDLObject.Property p : item.getProperties())
				Log.v(TAG, p.getDescriptorName() + " " + p.toString());
		}

		return list;
	}

	@Override
	public void browse(String directoryID, final String parent, final ContentDirectoryFragment.ContentCallback callback)
	{
		if (getContentDirectoryService() == null)
			return;

		controlPoint.execute(new Browse(getContentDirectoryService(), directoryID, BrowseFlag.DIRECT_CHILDREN, "*", 0,
				null, new SortCriterion(true, "dc:title"))
		{
			@Override
			public void received(ActionInvocation actionInvocation, final DIDLContent didl)
			{
				callBack(didl);
			}

			@Override
			public void updateStatus(Status status)
			{
				Log.v(TAG, "updateStatus ! ");
			}

			@Override
			public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg)
			{
				Log.w(TAG, "Fail to browse ! " + defaultMsg);
				callBack(null);
			}

			public void callBack(final DIDLContent didl)
			{
				if(callback!=null)
				{
					try {
						if(didl!=null)
							callback.setContent(buildContentList(parent, didl));
						callback.call();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		});
	}

	public void search(String search, final String parent, final ContentDirectoryFragment.ContentCallback callback)
	{
		if (getContentDirectoryService() == null)
			return;

		controlPoint.execute(new Search(getContentDirectoryService(), parent, search)
		{
			@Override
			public void received(ActionInvocation actionInvocation, final DIDLContent didl)
			{
				if(callback!=null)
				{
					try {
						callback.setContent(buildContentList(parent, didl));
						callback.call();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}

			@Override
			public void updateStatus(Status status)
			{
				Log.v(TAG, "updateStatus ! ");
			}

			@Override
			public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg)
			{
				Log.w(TAG, "Fail to browse ! " + defaultMsg);
			}
		});
	}

	public boolean isSearchAvailable()
	{
		if (getContentDirectoryService() == null)
			return false;

		return false;
	}
}
