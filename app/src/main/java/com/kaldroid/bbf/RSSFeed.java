package com.kaldroid.bbf;

/*
 * @package: com.kaldroid.bbf
 * @activity: RSSFeed
 * @author: Kaldroid (kaldroid.co.uk)
 * @license: GNU/GPL
 * @description: RSSFeed class - basic handling only
 */

import java.util.List;
import java.util.Vector;

import android.text.TextUtils;

public class RSSFeed 
{
	private String _rawText = null;
	private String _title = null;
	private String _pubdate = null;
	private int _itemcount = 0;
	private List<RSSItem> _itemlist;
	
	
	RSSFeed()
	{
		_itemlist = new Vector<RSSItem>(0); 
	}
	int addItem(RSSItem item)
	{
		_itemlist.add(item);
		_itemcount++;
		return _itemcount;
	}
	RSSItem getItem(int location)
	{
		return _itemlist.get(location);
	}
	List<RSSItem> getAllItems()
	{
		return _itemlist;
	}
	int getItemCount()
	{
		return _itemcount;
	}
	void setTitle(String title)
	{
		_title = title;
	}
	void setPubDate(String pubdate)
	{
		_pubdate = pubdate;
	}
	String getTitle()
	{
		return _title;
	}
	String getPubDate()
	{
		return _pubdate;
	}
	public String toString() {
		_rawText = "<rss version=\"0.91\"><channel><title>" + _title + 
		           "</title>";
		for (int i=0; i<_itemcount; i++) {
			RSSItem itm = getItem(i);
			_rawText = _rawText + "<item><title>" + itm.getTitle() + "</title>";
			_rawText = _rawText + "<link>" + itm.getLink() + "</link>";
			String desc = itm.getDescription();
			if (!TextUtils.isEmpty(desc))
				_rawText = _rawText + "<description>" + TextUtils.htmlEncode(desc) + "</description>";
			else {
				desc = itm.getContent();
				_rawText = _rawText + "<content>" + TextUtils.htmlEncode(desc) + "</content>";
			}
			_rawText = _rawText + "</item>";
		}
		_rawText = _rawText + "</channel></rss>";
		return _rawText;
	}
}
