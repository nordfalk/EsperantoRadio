package dk.dr.radio.afspilning.wrapper;

import android.os.Build;

import dk.dr.radio.data.Programdata;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;

/**
 * Created by j on 14-11-15.
 */
public class Wrapperfabrikering {


  private static Class<? extends MediaPlayerWrapper> mediaPlayerWrapperKlasse = null;
  enum Hvilken { GammelMediaPlayer, NyExoPlayer, NyEmaPlayer };
  private static Hvilken hvilkenSidst;

  public static MediaPlayerWrapper opret() {
    if (mediaPlayerWrapperKlasse == null) {

      Hvilken hvilken = Hvilken.NyEmaPlayer;
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) hvilken = Hvilken.GammelMediaPlayer;

      hvilkenSidst = hvilken;
      if (App.prefs.getBoolean("tving_mediaplayer", Programdata.instans.grunddata.tving_mediaplayer)) hvilken = Hvilken.GammelMediaPlayer;
      if (App.prefs.getBoolean("tving_emaplayer", Programdata.instans.grunddata.tving_emaplayer)) hvilken = Hvilken.NyEmaPlayer;

      if (hvilken==Hvilken.NyEmaPlayer) {
        mediaPlayerWrapperKlasse = EmaPlayerWrapper.class;
      } else {
        mediaPlayerWrapperKlasse = AndroidMediaPlayerWrapper.class;
      }
      if (App.fejlsøgning) App.kortToast(mediaPlayerWrapperKlasse.getSimpleName());
    }
    try {
      Log.d("MediaPlayerWrapper opret() " + mediaPlayerWrapperKlasse);
      return mediaPlayerWrapperKlasse.newInstance();
    } catch (Exception e) {
      Log.rapporterFejl(e);
    }
    return new AndroidMediaPlayerWrapper();
  }

  public static void nulstilWrapper() {
    if (App.fejlsøgning) App.kortToast(("Fjerner wrapper\n"+mediaPlayerWrapperKlasse).replaceAll("dk.dr.radio.afspilning.wrapper.",""));
    mediaPlayerWrapperKlasse = null;
  }

}
