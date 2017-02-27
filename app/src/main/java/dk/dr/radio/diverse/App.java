/**
 DR Radio 2 is developed by Jacob Nordfalk, Hanafi Mughrabi and Frederik Aagaard.
 Some parts of the code are loosely based on Sveriges Radio Play for Android.

 DR Radio 2 for Android is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License version 2 as published by
 the Free Software Foundation.

 DR Radio 2 for Android is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 See the GNU General Public License for more details.

 You should have received a copy of the GNU General Public License along with
 DR Radio 2 for Android.  If not, see <http://www.gnu.org/licenses/>.

 */

package dk.dr.radio.diverse;

/**
 *
 * @author j
 */

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.accessibility.AccessibilityManager;
import android.widget.Toast;

import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.HttpStack;
import com.android.volley.toolbox.HurlStack;
import com.androidquery.callback.BitmapAjaxCallback;
import com.crashlytics.android.Crashlytics;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;

import dk.dr.radio.afspilning.Afspiller;
import dk.dr.radio.afspilning.Fjernbetjening;
import dk.dr.radio.akt.Basisaktivitet;
import dk.dr.radio.akt.diverse.EgenTypefaceSpan;
import dk.dr.radio.data.Programdata;
import dk.dr.radio.data.Grunddata;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.net.Diverse;
import dk.dr.radio.net.Netvaerksstatus;
import dk.dr.radio.net.volley.DrBasicNetwork;
import dk.dr.radio.net.volley.DrDiskBasedCache;
import dk.dr.radio.net.volley.DrVolleyResonseListener;
import dk.dr.radio.net.volley.DrVolleyStringRequest;
import dk.dr.radio.v3.BuildConfig;
import dk.dr.radio.v3.R;
import io.fabric.sdk.android.Fabric;

public class App extends Application {
  public static App instans;

  public static final boolean PRODUKTION = !BuildConfig.DEBUG;
  public static boolean EMULATOR = true; // Sæt i onCreate(), ellers virker det ikke i std Java
  public static boolean IKKE_Android_VM = false; // Hvis test fra almindelig JVM

  public static boolean ÆGTE_DR = false;
  /** Sæt sprogvalg til dansk eller esperanto alt efter hvilken version der køres med */
  public static Configuration sprogKonfig;
  /** Bruges på nye funktioner - for at tjekke om de altid er opfyldt i felten. Fjernes ved næste udgivelser */
  public static final boolean TJEK_ANTAGELSER = !PRODUKTION;
  public static String versionsnavn = "(ukendt)";

  public static final String P4_FORETRUKKEN_AF_BRUGER = "P4_FORETRUKKEN_AF_BRUGER";
  private static final String DRAMA_OG_BOG__A_Å_INDLÆST = "DRAMA_OG_BOG__A_Å_INDLÆST";
  public static final String FORETRUKKEN_KANAL = "FORETRUKKEN_kanal";
  public static final String NØGLE_advaretOmInstalleretPåSDKort = "erInstalleretPåSDKort";
  public static SharedPreferences prefs;
  public static ConnectivityManager connectivityManager;
  public static NotificationManager notificationManager;
  public static AudioManager audioManager;
  public static boolean fejlsøgning = false;
  public static Handler forgrundstråd;
  public static Typeface skrift_gibson;
  public static Typeface skrift_gibson_fed;
  public static Typeface skrift_georgia;

  public static Netvaerksstatus netværk;
  public static Fjernbetjening fjernbetjening;
  public static RequestQueue volleyRequestQueue;
  public static boolean erInstalleretPåSDKort;
  private DrDiskBasedCache volleyCache;
  public static EgenTypefaceSpan skrift_gibson_fed_span;
  public static DRFarver color;
  public static Resources res;
  /** Tidsstempel der kan bruges til at afgøre hvilke filer der faktisk er brugt efter denne opstart */
  private static long TIDSSTEMPEL_VED_OPSTART;
  public static AccessibilityManager accessibilityManager;
  private static SharedPreferences grunddata_prefs;


  @SuppressLint("NewApi")
  @Override
  public void onCreate() {
    TIDSSTEMPEL_VED_OPSTART = System.currentTimeMillis();
    instans = this;

    prefs = PreferenceManager.getDefaultSharedPreferences(this);
    fejlsøgning = prefs.getBoolean("fejlsøgning", false);

    App.ÆGTE_DR = App.prefs.getBoolean("ÆGTE_DR", App.ÆGTE_DR);

    sprogKonfig = new Configuration();
    sprogKonfig.locale = new Locale( ÆGTE_DR ?  "da_DK" : "eo");
    Locale.setDefault(sprogKonfig.locale);
    getResources().updateConfiguration(App.sprogKonfig, null);


    netværk = new Netvaerksstatus();
    EMULATOR = Build.PRODUCT.contains("sdk") || Build.MODEL.contains("Emulator") || IKKE_Android_VM;
    if (!EMULATOR) {
      Fabric.with(this, new Crashlytics());
      Log.d("Crashlytics startet");
    }
    super.onCreate();

    forgrundstråd = new Handler();
    connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    audioManager = (AudioManager) App.instans.getSystemService(Context.AUDIO_SERVICE);
    res = App.instans.getResources();
    App.color = new DRFarver();

    // HTTP-forbindelser havde en fejl præ froyo, men jeg har også set problemet på Xperia Play, der er 2.3.4 (!)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
      System.setProperty("http.keepAlive", "false");
    }
    String packageName = getPackageName();
    try {
      if (ÆGTE_DR) if ("dk.dr.radio".equals(packageName)) {
        if (!PRODUKTION) App.langToast("Sæt PRODUKTIONs-flaget");
      } else {
        if (PRODUKTION) App.langToast("Testudgave - fjern PRODUKTIONs-flaget");
      }
      //noinspection ConstantConditions
      PackageInfo pi = getPackageManager().getPackageInfo(packageName, 0);
      App.versionsnavn = packageName + "/" + pi.versionName;
      if (EMULATOR) App.versionsnavn += " EMU";
      Log.d("App.versionsnavn=" + App.versionsnavn);

      App.erInstalleretPåSDKort = 0!=(pi.applicationInfo.flags & ApplicationInfo.FLAG_EXTERNAL_STORAGE);
      if (!App.erInstalleretPåSDKort) prefs.edit().remove(NØGLE_advaretOmInstalleretPåSDKort).commit();
    } catch (Exception e) {
      Log.rapporterFejl(e);
    }
    accessibilityManager = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);


    if (!ÆGTE_DR) FilCache.init(new File(getCacheDir(), "FilCache"));
    // Initialisering af Volley

    HttpStack stack = new HurlStack();
//            new HttpClientStack(AndroidHttpClient.newInstance(App.versionsnavn))
//            new HttpClientStack(new DefaultHttpClient());

    // Vi bruger vores eget Netværkslag, da DRs Varnish-servere ofte svarer med HTTP-kode 500,
    // som skal håndteres som et timeout og at der skal prøves igen
    Network network = new DrBasicNetwork(stack);
    // Vi bruger vores egen DrDiskBasedCache, da den indbyggede i Volley
    // har en opstartstid på flere sekunder
    // Mappe ændret fra standardmappen "volley" til "dr_volley" 19. nov 2014.
    // Det skyldtes at et hukommelsesdump viste, at Volley indekserede alle filerne i standardmappen,
    // uden om vores implementation, hvilket gav et unødvendigt overhead på ~ 1MB
    File cacheDir = new File(getCacheDir(), "dr_volley");
    volleyCache = new DrDiskBasedCache(cacheDir);
    volleyRequestQueue = new RequestQueue(volleyCache, network);
    volleyRequestQueue.start();

    try {
      Programdata.instans = new Programdata();
      Programdata.instans.grunddata = new Grunddata();

      // Indlæsning af grunddata/stamdata.
      // Først tjekkes om vi har en udgave i prefs, og ellers bruges den i raw-mappen
      // På et senere tidspunkt henter vi nye grunddata
      grunddata_prefs = App.instans.getSharedPreferences("grunddata", 0);
      String grunddata = grunddata_prefs.getString(Programdata.GRUNDDATA_URL, null);

      if (grunddata == null || App.EMULATOR) { // Ingen grunddata fra sidste - det er nok en frisk installation
        grunddata = Diverse.læsStreng(res.openRawResource(R.raw.grunddata));
      }
      if (App.ÆGTE_DR) {
        Programdata.instans.grunddata.parseFællesGrunddata(grunddata);
      } else {
        Programdata.instans.grunddata.eo_parseFællesGrunddata(grunddata);
        Programdata.instans.grunddata.ŝarĝiKanalEmblemojn(true);
        Programdata.instans.grunddata.parseFællesGrunddata(grunddata);

        File fil = new File(FilCache.findLokaltFilnavn(Programdata.instans.grunddata.radioTxtUrl));
        if (fil.exists()) {
          String radioTxtStr = Diverse.læsStreng(new FileInputStream(fil));
          Programdata.instans.grunddata.leguRadioTxt(radioTxtStr);
        } else {
          String radioTxtStr = Diverse.læsStreng(res.openRawResource(R.raw.radio));
          Programdata.instans.grunddata.leguRadioTxt(radioTxtStr);
        }

        new Thread() {
          @Override
          public void run() {
            try {
              Programdata.instans.grunddata.ŝarĝiKanalEmblemojn(false);

              final String radioTxtStr = Diverse.læsStreng(new FileInputStream(FilCache.hentFil(Programdata.instans.grunddata.radioTxtUrl, false)));
              forgrundstråd.post(new Runnable() {
                @Override
                public void run() {
                  Programdata.instans.grunddata.leguRadioTxt(radioTxtStr);
                  // Povas esti ke la listo de kanaloj ŝanĝiĝis, pro tio denove kontrolu ĉu reŝarĝi bildojn
                  Programdata.instans.grunddata.ŝarĝiKanalEmblemojn(true);
                  opdaterObservatører(Programdata.instans.grunddata.observatører);
                }
              });
            } catch (Exception e) {
              Log.e(e);
            }
          }
        }.start();
      }

      String pn = App.instans.getPackageName();
      for (final Kanal k : Programdata.instans.grunddata.kanaler) {
        k.kanallogo_resid = res.getIdentifier("kanalappendix_" + k.kode.toLowerCase().replace('ø', 'o').replace('å', 'a'), "drawable", pn);
      }

      String kanalkode = prefs.getString(FORETRUKKEN_KANAL, null);
      // Hvis brugeren foretrækker P4 er vi nødt til at finde underkanalen
      kanalkode = tjekP4OgVælgUnderkanal(kanalkode);

      Kanal aktuelKanal = Programdata.instans.grunddata.kanalFraKode.get(kanalkode);
      if (aktuelKanal == null || aktuelKanal == Grunddata.ukendtKanal) {
        aktuelKanal = Programdata.instans.grunddata.forvalgtKanal;
        Log.d("forvalgtKanal=" + aktuelKanal);
      }

      if (!aktuelKanal.harStreams()) { // ikke && App.erOnline(), det kan være vi har en cachet udgave
        final Kanal kanal = aktuelKanal;
        Request<?> req = new DrVolleyStringRequest(aktuelKanal.getStreamsUrl(), new DrVolleyResonseListener() {
          @Override
          public void fikSvar(String json, boolean fraCache, boolean uændret) throws Exception {
            if (uændret) return; // ingen grund til at parse det igen
            kanal.setStreams(json);
            Log.d("hentStreams akt fraCache=" + fraCache + " => " + kanal);
          }
        }) {
          public Priority getPriority() {
            return Priority.HIGH;
          }
        };
        App.volleyRequestQueue.add(req);
      }

      Programdata.instans.afspiller = new Afspiller();
      Programdata.instans.afspiller.setLydkilde(aktuelKanal);


      registerReceiver(netværk, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
      netværk.onReceive(this, null); // Få opdateret netværksstatus
      fjernbetjening = new Fjernbetjening();

      // udeståendeInitialisering kaldes når aktivitet bliver synlig første gang
      // - muligvis aldrig hvis app'en kun betjenes via levende ikon

    } catch (Exception ex) {
      // Burde der være popop-advarsel til bruger om intern fejl og rapporter til udvikler-dialog ?
      Log.rapporterFejl(ex);
    }

    try { // DRs skrifttyper er ikke offentliggjort i SVN, derfor kan følgende fejle:
      skrift_gibson = Typeface.createFromAsset(getAssets(), "Gibson-Regular.otf");
      skrift_gibson_fed = Typeface.createFromAsset(getAssets(), "Gibson-SemiBold.otf");
      skrift_georgia = Typeface.createFromAsset(getAssets(), "Georgia.ttf");
    } catch (Exception e) {
      if (ÆGTE_DR) Log.d("DRs skrifttyper er ikke tilgængelige: "+ e);
      skrift_gibson = Typeface.DEFAULT;
      skrift_gibson_fed = Typeface.DEFAULT_BOLD;
      skrift_georgia = Typeface.SERIF;
    }
    skrift_gibson_fed_span = new EgenTypefaceSpan("Gibson fed", App.skrift_gibson_fed);

    if (!EMULATOR) AppOpdatering.tjekForNyAPK(this);
    Log.d("onCreate tog " + (System.currentTimeMillis() - TIDSSTEMPEL_VED_OPSTART) + " ms");
  }

  public static String tjekP4OgVælgUnderkanal(String kanalkode) {
    if (Kanal.P4kode.equals(kanalkode)) {
      kanalkode = App.prefs.getString(App.P4_FORETRUKKEN_AF_BRUGER, null);
      if (kanalkode == null) kanalkode = "KH4";
      Log.d("P4 underkanal=" + kanalkode);
    }
    return kanalkode;
  }

  public static void advarEvtOmAlarmerHvisInstalleretPåSDkort(Activity akt) {
    if (App.erInstalleretPåSDKort && prefs.getBoolean(NØGLE_advaretOmInstalleretPåSDKort, false)) {
      AlertDialog.Builder dialog = new AlertDialog.Builder(akt);
      dialog.setTitle("SD-kort");
      dialog.setIcon(R.drawable.dri_advarsel_hvid);
      dialog.setMessage("Vækning fungerer muligvis ikke, når app'en er flyttet til SD-kort");
      dialog.setPositiveButton(android.R.string.ok, new AlertDialog.OnClickListener() {
        public void onClick(DialogInterface arg0, int arg1) {
          prefs.edit().putBoolean(NØGLE_advaretOmInstalleretPåSDKort, true).commit();
        }
      });
      dialog.show();
    }
  }

  /**
   * Initialisering af resterende data.
   * Dette sker når app'en er synlig og telefonen er online
   */
  private Runnable onlineinitialisering = new Runnable() {
    int forsinkelse = 15000;
    @Override
    public void run() {
      if (!erOnline()) return;
      boolean færdig = true;
      Log.d("Onlineinitialisering starter efter " + (System.currentTimeMillis() - TIDSSTEMPEL_VED_OPSTART) + " ms");

      if (App.netværk.status == Netvaerksstatus.Status.WIFI) { // Tjek at alle kanaler har deres streamsurler
        for (final Kanal kanal : Programdata.instans.grunddata.kanaler) {
          if (kanal.harStreams() || Kanal.P4kode.equals(kanal.kode))  continue;
          String url = kanal.getStreamsUrl();
          if (url==null) { // EO ŝanĝo
            Log.rapporterFejl(new IllegalStateException("url er null for "+kanal));
            continue;
          }
          //        Log.d("run()1 " + (System.currentTimeMillis() - TIDSSTEMPEL_VED_OPSTART) + " ms");
          Request<?> req = new DrVolleyStringRequest(url, new DrVolleyResonseListener() {
            @Override
            public void fikSvar(String json, boolean fraCache, boolean uændret) throws Exception {
              if (uændret) return;
              kanal.setStreams(json);
              Log.d("hentStreams app fraCache=" + fraCache + " => " + kanal);
            }
          }) {
            public Priority getPriority() {
              return Priority.LOW;
            }
          };
          App.volleyRequestQueue.add(req);
        }
      }

      if (Programdata.instans.favoritter.getAntalNyeUdsendelser() < 0) {
        færdig = false;
        Programdata.instans.favoritter.startOpdaterAntalNyeUdsendelser.run();
      }


      if (!færdig) {
        Log.d("Onlineinitialisering ikke færdig - prøver igen om " + forsinkelse/1000 +" sekunder");
        App.forgrundstråd.removeCallbacks(this);
        App.forgrundstråd.postDelayed(this, forsinkelse); // prøv igen om 15 sekunder og se om alle data er klar der
        forsinkelse = 15*forsinkelse/10;
      }

      // Forsøg at indlæse Drama&Bog og alle kanaler A-Å én gang ved opstart
      // Der er givetvis en del der sjældent bruger disse funktioner,
      // og hvis telefonen tror den er online men man ikke kan få forbindelse,
      // kan der komme rigtig mange store anomdninger i kø
      //  - det gøres kun én gang, hvilket skulle dække de fleste scenarier
      // TODO den rigtige løsning burde være at svarene for Drama&Bog og A-Å bliver hængende i cachen, tjekket her burde være om de er i cachen eller ej
      if (ÆGTE_DR && færdig && !prefs.getBoolean(DRAMA_OG_BOG__A_Å_INDLÆST, false)) {
        prefs.edit().putBoolean(DRAMA_OG_BOG__A_Å_INDLÆST, true);
        færdig = false;
        Programdata.instans.dramaOgBog.startHentData();
        Programdata.instans.programserierAtilÅ.startHentData();
      }
      if (færdig) {
        netværk.observatører.remove(this); // Hold ikke mere øje med om vi kommer online
        onlineinitialisering = null;
        Log.d("Onlineinitialisering færdig");
      }
    }
  };


  public static Runnable hentEvtNyeGrunddata = new Runnable() {
    long sidstTjekket = 0;

    @Override
    public void run() {
      if (!App.erOnline()) return;
      if (sidstTjekket + (App.EMULATOR ? 1000 : Programdata.instans.grunddata.opdaterGrunddataEfterMs) > System.currentTimeMillis())
        return;
      sidstTjekket = System.currentTimeMillis();
      Log.d("hentEvtNyeGrunddata " + (sidstTjekket - App.TIDSSTEMPEL_VED_OPSTART));
      Request<?> req = new DrVolleyStringRequest(Programdata.GRUNDDATA_URL, new DrVolleyResonseListener() {
        @Override
        public void fikSvar(String nyeGrunddata, boolean fraCache, boolean uændret) throws Exception {
          if (uændret || fraCache) return; // ingen grund til at parse det igen
          String gamleGrunddata = grunddata_prefs.getString(Programdata.GRUNDDATA_URL, null);
          if (nyeGrunddata.equals(gamleGrunddata)) return; // Det samme som var i prefs
          Log.d("Vi fik nye grunddata: fraCache=" + fraCache + nyeGrunddata);
          if (!PRODUKTION || App.fejlsøgning) App.kortToast("Vi fik nye grunddata");
          if (!App.ÆGTE_DR) {
            Programdata.instans.grunddata.kanaler.clear(); // EO
            Programdata.instans.grunddata.p4koder.clear(); // EO
            Programdata.instans.grunddata.eo_parseFællesGrunddata(nyeGrunddata);
          }
          Programdata.instans.grunddata.parseFællesGrunddata(nyeGrunddata);
          String pn = App.instans.getPackageName();
          for (final Kanal k : Programdata.instans.grunddata.kanaler) {
            k.kanallogo_resid = res.getIdentifier("kanalappendix_" + k.kode.toLowerCase().replace('ø', 'o').replace('å', 'a'), "drawable", pn);
          }
          if (!App.ÆGTE_DR) Programdata.instans.grunddata.ŝarĝiKanalEmblemojn(true);
          // fix for https://mint.splunk.com/dashboard/project/cd78aa05/errors/2774928662
          opdaterObservatører(Programdata.instans.grunddata.observatører);
          // Er vi nået hertil så gik parsning godt - gem de nye stamdata i prefs, så de også bruges ved næste opstart
          grunddata_prefs.edit().putString(Programdata.GRUNDDATA_URL, nyeGrunddata).commit();
        }
      }) {
        public Priority getPriority() {
          return Priority.LOW;
        }
      };
      App.volleyRequestQueue.add(req);
    }
  };

  /** Opdaterer alle observatører fra forgrundstråden, præcist én gang (selv ved gentagne kald) */
  public static void opdaterObservatører(ArrayList<Runnable> observatører) {
    // xxx ne funcias
    /*
    Lydkilde lk = DRData.instans.afspiller.getLydkilde();
    if (lk instanceof Kanal) {
      lk = DRData.instans.grunddata.kanalFraSlug.get(lk.getKanal().slug);
      DRData.instans.afspiller.setLydkilde(lk);
    }
    */

    // fix for https://mint.splunk.com/dashboard/project/cd78aa05/errors/2774928662
    for (Runnable r : new ArrayList<Runnable>(observatører)) {
      forgrundstråd.removeCallbacks(r);
      forgrundstråd.post(r);
    }
  }

  /*
   * Kilde: http://developer.android.com/training/basics/network-ops/managing.html
   */
  public static boolean erOnline() {
    NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
    return (networkInfo != null && networkInfo.isConnected());
  }


  public static Activity aktivitetIForgrunden = null;
  public static Activity senesteAktivitetIForgrunden = null;
  private static int erIGang = 0;

  private static LinkedHashMap<String, Integer> hvadErIGang = new LinkedHashMap<String, Integer>();
  /**
   * Signalerer over for brugeren at netværskommunikation er påbegyndt eller afsluttet.
   * Forårsager at det 'drejende hjul' (ProgressBar) vises på den aktivitet der er synlig p.t.
   * @param netværkErIGang true for påbegyndt og false for afsluttet.
   */
  public static synchronized void sætErIGang(boolean netværkErIGang, String hvad) {
    boolean før = erIGang > 0;
    if (App.EMULATOR) {
      Integer antal = hvadErIGang.get(hvad);
      antal = (antal==null?0:antal) + (netværkErIGang?1:-1);
      hvadErIGang.put(hvad, antal);
      if (antal>1) Log.d("sætErIGang: "+hvad+" har "+antal+" samtidige anmodninger");
      else if (antal<0) Log.e(new IllegalStateException("erIGang manglede " + hvad));
      else if (netværkErIGang) Log.d("sætErIGang: "+hvad);
      //if (!netværkErIGang && hvad.trim().length()==0) Log.e(new IllegalStateException("hvad er tom"));
    }
    erIGang += netværkErIGang ? 1 : -1;
    boolean nu = erIGang > 0;
    if (fejlsøgning) Log.d("erIGang = " + erIGang);
    if (erIGang < 0) {
      if (App.EMULATOR) Log.e(new IllegalStateException("erIGang er " + erIGang + " hvadErIGang="+hvadErIGang));
      erIGang = 0;
    }
    if (før != nu && aktivitetIForgrunden != null) forgrundstråd.post(sætProgressbar);
    // Fejltjek
  }

  private static Runnable sætProgressbar = new Runnable() {
    public void run() {
      if (aktivitetIForgrunden instanceof Basisaktivitet) {
        ((Basisaktivitet) aktivitetIForgrunden).sætProgressBar(erIGang > 0);
      }
    }
  };

  public void aktivitetStartet(Activity akt) {
    senesteAktivitetIForgrunden = aktivitetIForgrunden = akt;
    sætProgressbar.run();
    if (onlineinitialisering != null) {
      if (App.erOnline()) {
        App.forgrundstråd.postDelayed(onlineinitialisering, 250); // Initialisér onlinedata
      } else {
        App.netværk.observatører.add(onlineinitialisering); // Vent på at vi kommer online og lav så et tjek
      }
    }
    if (kørFørsteGangAppIkkeMereErSynlig != null) forgrundstråd.removeCallbacks(kørFørsteGangAppIkkeMereErSynlig);
    forgrundstråd.postDelayed(synlighedsSporing, 50);
  }

  public void aktivitetStoppet(Activity akt) {
    if (akt != aktivitetIForgrunden) return; // en anden aktivitet er allerede startet
    aktivitetIForgrunden = null;
    if (kørFørsteGangAppIkkeMereErSynlig != null) forgrundstråd.postDelayed(kørFørsteGangAppIkkeMereErSynlig, 1000);
    forgrundstråd.postDelayed(synlighedsSporing, 50);
  }

  Runnable synlighedsSporing = new Runnable() {
    boolean sidstSynlig = false;
    @Override
    public void run() {
      boolean synligNu = aktivitetIForgrunden!=null;
      if (sidstSynlig == synligNu) return;
      sidstSynlig = synligNu;
      Sidevisning.i().synlig(synligNu);
    }
  };

  /**
   * Køres et sekund efter at app'en ikke mere er synlig.
   * Her rydder vi op i filer
   */
  private Runnable kørFørsteGangAppIkkeMereErSynlig = new Runnable() {
    @Override
    public void run() {
      if (aktivitetIForgrunden != null) return;
      if (App.fejlsøgning) App.kortToast("kørFørsteGangAppIkkeMereErSynlig");
      final int DAGE = 24 * 60 * 60 * 1000;
      int volleySlettet = volleyCache.sletFilerÆldreEnd(TIDSSTEMPEL_VED_OPSTART-10*DAGE);
      int aqSlettet = Diverse.sletFilerÆldreEnd(new File(getCacheDir(), "aquery"), TIDSSTEMPEL_VED_OPSTART-4*DAGE);
      // Mappe ændret fra standardmappen "volley" til "dr_volley" 19. nov 2014.
      // Det skyldtes at et hukommelsesdump viste, at Volley indekserede alle filerne i standardmappen,
      // uden om vores implementation, hvilket gav et unødvendigt overhead på ~ 1MB
      File gammelVolleyCacheDir = new File(getCacheDir(), "volley");
      Diverse.sletFilerÆldreEnd(gammelVolleyCacheDir, TIDSSTEMPEL_VED_OPSTART-7*DAGE);

      if (fejlsøgning) {
        App.kortToast("volleyCache: " + volleySlettet / 1000 + " kb frigivet");
        App.kortToast("AQ: " + aqSlettet / 1000 + " kb kunne frigivet");
      }
      kørFørsteGangAppIkkeMereErSynlig = null;
    }
  };

  private static Toast forrigeToast;
  public static void langToast(String txt) {
    Log.d("langToast(" + txt);
    if (aktivitetIForgrunden == null) txt = "DR Radio:\n" + txt;
    final String txt2 = txt;
    forgrundstråd.post(new Runnable() {
      @Override
      public void run() {
        // lange toasts bør blive hængende
        if (forrigeToast!=null && forrigeToast.getDuration()==Toast.LENGTH_SHORT && !App.fejlsøgning && !App.EMULATOR) forrigeToast.cancel();
        forrigeToast = Toast.makeText(instans, txt2, Toast.LENGTH_LONG);
        forrigeToast.show();
      }
    });
  }

  public static void kortToast(String txt) {
    Log.d("kortToast(" + txt);
    if (aktivitetIForgrunden == null) txt = "DR Radio:\n" + txt;
    final String txt2 = txt;
    forgrundstråd.post(new Runnable() {
      @Override
      public void run() {
        // lange toasts bør blive hængende
        if (forrigeToast!=null && forrigeToast.getDuration()==Toast.LENGTH_SHORT && !App.fejlsøgning && !App.EMULATOR) forrigeToast.cancel();
        forrigeToast = Toast.makeText(instans, txt2, Toast.LENGTH_SHORT);
        forrigeToast.show();
      }
    });
  }

  public static void kortToast(int resId) { kortToast(instans.getResources().getString(resId));}
  public static void langToast(int resId) { langToast(instans.getResources().getString(resId));}

  public static void kontakt(Activity akt, String emne, String txt, String vedhæftning) {
    String[] modtagere;
    try {
      modtagere = Diverse.jsonArrayTilArrayListString(Programdata.instans.grunddata.android_json.getJSONArray("kontakt_modtagere")).toArray(new String[0]);
    } catch (Exception ex) {
      Log.e(ex);
      modtagere = new String[]{"jacob.nordfalk@gmail.com"};
    }

    Intent i = new Intent(Intent.ACTION_SEND);
    i.setType("text/plain");
    i.putExtra(Intent.EXTRA_EMAIL, modtagere);
    i.putExtra(Intent.EXTRA_SUBJECT, emne);


    android.util.Log.d("KONTAKT", txt);
    if (vedhæftning != null) try {
      String logfil = "programlog.txt";
      @SuppressLint("WorldReadableFiles") FileOutputStream fos = akt.openFileOutput(logfil, akt.MODE_WORLD_READABLE);
      fos.write(vedhæftning.getBytes());
      fos.close();
      Uri uri = Uri.fromFile(new File(akt.getFilesDir().getAbsolutePath(), logfil));

//      https://medium.com/google-developers/sharing-content-between-android-apps-2e6db9d1368b#.kkoqnbkar
//      Uri uriToImage = FileProvider.getUriForFile(
//              akt, FILES_AUTHORITY, imageFile);
// ??

      txt += "\n\nRul op øverst i meddelelsen og giv din feedback, tak.";
      i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
      i.putExtra(Intent.EXTRA_STREAM, uri);
    } catch (Exception e) {
      Log.e(e);
      txt += "\n" + e;
    }
    i.putExtra(Intent.EXTRA_TEXT, txt);
//    akt.startActivity(Intent.createChooser(i, "Send meddelelse..."));
    try {
      akt.startActivity(i);
    } catch (Exception e) {
      App.langToast(e.toString());
      Log.rapporterFejl(e);
    }
  }


  @Override
  public void onLowMemory() {
    // Ryd op når der mangler RAM
    BitmapAjaxCallback.clearCache();
    super.onLowMemory();
  }

  @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
  @Override
  public void onTrimMemory(int level) {
    if (level >= TRIM_MEMORY_BACKGROUND) BitmapAjaxCallback.clearCache();
    super.onTrimMemory(level);
  }

  /**
   * I fald telefonens ur går forkert kan det ses her - alle HTTP-svar bliver jo stemplet med servertiden
   */
  private static long serverkorrektionTilKlienttidMs = 0;

  /**
   * Giver et aktuelt tidsstempel på hvad serverens ur viser
   * @return tiden, i  millisekunder siden 1. Januar 1970 00:00:00.0 UTC.
   */
  public static long serverCurrentTimeMillis() {
    return System.currentTimeMillis() + serverkorrektionTilKlienttidMs;
  }

  public static void sætServerCurrentTimeMillis(long servertid) {
    long serverkorrektionTilKlienttidMs2 = servertid - System.currentTimeMillis();
    if (Math.abs(App.serverkorrektionTilKlienttidMs - serverkorrektionTilKlienttidMs2) > 120 * 1000) {
      Log.d("SERVERTID korrigerer tid med " + ((serverkorrektionTilKlienttidMs2 + App.serverkorrektionTilKlienttidMs) / 1000 / 60) + " minutter fra " + new Date(serverCurrentTimeMillis()) + " til " + new Date(servertid));
      App.serverkorrektionTilKlienttidMs = serverkorrektionTilKlienttidMs2;
      new Exception("SERVERTID korrigeret med " + serverkorrektionTilKlienttidMs2 / 1000 / 60 + " min til " + new Date(servertid)).printStackTrace();
    }
  }

  /** Kan kaldet til at afgøre om vi er igang med at teste noget fra en main()-metode eller app'en rent faktisk kører */
  public static boolean testFraMain() {
    return instans == null;
  }

  /**
   * Lille klasse der holder nogle farver vi ikke gider slå op i resurser efter hele tiden
   */
  public static class DRFarver {
    public int grå40 = res.getColor(R.color.grå40);
    public int blå = res.getColor(R.color.blå);
    public int grå60 = res.getColor(R.color.grå60);
  }
}
