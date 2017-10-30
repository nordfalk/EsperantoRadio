package dk.radiotv.backend;

import android.support.v4.app.Fragment;

import com.android.volley.Request;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;

import dk.dr.radio.data.Datoformater;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.FilCache;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.net.Diverse;
import dk.dr.radio.net.volley.DrVolleyResonseListener;
import dk.dr.radio.net.volley.DrVolleyStringRequest;
import dk.dr.radio.net.volley.Netsvar;
import dk.dr.radio.v3.R;

/**
 * Created by j on 09-10-17.
 */

public class Netkald {
  public static String hentStreng(String url) throws IOException {
    //String data = Diverse.læsStreng(new FileInputStream(FilCache.hentFil(url, false, true, 1000 * 60 * 60 * 24 * 7)));
    if (url==null) return null;
    url = url.replaceAll("Ø", "%C3%98");
    url = url.replaceAll("Å", "%C3%85");
    String fil = FilCache.hentFil(url, true, true, 12 * 1000 * 60 * 60);
    //Log.d(url + "    -> "+fil);
    String data = Diverse.læsStreng(new FileInputStream(fil));
    //Log.d(data);
    return data;
  }

  public void kald(Object kalder, String url, final NetsvarBehander netsvarBehander) {
    kald(kalder, url, null, netsvarBehander);
  }

  public void kald(final Object kalder, final String url, final Request.Priority priority, final NetsvarBehander netsvarBehander) {
    if (url==null) return;
    if (App.IKKE_Android_VM) {
      try {
        Netsvar s = new Netsvar(url, hentStreng(url), false, false);
        netsvarBehander.fikSvar(s);
      } catch (Exception e) {
        e.printStackTrace();
      }
      return;
    }
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
