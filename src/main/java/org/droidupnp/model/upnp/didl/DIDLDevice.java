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

package org.droidupnp.model.upnp.didl;

import org.droidupnp.R;
import org.droidupnp.model.upnp.IUpnpDevice;
import org.droidupnp.view.DeviceDisplay;

public class DIDLDevice implements IDIDLObject {

	IUpnpDevice device;
	protected int defaultIcon = R.drawable.ic_action_collection;

	public DIDLDevice(IUpnpDevice device)
	{
		this.device = device;
	}

	public IUpnpDevice getDevice()
	{
		return device;
	}

	@Override
	public String getDataType()
	{
		return "";
	}

	@Override
	public String getTitle() {
		return (new DeviceDisplay(device)).toString();
	}

	@Override
	public String getDescription() {
		return "";
	}

	@Override
	public String getCount() {
		return "";
	}

	@Override
	public Object getIcon() {
		return R.drawable.ic_action_collection;
	}

	@Override
	public String getParentID() {
		return "";
	}

	@Override
	public String getId() {
		return "";
	}
}
