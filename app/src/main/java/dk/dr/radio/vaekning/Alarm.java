/*
 * Copyright (C) 2009 The Android Open Source Project
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

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.DateFormatSymbols;
import java.util.Calendar;

import dk.dr.radio.v3.R;

public final class Alarm {
  final static int INVALID_ALARM_ID = -1;
  // Public fields
  public int id;
  public boolean enabled;
  public int hour;
  public int minutes;
  public DaysOfWeek daysOfWeek;
  public long time;
  public String kanalo = "";
  public String label = "";

  @Override
  public String toString() {
    return "" + id
        + "/" + (enabled ? 1 : 0)
        + "/" + hour
        + "/" + minutes
        + "/" + daysOfWeek.getCoded()
        + "/" + time
        + "/=" + URLEncoder.encode(kanalo)
        + "/=" + URLEncoder.encode(label)
        + "/";
  }

  public Alarm(String s) {
    String[] v = s.split("/");
    int n = 0;
    id = Integer.parseInt(v[n++]);
    enabled = v[n++].equals("1");
    hour = Integer.parseInt(v[n++]);
    minutes = Integer.parseInt(v[n++]);
    daysOfWeek = new DaysOfWeek(Integer.parseInt(v[n++]));
    time = Long.parseLong(v[n++]);
    kanalo = URLDecoder.decode(v[n++].substring(1));
    label = URLDecoder.decode(v[n++].substring(1));
  }

  // Creates a default alarm at the current time.
  public Alarm() {
    id = INVALID_ALARM_ID;
    Calendar c = Calendar.getInstance();
    c.setTimeInMillis(System.currentTimeMillis());
    hour = c.get(Calendar.HOUR_OF_DAY);
    minutes = c.get(Calendar.MINUTE);
    daysOfWeek = new DaysOfWeek(0);
  }

  public String getLabelOrDefault(Context context) {
    if (label == null || label.length() == 0) {
      return context.getString(R.string.default_label);
    }
    return label;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Alarm)) return false;
    final Alarm other = (Alarm) o;
    return id == other.id;
  }


  /*
   * Days of week code as a single int.
   * 0x00: no day
   * 0x01: Monday
   * 0x02: Tuesday
   * 0x04: Wednesday
   * 0x08: Thursday
   * 0x10: Friday
   * 0x20: Saturday
   * 0x40: Sunday
   */
  static final class DaysOfWeek {
    private static int[] DAY_MAP = new int[]{
        Calendar.MONDAY,
        Calendar.TUESDAY,
        Calendar.WEDNESDAY,
        Calendar.THURSDAY,
        Calendar.FRIDAY,
        Calendar.SATURDAY,
        Calendar.SUNDAY,};
    // Bitmask of all repeating days
    private int mDays;

    DaysOfWeek(int days) {
      mDays = days;
    }

    public String toString(Context context, boolean showNever) {
      StringBuilder ret = new StringBuilder();

      // no days
      if (mDays == 0) {
        return showNever
            ? context.getText(R.string.never).toString() : "";
      }

      // every day
      if (mDays == 0x7f) {
        return context.getText(R.string.every_day).toString();
      }

      // count selected days
      int dayCount = 0, days = mDays;
      while (days > 0) {
        if ((days & 1) == 1) dayCount++;
        days >>= 1;
      }

      // short or long form?
      DateFormatSymbols dfs = new DateFormatSymbols();
      String[] dayList = (dayCount > 1)
          ? dfs.getShortWeekdays()
          : dfs.getWeekdays();

      // selected days
      for (int i = 0; i < 7; i++) {
        if ((mDays & (1 << i)) != 0) {
          ret.append(dayList[DAY_MAP[i]]);
          dayCount -= 1;
          if (dayCount > 0) ret.append(
              context.getText(R.string.day_concat));
        }
      }
      return ret.toString();
    }

    private boolean isSet(int day) {
      return ((mDays & (1 << day)) > 0);
    }

    public void set(int day, boolean set) {
      if (set) {
        mDays |= (1 << day);
      } else {
        mDays &= ~(1 << day);
      }
    }

    public void set(DaysOfWeek dow) {
      mDays = dow.mDays;
    }

    public int getCoded() {
      return mDays;
    }

    // Returns days of week encoded in an array of booleans.
    public boolean[] getBooleanArray() {
      boolean[] ret = new boolean[7];
      for (int i = 0; i < 7; i++) {
        ret[i] = isSet(i);
      }
      return ret;
    }

    public boolean isRepeatSet() {
      return mDays != 0;
    }

    /**
     * returns number of days from today until next alarm
     * @param c must be set to today
     */
    public int getNextAlarm(Calendar c) {
      if (mDays == 0) {
        return -1;
      }

      int today = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7;

      int day = 0;
      int dayCount = 0;
      for (; dayCount < 7; dayCount++) {
        day = (today + dayCount) % 7;
        if (isSet(day)) {
          break;
        }
      }
      return dayCount;
    }
  }

}
