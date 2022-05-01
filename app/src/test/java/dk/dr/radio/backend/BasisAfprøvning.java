package dk.dr.radio.backend;

import android.app.Application;
import android.os.Handler;

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

import static org.junit.Assert.assertTrue;

/**
 * Created by j on 30-10-17.
 */

@Config(packageName = "dk.dr.radio.v3", sdk = 21, application = BasisAfprøvning.TestApp.class)
public class BasisAfprøvning {

  static {
    // Fix for https://stackoverflow.com/questions/60472729/robolectric-test-that-uses-okhttp-for-real-http-requests-throws-java-lang-nullpo/60472730#60472730
    System.setProperty("javax.net.ssl.trustStoreType", "JKS");
  }

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
      backend.hentKanalStreams(NetsvarBehander.TOM);
      assertTrue("Mangler streams for " + kanal , kanal.findBedsteStreams(false) != null);
    }
  }

  public void tjekAktuelleUdsendelser() throws Exception {
    Date dato = new Date(System.currentTimeMillis()-1000*60*60*12);
    String datoStr = Datoformater.apiDatoFormat.format(dato);
    for (Kanal kanal : backend.kanaler) {
      backend.hentUdsendelserPåKanal(kanal, datoStr, NetsvarBehander.TOM);

      int antalUdsendelser = 0;

      for (Udsendelse u : kanal.udsendelser) {
        kanal.getBackend().hentUdsendelseStreams(NetsvarBehander.TOM);
        Programserie ps = App.data.programserieFraSlug.get(u.programserieSlug);
        backend.hentProgramserie(NetsvarBehander.TOM);
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
      backend.hentProgramserie(NetsvarBehander.TOM);
      ArrayList<Udsendelse> udsendelser = ps.getUdsendelser();

      System.out.println(ps.slug + " " + ps.antalUdsendelser + " " + udsendelser.size());
      assertTrue(ps.slug + " har færre udsendelser end påstået:\n"+ps.titel, ps.antalUdsendelser>= udsendelser.size());
      samletAntalUdsendelser += udsendelser.size();
    }
    assertTrue("Kun "+samletAntalUdsendelser+" udsendelser!", samletAntalUdsendelser>10);
  }
}
