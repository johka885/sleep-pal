package com.jkarlsson.sleeppal;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
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
    int originalVolume;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onCreate()
    {
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
        audioManager.setMode(AudioManager.MODE_IN_CALL);
        audioManager.setSpeakerphoneOn(true);

        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.shared_pref_key), Context.MODE_PRIVATE);
        String savedValue = sharedPref.getString(String.valueOf(R.id.alarm_tone), RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString());

        if(!savedValue.equals("Silent")) {
            mp = MediaPlayer.create(this, Uri.parse(savedValue));
            mp.setLooping(true);
            mp.start();
        }

        v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        long[] pattern = {0, 1000, 600};

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

        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0);
    }
}
