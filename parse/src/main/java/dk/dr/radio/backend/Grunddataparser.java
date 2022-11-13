package dk.dr.radio.backend;

import com.example.feed.PodcastRssResponse;
import com.example.feed.PodcastsFetcher;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

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
      k.slug = k.kode = kJs.optString("kodo", null);
      if (k.kode ==null) continue;
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
        rektaElsendo.kanalSlug = k.navn;
        rektaElsendo.startTidKl = "REKTA";
        rektaElsendo.titel = "";
        rektaElsendo.sonoUrl.add(rektaElsendaSonoUrl);
        rektaElsendo.rektaElsendaPriskriboUrl = kJs.optString("rektaElsendaPriskriboUrl", null);
        rektaElsendo.slug = k.slug + "_rekta";
        k.eo_rektaElsendo = rektaElsendo;
        k.setStreams(rektaElsendo.streams);
      }
    }

    for (Kanal k : grunddata.kanaler) {
      grunddata.kanalFraKode.put(k.kode, k);
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
      System.out.println("===================================================================");
      System.out.println("k = " + k);
      System.out.println("k.eo_elsendojRssUrl = " + k.eo_elsendojRssUrl);
      if (k.eo_elsendojRssUrl==null) continue;
      ArrayList<Udsendelse> udsendelser1 = EoRssParsado.ŝarĝiElsendojnDeRssUrl(Diverse.hentUrlSomStreng(k.eo_elsendojRssUrl), k);

      PodcastRssResponse feed = new PodcastsFetcher().fetchPodcast(k.eo_elsendojRssUrl);
      k.udsendelser = new ArrayList(feed.getUdsendelses());
      System.out.println("feed = " + feed);
    }
  }
}
