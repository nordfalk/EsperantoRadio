package dk.dr.radio.data;

import android.app.Application;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Date;

import dk.dr.radio.data.dr_v3.DRJson;
import dk.dr.radio.data.dr_v3.MuOnlineRadioBackend;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.ApplicationSingleton;
import dk.dr.radio.diverse.FilCache;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.diverse.Udseende;
import dk.dr.radio.net.Diverse;
import dk.dr.radio.v3.BuildConfig;

import static dk.dr.radio.data.AfproevGammelDrRadioBackend.hentStreng;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricGradleTestRunner.class)
@Config(packageName = "dk.dr.radio.v3", constants = BuildConfig.class, sdk = 21, application = AfproevMuOnlineRadioBackend.TestApp.class)
public class AfproevMuOnlineRadioBackend {


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
      App.backend[0] = backend = new MuOnlineRadioBackend();
      //App.instans.initData(this);
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

  static MuOnlineRadioBackend backend;

  @Test
  public void tjekDirekteUdsendelser() throws Exception {
    for (Kanal kanal : App.grunddata.kanaler) {
      if (kanal.kode.equals("P4F")) continue;
      /* overflødigt - kanal har allerede streams
      String url = backend.getKanalStreamsUrl(kanal);
      String data = hentStreng(url);
      JSONObject o = new JSONObject(data);
      ArrayList<Lydstream> s = backend.parsStreams(o);
      kanal.setStreams(s);
      */
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
      int antalUdsendelser = 0;
      int antalUdsendelserMedPlaylister = 0;
      int antalUdsendelserMedLydstreams = 0;

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


        boolean gavNull = false;
        Programserie ps = i.programserieFraSlug.get(u.programserieSlug);
        if (ps == null) try {
          String str = hentStreng(backend.getProgramserieUrl(null, u.programserieSlug, 0));
          if ("null".equals(str)) gavNull = true;
          else {
            JSONObject data = new JSONObject(str);
            Log.d(kanal.navn + ": " + u.startTidKl + " "+ u.titel+" "+ps);
            ps = backend.parsProgramserie(data, null);
            JSONArray prg = data.getJSONArray(DRJson.Programs.name());
            ArrayList<Udsendelse> udsendelser = backend.parseUdsendelserForProgramserie(prg, kanal, App.data);
            ps.tilføjUdsendelser(0, udsendelser);
            i.programserieFraSlug.put(u.programserieSlug, ps);
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }


}
