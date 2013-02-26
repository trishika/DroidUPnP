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

package org.droidupnp.model.cling.didl;

import org.droidupnp.model.upnp.didl.IDIDLItem;
import org.fourthline.cling.support.model.item.Item;

import android.util.Log;

public class ClingDIDLItem extends ClingDIDLObject implements IDIDLItem {

	private static final String TAG = "ClingDIDLItem";

	public ClingDIDLItem(Item item)
	{
		super(item);
	}

	@Override
	public String getURI()
	{
		if (item != null)
		{
			Log.e(TAG, "Item : " + item.getFirstResource().getValue());
			if (item.getFirstResource() != null && item.getFirstResource().getValue() != null)
				return item.getFirstResource().getValue();
		}
		return null;
	}
}
