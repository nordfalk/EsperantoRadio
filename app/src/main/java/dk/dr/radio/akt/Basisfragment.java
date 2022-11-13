/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.dr.radio.akt;

import android.app.Activity;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.text.Spannable;
import android.text.SpannableString;
import android.util.DisplayMetrics;

import java.net.URL;

import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;

/**
 * @author j
 */
public class Basisfragment extends Fragment {

  public static final String P_KANALKODE = "kanal";
  public static final String P_UDSENDELSE = "udsendelse";
  public static final String P_PROGRAMSERIE = "programserie";


  static final boolean LOG_LIVSCYKLUS = false;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    if (LOG_LIVSCYKLUS) Log.d("onCreate " + this);
    super.onCreate(savedInstanceState);
  }

  @Override
  public void onStart() {
    if (LOG_LIVSCYKLUS) Log.d("onStart " + this);
    super.onStart();
  }

  protected void afbrydManglerData() {
    Log.d("FRAGMENT AFBRYDES " + this + " " + getArguments());
    //if (!App.PRODUKTION) App.kortToast("FRAGMENT AFBRYDES " + this + " " + getArguments());
    getFragmentManager().popBackStack();
  }

  @Override
  public void onResume() {
    if (LOG_LIVSCYKLUS) Log.d("onResume " + this);
    super.onResume();
  }

  @Override
  public void onPause() {
    if (LOG_LIVSCYKLUS) Log.d("onPause " + this);
    super.onPause();
  }

  @Override
  public void onDestroy() {
    if (LOG_LIVSCYKLUS) Log.d("onDestroy " + this);
    super.onDestroy();
  }

  @Override
  public void onAttach(Activity activity) {
    if (LOG_LIVSCYKLUS) Log.d("onAttach " + this + " til " + activity);
    super.onAttach(activity);
  }

  @Override
  public void onStop() {
    if (LOG_LIVSCYKLUS) Log.d("onStop " + this);
    super.onStop();
  }

  @Override
  public void onDestroyView() {
    if (LOG_LIVSCYKLUS) Log.d("onDestroyView " + this);
    super.onDestroyView();
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    if (LOG_LIVSCYKLUS) Log.d("onActivityCreated " + this);
    super.onActivityCreated(savedInstanceState);
  }

  /*
SKALERINGSFORHOLD
Skalering af billeder - 16/9/3
Forhold 16:9 for de store billeder
Forhold 1:1 for playlistebilleder - og de skal være 1/3-del i højden af de store billeder
 */
  public static final int bredde16 = 16;
  public static final int højde9 = 9;

  /**
   * Bredden af aktiviteten skal bruges forskellige steder til at afgøre skalering af billeder
   * Der bruges generelt halv bredde ved liggende visning.
   * Bemærk, værdien er *kun til læsning*, og ændrer sig ved skærmvending
   */
  public static int billedeBr;
  public static int billedeHø;
  public static boolean halvbreddebilleder;

  public static void sætBilledeDimensioner(DisplayMetrics metrics) {
    halvbreddebilleder = metrics.heightPixels < metrics.widthPixels;
    billedeBr = metrics.widthPixels;
    if (halvbreddebilleder) billedeBr = billedeBr / 2; // Halvbreddebilleder ved liggende visning
    billedeHø = billedeBr * højde9 / bredde16;
  }

  public static String skalérBillede(Udsendelse u) {
    if (u.billedeUrl != null) return u.billedeUrl;
    return u.getKanal().kanallogo_url;
  }


  /**
   * Billedeskalering til LastFM og discogs til playlister.
   * Image: "http://api.discogs.com/image/A-4970-1339439274-8053.jpeg",
   * ScaledImage: "http://asset.dr.dk/discoImages/?discoserver=api.discogs.com&file=%2fimage%2fA-4970-1339439274-8053.jpeg&h=400&w=400&scaleafter=crop&quality=85",
   */
  public static String skalérDiscoBilledeUrl(String url, int bredde, int højde) {
    if (App.fejlsøgning) Log.d("skalérDiscoBilledeUrl url1 = " + url);
    if (url == null || url.length() == 0 || "null".equals(url)) return null;
    try {
      URL u = new URL(url);
      //String skaleretUrl = "http://asset.dr.dk/discoImages/?discoserver=" + u.getHost() + ";w=" + bredde16 + "&amp;h=" + højde9 +
      //    "&amp;file=" + URLEncoder.encode(u.getPath(), "UTF-8") + "&amp;scaleAfter=crop&amp;quality=85";
      String skaleretUrl = "https://asset.dr.dk/discoImages/?discoserver=" + u.getHost() + "&w=" + bredde + "&h=" + højde +
          "&file=" + u.getPath() + "&scaleAfter=crop&quality=85";

      //Log.d("skalérDiscoBilledeUrl url2 = " + u);
      //Log.d("skalérDiscoBilledeUrl url3 = " + skaleretUrl);
      return skaleretUrl;
    } catch (Exception e) {
      Log.e("url=" + url, e);
      return null;
    }
  }


  protected static Spannable lavFedSkriftTil(String tekst, int fedTil) {
    Spannable spannable = new SpannableString(tekst);
    spannable.setSpan(App.skrift_gibson_fed_span, 0, fedTil, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    return spannable;
  }
}
