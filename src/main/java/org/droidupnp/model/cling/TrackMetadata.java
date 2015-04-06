package org.droidupnp.model.cling;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xmlpull.v1.XmlSerializer;

import android.util.Log;
import android.util.Xml;

public class TrackMetadata {

	protected static final String TAG = "TrackMetadata";

    @Override
	public String toString()
	{
		return "TrackMetadata [id=" + id + ", title=" + title + ", artist=" + artist + ", genre=" + genre + ", artURI="
				+ artURI + "res=" + getRes() + ", itemClass=" + itemClass + "]";
	}

    public TrackMetadata(String xml)
	{
		parseTrackMetadata(xml);
	}

	public TrackMetadata()
	{
	}

	public TrackMetadata(String id, String title, String artist, String genre, String artURI,
			String itemClass,List<Resource> res)
	{
		super();
		this.id = id;
		this.title = title;
		this.artist = artist;
		this.genre = genre;
		this.artURI = artURI;
		this.itemClass = itemClass;
        resources = res;
	}

	public String id;
	public String title;
	public String artist;
	public String genre;
	public String artURI;
	public String itemClass;
    private List<Resource> resources = new ArrayList<>();
    private String getRes() {
        return resources!=null&&resources.size()==0?"":resources.get(0).url;
    }
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
			Log.w(TAG, "Error while parsing metadata !");
			Log.w(TAG, "XML : " + xml);
		}
	}

	public String getXML()
	{
		XmlSerializer s = Xml.newSerializer();
		StringWriter sw = new StringWriter();

		try {
			s.setOutput(sw);

			s.startDocument(null,null);
			s.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);

			//start a tag called "root"
			s.startTag(null, "DIDL-Lite");
			s.attribute(null, "xmlns", "urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/");
			s.attribute(null, "xmlns:dc", "http://purl.org/dc/elements/1.1/");
			s.attribute(null, "xmlns:upnp", "urn:schemas-upnp-org:metadata-1-0/upnp/");
			s.attribute(null, "xmlns:dlna", "urn:schemas-dlna-org:metadata-1-0/");

			s.startTag(null, "item");
			s.attribute(null, "id", ""+id);
			s.attribute(null, "parentID", "");
			s.attribute(null, "restricted", "1");

			if(title!=null)
			{
				s.startTag(null, "dc:title");
				s.text(title);
				s.endTag(null, "dc:title");
			}

			if(artist!=null)
			{
				s.startTag(null, "dc:creator");
				s.text(artist);
				s.endTag(null, "dc:creator");
			}

			if(genre!=null)
			{
				s.startTag(null, "upnp:genre");
				s.text(genre);
				s.endTag(null, "upnp:genre");
			}

			if(artURI!=null)
			{
				s.startTag(null, "upnp:albumArtURI");
				s.attribute(null, "dlna:profileID", "JPEG_TN");
				s.text(artURI);
				s.endTag(null, "upnp:albumArtURI");
			}

            if(resources!=null)
                for(Resource r:resources){
                    s.startTag(null, "res");
                    if(r.protocolinfo!=null)s.attribute(null,"protocolInfo",r.protocolinfo);
                    s.text(r.url);
                    s.endTag(null, "res");
                }

			if(itemClass!=null)
			{
				s.startTag(null, "upnp:class");
				s.text(itemClass);
				s.endTag(null, "upnp:class");
			}

			s.endTag(null, "item");

			s.endTag(null, "DIDL-Lite");

			s.endDocument();
			s.flush();

		} catch (Exception e) {
			Log.e(TAG, "error occurred while creating xml file : " + e.toString() );
			e.printStackTrace();
		}

		String xml = sw.toString();
		Log.d(TAG, "TrackMetadata : " + xml);

		return xml;
	}

	public class UpnpItemHandler extends DefaultHandler {

		private final StringBuffer buffer = new StringBuffer();
        private Attributes attributes;

		@Override
		public void startElement(String namespaceURI, String localName, String qName, Attributes atts)
				throws SAXException
		{
			buffer.setLength(0);
			// Log.v(TAG, "startElement, localName="+ localName + ", qName=" + qName);
            attributes = atts;
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
                String protocolinfo = attributes==null?null:attributes.getValue("protocolInfo");
                resources.add(new Resource(buffer.toString(),protocolinfo));
			}
		}

		@Override
		public void characters(char[] ch, int start, int length)
		{
			buffer.append(ch, start, length);
		}
	}
    static public class Resource{
        String url,protocolinfo;
        public Resource(String url,String protocolinfo){
            this.url = url;
            this.protocolinfo = protocolinfo;
        }
    }
}