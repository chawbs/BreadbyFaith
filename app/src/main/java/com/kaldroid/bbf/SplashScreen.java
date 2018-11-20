package com.kaldroid.bbf;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SplashScreen extends Activity  implements OnTouchListener {

	private Timer myTimer = null;
	private int myTicks = 0;
	private String myVer = "";
	private GestureDetector gest = null;
	private boolean mained = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		BreadBootReceiver.restoreSettings(this);
		try {
			myVer = getBaseContext().getPackageManager().getPackageInfo(getBaseContext().getPackageName(), 0 ).versionName;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		if (BreadBootReceiver.sdkVersion < Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
			setTheme(BreadBootReceiver.ourTheme);
		} else {
			if (BreadBootReceiver.ourTheme == android.R.style.Theme_Black_NoTitleBar)
				setTheme(0x01030128);
			else
				setTheme(0x0103012b);
		}
		super.onCreate(savedInstanceState);
		if(BreadBootReceiver.sdkVersion < Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
			setContentView(R.layout.upgradesplash);
		else
			setContentView(R.layout.upgradesplashics);
		gest = new GestureDetector(this, new SplashGestureListener());
		LinearLayout ll = (LinearLayout)findViewById(R.id.splashLayout);
		ll.setOnTouchListener(this);
	}
	
	@Override
	public void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		if (myVer.equals(BreadBootReceiver.prevVersion)) {
			launchMain();
		}
		else {
			myTimer = new Timer();
			myTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					TimerMethod();
				}
			}, 0, 2000);
		}
	}
	
	@Override
	public void onBackPressed() {
		// don't do anything - force user to press icon
	}
	
	protected void TimerMethod() {
		this.runOnUiThread(Timer_Tick);
	}
	
	private Runnable Timer_Tick = new Runnable() {
		public void run() {
			if(myTicks < 2) {
				String[] updates = getResources().getStringArray(R.array.updates);
				StringBuffer sb = new StringBuffer("Bread by Faith (");
				sb.append(myVer);
				sb.append(")");
				TextView tv = (TextView)findViewById(R.id.upgradeTextHead);
				tv.setText(sb);
				sb = new StringBuffer();
				for(String update : updates) {
					if(update.contains("**"))
						sb.append("\n" + update + "\n");
					else
						sb.append("- " + update + "\n");
				}
				sb.append("\n");
				tv = (TextView)findViewById(R.id.upgradeText);
				tv.setText(sb);
			}
			myTicks++;
			if((myTicks > 20) || (myVer.equals(BreadBootReceiver.prevVersion))) {
				launchMain();
			}
		}
	};
	
	private Runnable JustRunMain = new Runnable() {
		public void run() {
			launchMain();
		}
	};
	
	public void dismissSplash(View v) {
		launchMain();
	}

	protected void launchMain() {
		if(mained) return;
		mained = true;
		BreadBootReceiver.prevVersion = myVer;
		BreadBootReceiver.saveSettings();
		Intent main = new Intent(getBaseContext(), BreadByFaithActivity.class);
		try {
			if(myTimer != null)
				myTimer.cancel();
		}
		catch (Exception ex) {
			;
		}
		finish();
		startActivity(main);
	}

	public boolean onTouch(View arg0, MotionEvent arg1) {
		return false;
	}

	class SplashGestureListener extends GestureDetector.SimpleOnGestureListener {
		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2,  
				final float velocityX, final float velocityY) {
			launchMain();
			return true;
		}
	}

}
