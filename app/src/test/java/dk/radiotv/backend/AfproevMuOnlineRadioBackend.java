package dk.radiotv.backend;

import android.app.Application;

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

import dk.dr.radio.data.Datoformater;
import dk.dr.radio.data.Grunddata;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Lydstream;
import dk.dr.radio.data.Programdata;
import dk.dr.radio.data.Programserie;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.ApplicationSingleton;
import dk.dr.radio.diverse.FilCache;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.diverse.Udseende;
import dk.dr.radio.net.Diverse;
import dk.dr.radio.v3.BuildConfig;

import static org.junit.Assert.assertTrue;

@RunWith(RobolectricGradleTestRunner.class)
@Config(packageName = "dk.dr.radio.v3", constants = BuildConfig.class, sdk = 21, application = AfproevMuOnlineRadioBackend.TestApp.class)
public class AfproevMuOnlineRadioBackend {

  public static class TestApp extends Application {
    @Override
    public void onCreate() {
      App.IKKE_Android_VM = true;
      Udseende.ESPERANTO = false;
      FilCache.init(new File("/tmp/drradio-cache"));
      Log.d("arbejdsmappe = " + new File(".").getAbsolutePath());
      super.onCreate();
      ApplicationSingleton.instans = this;
      App.instans = new App();
      //App.instans.init(this); - tager tid vi laver det vigtigste herunder
      App.res = getResources();
      App.assets = getAssets();
      App.pakkenavn = getPackageName();
      App.backend = new Backend[] { backend = new MuOnlineRadioBackend() };
      //App.instans.initData(this); - tager tid vi laver det vigtigste herunder
      App.data = new Programdata();
      try {
        String grunddataStr = Diverse.læsStreng(backend.getLokaleGrunddata(this));
        backend.initGrunddata(App.grunddata = new Grunddata(), grunddataStr);
      } catch (Exception e) {
        e.printStackTrace();
      }
      App.grunddata.kanaler = backend.kanaler;
      //App.fejlsøgning = true;
    }
  }

  static MuOnlineRadioBackend backend;

  @Test
  public void tjekDirekteUdsendelser() throws Exception {
    assertTrue(App.grunddata.kanaler.size()>0);
    System.out.println( "kode \tnavn \tslug \tstreams");
    for (Kanal kanal : App.grunddata.kanaler) {
      if (kanal.kode.equals("P4F")) continue;
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
      String udsPKstr = AfproevGammelDrRadioBackend.hentStreng(url);
      kanal.setUdsendelserForDag(backend.parseUdsendelserForKanal(udsPKstr, kanal, dato, App.data), datoStr);
      int antalUdsendelser = 0;
      int antalUdsendelserMedPlaylister = 0;
      int antalUdsendelserMedLydstreams = 0;

      for (Udsendelse u : kanal.udsendelser) {
        url = backend.getUdsendelseStreamsUrl(u);
        Log.d(kanal.navn + ": " + u.startTidKl + " "+ u.titel+" "+u+ "    "+url);
        if (url != null) {
          JSONObject obj = new JSONObject(AfproevGammelDrRadioBackend.hentStreng(url));
          Log.d(kanal.navn + ": " + u.startTidKl + " " + u.titel + " " + obj);
          ArrayList<Lydstream> s = backend.parsStreams(obj);
          u.setStreams(s);
          if (!u.kanHøres) Log.d("Ingen lydstreams!!");
        }


        boolean gavNull = false;
        Programserie ps = i.programserieFraSlug.get(u.programserieSlug);
        if (ps == null) try {
          String str = AfproevGammelDrRadioBackend.hentStreng(backend.getProgramserieUrl(null, u.programserieSlug, 0));
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
        if (antalUdsendelser++>20) break;
      }
    }
  }


}
