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

import org.droidupnp.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.TextView;

public class AboutDialog extends DialogFragment {

	private static final String TAG = "AboutDialog";

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		LayoutInflater inflater = getActivity().getLayoutInflater();
		builder.setView(inflater.inflate(R.layout.about, null));
		builder.setTitle(R.string.menu_about);

		Dialog d = builder.create();

		d.show();

		TextView textView;
		textView = (TextView) d.findViewById(R.id.version_number);
		if (textView != null)
		{
			textView.setMovementMethod(LinkMovementMethod.getInstance());
			try
			{
				textView.setText(getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName);
			}
			catch (NameNotFoundException e)
			{
				Log.e(TAG, "Application version name not found");
			}
		}

		textView = (TextView) d.findViewById(R.id.about_app);
		if (textView != null)
		{
			textView.setMovementMethod(LinkMovementMethod.getInstance());
			textView.setText(Html.fromHtml(getString(R.string.about_app)));
		}

		textView = (TextView) d.findViewById(R.id.about_cling);
		if (textView != null)
		{
			textView.setMovementMethod(LinkMovementMethod.getInstance());
			textView.setText(Html.fromHtml(getString(R.string.about_cling)));
		}

		return d;
	}
}
