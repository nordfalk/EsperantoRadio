package dk.radiotv.backend;

import android.app.Application;
import android.os.Handler;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

import dk.dr.radio.data.Datoformater;
import dk.dr.radio.data.Grunddata;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Programdata;
import dk.dr.radio.data.Programserie;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.ApplicationSingleton;
import dk.dr.radio.diverse.FilCache;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.net.Diverse;
import dk.dr.radio.v3.BuildConfig;

import static org.junit.Assert.assertTrue;

/**
 * Created by j on 30-10-17.
 */

@Config(packageName = "dk.dr.radio.v3", constants = BuildConfig.class, sdk = 21, application = AfproevMuOnlineRadioBackend.TestApp.class)
public class BasisAfprøvning {

  final Backend backend;

  public static class TestApp extends Application {
    @Override
    public void onCreate() {
      App.IKKE_Android_VM = true;
      FilCache.init(new File("/tmp/drradio-cache"));
      Log.d("arbejdsmappe = " + new File(".").getAbsolutePath());
      super.onCreate();
      ApplicationSingleton.instans = this;
      net.danlew.android.joda.JodaTimeAndroid.init(this);
      App.instans = new App();
      App.res = getResources();
      App.assets = getAssets();
      App.forgrundstråd = new Handler();
      App.pakkenavn = getPackageName();
      App.data = new Programdata();
    }
  }

  public BasisAfprøvning(Backend backenden) {
    backend = backenden;
    App.backend = new Backend[] { backenden };
    try {
      String grunddataStr = Diverse.læsStreng(backenden.getLokaleGrunddata(ApplicationSingleton.instans));
      backenden.initGrunddata(App.grunddata = new Grunddata(), grunddataStr);
    } catch (Exception e) {
      e.printStackTrace();
    }
    App.grunddata.kanaler = backenden.kanaler;
  }



  public void tjekDirekteUdsendelser() throws Exception {
    assertTrue(backend.kanaler.size()>0);
    System.out.println( "kode \tnavn \tslug \tstreams");
    for (Kanal kanal : backend.kanaler) {
      if (kanal.kode.equals("P4F")) continue;
      System.out.println( kanal.kode + "  \t" + kanal.navn + "  \t" + kanal.slug+ " \t" + kanal.streams);
      backend.hentKanalStreams(kanal, null, NetsvarBehander.TOM);
      assertTrue("Mangler streams for " + kanal , kanal.findBedsteStreams(false).size() > 0);
    }
  }

  public void tjekAktuelleUdsendelser() throws Exception {
    Date dato = new Date(System.currentTimeMillis()-1000*60*60*12);
    String datoStr = Datoformater.apiDatoFormat.format(dato);
    for (Kanal kanal : backend.kanaler) {
      if (kanal.kode.equals("P4F")) continue;
      if ("DRN".equals(kanal.kode)) continue; // ikke DR Nyheder
      if ("RAM".equals(kanal.kode)) continue; // ikke Ramasjang

      backend.hentUdsendelserPåKanal(this, kanal, dato, datoStr, NetsvarBehander.TOM);

      int antalUdsendelser = 0;

      for (Udsendelse u : kanal.udsendelser) {
        kanal.getBackend().hentUdsendelseStreams(u, NetsvarBehander.TOM);
        Programserie ps = App.data.programserieFraSlug.get(u.programserieSlug);
        backend.hentProgramserie(ps, u.programserieSlug, kanal, 0, NetsvarBehander.TOM);
        if (antalUdsendelser++>20) break;
      }

      assertTrue(kanal + " har antalUdsendelser "+antalUdsendelser, antalUdsendelser>5);
    }
  }

  public void tjek_hent_a_til_å() throws Exception {
    System.out.println("tjek_hent_a_til_å");
    backend.hentAlleProgramserierAtilÅ(NetsvarBehander.TOM);
    ArrayList<Programserie> liste = App.data.programserierAtilÅ.getListe();
    System.out.println("tjek_hent_a_til_å liste="+liste.size());
    assertTrue(liste.size()>0);

    int samletAntalUdsendelser = 0;

    // Tjek kun nummer 50 til nummer 100
    for (Programserie ps : liste.subList(50, 65)) {
      backend.hentProgramserie(ps, ps.slug, null, 0, NetsvarBehander.TOM);
      ArrayList<Udsendelse> udsendelser = ps.getUdsendelser();

      System.out.println(ps.slug + " " + ps.antalUdsendelser + " " + udsendelser.size());
      assertTrue(ps.slug + " har færre udsendelser end påstået:\n"+ps.titel, ps.antalUdsendelser>= udsendelser.size());
      samletAntalUdsendelser += udsendelser.size();
    }
    assertTrue("Kun "+samletAntalUdsendelser+" udsendelser!", samletAntalUdsendelser>10);
  }
}
