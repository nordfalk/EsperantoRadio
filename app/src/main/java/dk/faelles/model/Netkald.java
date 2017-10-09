package dk.faelles.model;

import com.android.volley.Request;
import com.android.volley.VolleyError;

import java.util.Date;

import dk.dr.radio.data.Datoformater;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.net.volley.DrVolleyResonseListener;
import dk.dr.radio.net.volley.DrVolleyStringRequest;
import dk.dr.radio.net.volley.Netsvar;

/**
 * Created by j on 09-10-17.
 */

public class Netkald {

  public void startHentUdsendelserPåKanal(Object kalder, final Kanal kanal, final Date dato, final NetsvarBehander svarBehander) {
    final String datoStr = Datoformater.apiDatoFormat.format(dato);
    //if (kanal.harUdsendelserForDag(datoStr)) { // brug værdier i RAMen
      //xxx svarBehander.fikSvar(new Svar());//opdaterListe();
    //}

    final String url = kanal.getBackend().getUdsendelserPåKanalUrl(kanal, datoStr);
    if (App.fejlsøgning) Log.d("startHentUdsendelserPåKanal url=" + url);

    Request<?> req = new DrVolleyStringRequest(url, new DrVolleyResonseListener() {
      @Override
      public void fikSvar(Netsvar s) throws Exception {
        // Log.d(kanal + " hentSendeplanForDag fikSvar for url " + url + " fraCache=" + fraCache+":\n"+json);
        if (!s.uændret) {
          if (s.json != null && !"null".equals(s.json)) {
            kanal.setUdsendelserForDag(kanal.getBackend().parseUdsendelserForKanal(s.json, kanal, dato, App.data), datoStr);
          }
        }
        if (kanal.harUdsendelserForDag(datoStr) && s.fraCache) return; // så er værdierne i RAMen gode nok
      }
    }).setTag(kalder);
    //Log.d("hentSendeplanForDag 2 " + (System.currentTimeMillis() - App.opstartstidspunkt) + " ms");
    App.volleyRequestQueue.add(req);

  }
}
