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

package org.droidupnp.model.cling;

import java.io.StringReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.droidupnp.model.upnp.ARendererState;
import org.fourthline.cling.support.model.MediaInfo;
import org.fourthline.cling.support.model.PositionInfo;
import org.fourthline.cling.support.model.TransportInfo;
import org.fourthline.cling.support.model.TransportState;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;

public class RendererState extends ARendererState {

	protected static final String TAG = "RendererState";

	public RendererState()
	{
		super();

		state = State.STOP;
		volume = -1;
		resetTrackInfo();

		notifyAllObservers();
	}

	// / Player info
	private State state;
	private int volume;
	private boolean mute;
	private int repeatMode; // TODO enum with different mode
	private int randomMode; // TODO enum with different mode

	// / Track info
	private PositionInfo positionInfo;
	private MediaInfo mediaInfo;
	private TransportInfo transportInfo;

	// / Getter/Setter

	@Override
	public State getState()
	{
		return state;
	}

	@Override
	public void setState(State state)
	{
		if (this.state == state)
			return;

		if (state == State.STOP && (this.state == State.PLAY || this.state == State.PAUSE))
		{
			// Stop !
			resetTrackInfo();
		}

		this.state = state;
		notifyAllObservers();
	}

	@Override
	public int getVolume()
	{
		return volume;
	}

	@Override
	public void setVolume(int volume)
	{
		if (this.volume == volume)
			return;

		this.volume = volume;
		notifyAllObservers();
	}

	@Override
	public boolean isMute()
	{
		return mute;
	}

	@Override
	public void setMute(boolean mute)
	{
		if (this.mute == mute)
			return;

		this.mute = mute;
		notifyAllObservers();
	}

	public void setPositionInfo(PositionInfo positionInfo)
	{
		// TODO Not sufficient, need to check parameters
		if (this.positionInfo.getRelTime().compareTo(positionInfo.getRelTime()) == 0
				&& this.positionInfo.getAbsTime().compareTo(positionInfo.getAbsTime()) == 0)
			return;

		this.positionInfo = positionInfo;
		notifyAllObservers();
	}

	public MediaInfo getMediaInfo()
	{
		return mediaInfo;
	}

	public void setMediaInfo(MediaInfo mediaInfo)
	{
		if (this.mediaInfo.hashCode() == mediaInfo.hashCode())
			return;

		this.mediaInfo = mediaInfo;
		// notifyAllObservers();
	}

	public TransportInfo getTransportInfo()
	{
		return transportInfo;
	}

	public void setTransportInfo(TransportInfo transportInfo)
	{
		this.transportInfo = transportInfo;

		if (transportInfo.getCurrentTransportState() == TransportState.PAUSED_PLAYBACK
				|| transportInfo.getCurrentTransportState() == TransportState.PAUSED_RECORDING)
			setState(State.PAUSE);
		else if (transportInfo.getCurrentTransportState() == TransportState.PLAYING)
			setState(State.PLAY);
		else
			// if(transportInfo.getCurrentTransportState() == TransportState.STOPPED)
			setState(State.STOP);
	}

	private TrackMetadata getTrackMetadata()
	{
		return new TrackMetadata(positionInfo.getTrackMetaData());
	}

	private String formatTime(long h, long m, long s)
	{
		return ((h >= 10) ? "" + h : "0" + h) + ":" + ((m >= 10) ? "" + m : "0" + m) + ":"
				+ ((s >= 10) ? "" + s : "0" + s);
	}

	@Override
	public String getRemainingDuration()
	{
		long t = positionInfo.getTrackRemainingSeconds();
		long h = t / 3600;
		long m = (t - h * 3600) / 60;
		long s = t - h * 3600 - m * 60;
		return "-" + formatTime(h, m, s);
	}

	@Override
	public String getDuration()
	{
		long t = positionInfo.getTrackDurationSeconds();
		long h = t / 3600;
		long m = (t - h * 3600) / 60;
		long s = t - h * 3600 - m * 60;
		return formatTime(h, m, s);
	}

	@Override
	public String getPosition()
	{
		long t = positionInfo.getTrackElapsedSeconds();
		long h = t / 3600;
		long m = (t - h * 3600) / 60;
		long s = t - h * 3600 - m * 60;
		return formatTime(h, m, s);
	}

	@Override
	public long getDurationSeconds()
	{
		return positionInfo.getTrackDurationSeconds();
	}

	public void resetTrackInfo()
	{
		positionInfo = new PositionInfo();
		mediaInfo = new MediaInfo();
		notifyAllObservers();
	}

	@Override
	public String toString()
	{
		return "RendererState [state=" + state + ", volume=" + volume + ", repeatMode=" + repeatMode + ", randomMode="
				+ randomMode + ", positionInfo=" + positionInfo + ", mediaInfo=" + mediaInfo + ", trackMetadata="
				+ new TrackMetadata(positionInfo.getTrackMetaData()) + "]";
	}

	public class TrackMetadata {

		@Override
		public String toString()
		{
			return "TrackMetadata [id=" + id + ", title=" + title + ", artist=" + artist + ", genre=" + genre
					+ ", artURI=" + artURI + ", itemClass=" + itemClass + "]";
		}

		public TrackMetadata(String xml)
		{
			parseTrackMetadata(xml);
		}

		public TrackMetadata()
		{
		}

		public String id;
		public String title;
		public String artist;
		public String genre;
		public String artURI;
		public String itemClass;

		private XMLReader initializeReader() throws ParserConfigurationException, SAXException
		{
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser parser = factory.newSAXParser();
			XMLReader xmlreader = parser.getXMLReader();
			return xmlreader;
		}

		public void parseTrackMetadata(String xml)
		{
			if (xml == null)
				return;

			try
			{
				XMLReader xmlreader = initializeReader();
				UpnpItemHandler upnpItemHandler = new UpnpItemHandler();

				xmlreader.setContentHandler(upnpItemHandler);
				xmlreader.parse(new InputSource(new StringReader(xml)));
			}
			catch (Exception e)
			{
				// e.printStackTrace();
				Log.e(TAG, "Error while parsing metadata !");
				Log.e(TAG, "XML : " + xml);
			}
		}

		public class UpnpItemHandler extends DefaultHandler {

			private final StringBuffer buffer = new StringBuffer();

			@Override
			public void startElement(String namespaceURI, String localName, String qName, Attributes atts)
					throws SAXException
			{
				buffer.setLength(0);
				// Log.v(TAG, "startElement, localName="+ localName + ", qName=" + qName);

				if (localName.equals("item"))
				{
					id = atts.getValue("id");
				}
			}

			@Override
			public void endElement(String uri, String localName, String qName) throws SAXException
			{

				// Log.v(TAG, "endElement, localName="+ localName + ", qName=" + qName + ", buffer=" +
				// buffer.toString());

				if (localName.equals("title"))
				{
					title = buffer.toString();
				}
				else if (localName.equals("creator"))
				{
					artist = buffer.toString();
				}
				else if (localName.equals("genre"))
				{
					genre = buffer.toString();
				}
				else if (localName.equals("albumArtURI"))
				{
					artURI = buffer.toString();
				}
				else if (localName.equals("class"))
				{
					itemClass = buffer.toString();
				}
			}

			@Override
			public void characters(char[] ch, int start, int length)
			{
				buffer.append(ch, start, length);
			}
		}
	}

	@Override
	public int getElapsedPercent()
	{
		return positionInfo.getElapsedPercent();
	}

	@Override
	public String getTitle()
	{
		return getTrackMetadata().title;
	}

	@Override
	public String getArtist()
	{
		return getTrackMetadata().artist;
	}
}
