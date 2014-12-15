package com.jkarlsson.sleeppal;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.os.PowerManager;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Calendar;


public class AlarmActivity extends FragmentActivity {

    static Boolean snoozing = false;
    Window window;
    Intent service;
    int id = 1000;
    PowerManager.WakeLock wakeLock;

    ImageView alarmClock;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock((PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "SleepPal");
        wakeLock.acquire();

        Bundle extras = getIntent().getExtras();
        Boolean silent = (extras != null) && extras.getBoolean("silent", false);

        window = this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        setContentView(R.layout.activity_alarm);

        Button dismiss = (Button) findViewById(R.id.dismiss);
        Button snooze = (Button) findViewById(R.id.snooze);

        if(MainActivity.buttonColor == 1){
            dismiss.setBackground(getResources().getDrawable(R.drawable.button_red));
            dismiss.setTextColor(Color.parseColor("#FFD9D9"));

            snooze.setBackground(getResources().getDrawable(R.drawable.button_red));
            snooze.setTextColor(Color.parseColor("#FFD9D9"));
        }

        alarmClock = (ImageView) findViewById(R.id.alarm_clock);

        RotateAnimation anim = new RotateAnimation(-14f, 14f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        anim.setInterpolator(new LinearInterpolator());
        anim.setRepeatCount(Animation.INFINITE);
        anim.setDuration(50);
        anim.setRepeatMode(Animation.REVERSE);


        if(!silent){
            alarmClock.startAnimation(anim);
            Intent resultIntent = new Intent(this, AlarmActivity.class);
            PendingIntent pendingIntent =
                    PendingIntent.getActivity(
                            this,
                            0,
                            resultIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );

            Notification notification = new Notification.Builder(this)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(getString(R.string.dismiss_message))
                    .setSmallIcon(R.drawable.notification)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .build();

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(id, notification);

            service = new Intent(this, AlarmPlayer.class);
            startService(service);
        }


        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.shared_pref_key), Context.MODE_PRIVATE);
        Boolean notificationAlarm = sharedPref.getBoolean(String.valueOf(id), true);

        TextView nextEvent = (TextView) findViewById(R.id.upcoming_event);
        if(notificationAlarm){

            Calendar now = Calendar.getInstance();
            Cursor events = CalendarUpdate.getFirstAppointment(this, now);
            if( events.moveToFirst()){
                String upcoming = events.getString(2);
                long millis = events.getLong(3);
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(millis);

                upcoming = CalendarUpdate.calendarNicePrint(c) + " - " + upcoming;
                nextEvent.setText(upcoming);
            } else{
                String upcoming = getString(R.string.no_upcoming_events);
                nextEvent.setText(upcoming);
            }
            events.close();
        } else{
            String upcoming = getString(R.string.no_upcoming_events);
            nextEvent.setText(upcoming);
        }

        super.onCreate(savedInstanceState);
    }

    public void dismissAlarm(View view) {

        snoozing = false;
        alarmClock.setAnimation(null);

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(id);

        if(service != null) stopService(service);

        Intent i = new Intent("com.jkarlsson.sleeppal.SETTINGS_CHANGED");
        sendBroadcast(i);

        wakeLock.release();
        this.finish();
    }

    public void snoozeAlarm(View view) {

        snoozing = true;
        alarmClock.setAnimation(null);

        if(service != null) stopService(service);

        Intent snooze = new Intent(this, AlarmActivity.class);
        PendingIntent alarm = PendingIntent.getActivity(this, 0, snooze, PendingIntent.FLAG_UPDATE_CURRENT);
        snooze.putExtra("silent", false);

        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.shared_pref_key), Context.MODE_PRIVATE);
        String snoozeTime = sharedPref.getString(String.valueOf(R.id.snooze_time), "");

        int m = Integer.parseInt(snoozeTime.split(":")[0]);
        int s = Integer.parseInt(snoozeTime.split(":")[1]);

        Calendar now = Calendar.getInstance();
        now.add(Calendar.MINUTE, m);
        now.add(Calendar.SECOND, s);

        AlarmManager am = (AlarmManager) this.getSystemService(ALARM_SERVICE);
        am.set(AlarmManager.RTC_WAKEUP, now.getTimeInMillis(), alarm);

        Intent resultIntent = new Intent(this, AlarmActivity.class);

        resultIntent.setAction("viewForDismiss"); //Make intents distinct
        resultIntent.putExtra("silent", true);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );


        Notification notification = new Notification.Builder(this)
                .setContentTitle(getString(R.string.snoozing_ongoing_message) + CalendarUpdate.calendarNicePrint(now))
                .setContentText(getString(R.string.dismiss_message))
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(id, notification);

        wakeLock.release();
        this.finish();
    }
}
