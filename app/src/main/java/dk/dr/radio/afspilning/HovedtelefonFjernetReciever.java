/**
 DR Radio 2 is developed by Jacob Nordfalk, Hanafi Mughrabi and Frederik Aagaard.
 Some parts of the code are loosely based on Sveriges Radio Play for Android.

 DR Radio 2 for Android is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License version 2 as published by
 the Free Software Foundation.

 DR Radio 2 for Android is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 See the GNU General Public License for more details.

 You should have received a copy of the GNU General Public License along with
 DR Radio 2 for Android.  If not, see <http://www.gnu.org/licenses/>.

 */

package dk.dr.radio.afspilning;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;

import dk.dr.radio.data.Programdata;
import dk.dr.radio.diverse.App;

/**
 * Appen skal stoppe med at spille når man tager hovedtelefoner fra telefonen.
 * Se http://developer.android.com/reference/android/media/AudioManager.html#ACTION_AUDIO_BECOMING_NOISY
 */
public class HovedtelefonFjernetReciever extends BroadcastReceiver {

  final IntentFilter FILTER = new IntentFilter();
  {
    FILTER.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
  }
  static boolean aktiv;


  @Override
  public void onReceive(Context context, Intent intent) {
    App.langToast("HovedtelefonFjernetReciever "+intent);

    if (App.prefs.getBoolean("Stop når hovedtelefoner fjernes", true)==false) return;
    if (intent.getAction().equals(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
      if (Programdata.instans.afspiller.getAfspillerstatus() != Status.STOPPET) {
        Programdata.instans.afspiller.pauseAfspilning();
      }
    }
  }
}