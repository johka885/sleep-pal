package com.jkarlsson.sleeppal;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.widget.Toast;

import java.util.Calendar;

public class SleepReminder extends Service {
    public SleepReminder() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    int id = 1000;

    public void onCreate()
    {
        Context context = getApplicationContext();

        SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.shared_pref_key), Context.MODE_PRIVATE);
        String beforeSleep = sharedPref.getString(String.valueOf(R.id.time_before_sleep_input), "");

        int h = Integer.parseInt(beforeSleep.split(":")[0]);
        int m = Integer.parseInt(beforeSleep.split(":")[1]);

        Calendar now = Calendar.getInstance();

        now.add(Calendar.HOUR_OF_DAY, h);
        now.add(Calendar.MINUTE, m);

        Intent i = new Intent("com.jkarlsson.sleeppal.SETTINGS_CHANGED");
        PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 0);

        Notification notification = new Notification.Builder(context)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.sleep_notif_first) + CalendarUpdate.calendarNicePrint(now) + getString(R.string.sleep_notif_last))
                .setSmallIcon(R.drawable.notification)
                .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(), 0))
                .setAutoCancel(true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setContentIntent(pi)
                .setDeleteIntent(pi)
                .build();


        Toast.makeText(this, "notifying", Toast.LENGTH_LONG).show();
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(id, notification);


        stopSelf();
    }
}
