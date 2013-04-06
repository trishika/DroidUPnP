package org.droidupnp.model.cling;

import java.io.StringReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;

public class TrackMetadata {

	protected static final String TAG = "TrackMetadata";

	@Override
	public String toString()
	{
		return "TrackMetadata [id=" + id + ", title=" + title + ", artist=" + artist + ", genre=" + genre + ", artURI="
				+ artURI + "res=" + res + ", itemClass=" + itemClass + "]";
	}

	public TrackMetadata(String xml)
	{
		parseTrackMetadata(xml);
	}

	public TrackMetadata()
	{
	}

	public TrackMetadata(String id, String title, String artist, String genre, String artURI, String res,
			String itemClass)
	{
		super();
		this.id = id;
		this.title = title;
		this.artist = artist;
		this.genre = genre;
		this.artURI = artURI;
		this.res = res;
		this.itemClass = itemClass;
	}

	public String id;
	public String title;
	public String artist;
	public String genre;
	public String artURI;
	public String res;
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

		Log.d(TAG, "XML : " + xml);

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

	public String getXML()
	{
		String xml = "<DIDL-Lite xmlns=\"urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:upnp=\"urn:schemas-upnp-org:metadata-1-0/upnp/\" xmlns:dlna=\"urn:schemas-dlna-org:metadata-1-0/\">"
				+ "<item id=\""
				+ id
				+ "\" parentID=\"\" restricted=\"1\">"
				+ "<dc:title>"
				+ title
				+ "</dc:title>"
				+ "<dc:creator>"
				+ artist
				+ "</dc:creator>"
				+ "<upnp:genre>"
				+ genre
				+ "</upnp:genre>"
				+ "<upnp:albumArtURI dlna:profileID=\"JPEG_TN\">"
				+ artURI
				+ "</upnp:albumArtURI>"
				+ "<res>"
				+ res
				+ "</res>" + "<upnp:class>" + itemClass + "</upnp:class>" + "</item>" + "</DIDL-Lite>";
		Log.d(TAG, xml);
		return xml;
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
			else if (localName.equals("res"))
			{
				res = buffer.toString();
			}
		}

		@Override
		public void characters(char[] ch, int start, int length)
		{
			buffer.append(ch, start, length);
		}
	}
}