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
import android.view.KeyEvent;

import dk.dr.radio.data.Programdata;
import dk.dr.radio.diverse.Log;

/**
 * Til håndtering af knapper på fjernbetjening (f.eks. på Bluetooth headset.)
 * Se også http://android-developers.blogspot.com/2010/06/allowing-applications-to-play-nicer.html
 */
public class FjernbetjeningReciever extends BroadcastReceiver {


  @Override
  public void onReceive(Context context, Intent intent) {
    KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
    Log.d("MediabuttonReciever " + event);

    if (!Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction()) || event == null || event.getAction() != KeyEvent.ACTION_DOWN) {
      return;
    }

    switch (event.getKeyCode()) {
      case KeyEvent.KEYCODE_HEADSETHOOK:
      case KeyEvent.KEYCODE_MEDIA_STOP:
      case KeyEvent.KEYCODE_MEDIA_PAUSE:
        if (Programdata.instans.afspiller.getAfspillerstatus() != Status.STOPPET) {
          Programdata.instans.afspiller.pauseAfspilning();
        }
        break;
      case KeyEvent.KEYCODE_MEDIA_PLAY:
      case KeyEvent.KEYCODE_MEDIA_REWIND:
      case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
        if (Programdata.instans.afspiller.getAfspillerstatus() == Status.STOPPET) {
          Programdata.instans.afspiller.startAfspilning();
        }
        break;
      case KeyEvent.KEYCODE_MEDIA_NEXT:
        Programdata.instans.afspiller.næste();
        break;
      case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
        Programdata.instans.afspiller.forrige();
        break;
      case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
      default:
        if (Programdata.instans.afspiller.getAfspillerstatus() == Status.STOPPET) {
          Programdata.instans.afspiller.startAfspilning();
        } else {
          Programdata.instans.afspiller.pauseAfspilning();
          if (Programdata.instans.afspiller.afspillerlyde) Programdata.instans.afspiller.afspillerlyd.stop.start();
        }
    }
  }
}