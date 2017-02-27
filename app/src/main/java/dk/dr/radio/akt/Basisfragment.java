/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.dr.radio.akt;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Spannable;
import android.text.SpannableString;
import android.util.DisplayMetrics;

import java.net.URL;
import java.net.URLEncoder;

import dk.dr.radio.data.Programserie;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.data.dr_v3.Backend;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;

/**
 * @author j
 */
//public class BasisFragment extends DialogFragment {
public class Basisfragment extends Fragment {

  public static String P_kode = "kanal.kode";


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

  public static final int LINKFARVE = 0xff00458f;

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


  /**
   * Finder bredden som et et velskaleret billede forventes at have
   * @param rod         listen eller rod-viewet hvor billedet skal vises
   * @param paddingView containeren der har polstring/padding
   */
  /*
  protected int bestemBilledebredde(View rod, View paddingView, int procent) {
    //if (!App.PRODUKTION && rod.getWidth() != billedeBr) throw new IllegalStateException(rod.getWidth()+" != "+ billedeBr);
    //int br = rod.getWidth() * procent / 100;
    //if (rod.getHeight() < 2 * br / 3) br = br / 2; // Halvbreddebilleder ved liggende visning
    int br = billedeBr;
    br = br - paddingView.getPaddingRight() - paddingView.getPaddingLeft();
    //Log.d("QQQQQ listView.getWidth()=" + rod.getWidth() + " getHeight()=" + rod.getHeight());
    //Log.d("QQQQQ billedeContainer.getPaddingRight()=" + paddingView.getPaddingRight() + "   .... så br=" + br);
    return br;
  }*/

/* Doku fra Nicolai
Alle billeder der ligger på dr.dk skal igennem "asset.dr.dk/imagescaler<http://asset.dr.dk/imagescaler>".

Artist billeder fra playlister i APIet skal igennem asset.dr.dk/discoImages,
da den bruger en whitelisted IP hos LastFM og discogs (de har normalt max requests).

Det er begge simple services, og du har ikke brug for andre parametre end dem der kan ses
her (w = width, h = height -- scaleAfter skal du ikke pille ved):

http://asset.dr.dk/imagescaler/?file=%2Fmu%2Fbar%2F52f8d952a11f9d0c90f8b3e3&w=300&h=169&scaleAfter=crop&server=www.dr.dk
( bliver til
http://asset.dr.dk/imagescaler/?file=/mu/programcard/imageuri/radioavis-24907&w=300&h=169&scaleAfter=crop  )

http://asset.dr.dk/discoImages/?discoserver=api.discogs.com&file=%2fimage%2fA-885103-1222266056.jpeg&h=400&w=400&scaleafter=crop&quality=85

Jeg bruger selv følgende macro'er i C til generering af URIs:

#define DRIMAGE(path, width, height) \
            FORMAT( \
                @"http://asset.dr.dk/drdkimagescale/?server=www.dr.dk&amp;w=%0.f&amp;h=%0.f&amp;file=%@&amp;scaleAfter=crop", \
                width * ScreenScale, \
                height * ScreenScale, \
                URLENCODE(path) \
            )

#define DISCOIMAGE(host, path, width, height) \
            FORMAT( \
                @"http://asset.dr.dk/discoImages/?discoserver=%@&amp;w=%0.f&amp;h=%0.f&amp;file=%@&amp;scaleAfter=crop&amp;quality=85", \
                host \
                width * ScreenScale, \
                height * ScreenScale, \
                URLENCODE(path) \
            )
 */

  /**
   * Billedeskalering af billeder på DRs servere.
   */
  public static String skalérDrDkBilledeUrl(String url, int bredde, int højde) {
    if (url == null || url.length() == 0 || "null".equals(url)) return null;
    try {
      URL u = new URL(url);
      String skaleretUrl = "http://asset.dr.dk/drdkimagescale/?server=www.dr.dk&amp;w=" + bredde + "&amp;h=" + højde +
          "&amp;file=" + URLEncoder.encode(u.getPath(), "UTF-8") + "&amp;scaleAfter=crop";

      Log.d("skalérDrDkBilledeUrl url1 = " + url);
      Log.d("skalérDrDkBilledeUrl url2 = " + u);
      Log.d("skalérDrDkBilledeUrl url3 = " + skaleretUrl);
      return skaleretUrl;
    } catch (Exception e) {
      Log.e("url=" + url, e);
      return null;
    }
  }


  /**
   * Billedeskalering af billeder ud fra en slug
   */
  private static String skalérBilledeFraSlug(String slug, int bredde, int højde) {
    String res = "http://asset.dr.dk/imagescaler/?file=/mu/programcard/imageuri/" + slug + "&w=" + bredde + "&h=" + højde + "&scaleAfter=crop";
    if (App.fejlsøgning) Log.d("skalérBilledeFraSlug " + slug + " " + bredde + "x" + højde + " giver: " + res);
    return res;
  }

  /**
   * Billedeskalering af billeder på DRs servere, ud fra en URL.
   * F.eks. http://asset.dr.dk/imagescaler/?file=http://www.dr.dk/mu/bar/544e40f7a11f9d16c4c96db7&w=620&h=349
   */
  private static String skalérBilledeFraUrl(String url, int bredde, int højde) {
    if (!App.ÆGTE_DR) return url;
    String res = "http://asset.dr.dk/imagescaler/?file=" + url + "&w=" + bredde + "&h=" + højde + "&scaleAfter=crop";
    if (App.fejlsøgning) Log.d("skalérBilledeFraUrl " + url + " " + bredde + "x" + højde + " giver: " + res);
    return res;
  }

  public static String skalérBillede(Udsendelse u, int bredde, int højde) {
    if (!App.ÆGTE_DR) {
      if (u.billedeUrl != null) return u.billedeUrl;
      return u.getKanal().eo_emblemoUrl;
    }
//    u.billedeUrl = null;
    return u.billedeUrl==null
        ? skalérBilledeFraSlug(u.slug, bredde, højde)
        : skalérBilledeFraUrl(u.billedeUrl, bredde, højde);
  }

  public static String skalérBillede(Programserie u, int bredde, int højde) {
//    u.billedeUrl = null;
    return u.billedeUrl==null
        ? skalérBilledeFraSlug(u.slug, bredde, højde)
        : skalérBilledeFraUrl(u.billedeUrl, bredde, højde);
  }


  public static String skalérBillede(Udsendelse u) {
    return skalérBillede(u, billedeBr, billedeHø);
  }


  public static String skalérBillede(Programserie u) {
    return skalérBillede(u, billedeBr, billedeHø);
  }


  /**
   * Billedeskalering til LastFM og discogs til playlister.
   * @see Backend#parsePlayliste(org.json.JSONArray)
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
      String skaleretUrl = "http://asset.dr.dk/discoImages/?discoserver=" + u.getHost() + "&w=" + bredde + "&h=" + højde +
          "&file=" + u.getPath() + "&scaleAfter=crop&quality=85";

      //Log.d("skalérDiscoBilledeUrl url2 = " + u);
      //Log.d("skalérDiscoBilledeUrl url3 = " + skaleretUrl);
      return skaleretUrl;
    } catch (Exception e) {
      Log.e("url=" + url, e);
      return null;
    }
  }

  /**
   * Skalering af andre billeder. Denne her virker til begge ovenstående, samt til skalering af andre biller, så den gemmer vi, selvom den ikke bliver brugt p.t.
   */
  public static String skalérAndenBilledeUrl(String url, int bredde, int højde) {
    if (url == null || url.length() == 0 || "null".equals(url)) return null;
    try {
      URL u = new URL(url);
      String skaleretUrl = "http://dr.dk/drdkimagescale/imagescale.drxml?server=" + u.getAuthority() + "&file=" + u.getPath() + "&w=" + bredde + "&h=" + højde + "&scaleafter=crop";

      Log.d("skalérAndenBilledeUrl url1 = " + url);
      Log.d("skalérAndenBilledeUrl url2 = " + u);
      Log.d("skalérAndenBilledeUrl url3 = " + skaleretUrl);
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
