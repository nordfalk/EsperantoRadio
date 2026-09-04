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
import android.os.PowerManager.WakeLock;

import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;

public class AlarmInitReceiver extends BroadcastReceiver {
  /**
   * Sets alarm on ACTION_BOOT_COMPLETED.  Resets alarm on
   * TIME_SET, TIMEZONE_CHANGED
   */
  @Override
  public void onReceive(final Context context, Intent intent) {
    final String action = intent.getAction();
    Log.d("AlarmInitReceiver" + action);
    if (App.fejlsøgning) App.langToast("AlarmInitReceiver onReceive(" + intent);

    //final PendingResult result = goAsync();
    final WakeLock wl = AlarmAlertWakeLock.createPartialWakeLock(context);
    if (wl!=null) wl.acquire(); // fix for https://mint.splunk.com/dashboard/project/cd78aa05/errors/3315048120

    Alarms.setNextAlert(context);
    //      result.finish();
    Log.d("AlarmInitReceiver finished");
    if (wl!=null) wl.release(); // fix for https://mint.splunk.com/dashboard/project/cd78aa05/errors/3315048120
  }
}
