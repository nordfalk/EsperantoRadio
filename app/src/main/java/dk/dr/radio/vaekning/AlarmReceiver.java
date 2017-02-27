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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Date;

import dk.dr.radio.akt.Hovedaktivitet;
import dk.dr.radio.data.Programdata;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;


/**
 * Glue class: connects AlarmAlert IntentReceiver to AlarmAlert
 * activity.  Passes through Alarm ID.
 */
public class AlarmReceiver extends BroadcastReceiver {
  /**
   * If the alarm is older than STALE_WINDOW, ignore.  It
   * is probably the result of a time or timezone change
   */
  private final static int STALE_WINDOW = 30 * 60 * 1000;

  @Override
  public void onReceive(final Context context, final Intent intent) {
    try {
      Log.d("AlarmReceiver onReceive(" + intent);
      if (App.fejlsøgning) App.langToast("AlarmReceiver onReceive(" + intent);
      Programdata.instans.afspiller.vækningIGang = true;
      Programdata.instans.afspiller.vækkeurWakeLock = AlarmAlertWakeLock.createPartialWakeLock(context);
      Programdata.instans.afspiller.vækkeurWakeLock.acquire(); // preferus temon, eble 120000 ĉi tie,
      Log.d("AlarmReceiver AlarmAlertWakeLock.createPartialWakeLock()");

      if (!Alarms.ALARM_ALERT_ACTION.equals(intent.getAction())) {
        // Unknown intent, bail.
        Log.rapporterFejl(new IllegalStateException("Forventet "+Alarms.ALARM_ALERT_ACTION+" fik "+intent));
        return;
      }

      Alarm alarm = null;
      // Grab the alarm from the intent. Since the remote AlarmManagerService
      // fills in the Intent to add some extra data, it must unparcel the
      // Alarm object. It throws a ClassNotFoundException when unparcelling.
      // To avoid this, do the marshalling ourselves.
      final String data = intent.getStringExtra(Alarms.ALARM_RAW_DATA);
      if (data != null) {
        alarm = new Alarm(data);
      }

      Alarms.tjekIndlæst(context);


      if (alarm == null) {
        Log.rapporterFejl(new IllegalStateException("Failed to parse the alarm from the intent"));
        // Make sure we set the next alert if needed.
        Alarms.setNextAlert(context);
        return;
      }

      // Disable this alarm if it does not repeat.
      if (!alarm.daysOfWeek.isRepeatSet()) {
        alarm.enabled = false;
        Alarms.setAlarm(context, alarm);
      } else {
        // Enable the next alert if there is one. The above call to
        // enableAlarm will call setNextAlert so avoid calling it twice.
        alarm.time = 0; // signalerer at næste alarmtid skal beregnes
        Alarms.setAlarm(context, alarm);
//        Alarms.setNextAlert(context);
      }

      // Intentionally verbose: always log the alarm time to provide useful
      // information in bug reports.
      long now = System.currentTimeMillis();
      Log.d("Recevied alarm set for " + new Date(alarm.time));

      // Always verbose to track down time change problems.
      if (now > alarm.time + STALE_WINDOW) {
        Log.rapporterFejl(new IllegalStateException("Ignoring stale alarm"), now +" > "+alarm.time + STALE_WINDOW);
        return;
      }


      /* Close dialogs and window shade */
      Intent closeDialogs = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
      context.sendBroadcast(closeDialogs);


      // Play the alarm alert and vibrate the device.
      Intent playAlarm = new Intent(context, Hovedaktivitet.class);
      //playAlarm.putExtra(Alarms.ALARM_INTENT_EXTRA, alarm.toString());
      playAlarm.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      context.startActivity(playAlarm);


      Kanal nyKanal = Programdata.instans.grunddata.kanalFraKode.get(alarm.kanalo);
      if (nyKanal == null) {
        Log.rapporterFejl(new IllegalStateException("Alarm: Kanal findes ikke!" + alarm.kanalo + " for alarmstr=" + data));
        nyKanal = Programdata.instans.grunddata.forvalgtKanal;
      }
      Programdata.instans.afspiller.setLydkilde(nyKanal);
      Programdata.instans.afspiller.startAfspilning();

      // Skru op til 2/5 styrke hvis volumen er lavere end det
      Programdata.instans.afspiller.tjekVolumenMindst5tedele(2);

    } catch (Exception ex) {
      Log.rapporterFejl(ex);
    }
  }
}
