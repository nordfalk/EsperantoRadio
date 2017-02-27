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
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Calendar;

import dk.dr.radio.akt.Basisaktivitet;
import dk.dr.radio.diverse.App;
import dk.dr.radio.v3.R;

/**
 * AlarmClock_akt application.
 */
public class AlarmClock_akt extends Basisaktivitet implements OnItemClickListener {
  static final String PREFERENCES = "AlarmClock";
  /**
   * This must be false for production.  If true, turns on logging,
   * test code, etc.
   */
  static final boolean DEBUG = false;
  private LayoutInflater mFactory;
  private ListView mAlarmsList;
  private AlarmTimeAdapter adapter;

  private void updateAlarm(boolean enabled, Alarm alarm) {
    alarm.enabled = enabled;
    if (enabled) {
      alarm.time = Alarms.calculateAlarm(alarm.hour, alarm.minutes, alarm.daysOfWeek).getTimeInMillis();
      SetAlarm_akt.popAlarmSetToast(this, alarm.time);
    }
    Alarms.setAlarm(this, alarm);
  }

  private class AlarmTimeAdapter extends BaseAdapter {
    public int getCount() {
      return Alarms.alarmer.size();
    } // der er omkring tusind drawables

    public Object getItem(int position) {
      return position;
    } // bruges ikke

    public long getItemId(int position) {
      return position;
    } // bruges ikke

    @Override
    public View getView(final int position, View view, ViewGroup parent) {

      view = mFactory.inflate(R.layout.deskclock_alarm_time_elemento, parent, false);

      DigitalClock digitalClock =
          (DigitalClock) view.findViewById(R.id.digitalClock);
      digitalClock.setLive(false);

      final Alarm alarm = Alarms.alarmer.get(position);

      View indicator = view.findViewById(R.id.indicator);

      // Set the initial state of the clock "checkbox"
      final CheckBox clockOnOff =
          (CheckBox) indicator.findViewById(R.id.clock_onoff);
      clockOnOff.setChecked(alarm.enabled);
      clockOnOff.setTypeface(App.skrift_gibson);

      // Clicking outside the "checkbox" should also change the state.
      indicator.setOnClickListener(new OnClickListener() {
        public void onClick(View v) {
          clockOnOff.toggle();
          updateAlarm(clockOnOff.isChecked(), alarm);
        }
      });


      // set the alarm text
      final Calendar c = Calendar.getInstance();
      c.set(Calendar.HOUR_OF_DAY, alarm.hour);
      c.set(Calendar.MINUTE, alarm.minutes);
      digitalClock.updateTime(c);

      // Set the repeat text or leave it blank if it does not repeat.
      TextView daysOfWeekView =
          (TextView) digitalClock.findViewById(R.id.daysOfWeek);
      daysOfWeekView.setTypeface(App.skrift_gibson);
      final String daysOfWeekStr =
          alarm.daysOfWeek.toString(AlarmClock_akt.this, false);
      if (daysOfWeekStr != null && daysOfWeekStr.length() != 0) {
        daysOfWeekView.setText(daysOfWeekStr);
        daysOfWeekView.setVisibility(View.VISIBLE);
      } else {
        daysOfWeekView.setVisibility(View.GONE);
      }

      // Display the label
      TextView labelView =
          (TextView) view.findViewById(R.id.label);
      labelView.setTypeface(App.skrift_gibson);
      if (alarm.label != null && alarm.label.length() != 0) {
        labelView.setText(alarm.label);
        labelView.setVisibility(View.VISIBLE);
      } else {
        labelView.setVisibility(View.GONE);
      }
      ((TextView) view.findViewById(R.id.timeDisplay)).setTypeface(App.skrift_gibson);
      return view;
    }
  }

  ;

  @Override
  public boolean onContextItemSelected(final MenuItem item) {
    final AdapterContextMenuInfo info =
        (AdapterContextMenuInfo) item.getMenuInfo();
    final int pos = (int) info.position;
    // Error check just in case.
    if (pos == -1) {
      return super.onContextItemSelected(item);
    }
    final Alarm alarm = Alarms.alarmer.get(pos);
    if (item.getItemId()==R.id.delete_alarm) {
        // Confirm that the alarm will be deleted.
        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_alarm))
            .setMessage(getString(R.string.delete_alarm_confirm))
            .setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface d, int w) {
                    Alarms.deleteAlarm(AlarmClock_akt.this, alarm.id);
                    adapter.notifyDataSetChanged(); // hack
                  }
                })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
        return true;
      }

    if (item.getItemId()==R.id.enable_alarm) {
        alarm.enabled = !alarm.enabled;
        if (alarm.enabled) {
          alarm.time = Alarms.calculateAlarm(alarm.hour, alarm.minutes, alarm.daysOfWeek).getTimeInMillis();
          SetAlarm_akt.popAlarmSetToast(this, alarm.time);
        }
        Alarms.setAlarm(this, alarm);
        adapter.notifyDataSetChanged();
        return true;
      }

    if (item.getItemId()==R.id.edit_alarm) {
        Intent intent = new Intent(this, SetAlarm_akt.class);
        intent.putExtra(Alarms.ALARM_INTENT_EXTRA, alarm.toString());
        startActivity(intent);
        return true;
    }
    return super.onContextItemSelected(item);
  }

  @Override
  protected void onCreate(Bundle icicle) {
    super.onCreate(icicle);

    mFactory = LayoutInflater.from(this);
    Alarms.tjekIndlæst(this);

    updateLayout();
    App.advarEvtOmAlarmerHvisInstalleretPåSDkort(this);
  }

  private void updateLayout() {
    setContentView(R.layout.deskclock_alarm_clock);
    mAlarmsList = (ListView) findViewById(R.id.alarms_list);
    adapter = new AlarmTimeAdapter();
    mAlarmsList.setAdapter(adapter);
    mAlarmsList.setVerticalScrollBarEnabled(true);
    mAlarmsList.setOnItemClickListener(this);
    mAlarmsList.setOnCreateContextMenuListener(this);

    View addAlarm = findViewById(R.id.add_alarm);
    addAlarm.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        addNewAlarm();
      }
    });
    // Make the entire view selected when focused.
    addAlarm.setOnFocusChangeListener(new View.OnFocusChangeListener() {
      public void onFocusChange(View v, boolean hasFocus) {
        v.setSelected(hasFocus);
      }
    });
    Button doneButton = (Button) findViewById(R.id.done);
    if (doneButton != null) {
      doneButton.setOnClickListener(new OnClickListener() {
        public void onClick(View v) {
          finish();
        }
      });
    }
    doneButton.setTypeface(App.skrift_gibson);
    ((TextView) findViewById(R.id.tilføj_alarm)).setTypeface(App.skrift_gibson);
    ((TextView) findViewById(R.id.advarsel)).setTypeface(App.skrift_gibson);
  }

  @Override
  public void onResume() {
    super.onResume();
    adapter.notifyDataSetChanged(); // hack
  }

  private void addNewAlarm() {
    startActivity(new Intent(this, SetAlarm_akt.class));
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    ToastMaster.cancelToast();
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View view,
                                  ContextMenuInfo menuInfo) {
    // Inflate the menu from xml.
    getMenuInflater().inflate(R.menu.deskclock_context_menu, menu);

    // Use the current item to create a custom view for the header.
    final AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
    final Alarm alarm = Alarms.alarmer.get(info.position);

    // Construct the Calendar to compute the time.
    final Calendar cal = Calendar.getInstance();
    cal.set(Calendar.HOUR_OF_DAY, alarm.hour);
    cal.set(Calendar.MINUTE, alarm.minutes);
    final String time = Alarms.formatTime(this, cal);

    // Inflate the custom view and set each TextView's text.
    final View v = mFactory.inflate(R.layout.deskclock_context_menu_header, null);
    TextView textView = (TextView) v.findViewById(R.id.header_time);
    textView.setTypeface(App.skrift_gibson);
    textView.setText(time);
    textView = (TextView) v.findViewById(R.id.header_label);
    textView.setText(alarm.label);
    textView.setTypeface(App.skrift_gibson);

    // Set the custom view on the menu.
    menu.setHeaderView(v);
    // Change the text based on the state of the alarm.
    if (alarm.enabled) {
      menu.findItem(R.id.enable_alarm).setTitle(R.string.disable_alarm);
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
      if (item.getItemId()==android.R.id.home) {
        finish();
        return true;
      }
      if (item.getItemId()==R.id.menu_item_add_alarm) {
        addNewAlarm();
        return true;
      }
    if (item.getItemId()==R.id.menu_item_done) {
      finish();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  /*
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.deskclock_alarm_list_menu_ubrugt, menu);
    return super.onCreateOptionsMenu(menu);
  }
  */

  @Override
  public void onItemClick(AdapterView parent, View v, int pos, long id) {
    final Alarm alarm = Alarms.alarmer.get(pos);
    Intent intent = new Intent(this, SetAlarm_akt.class);
    intent.putExtra(Alarms.ALARM_INTENT_EXTRA, alarm.toString());
    startActivity(intent);
  }
}
