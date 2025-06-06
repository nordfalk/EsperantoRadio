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
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.accessibility.AccessibilityManager;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.HttpStack;
import com.android.volley.toolbox.HurlStack;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;

import dk.dr.radio.afspilning.Afspiller;
import dk.dr.radio.afspilning.Fjernbetjening;
import dk.dr.radio.akt.Basisaktivitet;
import dk.dr.radio.akt.diverse.EgenTypefaceSpan;
import dk.dr.radio.backend.Backend;
import dk.dr.radio.data.Grunddata;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Programdata;
import dk.dr.radio.net.Diverse;
import dk.dr.radio.net.FilCache;
import dk.dr.radio.net.Netvaerksstatus;
import dk.dr.radio.net.volley.DrBasicNetwork;
import dk.dr.radio.net.volley.DrDiskBasedCache;
import dk.dr.radio.net.volley.Netsvar;
import dk.dr.radio.v3.BuildConfig;
import dk.dr.radio.v3.R;
import dk.dr.radio.backend.Netkald;
import dk.dr.radio.backend.NetsvarBehander;
import io.sentry.SentryLevel;
import io.sentry.android.core.SentryAndroid;

public class App {
  public static App instans;
  public static Programdata data;
  public static Grunddata grunddata;
  public static Afspiller afspiller;
  public static final Talesyntese talesyntese = new Talesyntese();

  public static final boolean PRODUKTION = !BuildConfig.DEBUG;
  public static boolean EMULATOR = true; // Sæt i onCreate(), ellers virker det ikke i std Java
  public static boolean IKKE_Android_VM = false; // Hvis test fra almindelig JVM

  /** Sæt sprogvalg til dansk eller esperanto alt efter hvilken version der køres med */
  public static Configuration sprogKonfig;
  public static String versionsnavn = "(ukendt)";
  public static String pakkenavn;

  public static ConnectivityManager connectivityManager;
  public static NotificationManager notificationManager;
  public static AudioManager audioManager;
  public static AccessibilityManager accessibilityManager;

  private static SharedPreferences grunddata_prefs;

  public static final String FORETRUKKEN_KANAL = "FORETRUKKEN_kanal";
  private static final String NØGLE_advaretOmInstalleretPåSDKort = "erInstalleretPåSDKort";
  public static SharedPreferences prefs;
  public static boolean fejlsøgning = false;
  public static Handler forgrundstråd;
  public static Typeface skrift_gibson;
  public static Typeface skrift_gibson_fed;
  public static Typeface skrift_georgia;

  public static Netvaerksstatus netværk;
  public static Fjernbetjening fjernbetjening;
  public static RequestQueue volleyRequestQueue;
  private static boolean erInstalleretPåSDKort;
  public static Backend backend;
  public static Netkald netkald = new Netkald();
  private DrDiskBasedCache volleyCache;
  public static EgenTypefaceSpan skrift_gibson_fed_span;
  public static DRFarver color;
  public static Resources res;
  /** Tidsstempel der kan bruges til at afgøre hvilke filer der faktisk er brugt efter denne opstart */
  private static long TIDSSTEMPEL_VED_OPSTART;


  public void init(Application ctx) {
    TIDSSTEMPEL_VED_OPSTART = System.currentTimeMillis();

    prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
    fejlsøgning = FilCache.fejlsøgning = prefs.getBoolean("fejlsøgning", false);
    forgrundstråd = new Handler();
    connectivityManager = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
    notificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
    audioManager = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
    res = ctx.getResources();
    pakkenavn = ctx.getPackageName();

    backend = new Backend();

    sprogKonfig = new Configuration();

    EMULATOR = Build.PRODUCT.contains("sdk") || Build.MODEL.contains("Emulator") || IKKE_Android_VM;
    if (!EMULATOR) {
      SentryAndroid.init(ctx, options -> {
        options.setDsn("https://a251d3860be54aa5a7ebd02084a91a9a@o332889.ingest.us.sentry.io/1886051");
        // Add a callback that will be used before the event is sent to Sentry.
        // With this callback, you can modify the event or, when returning null, also discard the event.
        options.setBeforeSend((event, hint) -> {
          if (SentryLevel.DEBUG.equals(event.getLevel()))
            return null;
          else
            return event;
        });
      });
    }

    //com.jakewharton.threetenabp.AndroidThreeTen.init(ctx);
    net.danlew.android.joda.JodaTimeAndroid.init(ctx);

    try {
      PackageInfo pi = ctx.getPackageManager().getPackageInfo(pakkenavn, 0);
      App.versionsnavn = pakkenavn + "/" + pi.versionName;
      if (EMULATOR) App.versionsnavn += " EMU";
      Log.d("App.versionsnavn=" + App.versionsnavn);

      App.erInstalleretPåSDKort = 0 != (pi.applicationInfo.flags & ApplicationInfo.FLAG_EXTERNAL_STORAGE);
      if (!App.erInstalleretPåSDKort)
        prefs.edit().remove(NØGLE_advaretOmInstalleretPåSDKort).commit();
    } catch (Exception e) {
      Log.rapporterFejl(e);
    }


    FilCache.init(new File(ctx.getCacheDir(), "FilCache"));
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
    File cacheDir = new File(ctx.getCacheDir(), "dr_volley");
    volleyCache = new DrDiskBasedCache(cacheDir);
    volleyRequestQueue = new RequestQueue(volleyCache, network);
    volleyRequestQueue.start();


    netværk = new Netvaerksstatus();
    ContextCompat.registerReceiver(ctx, netværk, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION), ContextCompat.RECEIVER_EXPORTED);
    netværk.onReceive(ctx, null); // Få opdateret netværksstatus

    afspiller = new Afspiller();
    fjernbetjening = new Fjernbetjening();
  }

  public void initData(Application ctx) {
    data = new Programdata();
    grunddata = new Grunddata();
    // Indlæsning af grunddata/stamdata.
    // Først tjekkes om vi har en udgave i prefs, og ellers bruges den i raw-mappen
    // På et senere tidspunkt henter vi nye grunddata
    grunddata_prefs = ctx.getSharedPreferences("grunddata", 0);
    try {
      Log.d("Initialiserer backend "+backend);
      String grunddataStr = grunddata_prefs.getString(backend.getGrunddataUrl(), null);

      if (grunddataStr == null || App.EMULATOR) { // Ingen grunddata fra sidste start - det er nok en frisk installation
        grunddataStr = Diverse.læsStreng(backend.getLokaleGrunddata(ctx));
      }
      if (grunddataStr != null) {
        backend.initGrunddata(grunddata, grunddataStr);
        backend.startHentBg(grunddata);
      }
    } catch (Exception e) { Log.e(""+backend, e); }
    // undgå crash
    if (grunddata.json == null) grunddata.json = new JSONObject();
    if (grunddata.android_json == null) grunddata.android_json = new JSONObject();
    if (grunddata.forvalgtKanal == null) grunddata.forvalgtKanal = grunddata.kanaler.get(0); // Muzaiko / P1

    try {
      String kanalkode = prefs.getString(FORETRUKKEN_KANAL, null);

      Kanal aktuelKanal = grunddata.kanalFraSlug.get(kanalkode);
      if (aktuelKanal == null || aktuelKanal == Grunddata.ukendtKanal) {
        aktuelKanal = grunddata.forvalgtKanal;
        Log.d("forvalgtKanal=" + aktuelKanal);
      }

      if (aktuelKanal.getUdsendelse()==null) {
        Log.rapporterFejl(new IllegalArgumentException("Ingen udsendelser for "+aktuelKanal+" - skifter til "+grunddata.kanaler.get(0)));
        aktuelKanal = grunddata.kanaler.get(0); // Problemet er at afspiller forventer en udsendelse på kanalen
      }

      afspiller.setLydkilde(aktuelKanal);

      talesyntese.init(ctx);
    } catch (Exception ex) {
      Log.rapporterFejl(ex);
    }
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

      if (App.backend.favoritter.getAntalNyeUdsendelser() < 0) {
        færdig = false;
        App.backend.favoritter.startOpdaterAntalNyeUdsendelser.run();
      }


      if (!færdig) {
        Log.d("Onlineinitialisering ikke færdig - prøver igen om " + forsinkelse/1000 +" sekunder");
        App.forgrundstråd.removeCallbacks(this);
        App.forgrundstråd.postDelayed(this, forsinkelse); // prøv igen om 15 sekunder og se om alle data er klar der
        forsinkelse = 15*forsinkelse/10;
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
      if (sidstTjekket + (App.EMULATOR ? 1000 : grunddata.opdaterGrunddataEfterMs) > System.currentTimeMillis())
        return;
      sidstTjekket = System.currentTimeMillis();
      Log.d("hentEvtNyeGrunddata " + (sidstTjekket - App.TIDSSTEMPEL_VED_OPSTART));
      {
        App.netkald.kald(null, backend.getGrunddataUrl(), Request.Priority.LOW, new NetsvarBehander() {
          @Override
          public void fikSvar(Netsvar s) throws Exception {
            if (s.uændret || s.fraCache || s.fejl) return; // ingen grund til at parse det igen
            String nyeGrunddata = s.json.trim();
            String gamleGrunddata = grunddata_prefs.getString(backend.getGrunddataUrl(), null);
            if (nyeGrunddata.equals(gamleGrunddata)) return; // Det samme som var i prefs
            Log.d("Vi fik nye grunddata for "+backend+" : fraCache=" + s.fraCache+"\n"+s.url);
            if (!PRODUKTION || App.fejlsøgning) {
              if (gamleGrunddata!=null) Log.d("gl="+gamleGrunddata.length()+" "+gamleGrunddata.hashCode()+ " "+gamleGrunddata.replace('\n',' '));
              Log.d("ny="+nyeGrunddata.length()+" "+nyeGrunddata.hashCode()+ " "+nyeGrunddata.replace('\n',' '));
              if (gamleGrunddata!=null) Log.d("gl="+gamleGrunddata.substring(gamleGrunddata.length()-20).replace('\n',' ')+"'XX");
              Log.d("ny="+nyeGrunddata.substring(nyeGrunddata.length()-20).replace('\n',' ')+"'XX");
              App.kortToast("Vi fik nye grunddata for\n"+backend);
            }
            try {
              Grunddata grunddata2 = new Grunddata();
              backend.initGrunddata(grunddata2, nyeGrunddata);
              backend.startHentBg(grunddata2);

              // Er vi nået hertil så gik parsning godt - gem de nye stamdata i prefs, så de også bruges ved næste opstart
              grunddata.android_json = grunddata2.android_json;
              grunddata.json = grunddata2.json;
              grunddata.kanaler.clear();
              grunddata.kanaler.addAll(grunddata2.kanaler);
              grunddata.forvalgtKanal = grunddata2.forvalgtKanal;
              grunddata.kanalFraSlug.putAll(grunddata2.kanalFraSlug);
              grunddata_prefs.edit().putString(backend.getGrunddataUrl(), nyeGrunddata).commit();
            } catch (Exception e) { Log.rapporterFejl(e); } // rapportér problem med parsning af grunddata
            // fix for https://mint.splunk.com/dashboard/project/cd78aa05/errors/2774928662
            opdaterObservatører(grunddata.observatører);
          }
        });
      }
    }
  };

  /** Opdaterer alle observatører fra forgrundstråden, præcist én gang (selv ved gentagne kald) */
  public static void opdaterObservatører(ArrayList<Runnable> observatører) {
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


  public void aktivitetOnCreate(Basisaktivitet ctx) {
    ctx.getResources().updateConfiguration(App.sprogKonfig, null);

    // Forgrundsinitialisering der skal ske før app'en bliver synlig første gang
    // - muligvis aldrig hvis app'en kun betjenes via levende ikon, eller kun er aktiv i baggrunden

    if (App.color!=null) return; // initialisering allerede sket
    App.color = new DRFarver();
    accessibilityManager = (AccessibilityManager) ctx.getSystemService(Context.ACCESSIBILITY_SERVICE); // tager tid i test

    skrift_gibson = Typeface.DEFAULT;
    skrift_gibson_fed = Typeface.DEFAULT_BOLD;
    skrift_georgia = Typeface.SERIF;
    skrift_gibson_fed_span = new EgenTypefaceSpan("Gibson fed", App.skrift_gibson_fed);

    /*
    AppOpdatering.APK_URL = res.getString(R.string.AppOpdatering_APK_URL);
    if (!EMULATOR) AppOpdatering.tjekForNyAPK(ctx);
     */
    Log.d("onCreate tog " + (System.currentTimeMillis() - TIDSSTEMPEL_VED_OPSTART) + " ms");
  }

  public static Activity aktivitetIForgrunden = null;
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

  public void aktivitetOnStart(Activity akt) {
    aktivitetIForgrunden = akt;
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

  public void aktivitetOnStop(Activity akt) {
    if (akt != aktivitetIForgrunden) return; // en anden aktivitet er allerede startet
    aktivitetIForgrunden = null;
    if (kørFørsteGangAppIkkeMereErSynlig != null) forgrundstråd.postDelayed(kørFørsteGangAppIkkeMereErSynlig, 1000);
    forgrundstråd.postDelayed(synlighedsSporing, 50);
  }

  private Runnable synlighedsSporing = new Runnable() {
    boolean sidstSynlig = false;
    @Override
    public void run() {
      boolean synligNu = aktivitetIForgrunden!=null;
      if (sidstSynlig == synligNu) return;
      sidstSynlig = synligNu;
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
      int aqSlettet = Diverse.sletFilerÆldreEnd(new File(ApplicationSingleton.instans.getCacheDir(), "aquery"), TIDSSTEMPEL_VED_OPSTART-10*DAGE);

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
    if (aktivitetIForgrunden == null) txt = "Radio:\n" + txt;
    final String txt2 = txt;
    forgrundstråd.post(new Runnable() {
      @Override
      public void run() {
        // lange toasts bør blive hængende
        if (forrigeToast!=null && forrigeToast.getDuration()==Toast.LENGTH_SHORT && !App.fejlsøgning && !App.EMULATOR) forrigeToast.cancel();
        forrigeToast = Toast.makeText(ApplicationSingleton.instans, txt2, Toast.LENGTH_LONG);
        forrigeToast.show();
      }
    });
  }

  public static void kortToast(String txt) {
    Log.d("kortToast(" + txt);
    if (aktivitetIForgrunden == null) txt = "Radio:\n" + txt;
    final String txt2 = txt;
    forgrundstråd.post(new Runnable() {
      @Override
      public void run() {
        // lange toasts bør blive hængende
        if (forrigeToast!=null && forrigeToast.getDuration()==Toast.LENGTH_SHORT && !App.fejlsøgning && !App.EMULATOR) forrigeToast.cancel();
        forrigeToast = Toast.makeText(ApplicationSingleton.instans, txt2, Toast.LENGTH_SHORT);
        forrigeToast.show();
      }
    });
  }

  public static void kortToast(int resId) { kortToast(res.getString(resId));}
  public static void langToast(int resId) { langToast(res.getString(resId));}

  public static void kontakt(Activity akt, String emne, String txt, String vedhæftning) {
    String[] modtagere;
    try {
      modtagere = Diverse.jsonArrayTilArrayListString(grunddata.android_json.getJSONArray("kontakt_modtagere")).toArray(new String[0]);
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
    if (Math.abs(App.serverkorrektionTilKlienttidMs - serverkorrektionTilKlienttidMs2) > 600 * 1000) {
      Log.d("SERVERTID korrigerer tid med " + ((serverkorrektionTilKlienttidMs2 + App.serverkorrektionTilKlienttidMs) / 1000 / 60) + " minutter fra " + new Date(serverCurrentTimeMillis()) + " til " + new Date(servertid));
      App.serverkorrektionTilKlienttidMs = serverkorrektionTilKlienttidMs2;
      new Exception("SERVERTID korrigeret med " + serverkorrektionTilKlienttidMs2 / 1000 / 60 + " min til " + new Date(servertid)).printStackTrace();
    }
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
