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

import java.util.Observable;
import java.util.Observer;

import org.droidupnp.Main;
import org.droidupnp.R;
import org.droidupnp.model.cling.RendererState;
import org.droidupnp.model.upnp.ARendererState;
import org.droidupnp.model.upnp.IRendererCommand;
import org.droidupnp.model.upnp.IUpnpDevice;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class RendererFragment extends Fragment implements Observer
{
	private static final String TAG = "RendererFragment";

	private IUpnpDevice device;
	private ARendererState rendererState;
	private IRendererCommand rendererCommand;

	// NowPlaying Slide
	private ImageView stopButton;
	private ImageView play_pauseButton;
	private ImageView volumeButton;

	// Settings Slide
	SeekBar progressBar;
	SeekBar volume;

	TextView duration;
	boolean durationRemaining;

	public RendererFragment()
	{
		super();
		durationRemaining = true;
	}

	public RendererFragment getRenderer()
	{
		Fragment f = getFragmentManager().findFragmentById(R.id.RendererFragment);
		if(f != null)
			return (RendererFragment) f;
		return null;
	}

	public void hideRenderer()
	{
		getActivity().findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
		getActivity().findViewById(R.id.separator).setVisibility(View.INVISIBLE);
		getFragmentManager().beginTransaction().hide(getRenderer()).commit();
	}

	public void showRenderer()
	{
		getActivity().findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
		getActivity().findViewById(R.id.separator).setVisibility(View.VISIBLE);
		getFragmentManager().beginTransaction().show(getRenderer()).commit();
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		// Listen to renderer change
		if (Main.upnpServiceController != null)
			Main.upnpServiceController.addSelectedRendererObserver(this);
		else
			Log.w(TAG, "upnpServiceController was not ready !!!");

		// Initially hide renderer
		hideRenderer();

		Log.d(TAG, "Activity created");
	}

	@Override
	public void onStart()
	{
		super.onStart();

		// Call Main Initialise Function
		this.init();
	}

	@Override
	public void onResume()
	{
		super.onResume();
		startControlPoint();

		if (rendererCommand != null)
			rendererCommand.resume();
	}

	@Override
	public void onPause()
	{
		Log.d(TAG, "Pause Renderer");
		device = null;
		if (rendererCommand != null)
			rendererCommand.pause();
		super.onPause();
	}

	@Override
	public void onDestroy()
	{
		Log.i(TAG, "Destroy");
		Main.upnpServiceController.delSelectedRendererObserver(this);
		super.onDestroy();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		return inflater.inflate(R.layout.renderer_fragment, container, false);
	}

	public void startControlPoint()
	{
		if (Main.upnpServiceController.getSelectedRenderer() == null)
		{
			if (device != null)
			{
				Log.i(TAG, "Current renderer have been removed");
				device = null;

				final Activity a = getActivity();
				if (a == null)
					return;

				a.runOnUiThread(new Runnable() {
					@Override
					public void run()
					{
						hideRenderer();
					}
				});
			}
			return;
		}

		if (device == null || rendererState == null || rendererCommand == null
				|| !device.equals(Main.upnpServiceController.getSelectedRenderer()))
		{
			device = Main.upnpServiceController.getSelectedRenderer();

			Log.i(TAG, "Renderer changed !!! " + Main.upnpServiceController.getSelectedRenderer().getDisplayString());

			rendererState = Main.factory.createRendererState();
			rendererCommand = Main.factory.createRendererCommand(rendererState);

			if (rendererState == null || rendererCommand == null)
			{
				Log.e(TAG, "Fail to create renderer command and/or state");
				return;
			}

			rendererCommand.resume();

			rendererState.addObserver(this);
			rendererCommand.updateFull();
		}
		updateRenderer();
	}

	public void updateRenderer()
	{
		Log.v(TAG, "updateRenderer");

		if (rendererState != null)
		{
			final Activity a = getActivity();
			if (a == null)
				return;

			a.runOnUiThread(new Runnable() {
				@Override
				public void run()
				{
					showRenderer();

					TextView title = (TextView) a.findViewById(R.id.title);
					TextView artist = (TextView) a.findViewById(R.id.artist);
					SeekBar seek = (SeekBar) a.findViewById(R.id.progressBar);
					SeekBar volume = (SeekBar) a.findViewById(R.id.volume);
					TextView durationElapse = (TextView) a.findViewById(R.id.trackDurationElapse);

					if (title == null || artist == null || seek == null || duration == null || durationElapse == null)
						return;

					if (durationRemaining)
						duration.setText(rendererState.getRemainingDuration());
					else
						duration.setText(rendererState.getDuration());

					durationElapse.setText(rendererState.getPosition());

					seek.setProgress(rendererState.getElapsedPercent());

					title.setText(rendererState.getTitle());
					artist.setText(rendererState.getArtist());

					if (rendererState.getState() == RendererState.State.PLAY)
						play_pauseButton.setImageResource(R.drawable.pause);
					else
						play_pauseButton.setImageResource(R.drawable.play);

					if (rendererState.isMute())
						volumeButton.setImageResource(R.drawable.volume_mute);
					else
						volumeButton.setImageResource(R.drawable.volume);

					volume.setProgress(rendererState.getVolume());
				}
			});

			Log.v(TAG, rendererState.toString());
		}
	}

	@Override
	public void update(Observable observable, Object data)
	{
		Log.d(TAG, "Renderer have changed");
		startControlPoint();
	}

	private void init()
	{
		SetupButtons();
		SetupButtonListeners();
	}

	private void SetupButtons()
	{
		// Now_Playing Footer Buttons
		play_pauseButton = (ImageView) getActivity().findViewById(R.id.play_pauseButton);
		volumeButton = (ImageView) getActivity().findViewById(R.id.volumeIcon);
		stopButton = (ImageView) getActivity().findViewById(R.id.stopButton);
		progressBar = (SeekBar) getActivity().findViewById(R.id.progressBar);
		volume = (SeekBar) getActivity().findViewById(R.id.volume);
	}

	private void SetupButtonListeners()
	{
		if (play_pauseButton != null)
			play_pauseButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v)
				{
					if (rendererCommand != null)
						rendererCommand.commandToggle();
				}
			});

		if (stopButton != null)
			stopButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v)
				{
					if (rendererCommand != null)
						rendererCommand.commandStop();
				}
			});

		if (volumeButton != null)
			volumeButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v)
				{
					if (rendererCommand != null)
						rendererCommand.toggleMute();
				}
			});

		if (progressBar != null)
			progressBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

				@Override
				public void onStopTrackingTouch(SeekBar seekBar)
				{
					if (rendererState == null)
						return;

					int position = seekBar.getProgress();

					long t = (long) ((1.0 - ((double) seekBar.getMax() - position) / (seekBar.getMax())) * rendererState
							.getDurationSeconds());
					long h = t / 3600;
					long m = (t - h * 3600) / 60;
					long s = t - h * 3600 - m * 60;
					String seek = formatTime(h, m, s);

					Toast.makeText(getActivity().getApplicationContext(), "Seek to " + seek, Toast.LENGTH_SHORT).show();
					Log.d(TAG, "Seek to " + seek);
					if (rendererCommand != null)
						rendererCommand.commandSeek(seek);
				}

				@Override
				public void onStartTrackingTouch(SeekBar seekBar)
				{
				}

				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
				{
				}
			});

		if (volume != null)
			volume.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

				@Override
				public void onStopTrackingTouch(SeekBar seekBar)
				{
					Toast.makeText(getActivity().getApplicationContext(), "Set volume to " + seekBar.getProgress(),
							Toast.LENGTH_SHORT).show();

					if (rendererCommand != null)
						rendererCommand.setVolume(seekBar.getProgress());
				}

				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
				{
				}

				@Override
				public void onStartTrackingTouch(SeekBar seekBar)
				{
				}
			});

		duration = (TextView) getActivity().findViewById(R.id.trackDurationRemaining);
		if (duration != null)
			duration.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v)
				{
					if (rendererState == null)
						return;

					durationRemaining = !durationRemaining;
					if (durationRemaining)
						duration.setText(rendererState.getRemainingDuration());
					else
						duration.setText(rendererState.getDuration());
				}
			});
	}

	private String formatTime(long h, long m, long s)
	{
		return ((h >= 10) ? "" + h : "0" + h) + ":" + ((m >= 10) ? "" + m : "0" + m) + ":"
				+ ((s >= 10) ? "" + s : "0" + s);
	}
}
