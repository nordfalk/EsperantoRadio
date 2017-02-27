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

package dk.dr.radio.diverse;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.AudioManager;
import android.os.Build;

import com.crashlytics.android.Crashlytics;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.LinkedHashSet;

import dk.dr.radio.data.Programdata;

/**
 * Loggerklasse
 * - hvor man slipper for at angive tag
 * - man kan logge objekter (få kaldt toString)
 * - cirkulær buffer tillader at man kan gemme loggen til fejlrapportering
 * @author j
 */
public class Log {
  public static final String TAG = "EoRadio";

  private static final StringBuilder log = new StringBuilder(18000);

  /**
   * Føjer data til loggen.
   * Er loggen blevet for lang trimmes den.
   * Er synkroniseret da der enkelte gange er blevet set crashes fordi der blev skrevet
   * loggen samtidig med at den var ved at blive trimmet.
   * Af performancehensyn bør logning nok begrænses til kun at omfatte det vi som udviklere
   * tror vil afhjælpe en evt senere fejlfinding
   */
  private static synchronized void logappend(String s) {
    if (log.length() > 57500) {
      log.delete(0, 10000);
    }
    // Roterende log
    int n = s.length();
    if (n > 10000) n = 10000;
    log.append(s, 0, n);
    log.append('\n');
  }

  public static synchronized String getLog() {
    return log.toString();
  }

  /**
   * Logfunktion uden TAG som tager et objekt. Sparer bytekode og tid
   */
  public static void d(Object o) {
    String s = String.valueOf(o);
    logappend(s);
    if (App.instans == null || App.IKKE_Android_VM) {
      System.out.println(o);
      return; // Hop ud hvis vi ikke kører i en Android VM
    }
    android.util.Log.d(TAG, s);
  }

  public static void e(Throwable e) {
    e("fejl", e);
  }

  public static void e(String tekst, Throwable e) {
    if (e == null) e = new Exception(tekst);
    if (App.instans == null || App.IKKE_Android_VM) {
      System.err.println(tekst);
      e.printStackTrace();
      return; // Hop ud hvis vi ikke kører i en Android VM
    }
    android.util.Log.e(TAG, tekst, e);
    //e.printStackTrace();
    logappend(tekst);
    logappend(android.util.Log.getStackTraceString(e));
  }


  static int fejlRapporteret = 0;

  public static void rapporterFejl(final Throwable e) {
    Log.e(e);
    if (fejlRapporteret++ > 2) return; // rapportér ikke mere end 2 fejl per kørsel
    if (!App.EMULATOR) {
      Crashlytics.logException(e);
      //Mint.logException(e);
      if (!App.PRODUKTION && App.instans!=null) App.langToast("fejl: " + e);
    }
  }

  public static void rapporterFejl(final Throwable e, final Object f) {
    Log.e("" + f, e);
    if (fejlRapporteret++ > 2) return; // rapportér ikke mere end 2 fejl per kørsel
    if (!App.EMULATOR) {
      Crashlytics.log("fejl " + f);
      Crashlytics.logException(e);
      //Mint.logExceptionMessage("fejl", "" + f, e);
    }
    if (!App.PRODUKTION && App.instans!=null) App.langToast("Fejl: " + f);
  }


  public static void rapporterOgvisFejl(final Activity akt, final Exception e) {
    if (!App.EMULATOR) {
      Crashlytics.logException(e);
      //Mint.logException(e);
    }
    Log.e(e);

    Builder ab = new Builder(akt);
    ab.setTitle("Beklager, der skete en fejl");
    ab.setMessage(e.toString());
    ab.setNegativeButton("Fortsæt", null);
    ab.setPositiveButton("Indsend fejl", new Dialog.OnClickListener() {
      public void onClick(DialogInterface arg0, int arg1) {
        String brødtekst = "Skriv, hvad der skete:\n\n\n---\n";
        brødtekst += "\nFejlspor;\n" + android.util.Log.getStackTraceString(e);
        brødtekst += "\n\n" + lavKontaktinfo();
        App.kontakt(akt, "Fejl DR Radio", brødtekst, Log.log.toString());
      }

    });
    ab.create().show();
  }


  public static void fejlantagelse(String fejl) {
    IllegalStateException e = new IllegalStateException(fejl);
    if (!App.PRODUKTION) throw e;
    else Log.rapporterFejl(e);
  }

  //private static LinkedHashMap<String, String> afprøvedeTing = new LinkedHashMap<String, String>();

  public static final void registrérTestet(String hvad, String res) {
    return; //afprøvedeTing.put(hvad, res);
  }

  public static String lavKontaktinfo() {
    String ret = "";
    try {

      /*
      for (String afprøvet : afprøvedeTing.keySet()) {
        ret += "\n" + afprøvet + ": " + afprøvedeTing.get(afprøvet);
      }
      ret += "\nOvenstående er korrekt: JA/NEJ\n\n";

      PackageManager pm = instans.getPackageManager();
      String version;
      try {
        PackageInfo pi = pm.getPackageInfo(instans.getPackageName(), 0);
        version = pi.versionName;
      } catch (Exception e) {
        version = e.toString();
        e.printStackTrace();
      }

      ret += instans.getPackageName() + " (v " + version + ")" + "\nTelefonmodel: " + Build.MODEL + " " + Build.PRODUCT + "\nAndroid v" + Build.VERSION.RELEASE + " (sdk: " + Build.VERSION.SDK + ")";
      */
      AudioManager am = (AudioManager) App.instans.getSystemService(Context.AUDIO_SERVICE);
      LinkedHashSet<String> udgange = new LinkedHashSet<>();
      if (am.isBluetoothA2dpOn()) udgange.add("BluetoothA2dp");
      if (am.isBluetoothScoOn()) udgange.add("BluetoothSco");
      if (am.isSpeakerphoneOn()) udgange.add("Speakerphone");
      if (am.isWiredHeadsetOn()) udgange.add("(Kablede høretelefoner forbundne)");

      ret += "\nVersion: "+App.versionsnavn +
          "\nTelefonmodel: " + Build.MODEL + " " + Build.PRODUCT +
          "\nAndroid v" + Build.VERSION.RELEASE + " (sdk: " + Build.VERSION.SDK_INT + ")";
      ret += "\nFunktioner brugt: "+ Sidevisning.getViste();
  //    ret += "\nFunktioner ej brugt: "+ Sidevisning.getIkkeViste();
      ret += "\nIndstillinger: "+ App.prefs.getAll();
      ret += "\nAfspiller: "+ Programdata.instans.afspiller.toString();
      ret += "\nUdgange: "+ udgange;
    } catch (Exception e) {
      Log.rapporterFejl(e);
    }
    return ret;
  }

  /**
   * Læser logcat, for Jelly Bean og senere kun fra *egen* proces, og behøver ikke nogen tilladelser.
   * For tidligere udgaver af Android skulle tilladelsen READ_LOGS med i manifestet,
   * og man fik log fra alle processer (hvilket var en sikkerhedsrisiko)
   * @param log
   */
  public static void læsLogcat(StringBuilder log) {
    try {
      Process process = Runtime.getRuntime().exec("logcat -d");
      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

      String line = "";
      while ((line = bufferedReader.readLine()) != null) {
        log.append(line).append("\n");
      }
    } catch (Exception e) {
      Log.e(e);
    }
  }
}
