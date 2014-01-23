package org.droidupnp.view;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;

import org.droidupnp.Main;
import org.droidupnp.R;
import org.droidupnp.model.upnp.CallableRendererFilter;
import org.droidupnp.model.upnp.IUpnpDevice;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.ArrayAdapter;

public class RendererDialog extends DialogFragment {

	private Callable<Void> callback = null;

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		final Collection<IUpnpDevice> upnpDevices = Main.upnpServiceController.getServiceListener()
				.getFilteredDeviceList(new CallableRendererFilter());

		ArrayList<DeviceDisplay> list = new ArrayList<DeviceDisplay>();
		for (IUpnpDevice upnpDevice : upnpDevices)
			list.add(new DeviceDisplay(upnpDevice));

		ArrayAdapter<DeviceDisplay> rendererList = new ArrayAdapter<DeviceDisplay>(getActivity(),
				android.R.layout.simple_list_item_1, list);
		builder.setTitle(R.string.selectRenderer).setAdapter(rendererList, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				Main.upnpServiceController.setSelectedRenderer((IUpnpDevice) upnpDevices.toArray()[which]);
				try
				{
					if (callback != null)
						callback.call();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		});
		return builder.create();
	}

	public void setCallback(Callable<Void> callback)
	{
		this.callback = callback;
	}
}
