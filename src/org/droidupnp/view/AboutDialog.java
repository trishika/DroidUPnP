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
