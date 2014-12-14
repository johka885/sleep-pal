package com.jkarlsson.sleeppal;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import java.util.Calendar;


public class AlarmActivity extends FragmentActivity {

    static Boolean snoozing = false;
    Window window;
    Intent service;
    int id = 509;
    PowerManager.WakeLock wakelock;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        wakelock = pm.newWakeLock((PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "MyService");
        wakelock.acquire();

        Bundle extras = getIntent().getExtras();
        Boolean silent = (extras != null) && getIntent().getExtras().getBoolean("silent", false);

        window = this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        setContentView(R.layout.activity_alarm);

        if(!silent){
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
                    .setContentText("Press here to dismiss")
                    .setSmallIcon(R.drawable.notification)
                            // .setFullScreenIntent(pendingIntent, true)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .build();

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(id, notification);

            service = new Intent(this, AlarmPlayer.class);
            startService(service);
        }

        super.onCreate(savedInstanceState);
    }

    public void dismissAlarm(View view) {
        snoozing = false;

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(id);

        if(service != null) stopService(service);

        Intent i = new Intent("com.jkarlsson.sleeppal.SETTINGS_CHANGED");
        sendBroadcast(i);

        wakelock.release();
        this.finish();
    }

    public void snoozeAlarm(View view) {

        snoozing = true;

        if(service != null) stopService(service);

        Intent snooze = new Intent(this, AlarmActivity.class);
        PendingIntent alarm = PendingIntent.getActivity(this, 0, snooze, PendingIntent.FLAG_UPDATE_CURRENT);
        snooze.putExtra("silent", false);

        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.shared_pref_key), Context.MODE_PRIVATE);
        String snoozeTime = sharedPref.getString(String.valueOf(R.id.snooze_time), getString(R.string.snooze_time_default));

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
                .setContentTitle("Snoozing - ringing again " + CalendarUpdate.calendarNicePrint(now))
                .setContentText("Press here to dismiss")
                .setSmallIcon(R.drawable.ic_launcher)
                        // .setFullScreenIntent(pendingIntent, true)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(id, notification);

        wakelock.release();
        this.finish();
    }
}
