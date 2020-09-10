package dk.dr.radio.afspilning.wrapper;

import android.os.Build;

import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;

/**
 * Created by j on 14-11-15.
 */
public class Wrapperfabrikering {


  private static Class<? extends MediaPlayerWrapper> mediaPlayerWrapperKlasse = null;
  //if (App.prefs.getBoolean("tving_mediaplayer", false)) hvilken = Hvilken.GammelMediaPlayer;
  //if (App.prefs.getBoolean("tving_emaplayer", false)) hvilken = Hvilken.NyEmaPlayer;

  public static MediaPlayerWrapper opret() {
    return new EmaPlayerWrapper();
  }

  public static void nulstilWrapper() {
    if (App.fejlsøgning) App.kortToast(("Fjerner wrapper\n"+mediaPlayerWrapperKlasse).replaceAll("dk.dr.radio.afspilning.wrapper.",""));
    mediaPlayerWrapperKlasse = null;
  }

}
