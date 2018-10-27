package dk.dr.radio.diverse;

import android.os.AsyncTask;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;

import dk.dr.radio.data.Lydstream;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.data.esperanto.EoKanal;

/**
 * Bedaŭrinde la rektaj elsendoj de Muzaiko estas nur haveblaj en certaj landoj.
 * Jen mi esploras ĉu iu elsendo estas blokata aŭ ne.
 */

public class EoGeoblokaDetektilo {
  private static final String NE_BLOKITA = "NE_BLOKITA";
  private static final String BLOKITA = "BLOKITA";
  private static final HashSet<String> esploritajUrl = new HashSet<>();
  private static final HashSet<String> blokitajUrl = new HashSet<>();

  public static void esploruĈuEstasBlokata(EoKanal kanal, final Udsendelse udsendelse, final String alternativaUrl) {
    if (udsendelse.rektaElsendaPriskriboUrl == null) return;
    if (udsendelse.berigtigelseTekst != null) return;

    udsendelse.berigtigelseTekst = NE_BLOKITA;

    try {
      final Lydstream stream = udsendelse.findBedsteStreams(false).get(0);
      final Lydstream stream1 = kanal.findBedsteStreams(false).get(0);
      if (blokitajUrl.contains(stream.url)) {
        udsendelse.berigtigelseTekst = BLOKITA;
      }
      if (!esploritajUrl.contains(stream.url)) {
        esploritajUrl.add(stream.url);
        new AsyncTask() {
          @Override
          protected Object doInBackground(Object[] params) {
            try {
              HttpURLConnection uc = (HttpURLConnection) new URL(stream.url).openConnection();
              uc.setInstanceFollowRedirects(false);
              uc.connect();
              if (uc.getResponseMessage().contains("oved")  // 302 Temporarily Moved
                      && (uc.getHeaderFields().toString().contains("geoblock"))) {
                //  [HTTP/1.0 302 Temporarily Moved], Content-Type=[text/html], Location=[http://streaming.radionomy.com/geoblocking.mp3?mount=Muzaiko],
                blokitajUrl.add(stream.url);
                if (alternativaUrl!=null && !alternativaUrl.isEmpty()) {
                  stream.url = alternativaUrl;
                  stream1.url = alternativaUrl;
                } else {
                  udsendelse.berigtigelseTekst = BLOKITA;
                }
                esploritajUrl.add(stream.url);
              }
              uc.disconnect();
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
