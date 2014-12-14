package com.jkarlsson.sleeppal;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class CalendarUpdate extends BroadcastReceiver {
    public CalendarUpdate() {
    }

    int id = 1000;

    Boolean[] alarmDays = new Boolean[8];
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.shared_pref_key), Context.MODE_PRIVATE);
        alarmDays[Calendar.MONDAY] = sharedPref.getBoolean(String.valueOf(R.id.alarm_monday), true);
        alarmDays[Calendar.TUESDAY] = sharedPref.getBoolean(String.valueOf(R.id.alarm_tuesday), true);
        alarmDays[Calendar.WEDNESDAY] = sharedPref.getBoolean(String.valueOf(R.id.alarm_wednesday), true);
        alarmDays[Calendar.THURSDAY] = sharedPref.getBoolean(String.valueOf(R.id.alarm_thursday), true);
        alarmDays[Calendar.FRIDAY] = sharedPref.getBoolean(String.valueOf(R.id.alarm_friday), true);
        alarmDays[Calendar.SATURDAY] = sharedPref.getBoolean(String.valueOf(R.id.alarm_saturday), true);
        alarmDays[Calendar.SUNDAY] = sharedPref.getBoolean(String.valueOf(R.id.alarm_sunday), true);

        if(alarmDays[Calendar.MONDAY] ||
                alarmDays[Calendar.TUESDAY] ||
                alarmDays[Calendar.WEDNESDAY] ||
                alarmDays[Calendar.THURSDAY] ||
                alarmDays[Calendar.FRIDAY] ||
                alarmDays[Calendar.SATURDAY] ||
                alarmDays[Calendar.SUNDAY] ) {
            setAlarmNew(context);
        } else {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(id);
        }
    }


    public void setAlarmNew(Context context) {
        if(AlarmActivity.snoozing) return;

        SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.shared_pref_key), Context.MODE_PRIVATE);
        String time1 = sharedPref.getString(String.valueOf(R.id.latest_wake_up_time), context.getString(R.string.latest_wake_up_time_default));
        String time2 = sharedPref.getString(String.valueOf(R.id.time_before_wake_up), context.getString(R.string.time_before_wake_up_default));

        Boolean eventAlarm = sharedPref.getBoolean(String.valueOf(R.id.alarm_appointments), true);
        Boolean everyDayAlarm = sharedPref.getBoolean(String.valueOf(R.id.alarm_no_appointments), true);

        if( !eventAlarm && !everyDayAlarm){
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(id);
            return;
        }

        Calendar alarmTime = Calendar.getInstance();

        String recurringMsg = context.getString(R.string.good_morning);
        String appointmentMsg = "";
        String notificationMsg = "";
        Boolean validAlarm = false;

        for(int i = 0; i < 7; ++i){
            Calendar appointment = Calendar.getInstance();
            appointment.add(Calendar.DAY_OF_YEAR, i);
            Calendar recurring = Calendar.getInstance();
            recurring.add(Calendar.DAY_OF_YEAR, i);

            validAlarm = false;


            Cursor cursor = getFirstAppointment(context, appointment);

            if(eventAlarm && cursor.moveToFirst()) {
                appointment.setTimeInMillis(cursor.getLong(3));

                appointmentMsg = calendarToWeekDay(appointment) + " " + calendarNicePrint(appointment) + ": " + cursor.getString(2);

                appointment.add(Calendar.HOUR_OF_DAY, -Integer.parseInt(time2.split(":")[0]));
                appointment.add(Calendar.MINUTE, -Integer.parseInt(time2.split(":")[1]));

                if(alarmTime.before(appointment)){
                    validAlarm = true;
                } else{
                    ;
                }

                cursor.close();
            } else {
                appointment.add(Calendar.DAY_OF_YEAR, 20);
            }

            if(everyDayAlarm){
                recurring = getFirstRecurring(recurring, time1);
                if(recurring.before(appointment)) {
                    if (alarmTime.before(recurring)) {
                        validAlarm = true;
                    } else {
                        validAlarm = false;
                    }
                }
            } else {
                recurring.add(Calendar.DAY_OF_YEAR, 20);
            }

            if(i == 0){
                alarmTime.set(Calendar.HOUR_OF_DAY, 0);
                alarmTime.set(Calendar.MINUTE, 0);
            }
            if( !validAlarm){
                alarmTime.add(Calendar.DAY_OF_YEAR, 1);
                continue;
            }

            long millis = alarmTime.getTimeInMillis();

            if(appointment.before(recurring)){
                notificationMsg = appointmentMsg;
                alarmTime.setTimeInMillis(appointment.getTimeInMillis());
            } else{
                notificationMsg = recurringMsg;
                alarmTime.setTimeInMillis(recurring.getTimeInMillis());
            }

            if(!alarmDays[alarmTime.get(Calendar.DAY_OF_WEEK)]){
                alarmTime.setTimeInMillis(millis);
                alarmTime.add(Calendar.DAY_OF_YEAR, 1);
                continue;
            }
            break;
        }

        Boolean showNotification = sharedPref.getBoolean("showNotification", true);

        if(showNotification) {
            String notificationTitle = context.getString(R.string.next_alarm) + " " + calendarToWeekDay(alarmTime) + " " + calendarNicePrint(alarmTime);

            if (!validAlarm) {
                notificationTitle = context.getString(R.string.app_name);
                notificationMsg = context.getString(R.string.no_upcoming_events);
            }

            Notification notification = new Notification.Builder(context)
                    .setContentTitle(notificationTitle)
                    .setContentText(notificationMsg)
                    .setSmallIcon(R.drawable.notification)
                    .setOngoing(true)
                    .build();

            Intent i = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent =
                    PendingIntent.getActivity(
                            context,
                            0,
                            i,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );

            notification.contentIntent = pendingIntent;
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(id, notification);
        }
        if(validAlarm){

            Intent alarm = new Intent(context, AlarmActivity.class);
            alarm.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            PendingIntent alarmAct =
                    PendingIntent.getActivity(
                            context,
                            0,
                            alarm,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );

            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            am.set(AlarmManager.RTC_WAKEUP, alarmTime.getTimeInMillis(), alarmAct);

            Boolean remind = sharedPref.getBoolean(String.valueOf(R.id.notify_before_sleep), true);

            if(remind){

                Intent notify = new Intent(context, SleepReminder.class);
                PendingIntent notification =
                        PendingIntent.getService(
                                context,
                                0,
                                notify,
                                0//PendingIntent.FLAG_UPDATE_CURRENT
                        );

                String beforeSleep = sharedPref.getString(String.valueOf(R.id.time_before_sleep_input), context.getString(R.string.time_before_sleep_default));
                String sleepHours = sharedPref.getString("sleep_hours", context.getString(R.string.sleep_time));

                int h = Integer.parseInt(beforeSleep.split(":")[0]);
                int m = Integer.parseInt(beforeSleep.split(":")[1]);
                h += Integer.parseInt(sleepHours.split(":")[0]);
                m += Integer.parseInt(sleepHours.split(":")[1]);

                alarmTime.add(Calendar.HOUR_OF_DAY, -h);
                alarmTime.add(Calendar.MINUTE, -m);


                Calendar currentTime = Calendar.getInstance();

                if(currentTime.before(alarmTime)) {
                    am.cancel(notification);
                    am.set(AlarmManager.RTC, alarmTime.getTimeInMillis(), notification);
                }
            }
        }
    }


    public static String calendarNicePrint(Calendar c){
        int h = c.get(Calendar.HOUR_OF_DAY);
        String hour = h < 10 ? "0" + h : "" + h;
        int m = c.get(Calendar.MINUTE);
        String minute = m < 10 ? "0" + m : "" + m;

        return hour + ":" + minute;
    }

    public static String calendarToWeekDay(Calendar calendar){
        SimpleDateFormat dayFormat = new SimpleDateFormat("E", Locale.getDefault());
        return dayFormat.format(calendar.getTime());
    }


    public static Cursor getFirstAppointment(Context context, Calendar calendar) {

        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(calendar.getTimeInMillis());

        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);

        Uri.Builder eventsUriBuilder = CalendarContract.Instances.CONTENT_URI
                .buildUpon();
        ContentUris.appendId(eventsUriBuilder, c.getTimeInMillis());
        c.add(Calendar.DAY_OF_YEAR, 1);
        ContentUris.appendId(eventsUriBuilder, c.getTimeInMillis());
        Uri eventsUri = eventsUriBuilder.build();
        Cursor cursor = null;

        String[] projection = new String[]{
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.ACCOUNT_NAME,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.DTEND};

        String selection = "(" + CalendarContract.Events.ALL_DAY + " = 0)";

        cursor = context.getContentResolver().query(eventsUri, projection, selection, null, CalendarContract.Instances.DTSTART + " ASC");

        if(cursor.moveToFirst()){

            return cursor;
        }
        return cursor;
    }


    public Calendar getFirstRecurring(Calendar calendar, String time) {
        calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(time.split(":")[0]));
        calendar.set(Calendar.MINUTE, Integer.parseInt(time.split(":")[1]));
        calendar.set(Calendar.SECOND, 0);

        return calendar;
    }
}
