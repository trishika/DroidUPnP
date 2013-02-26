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

import org.droidupnp.model.upnp.IUpnpDevice;
import org.fourthline.cling.model.meta.Action;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.types.ServiceType;
import org.fourthline.cling.model.types.UDAServiceType;

import android.util.Log;

@SuppressWarnings("rawtypes")
public class CDevice implements IUpnpDevice {

	private static final String TAG = "ClingDevice";

	Device device;

	public CDevice(Device device)
	{
		this.device = device;
	}

	public Device getDevice()
	{
		return device;
	}

	@Override
	public String getDisplayString()
	{
		return device.getDisplayString();
	}

	@Override
	public String getFriendlyName()
	{
		return (device.getDetails() != null && device.getDetails().getFriendlyName() != null) ? device.getDetails()
				.getFriendlyName() : getDisplayString();
	}

	@Override
	public boolean equals(IUpnpDevice otherDevice)
	{
		return device.getIdentity().getUdn().equals(((CDevice) otherDevice).getDevice().getIdentity().getUdn());
	}

	@Override
	public String getUID()
	{
		return device.getIdentity().getUdn().toString();
	}

	@Override
	public String getExtendedInformation()
	{
		String info = "";
		if (device.findServiceTypes() != null)
			for (ServiceType cap : device.findServiceTypes())
			{
				info += "\n\t" + cap.getType() + " : " + cap.toFriendlyString();
			}
		;
		return info;
	}

	@Override
	public void printService()
	{
		Service[] services = device.findServices();
		for (Service service : services)
		{
			Log.i(TAG, "\t Service : " + service);
			for (Action a : service.getActions())
			{
				Log.i(TAG, "\t\t Action : " + a);
			}
		}
	}

	@Override
	public boolean asService(String service)
	{
		return (device.findService(new UDAServiceType(service)) != null);
	}

	@Override
	public String getManufacturer()
	{
		return device.getDetails().getManufacturerDetails().getManufacturer();
	}

	@Override
	public String getManufacturerURL()
	{
		if (device.getDetails().getManufacturerDetails() != null)
			return device.getDetails().getManufacturerDetails().getManufacturerURI().toString();
		else
			return "";
	}

	@Override
	public String getModelName()
	{
		return device.getDetails().getModelDetails().getModelName();
	}

	@Override
	public String getModelDesc()
	{
		return device.getDetails().getModelDetails().getModelDescription();
	}

	@Override
	public String getModelNumber()
	{
		return device.getDetails().getModelDetails().getModelNumber();
	}

	@Override
	public String getModelURL()
	{
		if (device.getDetails().getModelDetails() != null)
			return device.getDetails().getModelDetails().getModelURI().toString();
		else
			return "";
	}

	@Override
	public String getXMLURL()
	{
		if (device.getDetails().getBaseURL() != null)
			return device.getDetails().getBaseURL().toString();
		else
			return "";
	}

	@Override
	public String getPresentationURL()
	{
		if (device.getDetails().getPresentationURI() != null)
			return device.getDetails().getPresentationURI().toString();
		else
			return "";
	}

	@Override
	public String getSerialNumber()
	{
		return device.getDetails().getSerialNumber();
	}

	@Override
	public String getUDN()
	{
		if (device.getIdentity().getUdn() != null)
			return device.getIdentity().getUdn().toString();
		else
			return "";
	}
}
