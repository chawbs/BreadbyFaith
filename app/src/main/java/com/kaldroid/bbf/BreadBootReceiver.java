package com.kaldroid.bbf;

/*
 * @package: com.kaldroid.bbf
 * @activity: BreadBootReceiver
 * @author: Kaldroid (kaldroid.co.uk)
 * @license: GNU/GPL
 * @description: Broadcast receiver to get wake-up when device first boots
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

public class BreadBootReceiver extends BroadcastReceiver {

	public static int ourTheme = android.R.style.Theme_Black_NoTitleBar;
	public static int ourBGColor = android.R.color.black;
	public static int ourFGColor = android.R.color.white;
	public static int refreshInt = 60;
	public static int transEffectRight = R.anim.slide_in_right;
	public static int transEffectLeft = R.anim.slide_in_left;
	public static Uri notifyUri = null;
	public static boolean debugMe = false;
	public static int sdkVersion = android.os.Build.VERSION.SDK_INT;
	public static String sdkRelease = android.os.Build.VERSION.RELEASE;
	public static String verseLookup = "http://bbf.fbcxt.org/getKJVpassage.php?passage=";
	public static String verseTranslation = "KJV";
	public static String[] bibleTranslations = null;
	public static String[] bibleLookups = null;
	public static boolean stayAwake = true;
	public static String bcUpdateEvent = "com.kaldroid.bbf.event.UPDATE";
	public static String bcSpeechDone = "com.kaldroid.bbf.event.SPEECHDONE";
	public static float scale = 100;
	public static int daysToLoad = 5;
	public static String prevVersion = "";
	private static final String TAG = "BCReceiver";

	//private static String bibleStack = "http://bbf.fbcxt.org/translations.php";

	public static void saveSettings() {
		try {
			File sdCard = Environment.getExternalStorageDirectory();
			File dir = new File (sdCard.getAbsolutePath() + "/BreadbyFaith");
			File sett = new File(dir, "settings.txt");
			String eol = System.getProperty("line.separator");

			// save our settings to file
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(sett)));
			writer.write(Integer.toString(refreshInt) + eol);
			writer.write(Integer.toString(ourTheme) + eol);
			writer.write(Integer.toString(transEffectLeft) + eol);
			writer.write(Integer.toString(transEffectRight) + eol);
			writer.write(notifyUri.toString() + eol);
			writer.write(verseTranslation + eol);
			writer.write(verseLookup + eol);
			writer.write(Boolean.toString(stayAwake) + eol);
			writer.write(Float.toString(scale) + eol);
			writer.write(Integer.toString(daysToLoad) + eol);
			writer.write(prevVersion + eol);
			writer.close();
		}
		catch (Exception ex) {
			Log.e(TAG, "Exception Saving:" + ex.getMessage());
		}
	}
	
	public static void restoreSettings(Context context) {
		// first try get bible translations available
		getBibleTranslations(context);
		try {
			Boolean fnd = false;
			Integer fint = 0;
			File sdCard = Environment.getExternalStorageDirectory();
			File dir = new File (sdCard.getAbsolutePath() + "/BreadbyFaith");
			File sett = new File(dir, "settings.txt");

			// zap last fetch time to force get from internet
			BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(sett)));
			String line = input.readLine();
			refreshInt = Integer.parseInt(line);
			line = input.readLine();
			ourTheme = Integer.parseInt(line);
			line = input.readLine();
			transEffectLeft = Integer.parseInt(line);
			line = input.readLine();
			transEffectRight = Integer.parseInt(line);
			try {
				line = input.readLine();
				notifyUri = Uri.parse(line);
			}
			catch (Exception e2) {
				notifyUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
			}
			try {
				line = input.readLine();
				if(line.contains("http"))
					verseLookup = line;
				else
					verseTranslation = line;
			}
			catch (Exception e2) {
				verseTranslation = "KJV";
			}
			try {
				line = input.readLine();
				if(line.contains("http"))
					verseLookup = line;
				else
					verseTranslation = line;
			}
			catch (Exception e2) {
				verseLookup = context.getText(R.string.verse_url).toString();
			}
			// DCJ check if new translations, if not, renew
			for(String tran : bibleLookups) {
				if (tran.equals(verseLookup))
					fnd = true;
			}
			for(String tran : bibleTranslations) {
				if (tran.equals(verseTranslation))
					break;
				fint = fint + 1;
			}
			if(!fnd) {
				if (fint < bibleLookups.length) {
					verseLookup = bibleLookups[fint];
				}
			}
			try {
				line = input.readLine();
				stayAwake = Boolean.parseBoolean(line);
			}
			catch (Exception e2) {
				stayAwake = true;
			}
			try {
				line = input.readLine();
				scale = Math.round(Float.parseFloat(line));
			}
			catch (Exception e2) {
				scale = (float) 100.0;
			}
			try {
				line = input.readLine();
				daysToLoad = Integer.parseInt(line);
				line = input.readLine();
				prevVersion = line;
			}
			catch (Exception e2) {
				daysToLoad = 5;
				prevVersion = "";
			}
			input.close();
		}
		catch (Exception ex) {
			Log.e(TAG, "Exception Restore:" + ex.getMessage());
			refreshInt = 60;
			ourTheme = android.R.style.Theme_Light_NoTitleBar;
			transEffectLeft = R.anim.slide_in_left;
			transEffectRight = R.anim.slide_in_right;
			notifyUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
			verseTranslation = "KJV";
			verseLookup = context.getText(R.string.verse_url).toString();
			stayAwake = true;
			scale = (float) 100.0;
			daysToLoad = 5;
			prevVersion = "";
			// now save it to write a good file
			saveSettings();
		}
		
	}
	
	public static void getBibleTranslations(Context context) {
		Resources res = context.getResources();
		bibleTranslations = res.getStringArray(R.array.bibleTranslations);
		bibleLookups = res.getStringArray(R.array.bibleLookups);
	}
	
	@Override
	public void onReceive(Context context, Intent intentIn) {
		// now set the timer for the next wake-up and check...
		restoreSettings(context);
		
		// schedule first lookup after 1 minute for quicker wakeup
		Intent intent = new Intent(context, AlarmReceiver.class);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 001000, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		if(refreshInt > 0)
			alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1 * (60 * 1000), refreshInt * 60 * 1000, pendingIntent);
	
		
	}

}
