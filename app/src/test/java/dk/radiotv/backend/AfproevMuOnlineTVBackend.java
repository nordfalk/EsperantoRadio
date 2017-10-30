package dk.radiotv.backend;

import android.app.Application;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

import dk.dr.radio.data.Datoformater;
import dk.dr.radio.data.Grunddata;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Lydstream;
import dk.dr.radio.data.Programdata;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.ApplicationSingleton;
import dk.dr.radio.diverse.FilCache;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.net.Diverse;
import dk.dr.radio.v3.BuildConfig;

import static org.junit.Assert.assertTrue;

@RunWith(RobolectricGradleTestRunner.class)
public class AfproevMuOnlineTVBackend extends BasisAfprøvning {
  public AfproevMuOnlineTVBackend() { super(backend = new MuOnlineTVBackend()); }

  static MuOnlineTVBackend backend;

  @Test
  public void tjekDirekteUdsendelser() throws Exception {
    assertTrue(App.grunddata.kanaler.size()>0);
    System.out.println( "kode \tnavn \tslug \tstreams");
    for (Kanal kanal : App.grunddata.kanaler) {
      System.out.println( kanal.kode + "  \t" + kanal.navn + "  \t" + kanal.slug+ " \t" + kanal.streams);
      assertTrue(kanal.findBedsteStreams(false).size() > 0);
    }
  }

  @Test
  public void tjekAktuelleUdsendelser() throws Exception {

    //System.out.println("java egenskaber "+System.getenv().toString().replace(", ","\n"));
    //System.out.println("java egenskaber "+System.getProperties().toString().replace(", ","\n"));

    Programdata i = App.data;// = new DRData();
    Date dato = new Date(System.currentTimeMillis()-1000*60*60*12);
    String datoStr = Datoformater.apiDatoFormat.format(dato);
    for (Kanal kanal : App.grunddata.kanaler) {
      if (kanal.kode.equals("P4F")) continue;
      if ("DRN".equals(kanal.kode)) continue; // ikke DR Nyheder

      String url = backend.getUdsendelserPåKanalUrl(kanal, datoStr);
      String udsPKstr = Netkald.hentStreng(url);
      kanal.setUdsendelserForDag(backend.parseUdsendelserForKanal(udsPKstr, kanal, dato, App.data), datoStr);

      for (Udsendelse u : kanal.udsendelser) {
        url = backend.getUdsendelseStreamsUrl(u);
        Log.d(kanal.navn + ": " + u.startTidKl + " "+ u.titel+" "+u+ "    "+url);
        if (url != null) {
          JSONObject obj = new JSONObject(Netkald.hentStreng(url));
          Log.d(kanal.navn + ": " + u.startTidKl + " " + u.titel + " " + obj);
          ArrayList<Lydstream> s = backend.parsStreams(obj);
          u.setStreams(s);
          if (!u.kanHøres) Log.d("Ingen lydstreams!!");
        }
      }
    }
  }


}
