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

public class SleepReminder extends Service {
    public SleepReminder() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    int id = 50050;

    public void onCreate()
    {
        Context context = getApplicationContext();

        SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.shared_pref_key), Context.MODE_PRIVATE);
        String beforeSleep = sharedPref.getString(String.valueOf(R.id.time_before_sleep_input), context.getString(R.string.time_before_sleep_default));
        Notification notification = new Notification.Builder(context)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.sleep_notif_first) + beforeSleep + getString(R.string.sleep_notif_last))
                .setSmallIcon(R.drawable.notification)
                .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(), 0))
                .setAutoCancel(true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .build();


        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(id, notification);

        stopSelf();
    }
}
