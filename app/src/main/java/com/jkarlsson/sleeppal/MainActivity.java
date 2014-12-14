package com.jkarlsson.sleeppal;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;


import com.leanplum.Leanplum;
import com.leanplum.activities.LeanplumFragmentActivity;
import com.leanplum.annotations.Variable;
import com.leanplum.callbacks.StartCallback;

public class MainActivity extends LeanplumFragmentActivity {

    EditText alarmTone;
    RingtoneManager ringtoneManager;

    //Default values set from Leanplum for ab-testing
    @Variable public static String beforeWakeUp = "01:30";
    @Variable public static String latestWakeUp = "09:30";
    @Variable public static String beforeSleep = "01:00";
    @Variable public static String snoozeTime = "09:00";

    @Variable public static boolean isVibrating = true;
    @Variable public static boolean isAppointmentAlarming = true;
    @Variable public static boolean isLatestWakeUpAlarming = true;
    @Variable public static boolean isSleepReminding = true;

    @Variable public static int buttonColor = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if (BuildConfig.DEBUG) {
            Leanplum.setAppIdForDevelopmentMode("aiNYg5ZSkjRBaMhhMR83PjCIjcJaLLqzS5KO3WJRREA", "NvfHnxqNA1ogil6Q6XYk9rhHub4y3iaQbDdB3a5xbm8");
        } else {
            Leanplum.setAppIdForProductionMode("aiNYg5ZSkjRBaMhhMR83PjCIjcJaLLqzS5KO3WJRREA", "IV3XDRbQAKrmIe7JouJ37vFtd1gbHSW6bDvM3IaYbJE");
        }

        Leanplum.enableVerboseLoggingInDevelopmentMode();
        Leanplum.start(this);


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Leanplum.addStartResponseHandler(new StartCallback() {
            @Override
            public void onResponse(boolean success) {
                findViewById(R.id.wrapper_layout).setVisibility(View.VISIBLE);
                loadDefaultValues();
                setButtonColors();
            }
        });

        Intent i = new Intent("com.jkarlsson.sleeppal.SETTINGS_CHANGED");
        sendBroadcast(i);

    }

    private void setButtonColors() {
        Button previewButton = (Button) findViewById(R.id.preview_button);

        if(buttonColor == 1) {
            previewButton.setBackground(getResources().getDrawable(R.drawable.button_red));
            previewButton.setTextColor(Color.parseColor("#FFD9D9"));
        }
    }

    public void loadDefaultValues(){
        loadDefaultRingtone(R.id.alarm_tone, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString());

        loadDefaultTime(R.id.time_before_wake_up, beforeWakeUp);
        loadDefaultTime(R.id.time_before_sleep_input, beforeSleep);
        loadDefaultTime(R.id.snooze_time, snoozeTime);
        loadDefaultTime(R.id.latest_wake_up_time, latestWakeUp);

        loadDefaultCheckbox(R.id.vibrate, isVibrating);
        loadDefaultCheckbox(R.id.alarm_no_appointments, isLatestWakeUpAlarming);
        loadDefaultCheckbox(R.id.alarm_appointments, isAppointmentAlarming);
        loadDefaultCheckbox(R.id.notify_before_sleep, isSleepReminding);

        loadDefaultCheckbox(R.id.alarm_monday, true);
        loadDefaultCheckbox(R.id.alarm_tuesday, true);
        loadDefaultCheckbox(R.id.alarm_wednesday, true);
        loadDefaultCheckbox(R.id.alarm_thursday, true);
        loadDefaultCheckbox(R.id.alarm_friday, true);
        loadDefaultCheckbox(R.id.alarm_saturday, true);
        loadDefaultCheckbox(R.id.alarm_sunday, true);
    }

    private void loadDefaultCheckbox(int id, Boolean defaultValue) {

        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.shared_pref_key), Context.MODE_PRIVATE);
        Boolean checked = sharedPref.getBoolean(String.valueOf(id), defaultValue);
        if(!sharedPref.contains(String.valueOf(id))){
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean(String.valueOf(id), defaultValue);
            editor.apply();
        }

        CheckBox cb = (CheckBox) findViewById(id);
        cb.setChecked(checked);
    }

    private void loadDefaultTime(int id, String defaultValue) {
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.shared_pref_key), Context.MODE_PRIVATE);
        String savedValue = sharedPref.getString(String.valueOf(id), defaultValue);

        if(!sharedPref.contains(String.valueOf(id))){
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(String.valueOf(id), defaultValue);
            editor.apply();
        }
        EditText editText = (EditText) findViewById(id);
        editText.setText(savedValue);
    }

    private void loadDefaultRingtone(int id, String defaultValue) {
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.shared_pref_key), Context.MODE_PRIVATE);
        String savedValue = sharedPref.getString(String.valueOf(id), defaultValue);

        if(!sharedPref.contains(String.valueOf(id))){
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(String.valueOf(id), defaultValue);
            editor.apply();
        }

        String name;

        if(savedValue.equals("Silent")) {
            name = getString(R.string.silent);
        } else {
            name = getRingtoneNameFromUri(Uri.parse(savedValue));
        }

        EditText editText = (EditText) findViewById(id);
        editText.setText(name);
    }

    public void showTimePickerDialog(View view) {
        EditText et = (EditText) view;
        String[] time = String.valueOf(et.getText()).split(":");
        int hour = Integer.parseInt(time[0]);
        int minute = Integer.parseInt(time[1]);

        TimePickerDialog mTimePicker;
        final View view1 = view;
        mTimePicker = new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                String hours = selectedHour < 10 ? "0" + selectedHour : "" + selectedHour;
                String minutes = selectedMinute < 10 ? "0" + selectedMinute : "" + selectedMinute;
                EditText time = (EditText ) view1;

                SharedPreferences sharedPref = getSharedPreferences(getString(R.string.shared_pref_key), Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(String.valueOf(time.getId()), hours + ":" + minutes);
                editor.apply();

                Intent i = new Intent("com.jkarlsson.sleeppal.SETTINGS_CHANGED");
                sendBroadcast(i);

                time.setText(hours + ":" + minutes);

                double clock = Double.parseDouble(hours) + Double.parseDouble(minutes)/60;

                int id = view1.getId();
                double defaultClock;
                switch(id){
                    case R.id.time_before_wake_up:
                        defaultClock = Double.parseDouble(beforeWakeUp.split(":")[0]) + Double.parseDouble(beforeWakeUp.split(":")[1])/60;
                        Leanplum.track("timeBeforeWakeUpChanged", clock < defaultClock ? -1 : 1);
                        break;
                    case R.id.time_before_sleep_input:
                        defaultClock = Double.parseDouble(beforeSleep.split(":")[0]) + Double.parseDouble(beforeSleep.split(":")[1])/60;
                        Leanplum.track("timeBeforeSleepChanged", clock < defaultClock ? -1 : 1);
                        break;
                    case R.id.latest_wake_up_time:
                        defaultClock = Double.parseDouble(latestWakeUp.split(":")[0]) + Double.parseDouble(latestWakeUp.split(":")[1])/60;
                        Leanplum.track("latestWakeUpTimeChanged", clock < defaultClock ? -1 : 1);
                        break;
                    case R.id.snooze_time:
                        defaultClock = Double.parseDouble(snoozeTime.split(":")[0]) + Double.parseDouble(snoozeTime.split(":")[1])/60;
                        Leanplum.track("snoozeTimeChanged", clock < defaultClock ? -1 : 1);
                        break;
                }
            }
        }, hour, minute, true);
        mTimePicker.setTitle(getString(R.string.select_time));
        mTimePicker.show();
    }

    public void showAlarmTonePickerDialog(View view) {
        ringtoneManager = new RingtoneManager(this);

        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.shared_pref_key), Context.MODE_PRIVATE);
        String savedValue = sharedPref.getString(String.valueOf(R.id.alarm_tone),
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString());

        Uri uri = Uri.parse(savedValue);

        alarmTone = (EditText) view;

        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.select_ringtone));
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, uri);
        startActivityForResult(intent, 1);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch (resultCode) {
            case RESULT_OK:
                Uri uri = intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                String tone = getRingtoneNameFromUri(uri);
                alarmTone.setText(tone);

                String path = tone.equals("Silent") ? "Silent" : uri.toString();
                SharedPreferences sharedPref = getSharedPreferences(getString(R.string.shared_pref_key), Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(String.valueOf(R.id.alarm_tone), path);
                editor.apply();

        }
    }

    public Uri getAbsoluteURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] projection = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri,  projection, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return Uri.parse(cursor.getString(column_index));
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
    public String getRingtoneNameFromUri(Uri uri){
        String tone = "Silent";
        if(uri != null) {
            tone = RingtoneManager.getRingtone(MainActivity.this, uri).getTitle(MainActivity.this);
            if(tone.matches("\\d{1,3}")) {
                try {
                    Uri absoluteUri = getAbsoluteURI(MainActivity.this, uri);
                    tone = absoluteUri.getLastPathSegment();
                } catch (Exception e) {
                    tone = RingtoneManager.getRingtone(MainActivity.this, uri).getTitle(MainActivity.this);
                }
            }
        }

        return tone;
    }

    private Handler handler = new Handler();

    public void previewAlarm(View view) {
        Toast.makeText(this, "Alarm will sound in 5 seconds", Toast.LENGTH_LONG).show();
        Leanplum.track("alarmPreviewed");
        handler.postDelayed(mUpdateTimeTask, 5000);
    }

    private Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            Intent intent = new Intent(getApplicationContext(), AlarmActivity.class);
            startActivity(intent);
        }
    };

    public void updateCheckBoxValue(View view) {
        CheckBox cb = (CheckBox) view;

        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.shared_pref_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(String.valueOf(cb.getId()), cb.isChecked());
        editor.apply();

        Intent i = new Intent("com.jkarlsson.sleeppal.SETTINGS_CHANGED");
        sendBroadcast(i);

        int id = cb.getId();
        switch(id){
            case R.id.snooze:
                Leanplum.track("snoozeActivation");
                break;
            case R.id.notify_before_sleep:
                Leanplum.track("goToSleepActivation");
                break;
            case R.id.alarm_appointments:
                Leanplum.track("appointmentAlarmActivation");
                break;
            case R.id.alarm_no_appointments:
                Leanplum.track("noAppointmentsAlarmActivation");
                break;
        }
    }
}
