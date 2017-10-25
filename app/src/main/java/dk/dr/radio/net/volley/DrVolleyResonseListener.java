package dk.dr.radio.net.volley;

import com.android.volley.Response;
import com.android.volley.VolleyError;

import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.radiotv.backend.NetsvarBehander;

/**
 * DR Radios ResponseListener-klient til Volley.
 * Lavet sådan at også cachede (eventuelt forældede) værdier leveres.
 * Håndterer også at signalere over for brugeren når netværskommunikation er påbegyndt eller afsluttet
 * Created by j on 13-03-14.
 */
public abstract class DrVolleyResonseListener implements Response.Listener<String>, Response.ErrorListener, NetsvarBehander {

  /**
   * URL på anmodningen - rar at have til logning
   */
  protected String url;
  String cachetVærdi;
  Exception startetHerfra;

  public DrVolleyResonseListener() {
    startetHerfra = new Exception();
  }

  @Override
  public final void onResponse(String response) {
    try {
      boolean uændret = response != null && response.equals(cachetVærdi);
      if ("null".equals(response)) response = null;
      fikSvar(new Netsvar(response, false, uændret));
      App.sætErIGang(false, url);
    } catch (Exception e) {
      Log.e(url, e);
      Log.d("response = "+response);
      onErrorResponse(new VolleyError(e));
    }
  }

  @Override
  public final void onErrorResponse(VolleyError error) {
    App.sætErIGang(false, url);
    Log.e("fikFejl networkResponse='" + error.networkResponse + "' for " + url, error);
    //error.printStackTrace();
    if (App.fejlsøgning || App.EMULATOR) Log.e("fikFejl startet herfra:", startetHerfra);
    try {
      Netsvar s = new Netsvar(null, false, false);
      s.fejl = true;
      s.exception = error;
      fikSvar(s);
    } catch (Exception e) {
      Log.e(url, e);
    }
  }

  /**
   * Kaldes med svaret fra cachen (hvis der er et) og igen når svaret fra serveren ankommer
   * @throws Exception Hvis noget går galt i behandlingen - f.eks. ulovligt JSON kaldes fikFejl
   */
  public abstract void fikSvar(Netsvar s) throws Exception;
  /**
   * Kaldes (fra DrVolleyStringRequest) hvis forespørgslen blev annulleret
   */
  void annulleret() {
    Log.d("annulleret for " + url);
    App.sætErIGang(false, url);
  }
}
