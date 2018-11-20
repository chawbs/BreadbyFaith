package com.kaldroid.bbf;

/*
 * @package: com.kaldroid.bbf
 * @activity: BreadByFaithSettings
 * @author: Kaldroid (kaldroid.co.uk)
 * @license: GNU/GPL
 * @description: Activity to handle setup of basic app settings
 */

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Messenger;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Spinner;
import android.widget.Toast;

public class BreadByFaithSettings extends Activity {
	
	private RingtoneManager ringtoneManager = null;
	private static final String TAG = "getRingtones";
	private int refreshInterval = 60;
	private int ourTheme = 0x01030128;
	private int transitionLeft = R.anim.slide_in_left;
	private int transitionRight = R.anim.slide_in_right;
	private Uri ringtone_uri = null;
	private int translation = 0;
	private boolean playMe = false;
	private boolean mustRefresh = false;
	private boolean wakeLock = true;
	private int zoomAmount = 100;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// flag to prevent play when first open
		playMe = false;
		// if we change versions we need to refetch everything
		mustRefresh = false;
		wakeLock = BreadBootReceiver.stayAwake;
		// choose different layout and theme for ICS+
		// hard-coded theme numbers
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
			setContentView(R.layout.breadsettings);
		else
			setContentView(R.layout.breadsettingsics);
		
		int position = 0;
		Spinner spinner = (Spinner) findViewById(R.id.spinnerRefresh);
	    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
	            this, R.array.refresh_interval, android.R.layout.simple_spinner_item);
	    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    spinner.setAdapter(adapter);
	    switch (BreadBootReceiver.refreshInt) {
	    case -1:
	    	position = 4;
	    	break;
	    case 30:
	    	position = 0;
	    	break;
	    case 60:
	    	position = 1;
	    	break;
	    case 120:
	    	position = 2;
	    	break;
	    case 3600:
	    	position = 3;
	    	break;
	    default:
	    	position = 0;
	    	break;
	    }
	    spinner.setSelection(position);
	    spinner.setOnItemSelectedListener(new MyOnItemSelectedListener());

		spinner = (Spinner) findViewById(R.id.spinnerTheme);
	    adapter = ArrayAdapter.createFromResource(
	            this, R.array.themes, android.R.layout.simple_spinner_item);
	    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    spinner.setAdapter(adapter);
	    switch (BreadBootReceiver.ourTheme) {
	    case android.R.style.Theme_Black_NoTitleBar:
	    	position = 0;
	    	break;
	    default:
	    	position = 1;
	    	break;
	    }
	    spinner.setSelection(position);
	    spinner.setOnItemSelectedListener(new MyOnItemSelectedListener());

		spinner = (Spinner) findViewById(R.id.spinnerTransition);
	    adapter = ArrayAdapter.createFromResource(
	            this, R.array.transitions, android.R.layout.simple_spinner_item);
	    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    spinner.setAdapter(adapter);
	    switch (BreadBootReceiver.transEffectLeft) {
	    case android.R.anim.fade_in:
	    	position = 0;
	    	break;
	    default:
	    	position = 1;
	    	break;
	    }
	    spinner.setSelection(position);
	    spinner.setOnItemSelectedListener(new MyOnItemSelectedListener());
	    
	    spinner = (Spinner)findViewById(R.id.spinnerTranslations);
	    adapter = ArrayAdapter.createFromResource(
	    		this, R.array.bibleTranslations, android.R.layout.simple_spinner_item);
	    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    spinner.setAdapter(adapter);
	    // crash reported 10-sep-2013 bibleTranslations = null
	    BreadBootReceiver.getBibleTranslations(getBaseContext());
	    if (BreadBootReceiver.bibleTranslations != null) {
	    	for(position=0; position<BreadBootReceiver.bibleTranslations.length; position++) {
	    		if(BreadBootReceiver.bibleTranslations[position].equals(BreadBootReceiver.verseTranslation)) {
	    			break;
	    		}
	    	}
	    	if (position == BreadBootReceiver.bibleTranslations.length) {
	    		position = 0;
	    	}
	    }
	    else {
	    	position = 0;
	    }
	    spinner.setSelection(position);
	    translation = position;
	    spinner.setOnItemSelectedListener(new MyOnItemSelectedListener());
	    
	    ringtone_uri = BreadBootReceiver.notifyUri;
	    if (ringtone_uri == null) {               
	    	ringtone_uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
	    	BreadBootReceiver.notifyUri = ringtone_uri;
	    }

	    ringtoneManager = new RingtoneManager(this);

	    ringtoneManager.setType(RingtoneManager.TYPE_NOTIFICATION);
	    ringtoneManager.setIncludeDrm(true);

	    Cursor ringtoneCursor = ringtoneManager.getCursor();
	    Log.v(TAG, "ringtones column count: " + ringtoneCursor.getColumnCount());

	    List<CharSequence> ringtoneList = new ArrayList<CharSequence>(); 
	    Log.v(TAG, "ringtones row count: " + ringtoneCursor.getCount());
	    // Add in SILENT
	    ringtoneList.add("Silent");
	    int x = ringtoneCursor.getCount();
	    position = 0;
	    for (int i = 0; i < x; i++) {
	    	try {
		    	if (ringtoneManager.getRingtoneUri(i).compareTo(ringtone_uri) == 0) {
		    		position = (i+1); // add extra because of silent
		    	}
		    	ringtoneList.add(ringtoneCursor.getString(RingtoneManager.TITLE_COLUMN_INDEX));
	    	}
	    	catch (Exception ex) {
	    		// failed to load this one, maybe DRM protected
	    	}
	    	ringtoneCursor.moveToNext(); 
	    }

	    spinner = (Spinner) findViewById(R.id.spinnerNotify);
	    adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, ringtoneList);
	    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    spinner.setAdapter(adapter);
	    spinner.setSelection(position);
	    spinner.setOnItemSelectedListener(new MyOnItemSelectedListener());

    	CheckBox checkbox = (CheckBox)findViewById(R.id.cbWakeLock);
    	checkbox.setChecked(wakeLock);
    	checkbox.setOnCheckedChangeListener(new MyOnCheckedChangeListener());
    	
		spinner = (Spinner) findViewById(R.id.spinnerFetch);
	    adapter = ArrayAdapter.createFromResource(
	            this, R.array.days_to_fetch, android.R.layout.simple_spinner_item);
	    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    spinner.setAdapter(adapter);
	    position = BreadBootReceiver.daysToLoad-1;
	    spinner.setSelection(position);
	    spinner.setOnItemSelectedListener(new MyOnItemSelectedListener());

		spinner = (Spinner) findViewById(R.id.spinnerZoom);
	    adapter = ArrayAdapter.createFromResource(
	            this, R.array.zooms, android.R.layout.simple_spinner_item);
	    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    spinner.setAdapter(adapter);
	    zoomAmount = (int)Math.round(BreadBootReceiver.scale);
	    switch(zoomAmount) {
	    case 75:
	    	position = 0;
	    	break;
	    case 100:
	    	position = 1;
	    	break;
	    case 125:
	    	position = 2;
	    	break;
	    case 150:
	    	position = 3;
	    	break;
	    case 175:
	    	position = 4;
	    	break;
	    case 200:
	    	position = 5;
	    	break;
	    case 225:
	    	position = 6;
	    	break;
	    case 250:
	    	position = 7;
	    	break;
    	default:
    		position = 2;
    		break;
	    }
	    spinner.setSelection(position);
	    spinner.setOnItemSelectedListener(new MyOnItemSelectedListener());

	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.menusettings, menu);
	    return true;
	} 
	
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	// goSaveSettings
    	MenuItem mi = (MenuItem) menu.findItem(R.id.goSaveSettings);
    	if (BreadBootReceiver.sdkVersion < Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
    		mi.setIcon(R.drawable.save);
    	}
    	else {
    		if (BreadBootReceiver.ourTheme == android.R.style.Theme_Black_NoTitleBar) {
    			mi.setIcon(R.drawable.ics_save_dark);
    		} else {
    			mi.setIcon(R.drawable.ics_save);
    		}
    	}
    	return super.onPrepareOptionsMenu(menu);
    }
    
    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // safe to reset after we have created spinners or the value change triggers a play audio!
	    playMe = true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
      switch (item.getItemId()) {
      case R.id.goSaveSettings:
        Toast.makeText(this, "Saved Settings. Restarting...", Toast.LENGTH_SHORT).show();
        saveAndRestart();
        break;

      default:
        break;
      }

      return true;
    } 
	
    public class MyOnCheckedChangeListener implements OnCheckedChangeListener {

		public void onCheckedChanged(CompoundButton cb, boolean isSet) {
			if(cb.getId() == R.id.cbWakeLock) {
				wakeLock = isSet;
			}
			
		}
    	
    }
    
	public class MyOnItemSelectedListener implements OnItemSelectedListener {

		public void onItemSelected(AdapterView<?> parent,
				View view, int pos, long id) {
			if (parent.getId() == R.id.spinnerRefresh) {
				switch (pos) {
				case 0:
					if (BreadBootReceiver.debugMe) {
						refreshInterval = 1;
					}
					else {
						refreshInterval=30;
					}
					break;
				case 1:
					refreshInterval=60;
					break;
				case 2:
					refreshInterval=120;
					break;
				case 3:
					refreshInterval=3600;
					break;
				case 4:
					refreshInterval=-1;
					break;
				default:
					refreshInterval=60;
					break;
				}
			}
			else if (parent.getId() == R.id.spinnerTheme) {
				if (pos == 0) {
					// dark
					ourTheme = android.R.style.Theme_Black_NoTitleBar;
					BreadBootReceiver.ourBGColor = android.R.color.black;
					BreadBootReceiver.ourFGColor = android.R.color.white;
				}
				else {
					// light
					ourTheme = android.R.style.Theme_Light_NoTitleBar;
					BreadBootReceiver.ourBGColor = android.R.color.white;
					BreadBootReceiver.ourFGColor = android.R.color.black;
				}
			}
			else if (parent.getId() == R.id.spinnerTransition) {
				if (pos == 0) {
					transitionLeft = android.R.anim.fade_in;
					transitionRight = android.R.anim.fade_in;
				}
				else {
					transitionLeft = R.anim.slide_in_left;
					transitionRight = R.anim.slide_in_right;
				}
			}
			else if (parent.getId() == R.id.spinnerNotify) {
				if (pos == 0) {
					playMe = false;
					ringtone_uri = Uri.EMPTY;
				}
				else {
					ringtone_uri = ringtoneManager.getRingtoneUri(pos - 1);
				}
				// play the notification
				if (playMe) {
					try {
						MediaPlayer mMediaPlayer = new MediaPlayer();
						mMediaPlayer.setDataSource(getBaseContext(), ringtone_uri);
						final AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
						if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
							mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
							mMediaPlayer.setLooping(false);
							mMediaPlayer.prepare();
							mMediaPlayer.start();
						}
					}
					catch (Exception ex) {
					}
				}
			}
			else if (parent.getId() == R.id.spinnerTranslations) {
				translation = pos;
				mustRefresh = true;
			}
			else if (parent.getId() == R.id.goSaveSettings) {
		        Toast.makeText(getBaseContext(), "Saved Settings. Restarting...", Toast.LENGTH_SHORT).show();
		        saveAndRestart();				
			}
			else if (parent.getId() == R.id.spinnerFetch) {
				BreadBootReceiver.daysToLoad = pos+1;
			}
			else if(parent.getId() == R.id.spinnerZoom) {
				switch(pos) {
				case 0:
					zoomAmount = 75;
					break;
				case 1:
					zoomAmount = 100;
					break;
				case 2:
					zoomAmount = 125;
					break;
				case 3:
					zoomAmount = 150;
					break;
				case 4:
					zoomAmount = 175;
					break;
				case 5:
					zoomAmount = 200;
					break;
				case 6:
					zoomAmount = 225;
					break;
				case 7:
					zoomAmount = 250;
					break;
				}
				BreadBootReceiver.scale = (float)zoomAmount;
			}
		}

	    public void onNothingSelected(AdapterView<?> parent) {
	      // Do nothing.
	    }
	}
	
	public void saveSettingsHandler(View view) {
		// set refresh interval
		Toast.makeText(getBaseContext(), "Saved Settings. Restarting...", Toast.LENGTH_SHORT).show();
		saveAndRestart();
	}
	
	public void saveAndRestart() {
   		// set up a new alarm
		Intent intent = new Intent(getBaseContext(), AlarmReceiver.class);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(getBaseContext(), 001000, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(pendingIntent);
		if(refreshInterval > 0)
			alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1 * (60 * 1000), refreshInterval * 60 * 1000, pendingIntent);
		// save everything
		BreadBootReceiver.refreshInt = refreshInterval;
		// set theme
		BreadBootReceiver.ourTheme = ourTheme;
		// set transition
		BreadBootReceiver.transEffectLeft = transitionLeft;
		BreadBootReceiver.transEffectRight = transitionRight;
		// set ringtone
		BreadBootReceiver.notifyUri = ringtone_uri;
		// set translation
		try {
			BreadBootReceiver.verseTranslation = BreadBootReceiver.bibleTranslations[translation];
			BreadBootReceiver.verseLookup = BreadBootReceiver.bibleLookups[translation];
		}
		catch (Exception ex) {
			BreadBootReceiver.verseTranslation = "KJV";
			BreadBootReceiver.verseLookup = getBaseContext().getText(R.string.verse_url).toString();
		}
		// wake lock
		BreadBootReceiver.stayAwake = wakeLock;
		// save
		BreadBootReceiver.saveSettings();
		// and go home...

        // zap timestamp to force reget
		if(mustRefresh) {
			File sdCard = Environment.getExternalStorageDirectory();
			File dir = new File (sdCard.getAbsolutePath() + "/BreadbyFaith");
			File last = new File(dir, "lastTimestamp.txt");
			last.delete();
			
			// send refresh now
			Intent service = new Intent(getBaseContext(), BreadCheckService.class);
			Messenger messenger = null;
			service.putExtra("MESSENGER", messenger);
			getBaseContext().startService(service);
		}

		Intent res = new Intent();
        if (getParent() == null) {
            setResult(Activity.RESULT_OK, res);
        } else {
            getParent().setResult(Activity.RESULT_OK, res);
        }
		finish();
	}

}
