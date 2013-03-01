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

package org.droidupnp.controller.cling;

import org.droidupnp.Main;
import org.droidupnp.model.cling.CDevice;
import org.droidupnp.model.cling.RendererState;
import org.droidupnp.model.upnp.IRendererCommand;
import org.fourthline.cling.controlpoint.ControlPoint;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.types.UDAServiceType;
import org.fourthline.cling.support.avtransport.callback.GetMediaInfo;
import org.fourthline.cling.support.avtransport.callback.GetPositionInfo;
import org.fourthline.cling.support.avtransport.callback.GetTransportInfo;
import org.fourthline.cling.support.avtransport.callback.Pause;
import org.fourthline.cling.support.avtransport.callback.Play;
import org.fourthline.cling.support.avtransport.callback.Seek;
import org.fourthline.cling.support.avtransport.callback.SetAVTransportURI;
import org.fourthline.cling.support.avtransport.callback.Stop;
import org.fourthline.cling.support.model.MediaInfo;
import org.fourthline.cling.support.model.PositionInfo;
import org.fourthline.cling.support.model.TransportInfo;
import org.fourthline.cling.support.renderingcontrol.callback.GetMute;
import org.fourthline.cling.support.renderingcontrol.callback.GetVolume;
import org.fourthline.cling.support.renderingcontrol.callback.SetMute;
import org.fourthline.cling.support.renderingcontrol.callback.SetVolume;

import android.util.Log;

@SuppressWarnings("rawtypes")
public class RendererCommand implements Runnable, IRendererCommand {

	private static final String TAG = "RendererCommand";

	private final RendererState rendererState;
	private final ControlPoint controlPoint;

	public Thread thread;

	public RendererCommand(ControlPoint controlPoint, RendererState rendererState)
	{
		this.rendererState = rendererState;
		this.controlPoint = controlPoint;

		thread = new Thread(this);
		thread.start();
	}

	public static Service getRenderingControlService()
	{
		if (Main.upnpServiceController.getSelectedRenderer() == null)
			return null;

		return ((CDevice) Main.upnpServiceController.getSelectedRenderer()).getDevice().findService(
				new UDAServiceType("RenderingControl"));
	}

	public static Service getAVTransportService()
	{
		if (Main.upnpServiceController.getSelectedRenderer() == null)
			return null;

		return ((CDevice) Main.upnpServiceController.getSelectedRenderer()).getDevice().findService(
				new UDAServiceType("AVTransport"));
	}

	@Override
	public void commandPlay()
	{
		if (getAVTransportService() == null)
			return;

		controlPoint.execute(new Play(getAVTransportService()) {
			@Override
			public void success(ActionInvocation invocation)
			{
				Log.v(TAG, "Success playing ! ");
				// TODO update player state
			}

			@Override
			public void failure(ActionInvocation arg0, UpnpResponse arg1, String arg2)
			{
				Log.w(TAG, "Fail to play ! " + arg2);
			}
		});
	}

	@Override
	public void commandStop()
	{
		if (getAVTransportService() == null)
			return;

		controlPoint.execute(new Stop(getAVTransportService()) {
			@Override
			public void success(ActionInvocation invocation)
			{
				Log.v(TAG, "Success stopping ! ");
				// TODO update player state
			}

			@Override
			public void failure(ActionInvocation arg0, UpnpResponse arg1, String arg2)
			{
				Log.w(TAG, "Fail to stop ! " + arg2);
			}
		});
	}

	@Override
	public void commandPause()
	{
		if (getAVTransportService() == null)
			return;

		controlPoint.execute(new Pause(getAVTransportService()) {
			@Override
			public void success(ActionInvocation invocation)
			{
				Log.v(TAG, "Success pausing ! ");
				// TODO update player state
			}

			@Override
			public void failure(ActionInvocation arg0, UpnpResponse arg1, String arg2)
			{
				Log.w(TAG, "Fail to pause ! " + arg2);
			}
		});
	}

	@Override
	public void commandToggle()
	{
		RendererState.State state = rendererState.getState();
		if (state == RendererState.State.PLAY)
		{
			commandPause();
		}
		else
		{
			commandPlay();
		}
	}

	@Override
	public void commandSeek(String relativeTimeTarget)
	{
		if (getAVTransportService() == null)
			return;

		controlPoint.execute(new Seek(getAVTransportService(), relativeTimeTarget) {
			// TODO fix it, what is relativeTimeTarget ? :)

			@Override
			public void success(ActionInvocation invocation)
			{
				Log.v(TAG, "Success seeking !");
				// TODO update player state
			}

			@Override
			public void failure(ActionInvocation arg0, UpnpResponse arg1, String arg2)
			{
				Log.w(TAG, "Fail to seek ! " + arg2);
			}
		});
	}

	@Override
	public void setVolume(final int volume)
	{
		if (getRenderingControlService() == null)
			return;

		controlPoint.execute(new SetVolume(getRenderingControlService(), volume) {
			@Override
			public void success(ActionInvocation invocation)
			{
				super.success(invocation);
				Log.v(TAG, "Success to set volume");
				rendererState.setVolume(volume);
			}

			@Override
			public void failure(ActionInvocation arg0, UpnpResponse arg1, String arg2)
			{
				Log.w(TAG, "Fail to set volume ! " + arg2);
			}
		});
	}

	@Override
	public void setMute(final boolean mute)
	{
		if (getRenderingControlService() == null)
			return;

		controlPoint.execute(new SetMute(getRenderingControlService(), mute) {
			@Override
			public void success(ActionInvocation invocation)
			{
				Log.v(TAG, "Success setting mute status ! ");
				rendererState.setMute(mute);
			}

			@Override
			public void failure(ActionInvocation arg0, UpnpResponse arg1, String arg2)
			{
				Log.w(TAG, "Fail to set mute status ! " + arg2);
			}
		});
	}

	@Override
	public void toggleMute()
	{
		setMute(!rendererState.isMute());
	}

	@Override
	public void setURI(final String uri)
	{
		if (getAVTransportService() == null)
			return;

		controlPoint.execute(new SetAVTransportURI(getAVTransportService(), uri) {
			@Override
			public void success(ActionInvocation invocation)
			{
				super.success(invocation);
				Log.i(TAG, "URI successfully set !");
				commandPlay();
			}

			@Override
			public void failure(ActionInvocation arg0, UpnpResponse arg1, String arg2)
			{
				Log.w(TAG, "Fail to set mute status ! " + arg2);
			}
		});
	}

	// Update

	public void updateMediaInfo()
	{
		if (getAVTransportService() == null)
			return;

		controlPoint.execute(new GetMediaInfo(getAVTransportService()) {
			@Override
			public void received(ActionInvocation arg0, MediaInfo arg1)
			{
				Log.d(TAG, "Receive media info ! " + arg1);
				rendererState.setMediaInfo(arg1);
			}

			@Override
			public void failure(ActionInvocation arg0, UpnpResponse arg1, String arg2)
			{
				Log.w(TAG, "Fail to get media info ! " + arg2);
			}
		});
	}

	public void updatePositionInfo()
	{
		if (getAVTransportService() == null)
			return;

		controlPoint.execute(new GetPositionInfo(getAVTransportService()) {
			@Override
			public void received(ActionInvocation arg0, PositionInfo arg1)
			{
				Log.d(TAG, "Receive position info ! " + arg1);
				rendererState.setPositionInfo(arg1);
			}

			@Override
			public void failure(ActionInvocation arg0, UpnpResponse arg1, String arg2)
			{
				Log.w(TAG, "Fail to get position info ! " + arg2);
			}
		});
	}

	public void updateTransportInfo()
	{
		if (getAVTransportService() == null)
			return;

		controlPoint.execute(new GetTransportInfo(getAVTransportService()) {
			@Override
			public void failure(ActionInvocation arg0, UpnpResponse arg1, String arg2)
			{
				Log.w(TAG, "Fail to get position info ! " + arg2);
			}

			@Override
			public void received(ActionInvocation arg0, TransportInfo arg1)
			{
				Log.d(TAG, "Receive position info ! " + arg1);
				rendererState.setTransportInfo(arg1);
			}
		});
	}

	@Override
	public void updateVolume()
	{
		if (getRenderingControlService() == null)
			return;

		controlPoint.execute(new GetVolume(getRenderingControlService()) {
			@Override
			public void received(ActionInvocation arg0, int arg1)
			{
				Log.d(TAG, "Receive volume ! " + arg1);
				rendererState.setVolume(arg1);
			}

			@Override
			public void failure(ActionInvocation arg0, UpnpResponse arg1, String arg2)
			{
				Log.w(TAG, "Fail to get volume ! " + arg2);
			}
		});
	}

	public void updateMute()
	{
		if (getRenderingControlService() == null)
			return;

		controlPoint.execute(new GetMute(getRenderingControlService()) {
			@Override
			public void received(ActionInvocation arg0, boolean arg1)
			{
				Log.d(TAG, "Receive mute status ! " + arg1);
				rendererState.setMute(arg1);
			}

			@Override
			public void failure(ActionInvocation arg0, UpnpResponse arg1, String arg2)
			{
				Log.w(TAG, "Fail to get mute status ! " + arg2);
			}
		});
	}

	@Override
	public void updateFull()
	{
		updateMediaInfo();
		updatePositionInfo();
		updateVolume();
		updateMute();
		updateTransportInfo();
	}

	@Override
	public void run()
	{
		// LastChange lastChange = new LastChange(new AVTransportLastChangeParser(),
		// AVTransportVariable.CurrentTrackMetaData.class);

		// SubscriptionCallback callback = new SubscriptionCallback(getRenderingControlService(), 600) {
		//
		// @Override
		// public void established(GENASubscription sub)
		// {
		// Log.e(TAG, "Established: " + sub.getSubscriptionId());
		// }
		//
		// @Override
		// public void failed(GENASubscription sub, UpnpResponse response, Exception ex, String msg)
		// {
		// Log.e(TAG, createDefaultFailureMessage(response, ex));
		// }
		//
		// @Override
		// public void ended(GENASubscription sub, CancelReason reason, UpnpResponse response)
		// {
		// // Reason should be null, or it didn't end regularly
		// }
		//
		// @Override
		// public void eventReceived(GENASubscription sub)
		// {
		// Log.e(TAG, "Event: " + sub.getCurrentSequence().getValue());
		// Map<String, StateVariableValue> values = sub.getCurrentValues();
		// StateVariableValue status = values.get("Status");
		// if (status != null)
		// Log.e(TAG, "Status is: " + status.toString());
		// }
		//
		// @Override
		// public void eventsMissed(GENASubscription sub, int numberOfMissedEvents)
		// {
		// Log.e(TAG, "Missed events: " + numberOfMissedEvents);
		// }
		// };

		// controlPoint.execute(callback);

		int count = 0;
		while (true)
		{
			Log.d(TAG, "Update state !");

			count++;

			updatePositionInfo();

			if ((count % 3) == 0)
			{
				updateVolume();
				updateMute();
				updateTransportInfo();
			}

			if ((count % 6) == 0)
			{
				updateMediaInfo();
			}

			try
			{
				Thread.sleep(1000);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
	}

	@Override
	public void updateStatus()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void updatePosition()
	{
		// TODO Auto-generated method stub

	}
}
