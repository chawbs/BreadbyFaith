package com.kaldroid.bbf;

/*
 * @package: com.kaldroid.bbf
 * @activity: HelpBread
 * @author: Kaldroid (kaldroid.co.uk)
 * @license: GNU/GPL
 * @description: Activity to show basic help - need to expand
 */

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.text.Html;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager.LayoutParams;
import android.widget.TextView;

public class HelpBread extends Activity {
	private Menu myMenu = null;
	private BroadcastReceiver bcUpdateEvent = null;
	private BroadcastReceiver bcSpeechDone = null;
	private WakeLock mWakeLock;
	private static PowerManager pm;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		if (BreadBootReceiver.sdkVersion < Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
			setTheme(BreadBootReceiver.ourTheme);
		} else {
			if (BreadBootReceiver.ourTheme == android.R.style.Theme_Black_NoTitleBar)
				setTheme(0x01030128);
			else
				setTheme(0x0103012b);
		}
		super.onCreate(savedInstanceState);
		if (BreadBootReceiver.sdkVersion < Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
			setContentView(R.layout.helpbread);
		else
			setContentView(R.layout.helpbreadics);
		
		TextView tv = (TextView)findViewById(R.id.helpView2);
		CharSequence txt = tv.getText();
		StringBuffer sb = new StringBuffer(txt);
		try {
			sb.append("\n\n");
			sb.append((String)getBaseContext().getText(R.string.app_ver));
			sb.append(getBaseContext().getPackageManager().getPackageInfo(getBaseContext().getPackageName(), 0 ).versionName);
			sb.append("\n");
			sb.append((String)getBaseContext().getText(R.string.sdk_ver));
			sb.append(BreadBootReceiver.sdkRelease);
		} catch (NameNotFoundException e) {
			//e.printStackTrace();
		}
		tv.setText(sb);
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
	        		if (doit) {
	        			// must update something... not!
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
    		TextView tv = (TextView)findViewById(R.id.helpView2);
    		String txt = (String) tv.getText();
    		String words = stripHtml(txt);
            speakWords(words);
    		break;
    	}
		return true;
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
    
    private String stripHtml(String speech) {
    	return Html.fromHtml(speech.replaceAll("s/<(.*?, replacement)>//g","")).toString();
    }
    
    private void speakWords(String speech) {
        Intent speaker = new Intent(getBaseContext(), BreadSpeakService.class);
		speaker.putExtra("SPEAKTHIS", speech);
		if (getBaseContext().stopService(speaker) == false) {
			getBaseContext().startService(speaker);
			// now change menu item
			updateIcon(true);
		}
		else {
			updateIcon(false);
		}
	}

    public void updateIcon(Boolean stop) {
    	if (myMenu.equals(null))
    		return;
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
