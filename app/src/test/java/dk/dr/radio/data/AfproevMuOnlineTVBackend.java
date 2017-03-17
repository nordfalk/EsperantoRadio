package dk.dr.radio.data;

import android.app.Application;
import android.os.Build;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Date;

import dk.dk.niclas.MuOnlineTVBackend;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.ApplicationSingleton;
import dk.dr.radio.diverse.FilCache;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.diverse.Udseende;
import dk.dr.radio.net.Diverse;
import dk.dr.radio.v3.BuildConfig;

import static dk.dr.radio.data.AfproevGammelDrRadioBackend.hentStreng;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricGradleTestRunner.class)
@Config(packageName = "dk.dr.radio.v3", constants = BuildConfig.class, sdk = 21, application = AfproevMuOnlineTVBackend.TestApp.class)
public class AfproevMuOnlineTVBackend {


  public static class TestApp extends Application {
    static {
      App.IKKE_Android_VM = true;
      Udseende.ESPERANTO = false;
    }

    @Override
    public void onCreate() {
      Log.d("onCreate " + Build.PRODUCT + Build.MODEL);
      FilCache.init(new File("/tmp/drradio-cache"));
      Log.d("arbejdsmappe = " + new File(".").getAbsolutePath());
      super.onCreate();
      ApplicationSingleton.instans = this;
      App.instans = new App();
      //App.instans.init(this);
      App.res = getResources();
      App.assets = getAssets();
      App.pakkenavn = getPackageName();
      App.backend = backend = new MuOnlineTVBackend();
      App.data = new Programdata();
      try {
        String grunddataStr = Diverse.læsStreng(new FileInputStream("src/main/res/raw/grunddata.json"));
        App.grunddata = backend.initGrunddata(grunddataStr, null);
      } catch (Exception e) {
        e.printStackTrace();
      }
      //App.fejlsøgning = true;
    }
  }

  static MuOnlineTVBackend backend;

  @Test
  public void tjekDirekteUdsendelser() throws Exception {
    System.out.println( "kode \tnavn \tslug \tstreams");
    for (Kanal kanal : App.grunddata.kanaler) {
      System.out.println( kanal.kode + "  \t" + kanal.navn + "  \t" + kanal.slug+ " \t" + kanal.streams);
      assertTrue(kanal.findBedsteStreams(false).size() > 0);
    }
  }

  @Test
  public void tjekAktuelleUdsendelser() throws Exception {
    Programdata i = App.data;// = new DRData();
    Date dato = new Date(System.currentTimeMillis()-1000*60*60*12);
    String datoStr = Datoformater.apiDatoFormat.format(dato);
    for (Kanal kanal : App.grunddata.kanaler) {
      if (kanal.kode.equals("P4F")) continue;
      if ("DRN".equals(kanal.kode)) continue; // ikke DR Nyheder

      String url = backend.getUdsendelserPåKanalUrl(kanal, datoStr);
      String udsPKstr = hentStreng(url);
      kanal.setUdsendelserForDag(backend.parseUdsendelserForKanal(udsPKstr, kanal, dato, App.data), datoStr);

      for (Udsendelse u : kanal.udsendelser) {
        url = backend.getUdsendelseStreamsUrl(u);
        Log.d(kanal.navn + ": " + u.startTidKl + " "+ u.titel+" "+u+ "    "+url);
        if (url != null) {
          JSONObject obj = new JSONObject(hentStreng(url));
          Log.d(kanal.navn + ": " + u.startTidKl + " " + u.titel + " " + obj);
          ArrayList<Lydstream> s = backend.parsStreams(obj);
          u.setStreams(s);
          if (!u.kanHøres) Log.d("Ingen lydstreams!!");
        }
      }
    }
  }


}
