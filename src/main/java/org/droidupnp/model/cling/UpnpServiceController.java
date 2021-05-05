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

package org.droidupnp.model.cling;

import java.util.Observer;

import org.droidupnp.Main;
import org.droidupnp.controller.upnp.IUpnpServiceController;
import org.droidupnp.model.CObservable;
import org.droidupnp.model.upnp.ContentDirectoryDiscovery;
import org.droidupnp.model.upnp.IRendererCommand;
import org.droidupnp.model.upnp.IUpnpDevice;
import org.droidupnp.model.upnp.RendererDiscovery;

import android.app.Activity;
import android.net.Uri;
import android.util.Log;

public abstract class UpnpServiceController implements IUpnpServiceController {

	private static final String TAG = "UpnpServiceController";

	protected IUpnpDevice renderer;
	protected IUpnpDevice contentDirectory;

	protected CObservable rendererObservable;
	protected CObservable contentDirectoryObservable;

	private final ContentDirectoryDiscovery contentDirectoryDiscovery;
	private final RendererDiscovery rendererDiscovery;

	private Uri playOnNextSelectedRendererUri;

	@Override
	public ContentDirectoryDiscovery getContentDirectoryDiscovery()
	{
		return contentDirectoryDiscovery;
	}

	@Override
	public RendererDiscovery getRendererDiscovery()
	{
		return rendererDiscovery;
	}

	protected UpnpServiceController()
	{
		rendererObservable = new CObservable();
		contentDirectoryObservable = new CObservable();

		contentDirectoryDiscovery = new ContentDirectoryDiscovery(getServiceListener());
		rendererDiscovery = new RendererDiscovery(getServiceListener());

		playOnNextSelectedRendererUri = null;
	}

	@Override
	public void setSelectedRenderer(IUpnpDevice renderer)
	{
		setSelectedRenderer(renderer, false);
	}

	@Override
	public void setSelectedRenderer(IUpnpDevice renderer, boolean force)
	{
		// Skip if no change and no force
		if (!force && renderer != null && this.renderer != null && this.renderer.equals(renderer))
			return;

		this.renderer = renderer;
		rendererObservable.notifyAllObservers();

		// Play URI passed via intent
		if (renderer != null && playOnNextSelectedRendererUri != null) {
			playOnNextSelectedRenderer(playOnNextSelectedRendererUri);
			playOnNextSelectedRendererUri = null;
		}
		Log.d(TAG,"playOnNextSelectedRendererUri = " + playOnNextSelectedRendererUri);
	}

	@Override
	public void setSelectedContentDirectory(IUpnpDevice contentDirectory)
	{
		setSelectedContentDirectory(contentDirectory, false);
	}

	@Override
	public void setSelectedContentDirectory(IUpnpDevice contentDirectory, boolean force)
	{
		// Skip if no change and no force
		if (!force && contentDirectory != null && this.contentDirectory != null
				&& this.contentDirectory.equals(contentDirectory))
			return;

		this.contentDirectory = contentDirectory;
		contentDirectoryObservable.notifyAllObservers();
	}

	@Override
	public IUpnpDevice getSelectedRenderer()
	{
		return renderer;
	}

	@Override
	public IUpnpDevice getSelectedContentDirectory()
	{
		return contentDirectory;
	}

	@Override
	public void addSelectedRendererObserver(Observer o)
	{
		Log.i(TAG, "New SelectedRendererObserver");
		rendererObservable.addObserver(o);
	}

	@Override
	public void delSelectedRendererObserver(Observer o)
	{
		rendererObservable.deleteObserver(o);
	}

	@Override
	public void addSelectedContentDirectoryObserver(Observer o)
	{
		contentDirectoryObservable.addObserver(o);
	}

	@Override
	public void delSelectedContentDirectoryObserver(Observer o)
	{
		contentDirectoryObservable.deleteObserver(o);
	}

	// Pause the service
	@Override
	public void pause()
	{
		rendererDiscovery.pause(getServiceListener());
		contentDirectoryDiscovery.pause(getServiceListener());
	}

	// Resume the service
	@Override
	public void resume(Activity activity)
	{
		rendererDiscovery.resume(getServiceListener());
		contentDirectoryDiscovery.resume(getServiceListener());
	}

	@Override
	public void playOnNextSelectedRenderer(Uri uri) {
		IRendererCommand rendererCommand = Main.factory.createRendererCommand(Main.factory.createRendererState());
		if (renderer != null && rendererCommand != null) {
			Log.d(TAG, "Play URI " + uri + " immediately");
			rendererCommand.launchUri(uri);
		} else {
			Log.d(TAG, "Play URI " + uri + " when renderer becomes available");
			playOnNextSelectedRendererUri = uri;
		}
	}
}