package com.kaldroid.bbf;

/*
 * @package: com.kaldroid.bbf
 * @activity: BreadByFaithVerse
 * @author: Kaldroid (kaldroid.co.uk)
 * @license: GNU/GPL
 * @description: Activity to handle verse view - note the update is done in a thread
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Paint;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.text.Html;
//import android.os.PowerManager;
//import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager.LayoutParams;
import android.webkit.WebView;

public class BreadByFaithVerse extends Activity  {

	private WebView wv1 = null;
    private File bbfv = null;
	private String url1 = "";
	private String emptyVerse = "16 For God so loved the world, that he gave his only begotten Son, that whosoever believeth in him should not perish, but have everlasting life.";
	private String fetchVerse = "... fetching verse ...";
	private String actualVerse = "";
	private String formattedVerse = "";
	private String address = "";
	private String lAddress = "";
	private WakeLock mWakeLock;
	private static PowerManager pm;
	private Menu myMenu = null;
	private Boolean gotLiveVerse = false;
	private BroadcastReceiver bcUpdateEvent = null;
	private BroadcastReceiver bcSpeechDone = null;

    /** Called when the activity is first created. */
    @SuppressLint({ "SetJavaScriptEnabled"})
	@Override
    public void onCreate(Bundle savedInstanceState) {
        //this.requestWindowFeature(Window.FEATURE_NO_TITLE);
    	pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		if (BreadBootReceiver.sdkVersion < Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
			setTheme(BreadBootReceiver.ourTheme);
		} else {
			if (BreadBootReceiver.ourTheme == android.R.style.Theme_Black_NoTitleBar)
				setTheme(0x01030128);
			else
				setTheme(0x0103012b);
		}
//    	setTheme(BreadBootReceiver.ourTheme);
        super.onCreate(savedInstanceState);
        if (BreadBootReceiver.sdkVersion < Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
        	setContentView(R.layout.todaysverse);
        else
        	setContentView(R.layout.todaysverseics);
        
        if("".equals(BreadBootReceiver.verseLookup))
        	url1 = (String)getBaseContext().getText(R.string.verse_url);
        else
        	url1 = BreadBootReceiver.verseLookup;
        
        // get the address
        Bundle bundle = getIntent().getExtras();
        address = bundle.getString("ADDRESS");
        
		// grab the webview
        wv1 = (WebView) findViewById(R.id.webView1);
        wv1.setWebViewClient(new scaledWebViewClient());
        wv1.getSettings().setJavaScriptEnabled(true);
        wv1.getSettings().setBuiltInZoomControls(true);
        wv1.setBackgroundColor(BreadBootReceiver.ourBGColor);
        wv1.setInitialScale((int)BreadBootReceiver.scale);
		try {
		    Method setLayerTypeMethod = wv1.getClass().getMethod("setLayerType", new Class[] {int.class, Paint.class});
		    setLayerTypeMethod.invoke(wv1, new Object[] {1, null});
		} catch (NoSuchMethodException e) {
		    // Older OS, no HW acceleration anyway
		} catch (IllegalArgumentException e) {
		    e.printStackTrace();
		} catch (IllegalAccessException e) {
		    e.printStackTrace();
		} catch (InvocationTargetException e) {
		    e.printStackTrace();
		}

        
        // stay awake...
        if (BreadBootReceiver.stayAwake) {
        	getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
            mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
            //mWakeLock.acquire();
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
	        		if(doit){
	        			// ok, update the screen
	        		}
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
    	super.onPause();
    	// save settings - just in case user changed font size
    	BreadBootReceiver.saveSettings();
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
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.menuverse, menu);
		return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	// goBack
    	myMenu = menu;
    	MenuItem mi = (MenuItem) menu.findItem(R.id.goBack);
    	if (BreadBootReceiver.sdkVersion < Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
    		mi.setIcon(R.drawable.left);
    	}
    	else {
    		if (BreadBootReceiver.ourTheme == android.R.style.Theme_Black_NoTitleBar) {
    			mi.setIcon(R.drawable.ics_back_dark);
    		} else {
    			mi.setIcon(R.drawable.ics_back);
    		}
    	}
    	mi = (MenuItem) menu.findItem(R.id.goSpeakVerse);
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
	public boolean onOptionsItemSelected(MenuItem itm) {
    	switch(itm.getItemId()) {
    	case R.id.goBack:
    		// go back to previous activity
    		finish();
    		break;
    	case R.id.goSpeakVerse:
    		String words = stripHtml(formattedVerse);
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

    public void updateIcon(Boolean stop) {
    	if (myMenu != null) {
    		MenuItem mi = (MenuItem) myMenu.findItem(R.id.goSpeakVerse);
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

	@Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        try {
        	File sdCard = Environment.getExternalStorageDirectory();
        	File dir = new File (sdCard.getAbsolutePath() + "/BreadbyFaith");
        	if(!dir.exists()) {
        		if (!dir.mkdir()) {
        			// cannot make directory???
        			Log.e("BBFV","Cannot make folder:"+dir.getAbsolutePath());
        		}
        	}
        	bbfv = new File(dir, "bbfvcache.txt");
        }
        catch (Exception e1) {
        	bbfv = new File("bbfvcache.txt");
        }
        // save temporary bbf - this is a new cache
        if (!checkCacheExists(bbfv))
        	updateCache(bbfv, emptyVerse);
        
        // try fetch new verse
        fetchVerse(bbfv);
    }

    // This is the background task that does the fetching from internet
    // It updates the cache with the text
    // After execution it calls the refresh which refreshes from cache
    private class DownloadVersePageTask extends AsyncTask<String, Void, String> {
		@Override
		protected String doInBackground(String... verses) {
			String myVerse = emptyVerse;
			for (String myaddress : verses) {
    			URL url;
				try {
					url = new URL(url1 + URLEncoder.encode(myaddress, "UTF-8"));
	    			String eol = System.getProperty("line.separator");
	    			Log.i("BBF","Get: "+url.toString());
	    			BufferedReader input = new BufferedReader(new InputStreamReader(url.openStream()));
	    			String line;
	    			StringBuffer buffer = new StringBuffer();
	    			StringBuffer vbuffer = new StringBuffer();
	    			if ("KJV".equals(BreadBootReceiver.verseTranslation)) {
	    				buffer.append(myaddress + "|");
	    			} else {
	    				buffer.append(myaddress + eol);
	    			}
	    			while ((line = input.readLine()) != null) {
	    				buffer.append(line + eol);
	    				vbuffer.append(line + eol);
	    			}
	    			myVerse = buffer.toString();
	    			Log.i("BBF-IO","Got Verse+: "+myVerse);
	    			updateCache(bbfv, myVerse);
	    			actualVerse = vbuffer.toString();
				} catch (MalformedURLException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return "done";
		}

		@Override
		protected void onPostExecute(String result) {
			refreshVerse();
		}
	}

    private void fetchVerse(File bbfc) {
		String myVerse = emptyVerse;
		gotLiveVerse = false;
		
		// if cache available - use that
		Log.i("BBF","No connection, default verse");
        try {
        	File sdCard = Environment.getExternalStorageDirectory();
        	File dir = new File (sdCard.getAbsolutePath() + "/BreadbyFaith");
        	if(!dir.exists()) {
        		if (!dir.mkdir()) {
        			// cannot make directory???
        			Log.e("BBFV","Cannot make folder:"+dir.getAbsolutePath());
        		}
        	}
        	bbfv = new File(dir, "bbfvcache.txt");
        }
        catch (Exception e1) {
        	bbfv = new File("bbfvcache.txt");
        }
        // save temporary bbf - this is a new cache
        if (!checkCacheExists(bbfv)) {
        	updateCache(bbfv, emptyVerse);
        	myVerse = emptyVerse;
	        lAddress = "John 3:16"; //+ " " + (String)getBaseContext().getText(R.string.cache_verse);
        }
        else {
        	myVerse = getFileContents(bbfv);
        	if(lAddress == null) {
        		lAddress = "John 3:16";
        		address = "Network Error Fetching verse. Please Refresh and try again.";
        	}
        	// get verse from text, first line = strong verse strong...
        	//lAddress = address; //+ " " + (String)getBaseContext().getText(R.string.cache_verse);
        }

        // if cache not same as request, try fetch
        if (!lAddress.equals(address)) {
	    	try {
	    		// GET (address)
	    		if ((address.length() > 30) ||
	    			(address.contains(";")) ||
	    			(address.toLowerCase(Locale.US).contains("update")) ||
	    			(address.toLowerCase(Locale.US).contains("select")) ||
	    			(address.toLowerCase(Locale.US).contains("grant")) ||
	    			(address.toLowerCase(Locale.US).contains("delete")) ||
	    			(address.toLowerCase(Locale.US).contains("create"))
	    			){
	    			address = "Illegal access!";
	    			myVerse = "1 The bible verse you asked for is not valid" +
	    			          "2 Do not try to subvert this system" +
	    			          "3 All illegal access is logged";
	    		}
	    		else {
	    			getBaseContext();
					ConnectivityManager conMgr = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);  
	    			NetworkInfo info= conMgr.getActiveNetworkInfo();
	    			if ((info != null) && (info.isConnected())) {
	    				myVerse = fetchVerse;
	    				// need to spawn task to fetch bread in ICS etc
	        			DownloadVersePageTask task = new DownloadVersePageTask();
	        			task.execute(new String[] { address });
	        			gotLiveVerse = true;
	    			}
	    		}
	    	}
	    	catch (Exception ex) {
	    		address = (String)getBaseContext().getText(R.string.lookup_fail);
	    		myVerse = emptyVerse;
	    	}
        }
		
    	actualVerse = myVerse;
    	if (!gotLiveVerse) {
    		refreshVerse();
    	}
    }
    
    private String getFileContents(File bbf) {
    	//Read text from file
    	StringBuilder text = new StringBuilder();
    	BufferedReader br = null;
    	try {
    	    br = new BufferedReader(new FileReader(bbf));
    	    String line;
    	    // read first line for address
    	    lAddress = br.readLine();
    	    // now keep rest same as it was
    	    while ((line = br.readLine()) != null) {
    	        text.append(line);
    	        text.append('\n');
    	    }
    	    Log.i("BBF-IO","Fetch Verse+: "+text.toString());
    	}
    	catch (IOException e) {
    	    //You'll need to add proper error handling here
    		text.append(emptyVerse);
    	}
    	finally {
    		try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
    	}
    	return text.toString();
    }

	// Function to show the data received
    private void refreshVerse() {
    	formattedVerse = formatVerse(actualVerse);
    	wv1.loadDataWithBaseURL("", formattedVerse, "text/html", "UTF-8", "");
    }
    
    // Simple formatting of verse
    private String formatVerse(String text) {
    	String htmlbegin = "<div style=\"color:#";
    	if (BreadBootReceiver.ourTheme == android.R.style.Theme_Black_NoTitleBar)
    		htmlbegin += "FFFFFF; background-color:#000000;\">";
    	else
    		htmlbegin += "000000; background-color:#ffffff;\">";
    	String htmlend = " </div>";
    	String myVerse = "";
    	Log.i("BBF-IO","IN: "+text.toString());
    	
    	myVerse = htmlbegin + text;
    	
    	myVerse += htmlend;
    	Log.i("BBF-IO","OUT: "+myVerse);
    	return myVerse;
    }
    
    // get cached data
    private boolean checkCacheExists(File bbfc) {
    	// check if cache file exists
		try {
			String eol = System.getProperty("line.separator");
			BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(bbfc)));
			String line;
			StringBuffer buffer = new StringBuffer();
			lAddress = input.readLine();
			while ((line = input.readLine()) != null) {
				buffer.append(line + eol);
			}
			actualVerse = buffer.toString();
			input.close();
			return true;

		} catch (Exception e) {
			return false;
		}
    }
    
    // update cache with new data
    private void updateCache(File bbfc, String bread) {
    	try {
    		String eol = System.getProperty("line.separator");
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(bbfc)));
			writer.write(bread + eol);
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

}
