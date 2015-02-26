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

import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;

public class ContentDirectoryEnabler  implements CompoundButton.OnCheckedChangeListener
{
	private final Context mContext;
	private Switch mSwitch;

	public ContentDirectoryEnabler(Context context, Switch switch_)
	{
		mContext = context;
		setSwitch(switch_);
	}

	public void resume() {
		mSwitch.setOnCheckedChangeListener(this);
	}

	public void pause() {
		mSwitch.setOnCheckedChangeListener(null);
	}

	public void setSwitch(Switch switch_)
	{
		if(mSwitch == switch_) return;
		if(mSwitch != null)
			mSwitch.setOnCheckedChangeListener(null);
		mSwitch = switch_;
		mSwitch.setOnCheckedChangeListener(this);
		mSwitch.setChecked(isSwitchOn());
	}

	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
	{
//		if (/* Send intent message to activate content directory*/) {
//			// Intent has been taken into account, disable until new state is active
//			mSwitch.setEnabled(false);
//		} else {
//			// Error
//		}
		Log.d("ContentDirectoryEnabler", "onCheckedChanged " + ((isChecked) ? " true" : " false"));
		setConfig(isChecked);
	}

	private void setSwitchChecked(boolean checked)
	{
		if (checked != mSwitch.isChecked())
			mSwitch.setChecked(checked);
	}

	public void setConfig(boolean isChecked)
	{
		Log.d("ContentDirectoryEnabler", "setConfig " + ((isChecked) ? " true" : " false"));
		Editor editor = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
		editor.putBoolean(SettingsActivity.CONTENTDIRECTORY_SERVICE, isChecked);
		editor.apply();
	}

	public boolean isSwitchOn()
	{
		Log.d("ContentDirectoryEnabler", "isSwitchOn " + ((PreferenceManager.getDefaultSharedPreferences(mContext)
				.getBoolean(SettingsActivity.CONTENTDIRECTORY_SERVICE, true)) ? " true" : " false"));
		return PreferenceManager.getDefaultSharedPreferences(mContext)
				.getBoolean(SettingsActivity.CONTENTDIRECTORY_SERVICE, true);
	}
}
