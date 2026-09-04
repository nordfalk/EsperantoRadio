package dk.dr.radio.diverse;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

/**
 * Created by j on 09-11-17.
 */

public class Talesyntese {
  private TextToSpeech tts;
  private boolean initialiseret;
  private SharedPreferences prefs;
  private boolean talesynteseAktiv;
  private int antalSætninger;
  private int talesynteseHjælpAntal;

  public void prefsÆndret() {
    talesynteseAktiv = prefs.getBoolean("talesynteseAktiv", true);
    talesynteseHjælpAntal = prefs.getInt("talesynteseHjælpAntal", 0);
  }

  public void init(Context ctx) {
    prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
    prefsÆndret();
    tts = new TextToSpeech(ctx, new TextToSpeech.OnInitListener() {
      @Override
      public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
          initialiseret = true;
          int res = tts.setLanguage(new Locale("da", ""));
          if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
            res = tts.setLanguage(Locale.getDefault());
            if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
              res = tts.setLanguage(Locale.US);
              if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                initialiseret = false;
              }
            }
          }

//          udtal("Tekst til tale initialiseret for sproget " + tts.getLanguage().getDisplayLanguage(tts.getLanguage()));
        }
      }
    });
  }

  public void udtal(String tekst) {
    if (!initialiseret || !talesynteseAktiv) return;
    tts.speak(tekst, TextToSpeech.QUEUE_ADD, null);
    antalSætninger++;
    if (antalSætninger%5==0 && talesynteseHjælpAntal<3) {
      tts.speak("Du kan slå talesyntese fra i indstillingerne", TextToSpeech.QUEUE_ADD, null);
      talesynteseHjælpAntal++;
      prefs.edit().putInt("talesynteseHjælpAntal", talesynteseHjælpAntal).apply();
    }
  }
}
