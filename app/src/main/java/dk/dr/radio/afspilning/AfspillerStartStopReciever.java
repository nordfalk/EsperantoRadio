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

import dk.dr.radio.data.Programdata;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;

/**
 * BroadcastReceiver som aktiverer afspilleren og evt instantierer den.
 * I tilfælde af at processen har været smidt ud af hukommelsen er dette
 * her faktisk den første kode der køres, derfor er et fuldt
 * initialiseringstjek nødvendigt
 * @author j
 */
public class AfspillerStartStopReciever extends BroadcastReceiver {
  public static final String PAUSE = "pause";
  public static final String LUK = "luk";

  @Override
  public void onReceive(Context context, Intent intent) {
    Log.d("AfspillerReciever onReceive(" + intent + ") afspillerstatus =" + Programdata.instans.afspiller.afspillerstatus);
    try {
      Log.registrérTestet("10. Start/stop af afspilning via notifikation", "ja");

      if (LUK.equals(intent.getAction())) {
        Programdata.instans.afspiller.stopAfspilning(); // Stopper servicen og lukker notifikationen
        return;
      }

      if (Programdata.instans.afspiller.afspillerstatus == Status.STOPPET) {
        // DRData.instans.afspiller.startAfspilning();
        // Fix for fejl: Levende ikon/widget starte afspilning fejler første gang, hvis app'en ikke er i hukommelsen
        // Det skyldtes af kanalstreams ikke var færdigindlæst. Selv cacheded streams bliver først indlæst i en
        // Runnable postet til forgrundsståden. Det er derfor vigtigt at afspilningen startes som det SIDSTE, efter
        // at cachede streams er indlæst
        App.forgrundstråd.postDelayed(new Runnable() {
          @Override
          public void run() {
            Programdata.instans.afspiller.startAfspilning();
          }
        }, 1); // for en sikkerheds skyld. 1 millisekund skulle bringe den sidst i køen
      } else {
        if (PAUSE.equals(intent.getAction())) {
          Programdata.instans.afspiller.pauseAfspilning();
        } else {
          Programdata.instans.afspiller.stopAfspilning();
        }
        if (Programdata.instans.afspiller.afspillerlyde) Programdata.instans.afspiller.afspillerlyd.stop.start();
      }

    } catch (Exception ex) {
      Log.rapporterFejl(ex);
    }
  }
}
