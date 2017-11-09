package dk.dr.radio.diverse;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.view.inputmethod.InputMethodManager;

import java.util.Locale;

/**
 * Created by j on 09-11-17.
 */

public class Talesyntese {
  private TextToSpeech tts;
  private boolean initialiseret;

  public void init(Context ctx) {
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


          if (initialiseret) {
            Locale sprog = tts.getLanguage();
            tts.speak("Tekst til tale initialiseret for sproget " + sprog.getDisplayLanguage(sprog), TextToSpeech.QUEUE_ADD, null);
          }
        }
      }
    });
  }

  public void tal(String tekst) {
    if (!initialiseret) return;
    tts.speak(tekst, TextToSpeech.QUEUE_ADD, null);
  }
}
