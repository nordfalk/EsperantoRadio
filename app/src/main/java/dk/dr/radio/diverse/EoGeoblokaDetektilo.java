package dk.dr.radio.diverse;

import android.os.AsyncTask;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import dk.dr.radio.data.Lydstream;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.data.esperanto.EoKanal;

/**
 * Bedaŭrinde la rektaj elsendoj de Muzaiko estas nur haveblaj en certaj landoj.
 * Jen mi esploras ĉu iu elsendo estas blokata aŭ ne.
 */

public class EoGeoblokaDetektilo {
  private static final HashSet<String> esploritajUrl = new HashSet<>();
  private static final HashSet<String> blokitajUrl = new HashSet<>();
  private static final HashMap<String,String> blokitajAlMalblokitajUrl = new HashMap<>();

  public static boolean estasBlokataKajNeEblasMalbloki(Udsendelse udsendelse) {
    final Lydstream stream = udsendelse.findBedsteStreams(false).get(0);
    if (!esploritajUrl.contains(stream.url)) {
      Log.d(udsendelse + " kun "+stream.url + " ne estis esplorita");
      return false;
    }

    if (!blokitajUrl.contains(stream.url)) {
      Log.d(udsendelse + " kun " + stream.url + " ne estis blokita");
      return false;
    }

    Log.d(udsendelse + " kun "+stream.url + " estas blokita");
    String alternativaUrl = blokitajAlMalblokitajUrl.get(stream.url);
    if (alternativaUrl == null) {
      Log.d("Ne estis alternativaUrl");
      return true;
    }
    Log.d("Estas alternativaUrl "+alternativaUrl);
    stream.url = alternativaUrl;
    return false;
  }

  public static void esploruĈuEstasBlokata(EoKanal kanal, final Udsendelse udsendelse, final String alternativaUrl) {

    try {
      final Lydstream stream = udsendelse.findBedsteStreams(false).get(0);
      if (!esploritajUrl.contains(stream.url)) {
        esploritajUrl.add(stream.url);
        new AsyncTask() {
          @Override
          protected Object doInBackground(Object[] params) {
            try {
              HttpURLConnection uc = (HttpURLConnection) new URL(stream.url).openConnection();
              uc.setInstanceFollowRedirects(false);
              uc.connect();
              String responseMessage = uc.getResponseMessage();
              Map<String, List<String>> headerFields = uc.getHeaderFields();
              Log.d(udsendelse + " kun "+stream.url + " donas "+responseMessage + " " + headerFields);
              if (responseMessage.contains("oved")  // 302 Temporarily Moved
                      && (headerFields.toString().contains("geoblock"))) {
                //  [HTTP/1.0 302 Temporarily Moved], Content-Type=[text/html], Location=[http://streaming.radionomy.com/geoblocking.mp3?mount=Muzaiko],
                blokitajUrl.add(stream.url);
                Log.d( udsendelse + " kun "+stream.url + " estas blokita. alternativaUrl="+alternativaUrl);
                if (alternativaUrl!=null && !alternativaUrl.isEmpty()) {
                  blokitajAlMalblokitajUrl.put(stream.url, alternativaUrl);
                  stream.url = alternativaUrl;
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
}
