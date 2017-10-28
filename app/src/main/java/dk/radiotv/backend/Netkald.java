package dk.radiotv.backend;

import android.support.v4.app.Fragment;

import com.android.volley.Request;

import java.util.Date;

import dk.dr.radio.data.Datoformater;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.net.volley.DrVolleyResonseListener;
import dk.dr.radio.net.volley.DrVolleyStringRequest;
import dk.dr.radio.net.volley.Netsvar;
import dk.dr.radio.v3.R;

/**
 * Created by j on 09-10-17.
 */

public class Netkald {
  public void kald(Object kalder, String url, final NetsvarBehander netsvarBehander) {
    kald(kalder, url, null, netsvarBehander);
  }

  public void kald(final Object kalder, final String url, final Request.Priority priority, final NetsvarBehander netsvarBehander) {
    if (url==null) return;
    Request<?> req = new DrVolleyStringRequest(url, new DrVolleyResonseListener() {
      @Override
      public void fikSvar(Netsvar s) throws Exception {
        s.url = url;
        netsvarBehander.fikSvar(s);
      }
    }
    ) {
      @Override
      public Request.Priority getPriority() {
        Log.d("getPriority "+url);
        if (priority!=null) return priority;
        if (kalder instanceof Fragment) {
          Fragment f = (Fragment) kalder;
          return f.getUserVisibleHint() ? Request.Priority.NORMAL : Request.Priority.LOW;
        }
        return Request.Priority.NORMAL;
      }
    }.setTag(kalder);
    App.volleyRequestQueue.add(req);
  }

  public void annullerKald(Object kalder) {
    App.volleyRequestQueue.cancelAll(kalder);
  }
}
