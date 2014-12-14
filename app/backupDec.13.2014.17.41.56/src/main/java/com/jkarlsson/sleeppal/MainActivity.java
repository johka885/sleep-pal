package com.jkarlsson.sleeppal;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;


public class MainActivity extends FragmentActivity {

    EditText alarmTone;
    RingtoneManager ringtoneManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loadDefaultRingtone(R.id.alarm_tone, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString());

        loadDefaultTime(R.id.time_before_wake_up, getString(R.string.time_before_wake_up_default));
        loadDefaultTime(R.id.time_before_sleep_input, getString(R.string.time_before_sleep_default));
        loadDefaultTime(R.id.snooze_time, getString(R.string.snooze_time_default));
        loadDefaultTime(R.id.latest_wake_up_time, getString(R.string.latest_wake_up_time_default));

        loadDefaultCheckbox(R.id.vibrate, true);
        loadDefaultCheckbox(R.id.alarm_no_appointments, true);
        loadDefaultCheckbox(R.id.alarm_appointments, true);
        loadDefaultCheckbox(R.id.notify_before_sleep, true);

        loadDefaultCheckbox(R.id.alarm_monday, true);
        loadDefaultCheckbox(R.id.alarm_tuesday, true);
        loadDefaultCheckbox(R.id.alarm_wednesday, true);
        loadDefaultCheckbox(R.id.alarm_thursday, true);
        loadDefaultCheckbox(R.id.alarm_friday, true);
        loadDefaultCheckbox(R.id.alarm_saturday, true);
        loadDefaultCheckbox(R.id.alarm_sunday, true);
    }

    private void loadDefaultCheckbox(int id, boolean defaultValue) {

        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.shared_pref_key), Context.MODE_PRIVATE);
        Boolean checked = sharedPref.getBoolean(String.valueOf(id), defaultValue);
        CheckBox cb = (CheckBox) findViewById(id);
        cb.setChecked(checked);
    }

    private void loadDefaultTime(int id, String defaultValue) {
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.shared_pref_key), Context.MODE_PRIVATE);
        String savedValue = sharedPref.getString(String.valueOf(id), defaultValue);
        EditText editText = (EditText) findViewById(id);
        editText.setText(savedValue);
    }

    private void loadDefaultRingtone(int id, String defaultValue) {
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.shared_pref_key), Context.MODE_PRIVATE);
        String savedValue = sharedPref.getString(String.valueOf(id), defaultValue);
        String name;
        if( savedValue == "Silent" ) {
            name = "Silent";
        } else {
            name = getRingtoneNameFromUri(Uri.parse(savedValue));
        }
        EditText editText = (EditText) findViewById(id);
        editText.setText(name);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
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
                editor.commit();

                Intent i = new Intent("com.jkarlsson.sleeppal.SETTINGS_CHANGED");
                sendBroadcast(i);

                time.setText(hours + ":" + minutes);

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

        Intent intent = new Intent(ringtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(ringtoneManager.EXTRA_RINGTONE_TITLE, "Select alarm tone");
        intent.putExtra(ringtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
        intent.putExtra(ringtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        intent.putExtra(ringtoneManager.EXTRA_RINGTONE_TYPE,ringtoneManager.TYPE_ALARM);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, uri);
        startActivityForResult(intent, 1);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch (resultCode) {
            case RESULT_OK:
                if(ringtoneManager.EXTRA_RINGTONE_PICKED_URI != null){
                    Uri uri = intent.getParcelableExtra(ringtoneManager.EXTRA_RINGTONE_PICKED_URI);
                    String tone = getRingtoneNameFromUri(uri);
                    alarmTone.setText(tone);

                    String path = tone == "Silent" ? "Silent" : uri.toString();
                    SharedPreferences sharedPref = getSharedPreferences(getString(R.string.shared_pref_key), Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString(String.valueOf(R.id.alarm_tone), path);
                    editor.commit();
                }
        }
    }

    public Uri getAbsoluteURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            Uri uri = Uri.parse(cursor.getString(column_index));
            return uri;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
    public String getRingtoneNameFromUri(Uri uri){
        String tone = "Silent";
        if(uri != null) {
            tone = ringtoneManager.getRingtone(MainActivity.this, uri).getTitle(MainActivity.this);
            if(tone.matches("\\d{1,3}")) {
                try {
                    Uri absoluteUri = getAbsoluteURI(MainActivity.this, uri);
                    tone = absoluteUri.getLastPathSegment();
                } catch (Exception e) {
                    tone = ringtoneManager.getRingtone(MainActivity.this, uri).getTitle(MainActivity.this);
                }
            }
        }

        return tone;
    }

    private Handler handler = new Handler();

    public void previewAlarm(View view) {
        Toast.makeText(this, "Alarm will sound in 5 seconds", Toast.LENGTH_LONG).show();
        handler.postDelayed(mUpdateTimeTask, 2000);
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
        editor.commit();

        Intent i = new Intent("com.jkarlsson.sleeppal.SETTINGS_CHANGED");
        sendBroadcast(i);
    }
}
