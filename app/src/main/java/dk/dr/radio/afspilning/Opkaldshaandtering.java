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

import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import dk.dr.radio.diverse.Log;


/*
 * Denne klasse sørger for at stoppe afspilning hvis telefonen ringer
 */
public class Opkaldshaandtering extends PhoneStateListener {

  private Afspiller afspiller;
  private boolean venterPåKaldetAfsluttes;

  public Opkaldshaandtering(Afspiller afspiller) {
    this.afspiller = afspiller;
  }

  @Override
  public void onCallStateChanged(int state, String incomingNumber) {
    Status afspilningsstatus = afspiller.getAfspillerstatus();
    Log.d("Opkaldshaandtering " + state + " afspilningsstatus=" + afspilningsstatus+" nummer="+incomingNumber);
    switch (state) {
      case TelephonyManager.CALL_STATE_OFFHOOK:
      case TelephonyManager.CALL_STATE_RINGING:
        Log.d("Opkald i gang");
        if (afspilningsstatus != Status.STOPPET) {
          venterPåKaldetAfsluttes = true;
          afspiller.pauseAfspilning();
        }
        break;
      case TelephonyManager.CALL_STATE_IDLE:
        Log.d("Idle state detected");
        if (venterPåKaldetAfsluttes) {
          try {
            afspiller.startAfspilning();
          } catch (Exception e) {
            Log.e(e);
          }
          venterPåKaldetAfsluttes = false;
        }
    }
  }
}
