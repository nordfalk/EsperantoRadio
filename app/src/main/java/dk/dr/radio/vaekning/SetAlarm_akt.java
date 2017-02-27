/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dk.dr.radio.vaekning;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.support.v7.widget.Toolbar;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.ArrayList;

import dk.dr.radio.data.Programdata;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.v3.R;

/**
 * Manages each alarm
 */
public class SetAlarm_akt extends PreferenceActivity implements Preference.OnPreferenceChangeListener,
    TimePickerDialog.OnTimeSetListener, OnCancelListener {
  private static final String KEY_CURRENT_ALARM = "currentAlarm";
  private static final String KEY_ORIGINAL_ALARM = "originalAlarm";
  private static final String KEY_TIME_PICKER_BUNDLE = "timePickerBundle";
  private EditText mLabel;
  private CheckBoxPreference mEnabledPref;
  private Preference mTimePref;
  private RepeatPreference mRepeatPref;
  private int mId;
  private int mHour;
  private int mMinute;
  private TimePickerDialog mTimePickerDialog;
  private Alarm mOriginalAlarm;
  private ListPreference mKanaloPref;

  @Override
  protected void onCreate(Bundle icicle) {
    super.onCreate(icicle);

    // Override the default content view.
    setContentView(R.layout.deskclock_set_alarm);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    toolbar.setLogo(App.ÆGTE_DR ? R.drawable.dr_logo : R.drawable.appikon_eo);
    toolbar.setTitle(getString(R.string.Angiv_vækning));
// SdkVersion 24 og frem: toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_material);
    toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
    toolbar.setNavigationOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        finish();
      }
    });

    EditText label = (EditText) getLayoutInflater().inflate(R.layout.deskclock_alarm_label, null);

    ListView list = (ListView) findViewById(android.R.id.list);
    list.addFooterView(label);
    addPreferencesFromResource(R.xml.deskclock_alarm_prefs);

    // Get each preference so we can retrieve the value later.
    mLabel = label;
    mEnabledPref = (CheckBoxPreference) findPreference("enabled");
    mEnabledPref.setOnPreferenceChangeListener(this);
    mTimePref = findPreference("time");
    mKanaloPref = (ListPreference) findPreference("kanalo");
    mKanaloPref.setOnPreferenceChangeListener(this);
    mKanaloPref.setSummary("kanal");

    ArrayList<String> kk = new ArrayList<String>();
    ArrayList<String> kn = new ArrayList<String>();
    for (Kanal k : Programdata.instans.grunddata.kanaler) {
      kk.add(k.kode);
      kn.add(k.navn);
    }
    Log.d("kk=" + kk);
    Log.d("kn=" + kn);
    mKanaloPref.setEntries(kn.toArray(new String[kn.size()]));
    mKanaloPref.setEntryValues(kk.toArray(new String[kk.size()]));

    mRepeatPref = (RepeatPreference) findPreference("setRepeat");
    mRepeatPref.setOnPreferenceChangeListener(this);

    Intent i = getIntent();
    String data = i.getStringExtra(Alarms.ALARM_INTENT_EXTRA);
    Alarm alarm = null;
    if (data != null) alarm = new Alarm(data);

    if (alarm == null) {
      // No alarm means create a new alarm.
      alarm = new Alarm();
      alarm.kanalo = Programdata.instans.grunddata.forvalgtKanal.kode;
    }
    mOriginalAlarm = alarm;

    // Populate the prefs with the original alarm data.  updatePrefs also
    // sets mId so it must be called before checking mId below.
    updatePrefs(mOriginalAlarm);

    // We have to do this to get the save/cancel buttons to highlight on
    // their own.
    getListView().setItemsCanFocus(true);

    // Attach actions to each button.
    Button b = (Button) findViewById(R.id.alarm_save);
    b.setTypeface(App.skrift_gibson);
    b.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        long time = saveAlarm(null);
        if (mEnabledPref.isChecked()) {
          popAlarmSetToast(SetAlarm_akt.this, time);
        }
        finish();
      }
    });
    Button revert = (Button) findViewById(R.id.alarm_revert);
    revert.setTypeface(App.skrift_gibson);
    revert.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        revert();
        finish();
      }
    });
    b = (Button) findViewById(R.id.alarm_delete);
    b.setTypeface(App.skrift_gibson);
    if (mId == Alarm.INVALID_ALARM_ID) {
      b.setEnabled(false);
      b.setVisibility(View.GONE);
    } else {
      b.setVisibility(View.VISIBLE);
      b.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
          deleteAlarm();
        }
      });
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putString(KEY_ORIGINAL_ALARM, mOriginalAlarm.toString());
    outState.putString(KEY_CURRENT_ALARM, buildAlarmFromUi().toString());
    if (mTimePickerDialog != null) {
      if (mTimePickerDialog.isShowing()) {
        outState.putParcelable(KEY_TIME_PICKER_BUNDLE, mTimePickerDialog
            .onSaveInstanceState());
        mTimePickerDialog.dismiss();
      }
      mTimePickerDialog = null;
    }
  }

  @Override
  protected void onRestoreInstanceState(Bundle state) {
    super.onRestoreInstanceState(state);

    Alarm alarmFromBundle = new Alarm(state.getString(KEY_ORIGINAL_ALARM));
    if (alarmFromBundle != null) {
      mOriginalAlarm = alarmFromBundle;
    }

    alarmFromBundle = new Alarm(state.getString(KEY_CURRENT_ALARM));
    if (alarmFromBundle != null) {
      updatePrefs(alarmFromBundle);
    }

    Bundle b = state.getParcelable(KEY_TIME_PICKER_BUNDLE);
    if (b != null) {
      showTimePicker();
      mTimePickerDialog.onRestoreInstanceState(b);
    }
  }

  // Used to post runnables asynchronously.
  private static final Handler sHandler = new Handler();

  public boolean onPreferenceChange(final Preference p, Object newValue) {
    // Asynchronously save the alarm since this method is called _before_
    // the value of the preference has changed.
    final String ktekstomalnova = ("" + mKanaloPref.getEntry()).trim();
    sHandler.post(new Runnable() {
      public void run() {
        // Editing any preference (except enable) enables the alarm.
        if (p != mEnabledPref) {
          mEnabledPref.setChecked(true);
        }
        if (p == mKanaloPref) {

          String kteksto = ("" + mKanaloPref.getEntry()).trim();
          mKanaloPref.setSummary(kteksto);

          String lteksto = mLabel.getText().toString().trim();
          if (lteksto.length() == 0 || lteksto.equals(ktekstomalnova)) {
            mLabel.setText(kteksto);
          }
        }
        saveAlarm(null);
      }
    });
    return true;
  }

  private void updatePrefs(Alarm alarm) {
    mId = alarm.id;
    mEnabledPref.setChecked(alarm.enabled);
    mLabel.setText(alarm.label);
    mHour = alarm.hour;
    mMinute = alarm.minutes;
    mRepeatPref.setDaysOfWeek(alarm.daysOfWeek);
    mKanaloPref.setValue(alarm.kanalo);
    mKanaloPref.setSummary(mKanaloPref.getEntry());
    updateTime();
  }

  @Override
  public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
                                       Preference preference) {
    if (preference == mTimePref) {
      showTimePicker();
    }

    return super.onPreferenceTreeClick(preferenceScreen, preference);
  }

  @Override
  public void onBackPressed() {
    revert();
    finish();
  }

  private void showTimePicker() {
    if (mTimePickerDialog != null) {
      if (mTimePickerDialog.isShowing()) {
        Log.d("mTimePickerDialog is already showing.");
        mTimePickerDialog.dismiss();
      } else {
        Log.d("mTimePickerDialog is not null");
      }
      mTimePickerDialog.dismiss();
    }

    mTimePickerDialog = new TimePickerDialog(this, this, mHour, mMinute,
        DateFormat.is24HourFormat(this));
    mTimePickerDialog.setOnCancelListener(this);
    mTimePickerDialog.show();
  }

  public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
    // onTimeSet is called when the user clicks "Set"
    mTimePickerDialog = null;
    mHour = hourOfDay;
    mMinute = minute;
    updateTime();
    // If the time has been changed, enable the alarm.
    mEnabledPref.setChecked(true);
  }

  @Override
  public void onCancel(DialogInterface dialog) {
    mTimePickerDialog = null;
  }

  private void updateTime() {
    mTimePref.setSummary(Alarms.formatTime(this, mHour, mMinute,
        mRepeatPref.getDaysOfWeek()));
  }

  private long saveAlarm(Alarm alarm) {
    if (alarm == null) {
      alarm = buildAlarmFromUi();
    }

    Log.d("alarm.id = " + alarm.id);
    long time;
    if (alarm.id == Alarm.INVALID_ALARM_ID) {
      time = Alarms.addAlarm(this, alarm);
      // addAlarm populates the alarm with the new id. Update mId so that
      // changes to other preferences update the new alarm.
      mId = alarm.id;
    } else {
      time = Alarms.setAlarm(this, alarm);
    }
    return time;
  }

  private Alarm buildAlarmFromUi() {
    Alarm alarm = new Alarm();
    alarm.id = mId;
    alarm.enabled = mEnabledPref.isChecked();
    alarm.hour = mHour;
    alarm.minutes = mMinute;
    alarm.daysOfWeek = mRepeatPref.getDaysOfWeek();
    alarm.kanalo = mKanaloPref.getValue();
    alarm.label = mLabel.getText().toString();
    return alarm;
  }

  private void deleteAlarm() {
    new AlertDialog.Builder(this)
        .setTitle(getString(R.string.delete_alarm))
        .setMessage(getString(R.string.delete_alarm_confirm))
        .setPositiveButton(android.R.string.ok,
            new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface d, int w) {
                Alarms.deleteAlarm(SetAlarm_akt.this, mId);
                finish();
              }
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void revert() {
    int newId = mId;
    // "Revert" on a newly created alarm should delete it.
    if (mOriginalAlarm.id == -1) {
      Alarms.deleteAlarm(SetAlarm_akt.this, newId);
    } else {
      saveAlarm(mOriginalAlarm);
    }
  }

  /**
   * Display a toast that tells the user how long until the alarm
   * goes off.  This helps prevent "am/pm" mistakes.
   */
  static void popAlarmSetToast(Context context, long timeInMillis) {
    String toastText = formatToast(context, timeInMillis);
    Toast toast = Toast.makeText(context, toastText, Toast.LENGTH_LONG);
    ToastMaster.setToast(toast);
    toast.show();
  }

  /**
   * format "Alarm set for 2 days 7 hours and 53 minutes from
   * now"
   */
  static String formatToast(Context context, long timeInMillis) {
    long delta = timeInMillis - System.currentTimeMillis();
    long hours = delta / (1000 * 60 * 60);
    long minutes = delta / (1000 * 60) % 60;
    long days = hours / 24;
    hours = hours % 24;

    String daySeq = (days == 0) ? ""
        : (days == 1) ? context.getString(R.string.day)
        : context.getString(R.string.days, Long.toString(days));

    String minSeq = (minutes == 0) ? ""
        : (minutes == 1) ? context.getString(R.string.minute)
        : context.getString(R.string.minutes, Long.toString(minutes));

    String hourSeq = (hours == 0) ? ""
        : (hours == 1) ? context.getString(R.string.hour)
        : context.getString(R.string.hours, Long.toString(hours));

    boolean dispDays = days > 0;
    boolean dispHour = hours > 0;
    boolean dispMinute = minutes > 0;

    int index = (dispDays ? 1 : 0)
        | (dispHour ? 2 : 0)
        | (dispMinute ? 4 : 0);

    String[] formats = context.getResources().getStringArray(R.array.alarm_set);
    return String.format(formats[index], daySeq, hourSeq, minSeq);
  }

  @Override
  protected void onStart() {
    super.onStart();
    if (App.fejlsøgning) Log.d(this + " onStart()");
    App.instans.aktivitetStartet(this);
  }

  @Override
  protected void onStop() {
    super.onStop();
    if (App.fejlsøgning) Log.d(this + " onStop()");
    App.instans.aktivitetStoppet(this);
  }
}
