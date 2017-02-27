package dk.dr.radio.net.volley;

import com.android.volley.Response;
import com.android.volley.VolleyError;

import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;

/**
 * DR Radios ResponseListener-klient til Volley.
 * Lavet sådan at også cachede (eventuelt forældede) værdier leveres.
 * Håndterer også at signalere over for brugeren når netværskommunikation er påbegyndt eller afsluttet
 * Created by j on 13-03-14.
 */
public abstract class DrVolleyResonseListener implements Response.Listener<String>, Response.ErrorListener {

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
      fikSvar(response, false, uændret);
      App.sætErIGang(false, url);
    } catch (Exception e) {
      Log.e(url, e);
      onErrorResponse(new VolleyError(e));
    }
  }

  @Override
  public final void onErrorResponse(VolleyError error) {
    App.sætErIGang(false, url);
    fikFejl(error);
  }

  /**
   * Kaldes med svaret fra cachen (hvis der er et) og igen når svaret fra serveren ankommer
   * @param response Svaret
   * @param fraCache Normalt true første gang hvis svaret kommer fra cachen (og eventuelt er forældet).
   *                 Normalt false anden gang hvor svaret kommer fra serveren.
   * @param uændret  Serveren svarede med de samme data som der var i cachen
   * @throws Exception Hvis noget går galt i behandlingen - f.eks. ulovligt JSON kaldes fikFejl
   */
  protected abstract void fikSvar(String response, boolean fraCache, boolean uændret) throws Exception;

  /**
   * Kaldes af Volley hvis der skete en netværksfejl. Kaldes også hvis behandlingen i #fikSvar gik galt.
   * @param error Fejlen
   */
  protected void fikFejl(VolleyError error) {
    Log.e("fikFejl '" + error.networkResponse + "' for " + url, error);
    //error.printStackTrace();
    if (App.fejlsøgning) Log.e("fikFejl startet herfra:", startetHerfra);
  }

  /**
   * Kaldes (fra DrVolleyStringRequest) hvis forespørgslen blev annulleret
   */
  void annulleret() {
    Log.d("annulleret for " + url);
    App.sætErIGang(false, url);
  }
}
