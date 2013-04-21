package org.droidupnp.model.upnp;


public class DeviceListener {

	// UPNP device listener
	private RendererDiscovery rendererDiscovery = null;
	private ContentDirectoryDiscovery contentDirectoryDiscovery = null;

	public DeviceListener(IServiceListener serviceListener)
	{
		rendererDiscovery = new RendererDiscovery(serviceListener);
		contentDirectoryDiscovery = new ContentDirectoryDiscovery(serviceListener);
	}

	public RendererDiscovery getRendererDiscovery()
	{
		return rendererDiscovery;
	}

	public ContentDirectoryDiscovery getContentDirectoryDiscovery()
	{
		return contentDirectoryDiscovery;
	}
}
