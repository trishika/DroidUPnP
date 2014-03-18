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

import org.droidupnp.model.upnp.didl.IDIDLContainer;
import org.droidupnp.model.upnp.didl.IDIDLObject;

public class DIDLObjectDisplay {

	protected static final String TAG = "DIDLContentDisplay";

	private final IDIDLObject didl;

	public DIDLObjectDisplay(IDIDLObject didl)
	{
		this.didl = didl;
	}

	public IDIDLObject getDIDLObject()
	{
		return didl;
	}

	public String getTitle()
	{
		return didl.getTitle();
	}

	public String getDescription()
	{
		return didl.getDescription();
	}

	public String getCount()
	{
		return didl.getCount();
	}

	public Object getIcon()
	{
		return didl.getIcon();
	}

	@Override
	public String toString()
	{
		if (didl instanceof IDIDLContainer)
			return didl.getTitle() + " (" + ((IDIDLContainer) didl).getChildCount() + ")";

		return didl.getTitle();
	}
}
