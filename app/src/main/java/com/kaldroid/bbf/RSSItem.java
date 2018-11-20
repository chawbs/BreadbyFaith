package com.kaldroid.bbf;

/*
 * @package: com.kaldroid.bbf
 * @activity: RSSItem
 * @author: Kaldroid (kaldroid.co.uk)
 * @license: GNU/GPL
 * @description: RSSItem class - basic data handling only
 */

public class RSSItem 
{
	private String _title = null;
	private String _description = null;
	private String _link = null;
	private String _category = null;
	private String _pubdate = null;
	private String _content = null;
	
	RSSItem()
	{
	}
	void setTitle(String title)
	{
		_title = title;
	}
	void setDescription(String description)
	{
		_description = description;
	}
	void setLink(String link)
	{
		_link = link;
	}
	void setCategory(String category)
	{
		_category = category;
	}
	void setPubDate(String pubdate)
	{
		_pubdate = pubdate;
	}
	void setContent(String content)
	{
		_content = content;
	}
	String getTitle()
	{
		return _title;
	}
	String getDescription()
	{
		return _description;
	}
	String getLink()
	{
		return _link;
	}
	String getCategory()
	{
		return _category;
	}
	String getPubDate()
	{
		return _pubdate;
	}
	String getContent()
	{
		return _content;
	}
	public String toString()
	{
		// limit how much text we display
		if (_title.length() > 42)
		{
			return _title.substring(0, 42) + "...";
		}
		return _title;
	}
}
