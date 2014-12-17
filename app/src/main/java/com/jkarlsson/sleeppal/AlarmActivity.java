package com.jkarlsson.sleeppal;

import android.app.ActionBar;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.PowerManager;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Random;


public class AlarmActivity extends FragmentActivity {

    static Boolean snoozing = false;
    Window window;
    Intent service;
    int id = 1000;
    Boolean dialogShowing = false;
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


        int dismissType = sharedPref.getInt(String.valueOf(R.id.dismiss_method), MainActivity.DismissType.MATH);

        if(dismissType == MainActivity.DismissType.SENSOR_LIGHT){
            startSensorLight();
        }
        super.onCreate(savedInstanceState);
    }

    Toast infoMessage;
    public void dismissAlarm(View view) {

        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.shared_pref_key), Context.MODE_PRIVATE);
        int dismissType = sharedPref.getInt(String.valueOf(R.id.dismiss_method), MainActivity.DismissType.MATH);

        switch(dismissType){
            case MainActivity.DismissType.PRESS_BUTTON:
                dismissAlarm();
                break;
            case MainActivity.DismissType.CAPTCHA:
                showCaptcha();
                break;
            case MainActivity.DismissType.SENSOR_LIGHT:
                if(infoMessage != null){
                    infoMessage.cancel();
                }
                infoMessage = Toast.makeText(this, getString(R.string.point_toward_light_instruction), Toast.LENGTH_LONG);
                infoMessage.show();
                break;
            case MainActivity.DismissType.MATH:
                showMath();
                break;
        }

    }

    public static class Difficulty{
        public static final int EASY = 0;
        public static final int NORMAL = 1;
        public static final int HARD = 2;

        public static String printType(Context c, int type){
            switch(type){
                case EASY:
                    return c.getString(R.string.difficulty_easy);
                case NORMAL:
                    return c.getString(R.string.difficulty_normal);
                case HARD:
                    return c.getString(R.string.difficulty_hard);
            }
            return "";
        }
    }

    public class Counting{
        public static final int MULTIPLICATION = 0;
        public static final int DIVISION = 1;
        public static final int ADDITION = 2;
        public static final int SUBTRACTION = 3;

        public static final int NUMBER_OF_COUNTINGS = 4;
    }
    private void showMath() {
        Random random = new Random();
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.shared_pref_key), Context.MODE_PRIVATE);
        int difficulty = sharedPref.getInt("dismissDifficulty", 1);
        int counting = random.nextInt(Counting.NUMBER_OF_COUNTINGS);
        final int result;
        final String problem;

        int d = difficulty + 1;
        int n1 = 1;
        int n2 = 2;
        switch(counting){
            case Counting.MULTIPLICATION:
                while(n1*n2 > 80 + 20*d*d || n1*n2 < 5 + 6*d*d || n1 <= d || n2 <= d) {
                    n1 = random.nextInt(d * d * (random.nextInt(d * d) + 2 * d) + 20) + d * d;
                    n2 = random.nextInt(d * d * (random.nextInt(d * d) + 2 * d) + 20) + d * d;
                }
                result = n1*n2;
                problem = n1 + " * " + n2 + " = ?";
                break;
            case Counting.DIVISION:
                while(n1/(n2+0.0) != Math.round(n1/(n2+0.0)) || n1/n2 < 2+d){
                    n1 = random.nextInt(d*d * (random.nextInt(d*d*d) + 2*d*d) + 20)+5*d;
                    n2 = random.nextInt(d*d * (random.nextInt(d*d) + 2*d) + 20)+3*d;
                }
                result = n1/n2;
                problem = n1 + " / " + n2 + " = ?";
                break;
            case Counting.ADDITION:
                while(n1+n2 > 50 + 50*d*d || n1*n2 < 10*d*d || n1 < 4*d*d || n2 < 4*d*d) {
                    n1 = random.nextInt(d * d * d * (random.nextInt(d * d * d) + 2 * d) + 20) + 3 * d;
                    n2 = random.nextInt(d * d * d * (random.nextInt(d * d * d) + 2 * d) + 20) + 3 * d;
                }
                result = n1+n2;
                problem = n1 + " + " + n2 + " = ?";
                break;
            case Counting.SUBTRACTION:
                n1 = random.nextInt(d*d*d * (random.nextInt(d*d*d) + 3*d) + 20)+3*d;
                n2 = random.nextInt(d*d*d * (random.nextInt(d*d*d) + 3*d) + 20)+3*d;
                while(n1-n2 < 10*d*d || n1+n2 > 70 + 30*d*d || n1 < 4*d*d || n2 < 4*d*d){
                    n1 = random.nextInt(d*d*d * (random.nextInt(d*d*d) + 3*d) + 50)+3*d;
                    n2 = random.nextInt(d*d*d * (random.nextInt(d*d*d) + 3*d) + 20)+3*d;
                }
                result = n1-n2;
                problem = n1 + " - " + n2 + " = ?";
                break;
            default:
                result = 0;
                problem = "error, try again";
        }

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);

        final InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Answer the question to dismiss the alarm")
                .setMessage(problem)
                .setView(input)
                .setPositiveButton("Dismiss", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        inputMethodManager.hideSoftInputFromWindow(input.getWindowToken(), 0);
                        checkAnswerAndDismiss(input, result);
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        final InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        inputMethodManager.hideSoftInputFromWindow(dialogInput.getWindowToken(), 0);
                        Toast.makeText(getApplicationContext(), "canceled", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
        dialogShowing = true;


        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) input.getLayoutParams();
        layoutParams.setMargins(50,0,50,0);
        input.setLayoutParams(layoutParams);
        dialogInput = input;

        dialog.setCanceledOnTouchOutside(false);

        input.requestFocus();
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        input.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {

                    inputMethodManager.hideSoftInputFromWindow(input.getWindowToken(), 0);
                    dialog.dismiss();
                    checkAnswerAndDismiss(input, result);
                    return true;
                }
                return false;
            }
        });
    }

    private void checkAnswerAndDismiss(EditText input, int result) {
        int answer;
         try {
             answer = Integer.parseInt(input.getText().toString());
         } catch(NumberFormatException e){
             answer = -1;
        }

        if(answer == result){
            dismissAlarm();
        } else{
            Toast.makeText(getApplicationContext(), getString(R.string.incorrect_answer), Toast.LENGTH_SHORT).show();
        }
        dialogShowing = false;
    }

    EditText dialogInput;

    private void showCaptcha() {

        final EditText input = new EditText(this);
        dialogInput = input;
        String validLetters = getString(R.string.alphabet);
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.shared_pref_key), Context.MODE_PRIVATE);
        int difficulty = 1 + sharedPref.getInt("dismissDifficulty", 1);
        int numberOfLetters = 4 + 2*difficulty;
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < numberOfLetters; ++i){
            sb.append(validLetters.charAt(random.nextInt(validLetters.length())));
        }
        final String captcha = sb.toString();
        final InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        input.requestFocus();
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.RESULT_HIDDEN);

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Input the following text to dismiss the alarm")
                .setMessage(captcha)
                .setView(input)
                .setPositiveButton("Dismiss", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String s = input.getText().toString();
                       if(s.equals(captcha)){
                           dismissAlarm();
                       } else{
                           Toast.makeText(getApplicationContext(), "Incorrect, try again!", Toast.LENGTH_SHORT).show();
                       }
                        inputMethodManager.hideSoftInputFromWindow(input.getWindowToken(), 0);
                        dialogShowing = false;
                    }
                }).show();
        dialogShowing = false;

        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) input.getLayoutParams();
        layoutParams.setMargins(50,0,50,0);
        input.setLayoutParams(layoutParams);
        dialogInput = input;

        input.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {

                    inputMethodManager.hideSoftInputFromWindow(input.getWindowToken(), 0);

                    String s = input.getText().toString();
                    if(s.equals(captcha)){
                        dismissAlarm();
                    } else{
                        Toast.makeText(getApplicationContext(), "Incorrect, try again!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                    dialogShowing = false;
                    return true;
                }
                return false;
            }
        });
    }

    private void dismissAlarm() {
        if(infoMessage != null){
            infoMessage.cancel();
        }

        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.shared_pref_key), Context.MODE_PRIVATE);
        sharedPref.edit().putInt("numberOfSnoozes", 0).apply();
        snoozing = false;
        alarmClock.setAnimation(null);

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(id);

        if(service != null) stopService(service);

        Intent i = new Intent("com.jkarlsson.sleeppal.SETTINGS_CHANGED");
        sendBroadcast(i);


        if(sensorManager != null){
            sensorManager.unregisterListener(sensorEvent);
            sensorManager = null;
            sensorEvent = null;
        }

        wakeLock.release();
        this.finish();
    }

    public void snoozeAlarm(View view) {
        if(infoMessage != null){
            infoMessage.cancel();
        }

        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.shared_pref_key), Context.MODE_PRIVATE);
        int snoozes = sharedPref.getInt("numberOfSnoozes", 0);
        ++snoozes;
        sharedPref.edit().putInt("numberOfSnoozes",snoozes).apply();

        if(snoozes > 5){
            Toast.makeText(this, getString(R.string.time_to_get_up), Toast.LENGTH_LONG).show();
            return;
        }

        snoozing = true;
        alarmClock.setAnimation(null);

        if(service != null) stopService(service);

        Intent snooze = new Intent(this, AlarmActivity.class);
        PendingIntent alarm = PendingIntent.getActivity(this, 0, snooze, PendingIntent.FLAG_UPDATE_CURRENT);
        snooze.putExtra("silent", false);

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

        if(sensorManager != null){
            sensorManager.unregisterListener(sensorEvent);
            sensorManager = null;
            sensorEvent = null;
        }

        wakeLock.release();
        this.finish();
    }

    @Override
    protected void onPause() {
        if(dialogInput != null) {
            final InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(dialogInput.getWindowToken(), 0);
        }
        if(sensorManager != null){
            sensorManager.unregisterListener(sensorEvent);
        }
        super.onPause();
    }

    @Override
     protected void onResume() {
        if(dialogShowing) {
            final InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.showSoftInput(dialogInput, InputMethodManager.SHOW_FORCED);
        }

        if(sensorManager != null){
            sensorManager.registerListener(sensorEvent, lightSensor, SensorManager.SENSOR_DELAY_FASTEST);
        }
        super.onResume();
    }




    /* Testing out sensors */
    SensorManager sensorManager;
    SensorEventListener sensorEvent;
    Sensor lightSensor;

    float initial;

    public void startSensorLight(){
        if(sensorManager != null) return; // Don't crash if sensor somehow is started twice, shouldn't happen

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);


        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.shared_pref_key), Context.MODE_PRIVATE);
        final int sensitivity = sharedPref.getInt("dismissDifficulty", 1);
        final float requiredLight = 50 + 50*(sensitivity);

        initial = -1;

        sensorEvent = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if( initial == -1){
                    initial = event.values[0];
                }
                if( event.values[0] > requiredLight + initial || event.values[0] > requiredLight * 2){
                    dismissAlarm();
                } else if(event.values[0] > initial + 10){
                    if(infoMessage != null){
                        infoMessage.cancel();
                    }
                    infoMessage = Toast.makeText(getApplicationContext(), getString(R.string.closer_to_light_message), Toast.LENGTH_LONG);
                    infoMessage.show();
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };

        sensorManager.registerListener(sensorEvent, lightSensor, SensorManager.SENSOR_DELAY_FASTEST);

    }
}
