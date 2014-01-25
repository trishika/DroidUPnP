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
import org.droidupnp.model.cling.didl.ClingDIDLContainer;
import org.droidupnp.model.cling.didl.ClingDIDLItem;
import org.droidupnp.model.cling.didl.ClingDIDLParentContainer;
import org.droidupnp.model.upnp.IContentDirectoryCommand;
import org.droidupnp.view.DIDLObjectDisplay;
import org.fourthline.cling.controlpoint.ControlPoint;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.types.UDAServiceType;
import org.fourthline.cling.support.contentdirectory.callback.Browse;
import org.fourthline.cling.support.model.BrowseFlag;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.DIDLObject;
import org.fourthline.cling.support.model.SortCriterion;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.item.Item;

import android.app.Activity;
import android.util.Log;
import android.widget.ArrayAdapter;

import java.util.concurrent.Callable;

@SuppressWarnings("rawtypes")
public class ContentDirectoryCommand implements IContentDirectoryCommand {

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

	public void getSystemUpdateID()
	{
		if (getContentDirectoryService() == null)
			return;
	}

	@Override
	public void browse(final Activity activity, final ArrayAdapter<DIDLObjectDisplay> contentList, String directoryID)
	{
		browse(activity, contentList, directoryID, null, null);
	}

	@Override
	public void browse(final Activity activity, final ArrayAdapter<DIDLObjectDisplay> contentList, String directoryID,
					   final String parent)
	{
		browse(activity, contentList, directoryID, parent, null);
	}

	@Override
	public void browse(final Activity activity, final ArrayAdapter<DIDLObjectDisplay> contentList, String directoryID, final Callable<Void> callback)
	{
		browse(activity, contentList, directoryID, null, callback);
	}

	@Override
	public void browse(final Activity activity, final ArrayAdapter<DIDLObjectDisplay> contentList, String directoryID,
			final String parent, final Callable<Void> callback)
	{
		if (getContentDirectoryService() == null)
			return;

		controlPoint.execute(new Browse(getContentDirectoryService(), directoryID, BrowseFlag.DIRECT_CHILDREN, "*", 0,
				null, new SortCriterion(true, "dc:title")) {
			@Override
			public void received(ActionInvocation actionInvocation, final DIDLContent didl)
			{
				activity.runOnUiThread(new Runnable() {
					@Override
					public void run()
					{
						contentList.clear();

						if (parent != null)
							contentList.add(new DIDLObjectDisplay(new ClingDIDLParentContainer(parent)));

						for (Container item : didl.getContainers())
						{
							contentList.add(new DIDLObjectDisplay(new ClingDIDLContainer(item)));
							Log.v(TAG, "Add container : " + item.getTitle());
						}

						for (Item item : didl.getItems())
						{
							contentList.add(new DIDLObjectDisplay(new ClingDIDLItem(item)));
							Log.v(TAG, "Add item : " + item.getTitle());

							for (DIDLObject.Property p : item.getProperties())
								Log.v(TAG, p.getDescriptorName() + " " + p.toString());
						}

						if(callback!=null)
							try {
								callback.call();
							} catch (Exception e) {
								e.printStackTrace();
							}
					}
				});
			}

			@Override
			public void updateStatus(Status status)
			{
				Log.i(TAG, "updateStatus ! ");
			}

			@Override
			public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg)
			{
				Log.w(TAG, "Fail to browse ! " + defaultMsg);
			}
		});
	}

	public void search()
	{
		if (getContentDirectoryService() == null)
			return;
	}

	public void getSearchCapabilities()
	{
		if (getContentDirectoryService() == null)
			return;
	}

	public void getSortCapabilities()
	{
		if (getContentDirectoryService() == null)
			return;
	}
}
