package com.kaldroid.bbf;

import java.util.HashMap;
import java.util.Locale;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.widget.Toast;

public class BreadSpeakService extends Service implements TextToSpeech.OnInitListener  {

	private TextToSpeech myTTS = null;
	private Context context = null;
	private String speech;
	private Intent speechDone;
	
	@Override
	public void onCreate() {
		this.context  = this.getApplicationContext();
		myTTS = new TextToSpeech(this.context, this);
		speechDone = new Intent(BreadBootReceiver.bcSpeechDone);
		super.onCreate();
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle bundle = intent.getExtras();
        speech = bundle.getString("SPEAKTHIS");
    	return START_STICKY;
	}
	
	@Override
    public void onDestroy() {
		if(myTTS != null) {
			myTTS.stop();
			myTTS.shutdown();
			myTTS = null;
		}
        super.onDestroy();
    }

    //setup TTS
    @SuppressWarnings("deprecation")
	public void onInit(int initStatus) {
            //check for successful instantiation
        if (initStatus == TextToSpeech.SUCCESS) {
        	if(myTTS!=null) {
        		int result = myTTS.setLanguage(Locale.UK);
        		if(result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
        			result = myTTS.setLanguage(Locale.US);
        		}
        		if(result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
        			result = myTTS.setLanguage(Locale.ENGLISH);
        		}
        		if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
        			myTTS.setOnUtteranceCompletedListener(new OnUtteranceCompletedListener() {
        				public void onUtteranceCompleted(String uttID) {
        					sendBroadcast(speechDone);
        					if(myTTS != null) {
        						myTTS.stop();
        						myTTS.shutdown();
        						myTTS = null;
        					}
        					stopSelf();
        					
        				}
        			});
        			HashMap<String, String> params = new HashMap<String, String>();
        			params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,"BBFDone");
                    myTTS.speak(speech, TextToSpeech.QUEUE_FLUSH, params);
                }
        	}
        }
        else if (initStatus == TextToSpeech.ERROR) {
            Toast.makeText(this, "Sorry! Text To Speech failed...", Toast.LENGTH_LONG).show();
        }
    }
}
