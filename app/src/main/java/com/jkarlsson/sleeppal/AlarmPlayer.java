package com.jkarlsson.sleeppal;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.Vibrator;
import android.widget.Toast;

import java.io.FileDescriptor;

public class AlarmPlayer extends Service {
    public AlarmPlayer() {
    }

    MediaPlayer mp;
    Vibrator v;

    AudioManager audioManager;
    int originalVolume, audioMode;
    boolean speakerPhone;
    double volume;
    double currentVolume;

    private Handler handler = new Handler();

    private Runnable increaseVolume = new Runnable() {
        public void run() {
            currentVolume += 0.01;
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (int) Math.round(currentVolume * audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)), 0);

            if( currentVolume < volume) handler.postDelayed(increaseVolume, 1000);
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onCreate()
    {
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.shared_pref_key), Context.MODE_PRIVATE);
        String alarmTonePath = sharedPref.getString(String.valueOf(R.id.alarm_tone), RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString());
        Boolean increasing = sharedPref.getBoolean(String.valueOf(R.id.increasing), true);

        volume =  sharedPref.getInt(String.valueOf(R.id.volume), 10) / 10.0;

        audioMode = audioManager.getMode();
        speakerPhone = audioManager.isSpeakerphoneOn();

        audioManager.setMode(AudioManager.STREAM_MUSIC);
        audioManager.setSpeakerphoneOn(true);



        if(!alarmTonePath.equals("Silent")) {
            mp = MediaPlayer.create(this, Uri.parse(alarmTonePath));
            mp.setLooping(true);
            mp.start();

            if(increasing){
                currentVolume = 0;
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
                handler.postDelayed(increaseVolume, 1000);
            } else{
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (int) Math.round(volume * audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)), 0);
            }
        }

        v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        long[] pattern = {600, 600};

        Boolean vibrate = sharedPref.getBoolean(String.valueOf(R.id.vibrate), true);
        if(v.hasVibrator() && vibrate)
        {
            v.vibrate(pattern, 0);
        }


    }

    public void onDestroy()
    {
        if( mp != null) {
            mp.stop();
            mp.release();
        }
        if( v != null ){
            v.cancel();
        }

        currentVolume = 1;
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0);
        audioManager.setMode(audioMode);
        audioManager.setSpeakerphoneOn(speakerPhone);
    }
}
