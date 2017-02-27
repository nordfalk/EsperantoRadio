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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.format.DateFormat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import dk.dr.radio.data.Programdata;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;


/**
 * The Alarms provider supplies info about Alarm Clock settings
 */
public class Alarms {
  public static ArrayList<Alarm> alarmer;
  /*
   public static ArrayList<Alarm> alarmer = new ArrayList<Alarm>();

   static {
   Alarm a = new Alarm();
   a.daysOfWeek.set(4, true);
   a.enabled = true;
   a.hour = 17;
   a.minutes = 2;
   alarmer.add(a);
   }*/
  // This action triggers the AlarmReceiver as well as the AlarmKlaxon. It
  // is a public action used in the manifest for receiving Alarm broadcasts
  // from the alarm manager.
  public static final String ALARM_ALERT_ACTION = "dk.dr.radio.ALARM_ALERT";
  // This string is used when passing an Alarm object through an intent.
  public static final String ALARM_INTENT_EXTRA = "intent.extra.alarm";
  // This extra is the raw Alarm object data. It is used in the
  // AlarmManagerService to avoid a ClassNotFoundException when filling in
  // the Intent extras.
  public static final String ALARM_RAW_DATA = "intent.extra.alarm_raw";
  private final static String DM12 = "E h:mm aa";
  private final static String DM24 = "E kk:mm";
  private final static String M12 = "h:mm aa";
  // Shared with DigitalClock
  final static String M24 = "kk:mm";
  public static long næsteAktiveAlarm;

  /**
   * Creates a new Alarm and fills in the given alarm's id.
   */
  public static long addAlarm(Context context, Alarm alarm) {
    alarm.id = (int) System.currentTimeMillis(); // pseudounik ID
    alarmer.add(alarm);
    long timeInMillis = calculateAlarm(alarm);
    setNextAlert(context);
    gemAlarmer(context);
    return timeInMillis;
  }

  /**
   * Removes an existing Alarm.  If this alarm is snoozing, disables
   * snooze.  Sets next alert.
   */
  public static void deleteAlarm(Context context, int alarmId) {
    //Log.v("deleteAlarm id="+alarmId);
    boolean trovita = false;

    if (alarmId == Alarm.INVALID_ALARM_ID) return;
    for (int n = 0; n < alarmer.size(); n++) {
      Alarm a = alarmer.get(n);
      if (a.id == alarmId) {
        alarmer.remove(n);
        trovita = true;
        break;
      }
    }
    if (!trovita) Log.e("Ne trovis", new IllegalStateException("id ne eksitstis: " + alarmId));

    setNextAlert(context);
    gemAlarmer(context);
  }

  public static SharedPreferences prefs(final Context context) {
    SharedPreferences prefs = context.getSharedPreferences(AlarmClock_akt.PREFERENCES, 0);
    return prefs;
  }

  /**
   * A convenience method to set an alarm in the Alarms
   * content provider.
   * @return Time when the alarm will fire.
   */
  public static long setAlarm(Context context, Alarm alarm) {
    for (int n = 0; n < alarmer.size(); n++) {
      Alarm a = alarmer.get(n);
      if (a.id == alarm.id) {
        alarmer.set(n, alarm);
        break;
      }
    }

    long timeInMillis = calculateAlarm(alarm);

    setNextAlert(context);
    gemAlarmer(context);

    return timeInMillis;
  }

  /**
   * A convenience method to enable or disable an alarm.
   */
  private static Alarm calculateNextAlert(final Context context) {
    long minTime = Long.MAX_VALUE;
    long now = System.currentTimeMillis();

    tjekIndlæst(context);

    Alarm alarm = null;

    for (Alarm a : alarmer) {
      if (!a.enabled) continue;

      // A time of 0 indicates this is a repeating alarm, so
      // calculate the time to get the next alert.
      if (a.time == 0) {
        a.time = calculateAlarm(a);
      }

      if (a.time < now) {
        Log.d("Disabling expired alarm set for " + new Date(a.time));
        // Expired alarm, disable it and move along.
        a.enabled = false;
        Alarms.setAlarm(context, a);
        continue;
      }
      if (a.time < minTime) {
        minTime = a.time;
        alarm = a;
      }
    }

    return alarm;
  }

  public static void tjekIndlæst(final Context context) {
    if (alarmer == null) {
      alarmer = new ArrayList<Alarm>();
      String alarmoj = prefs(context).getString("alarmoj", null);
      if (alarmoj == null) try {
        alarmoj = Programdata.instans.grunddata.json.getString("sugestoj_por_alarmoj");
      } catch (Exception e) {
        Log.e("Rezignas pri sugestoj_por_alarmoj!", e);
        alarmoj = "";
      }
      Log.d("tjekIndlæst alarmo=\n" + alarmoj);
      for (String alarmo : alarmoj.split("\n"))
        try {
          alarmo = alarmo.trim();
          if (alarmo.length() == 0) continue;
          alarmer.add(new Alarm(alarmo));
        } catch (Exception e) {
          Log.e(e);
        }
    }
  }

  public static void gemAlarmer(final Context context) {
    StringBuilder sb = new StringBuilder();
    for (Alarm a : alarmer) sb.append(a + "\n");
    prefs(context).edit().putString("alarmoj", sb.toString()).commit();
    Log.d("gemAlarmer alarmo=\n" + sb.toString());
  }

  /**
   * Called at system startup, on time/timezone change, and whenever
   * the user changes alarm settings.  Activates snooze if set,
   * otherwise loads all alarms, activates next alert.
   */
  public static void setNextAlert(final Context context) {
    final Alarm alarm = calculateNextAlert(context);
    if (alarm != null) {
      enableAlert(context, alarm, alarm.time);
      næsteAktiveAlarm = alarm.time;
    } else {
      disableAlert(context);
      næsteAktiveAlarm = 0;
    }
    //if (!App.PRODUKTION) App.kortToast("enAlarmErAktiv  "+ næsteAktiveAlarm);
  }

  /**
   * Sets alert in AlarmManger and StatusBar.  This is what will
   * actually launch the alert when the alarm triggers.
   * @param alarm          Alarm.
   * @param atTimeInMillis milliseconds since epoch
   */
  private static void enableAlert(Context context, final Alarm alarm,
                                  final long atTimeInMillis) {
    AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

    Log.d("** setAlert atTime " + new Date(atTimeInMillis) + " -- " + alarm);

    Intent intent = new Intent(ALARM_ALERT_ACTION);

    intent.putExtra(ALARM_RAW_DATA, alarm.toString());

    PendingIntent sender = PendingIntent.getBroadcast(
        context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

    am.set(AlarmManager.RTC_WAKEUP, atTimeInMillis, sender);

    setStatusBarIcon(context, true);
    Calendar c = Calendar.getInstance();
    c.setTimeInMillis(atTimeInMillis);

    Kanal nyKanal = Programdata.instans.grunddata.kanalFraKode.get(alarm.kanalo);
    if (nyKanal == null) {
      Log.rapporterFejl(new IllegalStateException("Alarm: Kanal findes ikke!"), alarm.kanalo + " var ikke i "+ Programdata.instans.grunddata.kanalFraKode.keySet() );
      nyKanal = Programdata.instans.grunddata.forvalgtKanal;
    }

    String message = nyKanal.navn +"\n"+DateFormat.format(DM24, c);
    if (App.fejlsøgning) App.kortToast("Næste alarm sat til:\n"+message);
/*  writing to settings requires android.permission.WRITE_SETTINGS
    Settings.System.putString(App.instans.getContentResolver(), Settings.System.NEXT_ALARM_FORMATTED, message);
    */
  }

  /**
   * Disables alert in AlarmManger and StatusBar.
   */
  static void disableAlert(Context context) {
    AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    PendingIntent sender = PendingIntent.getBroadcast(
        context, 0, new Intent(ALARM_ALERT_ACTION),
        PendingIntent.FLAG_CANCEL_CURRENT);
    am.cancel(sender);
    setStatusBarIcon(context, false);
  }

  /**
   * Tells the StatusBar whether the alarm is enabled or disabled
   */
  private static void setStatusBarIcon(Context context, boolean enabled) {
    Intent alarmChanged = new Intent("android.intent.action.ALARM_CHANGED");
    alarmChanged.putExtra("alarmSet", enabled);
    context.sendBroadcast(alarmChanged);
  }

  private static long calculateAlarm(Alarm alarm) {
    return calculateAlarm(alarm.hour, alarm.minutes, alarm.daysOfWeek)
        .getTimeInMillis();
  }

  /**
   * Given an alarm in hours and minutes, return a time suitable for
   * setting in AlarmManager.
   */
  static Calendar calculateAlarm(int hour, int minute,
                                 Alarm.DaysOfWeek daysOfWeek) {

    // start with now
    Calendar c = Calendar.getInstance();
    c.setTimeInMillis(System.currentTimeMillis());

    int nowHour = c.get(Calendar.HOUR_OF_DAY);
    int nowMinute = c.get(Calendar.MINUTE);

    // if alarm is behind current time, advance one day
    if (hour < nowHour
        || hour == nowHour && minute <= nowMinute) {
      c.add(Calendar.DAY_OF_YEAR, 1);
    }
    c.set(Calendar.HOUR_OF_DAY, hour);
    c.set(Calendar.MINUTE, minute);
    c.set(Calendar.SECOND, 0);
    c.set(Calendar.MILLISECOND, 0);

    int addDays = daysOfWeek.getNextAlarm(c);
    if (addDays > 0) c.add(Calendar.DAY_OF_WEEK, addDays);
    return c;
  }

  static String formatTime(final Context context, int hour, int minute,
                           Alarm.DaysOfWeek daysOfWeek) {
    Calendar c = calculateAlarm(hour, minute, daysOfWeek);
    return formatTime(context, c);
  }

  /* used by AlarmAlert */
  static String formatTime(final Context context, Calendar c) {
    String format = get24HourMode(context) ? M24 : M12;
    return (c == null) ? "" : (String) DateFormat.format(format, c);
  }

  /**
   * @return true if clock is set to 24-hour mode
   */
  static boolean get24HourMode(final Context context) {
    return DateFormat.is24HourFormat(context);
  }
}
