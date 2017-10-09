package dk.dk.niclas.utilities;

import android.support.v4.app.Fragment;

import com.android.volley.Request;
import com.android.volley.VolleyError;

import java.util.ArrayList;

import dk.dr.radio.data.Backend;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Udseende;
import dk.dr.radio.net.volley.DrVolleyResonseListener;
import dk.dr.radio.net.volley.DrVolleyStringRequest;
import dk.dr.radio.net.volley.Netsvar;
import dk.dr.radio.v3.R;

/**
 * This class serves as the entry-point to the backend by providing the methods needed
 * to retrieve or update the data for the application.
 */

public class NetworkHelper {

  public TV tv = Udseende.ESPERANTO ? null : new TV();


  public static class TV {
    public Backend backend = App.backend[1]; //TV Backend   - MuOnlineTVBackend

    public void startHentMestSete(final String kanalSlug, int offset, final Fragment fragment) {
      int limit = 15;
      String url = "http://www.dr.dk/mu-online/api/1.3/list/view/mostviewed?channel=" + kanalSlug + "&channeltype=TV&limit=" + limit + "&offset=" + offset;
      if (kanalSlug == null) {
        limit = 150;
        url = "http://www.dr.dk/mu-online/api/1.3/list/view/mostviewed?&channeltype=TV&limit=" + limit + "&offset=" + offset;
      }

      Request<?> req = new DrVolleyStringRequest(url, new DrVolleyResonseListener() {

        @Override
        public void fikSvar(Netsvar s) throws Exception {
          if (s.uændret) return;
          if (s.json != null && !"null".equals(s.json)) {
            backend.parseMestSete(App.data.mestSete, App.data, s.json, kanalSlug);
            App.opdaterObservatører(App.data.mestSete.observatører);
          } else {
            App.langToast(R.string.Netværksfejl_prøv_igen_senere);
          }
        }
      }) {
        public Priority getPriority() {
          return fragment.getUserVisibleHint() ? Priority.NORMAL : Priority.LOW; //TODO Check if it works for lower than API 15
        }
      }.setTag(this);
      App.volleyRequestQueue.add(req);
    }

  }
}
