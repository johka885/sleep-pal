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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class CalendarUpdate extends BroadcastReceiver {
    public CalendarUpdate() {
    }

    int id = 500;

    Boolean[] alarmDays = new Boolean[7];
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.shared_pref_key), Context.MODE_PRIVATE);
       /* alarmDays[Calendar.MONDAY] = sharedPref.getBoolean(String.valueOf(R.id.alarm_monday), true);
        alarmDays[Calendar.TUESDAY] = sharedPref.getBoolean(String.valueOf(R.id.alarm_tuesday), true);
        alarmDays[Calendar.WEDNESDAY] = sharedPref.getBoolean(String.valueOf(R.id.alarm_wednesday), true);
        alarmDays[Calendar.THURSDAY] = sharedPref.getBoolean(String.valueOf(R.id.alarm_thursday), true);
        alarmDays[Calendar.FRIDAY] = sharedPref.getBoolean(String.valueOf(R.id.alarm_friday), true);
        alarmDays[Calendar.SATURDAY] = sharedPref.getBoolean(String.valueOf(R.id.alarm_saturday), true);
        alarmDays[Calendar.SUNDAY] = sharedPref.getBoolean(String.valueOf(R.id.alarm_sunday), true);*/

        if(true ||alarmDays[Calendar.MONDAY] ||
                alarmDays[Calendar.TUESDAY] ||
                alarmDays[Calendar.WEDNESDAY] ||
                alarmDays[Calendar.THURSDAY] ||
                alarmDays[Calendar.FRIDAY] ||
                alarmDays[Calendar.SATURDAY] ||
                alarmDays[Calendar.SUNDAY] ) {
            setAlarmNew(context, intent, 0);
        } else {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(id);
        }
    }


    public void setAlarmNew(Context context, Intent intent, int days) {
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

        Calendar now = Calendar.getInstance();

        String recurringMsg = "Good morning";
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

                if(now.before(appointment)){
                    validAlarm = true;
                }
            } else {
                appointment.add(Calendar.DAY_OF_YEAR, 20);
            }
            if(everyDayAlarm){
                recurring = getFirstRecurring(recurring, time1);
                if(now.before(recurring)){
                    validAlarm = true;
                }
            } else {
                recurring.add(Calendar.DAY_OF_YEAR, 20);
            }

            if(i == 0){
                now.set(Calendar.HOUR_OF_DAY, 0);
                now.set(Calendar.MINUTE, 0);
            }
            if( !validAlarm){
                now.add(Calendar.DAY_OF_YEAR, 1);
                continue;
            }

            if(appointment.before(recurring)){
                notificationMsg = appointmentMsg;
                now.setTimeInMillis(appointment.getTimeInMillis());
            } else{
                notificationMsg = recurringMsg;
                now.setTimeInMillis(recurring.getTimeInMillis());
            }
            break;
        }

        Boolean showNotification = sharedPref.getBoolean("showNotification", true);

        if(showNotification) {
            String notificationTitle = context.getString(R.string.next_alarm) + " " + calendarToWeekDay(now) + " " + calendarNicePrint(now);

            if (!validAlarm) {
                notificationTitle = context.getString(R.string.app_name);
                notificationMsg = "No upcoming events.";
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
            am.set(AlarmManager.RTC_WAKEUP, now.getTimeInMillis(), alarmAct);

            Boolean remind = sharedPref.getBoolean(String.valueOf(R.id.notify_before_sleep), true);

            if(remind){

                Intent notify = new Intent(context, SleepReminder.class);
                PendingIntent notification =
                        PendingIntent.getService(
                                context,
                                0,
                                notify,
                                PendingIntent.FLAG_UPDATE_CURRENT
                        );

                String beforeSleep = sharedPref.getString(String.valueOf(R.id.time_before_sleep_input), context.getString(R.string.time_before_sleep_default));
                String sleepHours = sharedPref.getString("sleep_hours", context.getString(R.string.sleep_time));

                int h = Integer.parseInt(beforeSleep.split(":")[0]);
                int m = Integer.parseInt(beforeSleep.split(":")[1]);
                h += Integer.parseInt(sleepHours.split(":")[0]);
                m += Integer.parseInt(sleepHours.split(":")[1]);

                now.add(Calendar.HOUR_OF_DAY, -h);
                now.add(Calendar.MINUTE, -m);


                Calendar currentTime = Calendar.getInstance();
                if(currentTime.after(now)) {
                    am.cancel(notification);
                    am.set(AlarmManager.RTC_WAKEUP, now.getTimeInMillis(), notification);
                }
            }
        }
    }

    public void setAlarm(Context context, Intent intent, int days){

        if(AlarmActivity.snoozing) return;

        Calendar c = Calendar.getInstance();
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


        Cursor alarms = getNextAlarmFromCalendar(context, days, time2, time1);

        String eventName = "";

        Calendar appointment = Calendar.getInstance();
        Boolean validAlarm = false;

        appointment.add(Calendar.DAY_OF_YEAR, days);

        if(true ||alarmDays[appointment.get(Calendar.DAY_OF_WEEK)-1]){

            if (alarms != null && alarms.moveToFirst() && eventAlarm) {

                eventName = alarms.getString(2);
                long timeStamp =  alarms.getLong(3);
                c.setTimeInMillis(timeStamp);

                int h2 = Integer.parseInt(time2.split(":")[0]);
                int m2 = Integer.parseInt(time2.split(":")[1]);
                c.add(Calendar.HOUR_OF_DAY, -h2);
                c.add(Calendar.MINUTE, -m2);

                Calendar now = Calendar.getInstance();
                if(c.after(now)){
                    validAlarm = true;
                }
            } else {
                c.add(Calendar.DAY_OF_YEAR, 10);
            }

            appointment.setTimeInMillis(c.getTimeInMillis());

            Calendar latestWakeUp = Calendar.getInstance();

            int h1 = Integer.parseInt(time1.split(":")[0]);
            int m1 = Integer.parseInt(time1.split(":")[1]);
            latestWakeUp.set(Calendar.HOUR_OF_DAY, h1);
            latestWakeUp.set(Calendar.MINUTE, m1);

            Calendar currentTime = Calendar.getInstance();
            if(currentTime.after(latestWakeUp)){
                latestWakeUp.add(Calendar.DAY_OF_YEAR, 1);
            }
            latestWakeUp.add(Calendar.DAY_OF_YEAR, days);

            if(c.after(latestWakeUp) && everyDayAlarm){
                c = latestWakeUp;
                appointment.setTimeInMillis(c.getTimeInMillis());
                eventName = "Good Morning";
                validAlarm = true;
            }
        }

        Boolean showNotification = sharedPref.getBoolean("showNotification", true);

        if(showNotification && (validAlarm || days >= 7)) {
            String alarmString = calendarNicePrint(c);
            String appointmentString = calendarNicePrint(appointment);

            String notificationTitle = context.getString(R.string.next_alarm) + " " + calendarToWeekDay(c) + " " + alarmString;
            String notificationContent = calendarToWeekDay(c) + " " + appointmentString + ": " + eventName;

            if(!validAlarm){
                notificationTitle = context.getString(R.string.app_name);
                notificationContent = "No upcoming events.";
            }
            Notification notification = new Notification.Builder(context)
                    .setContentTitle(notificationTitle)
                    .setContentText(notificationContent)
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

        if(!validAlarm){
            if( days < 7) {
                setAlarm(context, intent, ++days);
            }
            return;
        }

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
        am.set(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), alarmAct);

        Boolean remind = sharedPref.getBoolean(String.valueOf(R.id.notify_before_sleep), true);

        if(remind){

            Intent notify = new Intent(context, SleepReminder.class);
            PendingIntent notification =
                    PendingIntent.getService(
                            context,
                            0,
                            notify,
                            0
                    );

            String beforeSleep = sharedPref.getString(String.valueOf(R.id.time_before_sleep_input), "");
            String sleepHours = sharedPref.getString("sleep_hours", context.getString(R.string.sleep_time));

            int h = Integer.parseInt(beforeSleep.split(":")[0]);
            int m = Integer.parseInt(beforeSleep.split(":")[1]);
            h += Integer.parseInt(sleepHours.split(":")[0]);
            m += Integer.parseInt(sleepHours.split(":")[1]);

            c.add(Calendar.HOUR_OF_DAY, -h);
            c.add(Calendar.MINUTE, -m);


            Calendar now = Calendar.getInstance();
            if(c.after(now)) {
                am.cancel(notification);
                am.set(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), notification);
            }
        }
    }

    public Cursor getNextAlarmFromCalendar(Context context, int days, String beforeAppointment, String latestWakeUp) {

        Calendar c = Calendar.getInstance();
        Calendar temp = Calendar.getInstance();
        if(days > 0){
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);

            c.add(Calendar.DAY_OF_YEAR, days);
            temp.add(Calendar.DAY_OF_YEAR, days);
        }


        temp.set(Calendar.HOUR_OF_DAY, Integer.parseInt(latestWakeUp.split(":")[0]));
        temp.set(Calendar.MINUTE, Integer.parseInt(latestWakeUp.split(":")[1]));

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
            c.setTimeInMillis(cursor.getLong(3));
            if(c.before(temp)){
                return cursor;
            } else{
                return null;
            }
        }
        return cursor;
    }

    public static String calendarNicePrint(Calendar c){
        int h = c.get(Calendar.HOUR_OF_DAY);
        String hour = h < 10 ? "0" + h : "" + h;
        int m = c.get(Calendar.MINUTE);
        String minute = m < 10 ? "0" + m : "" + m;

        return hour + ":" + minute;
    }

    public static String calendarToWeekDay(Calendar calendar){
        SimpleDateFormat dayFormat = new SimpleDateFormat("E", Locale.US);
        return dayFormat.format(calendar.getTime());
    }


    public Cursor getFirstAppointment(Context context, Calendar calendar) {

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

        return calendar;
    }
}
