package dk.dr.radio.diverse;

import android.os.AsyncTask;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;

import dk.dr.radio.data.Lydkilde;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.Log;

/**
 * Bedaŭrinde la rektaj elsendoj de Muzaiko estas nur haveblaj en certaj landoj.
 * Jen mi esploras ĉu iu elsendo estas blokata aŭ ne.
 */

public class EoGeoblokaDetektilo {
  private static final String NE_BLOKITA = "NE_BLOKITA";
  private static final String BLOKITA = "BLOKITA";
  private static final HashSet<String> esploritajUrl = new HashSet<>();
  private static final HashSet<String> blokitajUrl = new HashSet<>();

  public static void esploruĈuEstasBlokata(final Udsendelse udsendelse) {
    if (udsendelse.rektaElsendaPriskriboUrl == null) return;
    if (udsendelse.berigtigelseTekst != null) return;

    udsendelse.berigtigelseTekst = NE_BLOKITA;

    try {
      final String url = udsendelse.findBedsteStreams(false).get(0).url;
      if (blokitajUrl.contains(url)) {
        udsendelse.berigtigelseTekst = BLOKITA;
      }
      if (!esploritajUrl.contains(url)) {
        esploritajUrl.add(url);
        new AsyncTask() {
          @Override
          protected Object doInBackground(Object[] params) {
            try {
              HttpURLConnection uc = (HttpURLConnection) new URL(url).openConnection();
              uc.setInstanceFollowRedirects(false);
              uc.connect();
              if (uc.getResponseMessage().contains("oved")  // 302 Temporarily Moved
                      && (uc.getHeaderFields().toString().contains("geoblock"))) {
                //  [HTTP/1.0 302 Temporarily Moved], Content-Type=[text/html], Location=[http://streaming.radionomy.com/geoblocking.mp3?mount=Muzaiko],
                udsendelse.berigtigelseTekst = BLOKITA;
                blokitajUrl.add(url);
              }
            } catch (Exception e) { Log.rapporterFejl(e); }
            return null;
          }
        }.execute();
      }
    } catch (Exception e) { Log.rapporterFejl(e); return; }

  }

  public static boolean estasBlokata(Udsendelse udsendelse) {
    return BLOKITA.equals(udsendelse.berigtigelseTekst);
  }

}
