package dk.dr.radio.backend;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;

import dk.dr.radio.data.Favoritter;
import dk.dr.radio.data.Grunddata;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Lydstream;
import dk.dr.radio.data.Programserie;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.FilCache;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.net.Diverse;
import dk.dr.radio.net.volley.Netsvar;
import dk.dr.radio.v3.R;

/**
 * Created by j on 01-03-17.
 */

public class EsperantoRadioBackend extends Backend {

  public EsperantoRadioBackend() {
    favoritter = new Favoritter();
  }

  public String getGrunddataUrl() {
    /*
scp /home/j/android/esperanto/EsperantoRadio/app/src/main/res/raw/esperantoradio_kanaloj_v8.json  javabog.dk:javabog.dk/privat/
     */
    return "http://javabog.dk/privat/esperantoradio_kanaloj_v8.json";
  }

  public InputStream getLokaleGrunddata(Context ctx) {
    if (ctx==null) try { return new FileInputStream("/home/j/andet/EspoRadio/EsperantoRadio/app/src/main/res/raw/esperantoradio_kanaloj_v8.json"); } catch (Exception e) { e.printStackTrace();}
    return ctx.getResources().openRawResource(R.raw.esperantoradio_kanaloj_v8);
  }

  public void initGrunddata(final Grunddata grunddata, String grunddataStr) throws JSONException, IOException {
    grunddata.json = new JSONObject(grunddataStr);
    grunddata.android_json = grunddata.json.getJSONObject("android");

    JSONArray kanalojJs = grunddata.json.getJSONArray("kanaloj");

    // Erstat med evt ny værdi
    //radioTxtUrl = json.optString("elsendojUrl", radioTxtUrl);
    kanaler.clear();

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
      k.eo_elsendojRssUrl2 = kJs.optString("elsendojRssUrl2", null);
      k.eo_elsendojRssIgnoruTitolon = kJs.optBoolean("elsendojRssIgnoruTitolon", false);
      k.eo_montruTitolojn = kJs.optBoolean("montruTitolojn", false);

      kanaler.add(k);

      if (rektaElsendaSonoUrl != null) {
        Udsendelse rektaElsendo = new Udsendelse(k);
        rektaElsendo.startTid = rektaElsendo.slutTid = new Date();
        rektaElsendo.kanalSlug = k.navn;
        rektaElsendo.startTidKl = "REKTA";
        rektaElsendo.titel = "";
        rektaElsendo.sonoUrl.add(rektaElsendaSonoUrl);
        rektaElsendo.rektaElsendaPriskriboUrl = kJs.optString("rektaElsendaPriskriboUrl", null);
        rektaElsendo.slug = k.slug + "_rekta";
        eoElsendoAlDaUdsendelse(rektaElsendo, k);
        k.eo_rektaElsendo = rektaElsendo;
        /*
        ArrayList<Lydstream> streams = new ArrayList<Lydstream>();
        Lydstream ls = new Lydstream();
        streams.add(ls);
        k.setStreams(streams);
        ls.url = rektaElsendaSonoUrl;
        ls.type = Lydstream.StreamType.Shoutcast;
        ls.kvalitet = Lydstream.StreamKvalitet.Medium;
        */
        k.setStreams(rektaElsendo.streams);
        //k.udsendelser.add(el);
      }
    }


    for (Kanal k : kanaler) {
      grunddata.kanalFraKode.put(k.kode, k);
      grunddata.kanalFraSlug.put(k.slug, k);
    }
    ŝarĝiKanalEmblemojn(true);

    try {
      grunddata.opdaterGrunddataEfterMs = grunddata.json.getJSONObject("intervals").getInt("settings") * 1000;
      grunddata.opdaterPlaylisteEfterMs = grunddata.json.getJSONObject("intervals").getInt("playlist") * 1000;
    } catch (Exception e) {
      Log.e(e);
    } // Ikke kritisk


    File fil = new File(FilCache.findLokaltFilnavn(radioTxtUrl));
    InputStream is = fil.exists() ? new FileInputStream(fil) : App.res.openRawResource(R.raw.radio);
    leguRadioTxt(grunddata, Diverse.læsStreng(is));

    new Thread() {
      @Override
      public void run() {
        try {
          ŝarĝiKanalEmblemojn(false);

          final String radioTxtStr = Diverse.læsStreng(new FileInputStream(FilCache.hentFil(radioTxtUrl, false)));
          App.forgrundstråd.post(new Runnable() {
            @Override
            public void run() {
              leguRadioTxt(grunddata, radioTxtStr);
              // Povas esti ke la listo de kanaloj ŝanĝiĝis, pro tio denove kontrolu ĉu reŝarĝi bildojn
              ŝarĝiKanalEmblemojn(true);
              App.opdaterObservatører(grunddata.observatører);
            }
          });
        } catch (Exception e) {
          Log.e(e);
        }
      }
    }.start();
    Log.d("parseKanaler gav " + kanaler + " for " + this.getClass().getSimpleName());
  }


  private boolean ŝarĝiKanalEmblemojn(boolean nurLokajn) {
    boolean ioEstisSxargxita = false;
    for (Kanal k : new ArrayList<>(kanaler)) {

      if (k.kanallogo_url != null && App.backend.kanallogo_eo.get(k.slug) == null) try {
        String dosiero = FilCache.findLokaltFilnavn(k.kanallogo_url);
        if (dosiero == null) continue;
        if (nurLokajn && !new File(dosiero).exists()) continue;
        FilCache.hentFil(k.kanallogo_url, true);
        /*
           int kiomDaDpAlta = 50; // 50 dp
           // Convert the dps to pixels
           final float scale = App.instans.getResources().getDisplayMetrics().density;
           int alteco = (int) (kiomDaDpAlta * scale + 0.5f);
           Bitmap res = kreuBitmapTiomAlta(dosiero, alteco);
           */
        Bitmap res = BitmapFactory.decodeFile(dosiero);

        if (res != null) {
          ioEstisSxargxita = true;
          int nedskaler = 1;
          while (res.getHeight()/nedskaler>300) nedskaler *= 2;
          if (nedskaler>1) {
            Log.d("Bitmap.createScaledBitmap "+k.kanallogo_url+"  "+ res.getHeight() + "/" + nedskaler );
            res = Bitmap.createScaledBitmap(res, res.getWidth()/nedskaler, res.getHeight()/nedskaler, true);
          }
        }
        App.backend.kanallogo_eo.put(k.slug, res);
      } catch (Exception ex) {
        Log.e(ex);
      }
    }
    return ioEstisSxargxita;
  }


  public String radioTxtUrl = "http://esperanto-radio.com/radio.txt";

  //public static final DateFormat datoformato = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
  public static final DateFormat datoformato = new SimpleDateFormat("yyyy-MM-dd", Locale.US);


  public static void eoElsendoAlDaUdsendelse(Udsendelse e, Kanal k) {
    e.programserieSlug = e.kanalSlug = k.slug;
    if (e.slug==null) e.slug = e.kanalSlug + ":" + e.startTidKl;
    App.data.udsendelseFraSlug.put(e.slug, e);

    e.slutTid = e.startTid;

    String url = e.sonoUrl.get(0);
    ArrayList<Lydstream> streams = lavSimpelLydstreamFraUrl(url);
    e.setStreams(streams);
    assert e.kanHentes == e.kanHøres;
  }

  @NonNull
  public static ArrayList<Lydstream> lavSimpelLydstreamFraUrl(String url) {
    Lydstream ls = new Lydstream();
    ls.url = url;
    ArrayList<Lydstream> streams = new ArrayList<Lydstream>();
    streams.add(ls);
    return streams;
  }


  public void leguRadioTxt(Grunddata grunddata, String radioTxt) {
    String kapo = null;
    HashSet<Kanal> kanalojDeRadioTxt = new HashSet<>();
    for (String unuo : radioTxt.split("\n\r?\n")) {
      unuo = unuo.trim();
      if (App.fejlsøgning) Log.d("Unuo: "+unuo);
      if (unuo.length() == 0) {
        continue;
      }
      if (kapo == null) {
        kapo = unuo;
      } else {
        try {
          Udsendelse e = new Udsendelse(null);
          String[] x = unuo.split("\n");
          /*
           3ZZZ en Esperanto
           2011-09-29
           http://www.melburno.org.au/3ZZZradio/mp3/2011-09-26.3ZZZ.radio.mp3
           Anonco : el retmesaĝo de Floréal Martorell « Katastrofo ĉe Vinilkosmo/ Eurokka Kanto : informo pri la kompaktdisko Hiphopa Kompilo 2 « Miela obsedo » Legado: el la verko de Ken Linton Kanako el Kananam ĉapitro 12 « Stranga ĝardeno » Lez el Monato de aŭgusto /septembro « Tantamas ŝtopiĝoj » de Ivo durwael Karlo el Monato » Eksplofas la popola kolero » [...]
           */
          e.kanalSlug = x[0].replaceAll(" ","").toLowerCase().replaceAll("ĉ", "cx");
          e.startTidKl = x[1];
          e.startTid = datoformato.parse(x[1]);
          e.sonoUrl.add(x[2]);
          e.titel = e.beskrivelse = x[3];

          Kanal k = grunddata.kanalFraSlug.get(e.kanalSlug);
          // Jen problemo. "Esperanta Retradio" nomiĝas "Peranto" en
          // http://esperanto-radio.com/radio.txt . Ni solvas tion serĉante ankaŭ por la kodo
          // "kodo": "peranto",
          // "nomo": "Esperanta Retradio",

          if (k == null) {
            k = grunddata.kanalFraKode.get(e.kanalSlug);
            if (k != null) e.kanalSlug = k.slug;
          }

          if (k == null) {
            Log.d("Nekonata kanalnomo - ALDONAS GXIN: " + e.kanalSlug);
            k = new Kanal();
            k.kode = k.slug = e.kanalSlug;
            k.navn = x[0];
            k.eo_datumFonto = "radio.txt";
            Log.d("Aldonas elsendon "+e.toString());
            k.udsendelser.add(e);
            kanalojDeRadioTxt.add(k);
            eoElsendoAlDaUdsendelse(e, k);
            k.eo_udsendelserFraRadioTxt = k.udsendelser;
            grunddata.kanalFraKode.put(k.kode, k);
            grunddata.kanalFraSlug.put(k.slug, k);
            kanaler.add(k);
          }
          e.kanal = k;
          if (!"rss".equals(k.eo_datumFonto)) {
            k.eo_datumFonto = "radio.txt";
            if (!kanalojDeRadioTxt.contains(k)) {
              kanalojDeRadioTxt.add(k);
              k.udsendelser.clear();
            }
            //Log.d("Aldonas elsendon "+e.toStri7ng());
            k.udsendelser.add(e);
            eoElsendoAlDaUdsendelse(e, k);
            k.eo_udsendelserFraRadioTxt = k.udsendelser;
          }
        } catch (Exception e) {
          Log.e("Ne povis legi unuon: " + unuo, e);
        }
      }
    }

    for (Kanal k : kanaler) {
      eo_opdaterProgramserieFraKanal((Kanal) k);
    }
  }

  static void eo_opdaterProgramserieFraKanal(Kanal k) {
    Programserie ps = App.data.programserieFraSlug.get(k.slug);
    if (ps==null) {
      ps = new Programserie();
      ps.billedeUrl = k.kanallogo_url;
      ps.beskrivelse = k.getNavn();
      ps.slug = k.slug;
      ps.titel = k.getNavn();
      App.data.programserieFraSlug.put(k.slug, ps);
    } else {
      ps.udsendelser.clear();
    }
    //Log.d(this + " tilføjUdsendelser:" + (udsendelserListe == null ? "nul" : udsendelserListe.size()) + "  får tilføjet " + (uds == null ? "nul" : uds.size()) + " elementer");
    //if (App.fejlsøgning) Log.d(this + " tilføjUdsendelser:" + (udsendelserListe == null ? "nul" : udsendelserListe.size()) + " elem liste:\n" + udsendelserListe + "\nfår tilføjet " + (uds == null ? "nul" : uds.size()) + " elem:\n" + uds);

    if (ps.udsendelser == null) {
      ps.udsendelser = new ArrayList<Udsendelse>(k.udsendelser);
    } else {
      ps.udsendelser.clear();
      ps.udsendelser.addAll(k.udsendelser);
    }
  }


  public void hentUdsendelserPåKanal(final Kanal kanal, final String datoStr, final NetsvarBehander netsvarBehander) {
    Log.d("eo RSS por "+kanal+" ="+kanal.eo_elsendojRssUrl);
    if (kanal.eo_elsendojRssUrl !=null &&  !"rss".equals(kanal.eo_datumFonto) && !kanal.harUdsendelserForDag(datoStr)) {
      App.netkald.kald(this, kanal.eo_elsendojRssUrl, new NetsvarBehander() {
        @Override
        public void fikSvar(Netsvar s) throws Exception {
          if (s.uændret) return;
          Log.d("eo RSS por "+kanal+" ="+s.json);
          EoRssParsado.ŝarĝiElsendojnDeRssUrl(s.json, kanal);
          netsvarBehander.fikSvar(s);

          if (kanal.eo_elsendojRssUrl2!=null) {
            final ArrayList<Udsendelse> uds1 = kanal.udsendelser;
            App.netkald.kald(this, kanal.eo_elsendojRssUrl2, new NetsvarBehander() {
              @Override
              public void fikSvar(Netsvar s) throws Exception {
                Log.d("eo RSS2 por " + kanal + " =" + s.json);
                EoRssParsado.ŝarĝiElsendojnDeRssUrl(s.json, kanal);
                kanal.udsendelser.addAll(uds1);
                Collections.sort(kanal.udsendelser);
                Collections.reverse(kanal.udsendelser);
                netsvarBehander.fikSvar(s);
              }
            });
          }
        }
      });
    } else {
      try {
        netsvarBehander.fikSvar(Netsvar.IKKE_NØDVENDIGT);
      } catch (Exception e) {
        Log.rapporterFejl(e);
      }
    }
  }

  @Override
  public void hentProgramserie(final NetsvarBehander netsvarBehander) {
    try {
      netsvarBehander.fikSvar(Netsvar.IKKE_NØDVENDIGT);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
