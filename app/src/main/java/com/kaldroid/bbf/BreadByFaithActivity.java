package com.kaldroid.bbf;

/*
 * @package: com.kaldroid.bbf
 * @activity: BreadByFaithActivity
 * @author: Kaldroid (kaldroid.co.uk)
 * @license: GNU/GPL
 * @description: This is the main activity for Bread By Faith showing 5 webViews in a viewFlipper
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager.LayoutParams;
import android.webkit.WebView;
import android.widget.Toast;

public class BreadByFaithActivity extends Activity {

	private int item = 0;
	private int scale = 0;
	private WebView cv = null;
	private RSSFeed feed = null;
	private RSSItem newest = null;
	private boolean mZoomable = true;
	private Menu myMenu = null;
	private BroadcastReceiver bcUpdateEvent = null;
	private BroadcastReceiver bcSpeechDone = null;
	private WakeLock mWakeLock;
	private static PowerManager pm;
	private String myPlainBread = "John 3:16";

	private String myAddress = "John 3:16";
	// we need a basic RSS to start with if we have no internet connection
	private String emptyBread = "<rss version=\"0.91\">\n" +
			"<channel>\n" +
			"<title>FBC XT Blog (cache)</title><link>http://blog.fbcxt.org/</link>\n" +
			"<description>Xtreme Teens Blogs</description>\n<language>en</language>\n" +
			"<item>\n" +
			"<title>Today</title>\n" +
			"<description>" +
			"&lt;strong&gt;Today&lt;/strong&gt;: John 3:16&lt;br /&gt; &lt;br /&gt;&lt;br /&gt; &lt;strong&gt;Key Verse&lt;/strong&gt;: John 3:16&lt;br /&gt; 16  For God so loved the world that He gave His only Son so that whosoever believes in Him should have eternal life.&lt;br /&gt; &lt;br /&gt;&lt;br /&gt; &lt;strong&gt;Devotion&lt;/strong&gt;:&lt;br /&gt; This is a place-holder until the real data is fetched" +
			"</description>\n" +
			"</item>\n" +
			"</channel>\n" +
			"</rss>";

	/** Called when the activity is first created. */
	@SuppressLint({ "SetJavaScriptEnabled" })
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// restore settings retained over reboots
		BreadBootReceiver.restoreSettings(this);
		pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		// set theme, hide title, do android stuff and select layout
		if (BreadBootReceiver.sdkVersion < Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
			setTheme(BreadBootReceiver.ourTheme);
		} else {
			if (BreadBootReceiver.ourTheme == android.R.style.Theme_Black_NoTitleBar) {
				setTheme(0x01030128);
			}
			else {
				setTheme(0x0103012b);
			}
		}
		//setTheme(BreadBootReceiver.ourTheme);
		//this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);

		if (BreadBootReceiver.sdkVersion < Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
			setContentView(R.layout.todaysbread);
		}
		else {
			setContentView(R.layout.todaysbreadics);
		}

		if(BreadBootReceiver.daysToLoad == 1) {
			mZoomable = true;
		}
		// grab the webViews and set up listeners and scales
		cv = (WebView) findViewById(R.id.webView1);
		cv.setWebViewClient(new scaledWebViewClient());
		cv.getSettings().setJavaScriptEnabled(true);
		try {
			Method setLayerTypeMethod = cv.getClass().getMethod("setLayerType", new Class[] {int.class, Paint.class});
			setLayerTypeMethod.invoke(cv, new Object[] {1, null});
		} catch (NoSuchMethodException e) {
			// Older OS, no HW acceleration anyway
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		scale = (int)BreadBootReceiver.scale;
		cv.setInitialScale(scale);
		cv.getSettings().setBuiltInZoomControls(mZoomable);
		cv.getSettings().setSupportZoom(mZoomable);

		// stay awake...
		if (BreadBootReceiver.stayAwake) {
			getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
			mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
		}

		bcUpdateEvent = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				// we have received something
				String action = intent.getAction();
				if (action.equals(BreadBootReceiver.bcUpdateEvent)) {
					// do a local screen update
					Bundle b = intent.getExtras();
					Boolean doit = b.getBoolean("update");
					if (doit)
						refreshBread();
				}
			}
		};
		bcSpeechDone = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				if(action.equals(BreadBootReceiver.bcSpeechDone)) {
					updateIcon(false);
				}
			}
		};
	}

	// our basic menu setup
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// goPrevious, goVerse, goNext, goShareText, goShareLink, goRefresh, goSettings, goAbout, goHelp
		myMenu = menu;
		MenuItem mi = (MenuItem) menu.findItem(R.id.goVerse);
		if (BreadBootReceiver.sdkVersion < Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
			mi.setIcon(R.drawable.verse);
		}
		else {
			if (BreadBootReceiver.ourTheme == android.R.style.Theme_Black_NoTitleBar) {
				mi.setIcon(R.drawable.ics_verse_dark);
			} else {
				mi.setIcon(R.drawable.ics_verse);
			}
		}
		mi = (MenuItem) menu.findItem(R.id.goPrevious);
		if (BreadBootReceiver.sdkVersion < Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
			mi.setIcon(R.drawable.left);
		}
		else {
			if (BreadBootReceiver.ourTheme == android.R.style.Theme_Black_NoTitleBar) {
				mi.setIcon(R.drawable.ics_left_dark);
			} else {
				mi.setIcon(R.drawable.ics_left);
			}
		}
		mi = (MenuItem) menu.findItem(R.id.goNext);
		if (BreadBootReceiver.sdkVersion < Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
			mi.setIcon(R.drawable.right);
		}
		else {
			if (BreadBootReceiver.ourTheme == android.R.style.Theme_Black_NoTitleBar) {
				mi.setIcon(R.drawable.ics_right_dark);
			} else {
				mi.setIcon(R.drawable.ics_right);
			}
		}
		mi = (MenuItem) menu.findItem(R.id.goShareText);
		if (BreadBootReceiver.sdkVersion < Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
			mi.setIcon(R.drawable.sharetxt);
		}
		else {
			if (BreadBootReceiver.ourTheme == android.R.style.Theme_Black_NoTitleBar) {
				mi.setIcon(R.drawable.ics_share_txt_dark);
			} else {
				mi.setIcon(R.drawable.ics_share_txt);
			}
		}
		mi = (MenuItem) menu.findItem(R.id.goShareLink);
		if (BreadBootReceiver.sdkVersion < Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
			mi.setIcon(R.drawable.sharelnk);
		}
		else {
			if (BreadBootReceiver.ourTheme == android.R.style.Theme_Black_NoTitleBar) {
				mi.setIcon(R.drawable.ics_share_lnk_dark);
			} else {
				mi.setIcon(R.drawable.ics_share_lnk);
			}
		}
		mi = (MenuItem) menu.findItem(R.id.goRefresh);
		if (BreadBootReceiver.sdkVersion < Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
			mi.setIcon(R.drawable.refresh);
		}
		else {
			if (BreadBootReceiver.ourTheme == android.R.style.Theme_Black_NoTitleBar) {
				mi.setIcon(R.drawable.ics_refresh_dark);
			} else {
				mi.setIcon(R.drawable.ics_refresh);
			}
		}
		mi = (MenuItem) menu.findItem(R.id.goSettings);
		if (BreadBootReceiver.sdkVersion < Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
			mi.setIcon(R.drawable.settings);
		}
		else {
			if (BreadBootReceiver.ourTheme == android.R.style.Theme_Black_NoTitleBar) {
				mi.setIcon(R.drawable.ics_settings_dark);
			} else {
				mi.setIcon(R.drawable.ics_settings);
			}
		}
		mi = (MenuItem) menu.findItem(R.id.goAbout);
		if (BreadBootReceiver.sdkVersion < Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
			mi.setIcon(R.drawable.about);
		}
		else {
			if (BreadBootReceiver.ourTheme == android.R.style.Theme_Black_NoTitleBar) {
				mi.setIcon(R.drawable.ics_about_dark);
			} else {
				mi.setIcon(R.drawable.ics_about);
			}
		}
		mi = (MenuItem) menu.findItem(R.id.goHelp);
		if (BreadBootReceiver.sdkVersion < Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
			mi.setIcon(R.drawable.about);
		}
		else {
			if (BreadBootReceiver.ourTheme == android.R.style.Theme_Black_NoTitleBar) {
				mi.setIcon(R.drawable.ics_help_dark);
			} else {
				mi.setIcon(R.drawable.ics_help);
			}
		}
		mi = (MenuItem) menu.findItem(R.id.goSpeakDevotion);
		if (BreadBootReceiver.sdkVersion < Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
			mi.setIcon(R.drawable.play);
		}
		else {
			if (BreadBootReceiver.ourTheme == android.R.style.Theme_Black_NoTitleBar) {
				mi.setIcon(R.drawable.ics_play_dark);
			} else {
				mi.setIcon(R.drawable.ics_play);
			}
		}
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public void onPause() {
		Intent speaker = new Intent(getBaseContext(), BreadSpeakService.class);
		getBaseContext().stopService(speaker);
		if(bcUpdateEvent != null) try { unregisterReceiver(bcUpdateEvent); } catch (Exception ex) {}
		if(bcSpeechDone != null) try { unregisterReceiver(bcSpeechDone); } catch (Exception ex) {}
		if(pm != null) {
			if(mWakeLock != null) {
				mWakeLock.release();
			}
		}
		// save settings - just in case user changed font size
		BreadBootReceiver.saveSettings();
		// now pause
		super.onPause();
	}

	@Override
	public void onResume() {
		if(bcUpdateEvent != null) registerReceiver(bcUpdateEvent, new IntentFilter(BreadBootReceiver.bcUpdateEvent));
		if(bcSpeechDone != null) registerReceiver(bcSpeechDone, new IntentFilter(BreadBootReceiver.bcSpeechDone));
		if(pm != null) {
			if(mWakeLock != null) {
				mWakeLock.acquire();
			}
		}
		super.onResume();
	}

	public void updateIcon(Boolean stop) {
		if (myMenu != null) {
			MenuItem mi = (MenuItem) myMenu.findItem(R.id.goSpeakDevotion);
			if (stop) {
				if (BreadBootReceiver.sdkVersion < Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
					mi.setIcon(R.drawable.stop);
				}
				else {
					if (BreadBootReceiver.ourTheme == android.R.style.Theme_Black_NoTitleBar) {
						mi.setIcon(R.drawable.ics_stop_dark);
					} else {
						mi.setIcon(R.drawable.ics_stop);
					}
				}

			}
			else {
				if (BreadBootReceiver.sdkVersion < Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
					mi.setIcon(R.drawable.play);
				}
				else {
					if (BreadBootReceiver.ourTheme == android.R.style.Theme_Black_NoTitleBar) {
						mi.setIcon(R.drawable.ics_play_dark);
					} else {
						mi.setIcon(R.drawable.ics_play);
					}
				}

			}
		}
	}

	// basic menu handling
	@Override
	public boolean onOptionsItemSelected(MenuItem itm) {
		switch(itm.getItemId()) {
		case R.id.goPrevious:
			if (item < (BreadBootReceiver.daysToLoad-1)) {
				getScale();
				item = item + 1;
				updateScale();
				refreshBread();
			}
			else {
				Toast.makeText(this, (String)getBaseContext().getText(R.string.last_day), Toast.LENGTH_SHORT).show();
			}
			break;

		case R.id.goRefresh:
			Toast.makeText(this, (String)getBaseContext().getText(R.string.reget), Toast.LENGTH_SHORT).show();

			// zap the time and schedule a re-get
			try {
				File sdCard = Environment.getExternalStorageDirectory();
				File dir = new File (sdCard.getAbsolutePath() + "/BreadbyFaith");
				File last = new File(dir, "lastTimestamp.txt");
				String eol = System.getProperty("line.separator");

				// zap last fetch time to force get from internet
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(last)));
				writer.write("0" + eol + "0" + eol);
				writer.close();
			}
			catch (Exception e1) {
				Log.e("BBF","Exception:"+e1.toString());
			}
			// do the update in the background else ICS complains
			Intent service = new Intent(getBaseContext(), BreadCheckService.class);
			getBaseContext().startService(service);

			// go home because the refresh will notify us
			finish();

			break;

		case R.id.goNext:
			if (item > 0) {
				getScale();
				item = item - 1;
				updateScale();
				refreshBread();
			}
			else {
				Toast.makeText(this, (String)getBaseContext().getText(R.string.curr_day), Toast.LENGTH_SHORT).show();
			}
			break;

		case R.id.goVerse:
			Intent myIntent = new Intent(BreadByFaithActivity.this, BreadByFaithVerse.class);
			Bundle bundle = new Bundle();
			bundle.putString("ADDRESS", myAddress);
			myIntent.putExtras(bundle);
			BreadByFaithActivity.this.startActivity(myIntent);
			break;

		case R.id.goSettings:
			Intent mySettings = new Intent(BreadByFaithActivity.this, BreadByFaithSettings.class);
			BreadByFaithActivity.this.startActivityForResult(mySettings, 0);
			break;

		case R.id.goAbout:
			Intent myAbout = new Intent(BreadByFaithActivity.this, AboutBread.class);
			BreadByFaithActivity.this.startActivity(myAbout);
			break;

		case R.id.goShareText:
			if(feed != null) {
				if (item < feed.getItemCount()) {
					RSSItem sharethis = feed.getItem(item);
					String bread = sharethis.getDescription();
					if (TextUtils.isEmpty(bread))
						bread = sharethis.getContent();
					bread = bread.replaceAll("<[/]*div>", "");
					bread = Html.fromHtml(bread).toString();
					Intent sharingIntent = new Intent(Intent.ACTION_SEND);
					sharingIntent.setType("text/plain");
					sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, bread);
					sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Bread by Faith");
					startActivity(Intent.createChooser(sharingIntent, "Share using..."));
				}
			}
			break;

		case R.id.goShareLink:
			if(feed!=null) {
				if (item < feed.getItemCount()) {
					RSSItem sharelink = feed.getItem(item);
					String breadlink = "I wanted to share this with you: " + sharelink.getLink();
					try {
						// we just do this to validate the link is a valid url
						URL url = new URL(sharelink.getLink());
						Log.i("BBF","Share link: "+url.toString());
					} catch (MalformedURLException e) {
						breadlink = "Sorry - No Links exist yet, please refresh the devotions...";
						Log.e("BBF","Exception:"+e.toString()+">"+sharelink.getLink());
					}
					Intent sharingLinkIntent = new Intent(Intent.ACTION_SEND);
					sharingLinkIntent.setType("text/plain");
					sharingLinkIntent.putExtra(android.content.Intent.EXTRA_TEXT, breadlink);
					sharingLinkIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Bread by Faith");
					startActivity(Intent.createChooser(sharingLinkIntent, "Share using..."));
				}
			}
			break;

		case R.id.goHelp:
			Intent myHelp = new Intent(BreadByFaithActivity.this, HelpBread.class);
			BreadByFaithActivity.this.startActivity(myHelp);
			break;

		case R.id.goSpeakDevotion:
			String words = stripHtml(myPlainBread);
			speakWords(words);
			break;
		}
		return true;
	}

	private String stripHtml(String speech) {
		return Html.fromHtml(speech.replaceAll("s/<(.*?, replacement)>//g","")).toString();
	}

	private void speakWords(String speech) {
		Intent speaker = new Intent(getBaseContext(), BreadSpeakService.class);
		speaker.putExtra("SPEAKTHIS", speech);
		if (getBaseContext().stopService(speaker) == false) {
			getBaseContext().startService(speaker);
			updateIcon(true);
		}
		else {
			updateIcon(false);
		}
	}

	// maintain each views scale
	private void getScale() {
		scale = (int)(100.0 * cv.getScale());
	}

	// try update views to same scale - does not seem to work :-(
	private void updateScale() {
		cv.setInitialScale(scale);
	}

	// this is a simple restart which will get triggered when the settings screen comes back with OK
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			// we need to restart but only when settings comes back - just in case theme changed
			Intent resintent = this.getIntent();
			finish();
			startActivity(resintent);
		}
	}

	@Override
	public void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		// do the update in the background or ICS complains
		Intent service = new Intent(getBaseContext(), BreadCheckService.class);
		getBaseContext().startService(service);

		// in the mean time, get and display the cached data
		File bbfc = null;
		try {
			File sdCard = Environment.getExternalStorageDirectory();
			File dir = new File (sdCard.getAbsolutePath() + "/BreadbyFaith");
			if (!dir.exists()) {
				if (!dir.mkdir()) {
					// cannot make directory???
					Log.e("BBF","Cannot make folder:"+dir.getAbsolutePath());
				}
			}
			bbfc = new File(dir, "bbfcache.xml");
		}
		catch (Exception e1) {
			bbfc = new File("bbfcache.xml");
			Log.e("BBF","Exception:"+e1.toString());
		}
		// save temporary bbf - this is a new cache
		if (!checkCacheExists(bbfc))
			updateCache(bbfc, emptyBread);
		refreshBread();

		// now set the timer for the next wake-up and check...
		// we do this 1 minute after initial load to make sure things settle down before we go for longer triggers
		Intent intent = new Intent(getBaseContext(), AlarmReceiver.class);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(getBaseContext(), 001000, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		if(BreadBootReceiver.refreshInt > 0)
			alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1 * (60 * 1000), BreadBootReceiver.refreshInt * 60 * 1000, pendingIntent);
	}

	private boolean checkCacheExists(File bbfc) {
		// check if cache file exists
		try {
			String eol = System.getProperty("line.separator");
			BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(bbfc)));
			String line;
			StringBuffer buffer = new StringBuffer();
			while ((line = input.readLine()) != null) {
				buffer.append(line + eol);
			}
			input.close();
			return true;

		} catch (Exception e) {
			Log.e("BBF","Exception:"+e.toString());
			return false;
		}
	}

	private void updateCache(File bbfc, String bread) {
		try {
			String eol = System.getProperty("line.separator");
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(bbfc)));
			if (feed == null)
				writer.write(bread + eol);
			else
				writer.write(feed.toString() + eol);
			writer.close();
		} catch (Exception e) {
			Log.e("BBF","Exception:"+e.toString());
			e.printStackTrace();
		}
	}

	// Just get data from cached file and display it - much faster
	// Let someone else update the file :-)
	public void refreshBread() {
		// refresh the data from the cache file
		Log.i("BBF","Refresh Bread called");
		File bbfc = null;
		try {
			File sdCard = Environment.getExternalStorageDirectory();
			File dir = new File (sdCard.getAbsolutePath() + "/BreadbyFaith");
			bbfc = new File(dir, "bbfcache.xml");
		}
		catch (Exception e1) {
			bbfc = new File("bbfcache.xml");
			Log.e("BBF","Exception:"+e1.toString());
		}
		feed = getFeedFromFile(bbfc);
		String bread = null;
		WebView wv = null;
		int i = 0;
		cv.setBackgroundColor(BreadBootReceiver.ourBGColor);
		String htmlbegin = "";
		if (BreadBootReceiver.ourTheme == android.R.style.Theme_Black_NoTitleBar) {
			htmlbegin = "<div style=\"color:#FFFFFF; background-color:#000000;\">";
		}
		else {
			htmlbegin = "<div style=\"color:#000000; background-color:#FFFFFF;\">";
		}
		String htmlend = " </div>";
		// get the latest bread by faith
		if ((feed == null) || (feed.equals(null))) {
			bread = htmlbegin + "<H2>" + (String)getBaseContext().getText(R.string.cannot_fetch) + "</H2>" + htmlend;
			wv = cv;
			myAddress = "";
			wv.loadDataWithBaseURL("", bread, "text/html", "UTF-8", "");
		}
		else {
			for(i=0; i<BreadBootReceiver.daysToLoad; i++) {
				if(i==item) {
					try {
						newest = feed.getItem(i);
						bread = newest.getDescription();
						if (TextUtils.isEmpty(bread))
							bread = newest.getContent();
						bread = bread.replaceAll("<[/]*div>", "");
						String[] lines = bread.split("<br[ ]+[/]+>", 2);
						String[] words = lines[0].split(":", 2);
						if (words.length > 1)
							myAddress = words[1].trim();
						else
							myAddress = lines[0].trim();
						bread = htmlbegin + bread + htmlend;
						myPlainBread = stripHtml(bread);
						// if we do go for verse pop-out instead of new activity, then
						// this is where the link will be created for javascript to work
						// or at least I think so...
						//myAddress[i] = TextUtils.htmlEncode(myAddress[i]);
					}
					catch (Exception ex) {
						Log.e("BBF","Exception:"+ex.toString());
						bread = "<H2>"+(String)getBaseContext().getText(R.string.cannot_fetch)+"</H2>";
					}
					try {
						cv.loadDataWithBaseURL("", bread, "text/html", "UTF-8", "");
					}
					catch (Exception ee) {
						Log.e("BBF","Exception:"+ee.toString());
					}
				}
			}
		}
	}


	// RSS stuff
	private RSSFeed getFeedFromFile(File bbfc)
	{
		try
		{
			// create the factory
			SAXParserFactory factory = SAXParserFactory.newInstance();
			// create a parser
			SAXParser parser = factory.newSAXParser();

			// create the reader (scanner)
			XMLReader xmlreader = parser.getXMLReader();
			// instantiate our handler
			RSSHandler theRssHandler = new RSSHandler();
			// assign our handler
			xmlreader.setContentHandler(theRssHandler);
			// get our data via the url class
			InputSource is = new InputSource(new FileInputStream(bbfc));
			// perform the synchronous parse           
			xmlreader.parse(is);
			// get the results - should be a fully populated RSSFeed instance, or null on error
			return theRssHandler.getFeed();
		}
		catch (Exception e2)
		{
			Log.e("BBF","Exception:"+e2.toString());
			// if we have a problem, simply return null
			return null;
		}
	}

}

