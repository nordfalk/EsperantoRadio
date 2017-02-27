/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.content.Context;
import android.os.PowerManager;

/**
 * Hold a wakelock that can be acquired in the AlarmReceiver and
 * released in the AlarmAlert activity
 */
public class AlarmAlertWakeLock {
  public static PowerManager.WakeLock createPartialWakeLock(Context context) {
    PowerManager pm =
        (PowerManager) context.getSystemService(Context.POWER_SERVICE);
    //return pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, Log.LOGTAG);
    //if (App.PRODUKTION) App.kortToast("createPartialWakeLock");

    return pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK
        | PowerManager.ON_AFTER_RELEASE
        | PowerManager.ACQUIRE_CAUSES_WAKEUP, AlarmAlertWakeLock.class.getName());

  }
}
