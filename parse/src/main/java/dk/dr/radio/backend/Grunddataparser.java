package dk.dr.radio.backend;

import com.example.feed.PodcastsFetcher;
import com.example.feed.Udsendelse2;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import dk.dr.radio.data.Grunddata;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.net.Diverse;

public class Grunddataparser {

  public static void initGrunddata2(Grunddata grunddata, String grunddataStr) throws JSONException {
    grunddata.json = new JSONObject(grunddataStr);
    grunddata.android_json = grunddata.json.getJSONObject("android");

    JSONArray kanalojJs = grunddata.json.getJSONArray("kanaloj");

    // Erstat med evt ny værdi
    //radioTxtUrl = json.optString("elsendojUrl", radioTxtUrl);

    int antal = kanalojJs.length();
    for (int i = 0; i < antal; i++) {
      JSONObject kJs = kanalojJs.getJSONObject(i);
      Kanal k = new Kanal();
      k.slug = kJs.optString("kodo", null);
      if (k.slug ==null) continue;
      k.navn = kJs.getString("nomo");
      String rektaElsendaSonoUrl = kJs.optString("rektaElsendaSonoUrl", null);
      k.eo_hejmpaĝoButono = kJs.optString("hejmpaĝoButono", null);
      k.eo_retpoŝto = kJs.optString("retpoŝto", null);
      k.kanallogo_url = kJs.optString("emblemoUrl", null);
      k.eo_elsendojRssUrl = kJs.optString("elsendojRssUrl", null);
      k.eo_elsendojRssIgnoruTitolon = kJs.optBoolean("elsendojRssIgnoruTitolon", false);
      k.eo_montruTitolojn = kJs.optBoolean("montruTitolojn", false);

      grunddata.kanaler.add(k);

      if (rektaElsendaSonoUrl != null) {
        Udsendelse rektaElsendo = new Udsendelse(k);
        rektaElsendo.startTid = new Date();
        rektaElsendo.startTidDato = "REKTA";
        rektaElsendo.titel = "";
        rektaElsendo.stream = rektaElsendaSonoUrl;
        rektaElsendo.rektaElsendaPriskriboUrl = kJs.optString("rektaElsendaPriskriboUrl", null);
        rektaElsendo.slug = k.slug + "_rekta";
        k.eo_rektaElsendo = rektaElsendo;
        k.stream = rektaElsendo.stream;
      }
    }

    for (Kanal k : grunddata.kanaler) {
      grunddata.kanalFraSlug.put(k.slug, k);
    }

    try {
      grunddata.opdaterGrunddataEfterMs = grunddata.json.getJSONObject("intervals").getInt("settings") * 1000;
      grunddata.opdaterPlaylisteEfterMs = grunddata.json.getJSONObject("intervals").getInt("playlist") * 1000;
    } catch (Exception e) {
      e.printStackTrace();
    } // Ikke kritisk
  }

  public static Grunddata getGrunddata() throws JSONException, IOException {
    Grunddata gd = new Grunddata();
    initGrunddata2(gd, Diverse.læsStreng(new FileInputStream("app/src/main/res/raw/esperantoradio_kanaloj_v8.json")));
    return gd;
  }


  public static void main(String[] args) throws Exception {
    Grunddata gd = getGrunddata();
    System.out.println("gd.kanaler = " + gd.kanaler);
    for (Kanal k : gd.kanaler) {
      System.out.println();
      System.out.println("===================================================================" + k);
      if (k.slug.contains("varsovia")) continue;
      System.out.println("k.eo_elsendojRssUrl = " + k.eo_elsendojRssUrl);
      if (k.eo_elsendojRssUrl==null) continue;
      System.out.println();
      String str = Diverse.hentUrlSomStreng(k.eo_elsendojRssUrl);
      //System.out.println("str = " + str);
      System.out.println("entry = " + str.split("<item")[1]);

      ArrayList<Udsendelse> udsendelser1 = EoRssParsado.ŝarĝiElsendojnDeRssUrl(str, k);
      if (udsendelser1.size()>0) System.out.println(udsendelser1.get(0));
      System.out.println();

      List<Udsendelse2> udsendelser2 = new PodcastsFetcher().parsRss(str, k);
      if (udsendelser2.size()>0) System.out.println(udsendelser2.get(0));
      System.out.println();

      System.out.println("udsendelser2.size() = " + udsendelser2.size());

      // System.out.println("feed = " + feed);

      /*
      for (int i=0; i<3; i++) {
        if (udsendelser1.size()>i) System.out.println(udsendelser1.get(i));
        if (udsendelser2.size()>i) System.out.println(udsendelser2.get(i));
        System.out.println();
        System.out.println();
        System.out.println("------------------------------------------------------------------");
      }*/
    }
  }
}
