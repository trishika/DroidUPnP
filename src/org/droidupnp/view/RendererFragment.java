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

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class RendererFragment extends Fragment implements Observer {

	private static final String TAG = "RendererFragment";

	private IUpnpDevice device;
	private ARendererState rendererState;
	private IRendererCommand rendererCommand;

	// NowPlaying Slide
	private ImageView previousButton;
	private ImageView stopButton;
	private ImageView play_pauseButton;
	private ImageView volumeButton;
	private ImageView nextButton;

	// Settings Slide
	SeekBar progressBar;
	SeekBar volume;
	CheckBox shuffleCheckBox;
	CheckBox repeatCheckBox;

	TextView duration;
	boolean durationRemaining;

	public RendererFragment()
	{
		super();
		durationRemaining = true;
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
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		return inflater.inflate(R.layout.renderer_fragment, container, false);
	}

	public void startControlPoint()
	{
		if (Main.upnpServiceController.getSelectedRenderer() == null)
			return;

		if (device == null || !device.equals(Main.upnpServiceController.getSelectedRenderer()))
		{
			device = Main.upnpServiceController.getSelectedRenderer();

			Log.i(TAG, "Renderer changed !!! " + Main.upnpServiceController.getSelectedRenderer().getDisplayString());

			rendererState = Main.factory.createRendererState();
			rendererCommand = Main.factory.createRendererCommand(rendererState);

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
			if (getActivity() == null)
				return;

			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run()
				{
					TextView title = (TextView) getActivity().findViewById(R.id.title);
					TextView artist = (TextView) getActivity().findViewById(R.id.artist);
					SeekBar seek = (SeekBar) getActivity().findViewById(R.id.progressBar);
					SeekBar volume = (SeekBar) getActivity().findViewById(R.id.volume);
					TextView durationElapse = (TextView) getActivity().findViewById(R.id.trackDurationElapse);

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
		// previousButton = (ImageView) getActivity().findViewById(R.id.previousButton);
		play_pauseButton = (ImageView) getActivity().findViewById(R.id.play_pauseButton);
		volumeButton = (ImageView) getActivity().findViewById(R.id.volumeIcon);
		stopButton = (ImageView) getActivity().findViewById(R.id.stopButton);
		// nextButton = (ImageView) getActivity().findViewById(R.id.nextButton);
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

		if (previousButton != null)
			previousButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v)
				{
					// TODO implement
					Toast.makeText(getActivity().getApplicationContext(), "Back !!!!", Toast.LENGTH_SHORT).show();
				}
			});

		if (nextButton != null)
			nextButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v)
				{
					// TODO implement
					Toast.makeText(getActivity().getApplicationContext(), "Next !!!!", Toast.LENGTH_SHORT).show();
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

					Log.d(TAG, "Max : " + seekBar.getMax());
					Log.d(TAG, "progress : " + position);
					Log.d(TAG, "Duration : " + rendererState.getDurationSeconds());
					long t = (long) ((1.0 - ((double) seekBar.getMax() - position) / (seekBar.getMax())) * rendererState
							.getDurationSeconds());
					long h = t / 3600;
					long m = (t - h * 3600) / 60;
					long s = t - h * 3600 - m * 60;
					String seek = formatTime(h, m, s);
					Log.d(TAG, "t : " + t);
					Log.d(TAG, "h : " + h);
					Log.d(TAG, "m : " + m);
					Log.d(TAG, "s : " + s);

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
