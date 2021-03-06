package com.kaldroid.bbf;

/*
 * @package: com.kaldroid.bbf
 * @activity: RSSHandler
 * @author: Kaldroid (kaldroid.co.uk)
 * @license: GNU/GPL
 * @description: RSSHandler class - basic handling only
 */

import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.*;

public class RSSHandler extends DefaultHandler 
{
	
	RSSFeed _feed;
	RSSItem _item;
	String _lastElementName = "";
	boolean bFoundChannel = false;
	final int RSS_TITLE = 1;
	final int RSS_LINK = 2;
	final int RSS_DESCRIPTION = 3;
	final int RSS_CATEGORY = 4;
	final int RSS_PUBDATE = 5;
	final int RSS_CONTENT = 6;
	
	int depth = 0;
	int currentstate = 0;
	/*
	 * Constructor 
	 */
	RSSHandler()
	{
	}
	
	/*
	 * getFeed - this returns our feed when all of the parsing is complete
	 */
	RSSFeed getFeed()
	{
		return _feed;
	}
	
	
	public void startDocument() throws SAXException
	{
		// initialize our RSSFeed object - this will hold our parsed contents
		_feed = new RSSFeed();
		// initialize the RSSItem object - we will use this as a crutch to grab the info from the channel
		// because the channel and items have very similar entries..
		_item = new RSSItem();

	}
	public void endDocument() throws SAXException
	{
	}
	public void startElement(String namespaceURI, String localName,String qName, Attributes atts) throws SAXException
	{
		depth++;
		if (localName.equals("channel") || localName.equals("feed"))
		{
			currentstate = 0;
			return;
		}
		if (localName.equals("image"))
		{
			// record our feed data - we temporarily stored it in the item :)
			_feed.setTitle(_item.getTitle());
			_feed.setPubDate(_item.getPubDate());
		}
		if (localName.equals("item") || localName.equals("entry"))
		{
			// create a new item
			_item = new RSSItem();
			return;
		}
		if (localName.equals("title"))
		{
			currentstate = RSS_TITLE;
			return;
		}
		if (localName.equals("description"))
		{
			currentstate = RSS_DESCRIPTION;
			_item.setDescription("");
			return;
		}
		if (localName.equals("link"))
		{
			currentstate = RSS_LINK;
			return;
		}
		if (localName.equals("category"))
		{
			currentstate = RSS_CATEGORY;
			return;
		}
		if (localName.equals("pubDate"))
		{
			currentstate = RSS_PUBDATE;
			return;
		}
		if (localName.equals("content") || localName.equals("encoded"))
		{
			currentstate = RSS_CONTENT;
			_item.setContent("");
			return;
		}
		// if we don't explicitly handle the element, make sure we don't wind up erroneously 
		// storing a newline or other bogus data into one of our existing elements
		//currentstate = 0;
	}
	
	public void endElement(String namespaceURI, String localName, String qName) throws SAXException
	{
		depth--;
		if (localName.equals("item") || localName.equals("entry"))
		{
			// add our item to the list!
			_feed.addItem(_item);
			return;
		}
		if (localName.equals("channel") || localName.equals("feed"))
		{
			currentstate = 0;
			return;
		}
		if (localName.equals("image"))
		{
			currentstate = 0;
			return;
		}
		if (localName.equals("title"))
		{
			currentstate = 0;
			return;
		}
		if (localName.equals("description"))
		{
			currentstate = 0;
			return;
		}
		if (localName.equals("link"))
		{
			currentstate = 0;
			return;
		}
		if (localName.equals("category"))
		{
			currentstate = 0;
			return;
		}
		if (localName.equals("pubDate"))
		{
			currentstate = 0;
			return;
		}
		if (localName.equals("content") || localName.equals("encoded"))
		{
			currentstate = 0;
			return;
		}
	}
	 
	public void characters(char ch[], int start, int length)
	{
		String theString = new String(ch,start,length);
		//Log.i("RSSReader","characters[" + theString + "]");
		
		switch (currentstate)
		{
			case RSS_TITLE:
				if (!theString.equals("\n"))
					_item.setTitle(theString);
				//currentstate = 0;
				break;
			case RSS_LINK:
				if (!theString.equals("\n"))
					_item.setLink(theString);
				//currentstate = 0;
				break;
			case RSS_DESCRIPTION:
				// count be multi-line
				if (!theString.equals("\n"))
					_item.setDescription(_item.getDescription() + theString);
				//currentstate = 0;
				break;
			case RSS_CATEGORY:
				if (!theString.equals("\n"))
					_item.setCategory(theString);
				//currentstate = 0;
				break;
			case RSS_PUBDATE:
				if (!theString.equals("\n"))
					_item.setPubDate(theString);
				//currentstate = 0;
				break;
			case RSS_CONTENT:
				// could be multi-line
				if (!theString.equals("\n"))
					_item.setContent(_item.getContent() + theString);
				//currentstate = 0;
				break;
			default:
				// must reset if unknown
				currentstate = 0;
				return;
		}
	}
}
