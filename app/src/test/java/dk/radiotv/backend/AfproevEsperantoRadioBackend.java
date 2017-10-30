/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.radiotv.backend;

import android.app.Application;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.FileInputStream;
import java.util.Iterator;

import dk.dr.radio.data.Grunddata;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Programdata;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.data.esperanto.EoKanal;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.ApplicationSingleton;
import dk.dr.radio.diverse.FilCache;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.diverse.Udseende;
import dk.dr.radio.net.Diverse;
import dk.dr.radio.v3.BuildConfig;

import static org.junit.Assert.assertTrue;

/**
 *
 * @author j
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21, application = AfproevEsperantoRadioBackend.TestApp.class)
public class AfproevEsperantoRadioBackend {

  public static class TestApp extends Application {
    @Override
    public void onCreate() {
      App.IKKE_Android_VM = true;
      FilCache.init(new File("/tmp/testcache-esperanto"));
      Log.d("arbejdsmappe = " + new File(".").getAbsolutePath());
      super.onCreate();
      ApplicationSingleton.instans = this;
      App.instans = new App();
      App.data = new Programdata();
      App.res = getResources();
      App.assets = getAssets();
      App.pakkenavn = getPackageName();

      App.backend = new Backend[] { backend = new EsperantoRadioBackend() };
      App.data = new Programdata();
      try {
        String grunddataStr = Diverse.læsStreng(new FileInputStream("src/main/res/raw/esperantoradio_kanaloj_v8.json"));
        backend.initGrunddata(App.grunddata = new Grunddata(), grunddataStr);
      } catch (Exception e) {
        e.printStackTrace();
      }
      App.grunddata.kanaler = backend.kanaler;
      //App.fejlsøgning = true;

    }
  }
  static EsperantoRadioBackend backend;

  @Test
  public void testLogik() throws Exception {
    assertTrue(App.grunddata.kanaler.size()>0);
    Grunddata ĉefdatumoj2 = App.grunddata;
    System.out.println("===================================================================2");

    //String radioTxtStr = Diverse.læsStreng(new FileInputStream(FilCache.hentFil(ĉefdatumoj2.radioTxtUrl, true)));
    //ĉefdatumoj2.leguRadioTxt(radioTxtStr);
    System.out.println("===================================================================3");
    ŝarĝiElsendojnDeRss(ĉefdatumoj2, false);
    //ĉefdatumoj2.ŝarĝiElsendojnDeRssUrl("http://radioverda.squarespace.com/storage/audio/radioverda.xml",
    //ĉefdatumoj2.ŝarĝiElsendojnDeRssUrl("http://radioverda.squarespace.com/programoj/rss.xml",
    //    ĉefdatumoj2.kanalkodoAlKanalo.get("radioverda"), true);


    System.out.println("===================================================================");
    System.out.println("===================================================================");
    System.out.println("===================================================================");
    rezumo(ĉefdatumoj2);
    System.out.println("===================================================================");
    System.out.println("===================================================================");
    System.out.println("===================================================================");
    forprenuMalplenajnKanalojn(ĉefdatumoj2);
  }



  public void forprenuMalplenajnKanalojn(Grunddata ĉefdatumoj2) {
    for (Iterator<Kanal> ki = ĉefdatumoj2.kanaler.iterator(); ki.hasNext(); ) {
      EoKanal k = (EoKanal) ki.next();
      if (k.udsendelser.isEmpty()) {
        Log.d("============ FORPRENAS "+k.kode +", ĉar ĝi ne havas elsendojn! "+k.eo_datumFonto);
      }
    }
  }



  public void rezumo(Grunddata ĉefdatumoj2) {
    for (Kanal k0 : ĉefdatumoj2.kanaler) {
      EoKanal k = (EoKanal) k0;
      Log.d("============ "+k.kode +" ============= "+k.udsendelser.size()+" "+k.eo_datumFonto);
      int n = 0;
      for (Udsendelse e : k.udsendelser) {
//        Log.d(n++ +" "+ e.startTidKl +" "+e.titel +" "+e.sonoUrl+" "+e.beskrivelse);
        Log.d(n++ +" '"+ e.slug+"'"+" "+e.titel);
        if (n>300) {
          Log.d("...");
          break;
        }
      }
      if (k.eo_udsendelserFraRadioTxt != null && k.eo_udsendelserFraRadioTxt.size()>k.udsendelser.size()) {
        Log.rapporterFejl(new IllegalStateException(), "k.eo_udsendelserFraRadioTxt.size()>k.udsendelser.size() for "+k+": "+k.eo_udsendelserFraRadioTxt.size()+" > " +k.udsendelser.size());
      }
    }
  }


  /**
   * @return true se io estis ŝarĝita
   */
  static void ŝarĝiElsendojnDeRss(Grunddata ĉefdatumoj2, boolean nurLokajn) {
    for (Kanal k0 : ĉefdatumoj2.kanaler) {
      EoKanal k = (EoKanal) k0;
      ŝarĝiElsendojnDeRssUrl(k.eo_elsendojRssUrl, k, nurLokajn);
      //ŝarĝiElsendojnDeRssUrl(k.eo_json.optString("elsendojRssUrl1", null), k, nurLokajn);
      //ŝarĝiElsendojnDeRssUrl(k.json.optString("elsendojRssUrl2", null), k, nurLokajn);
    }
  }


  static void ŝarĝiElsendojnDeRssUrl(String elsendojRssUrl, EoKanal k, boolean nurLokajn) {
    try {
      if (elsendojRssUrl== null) return;
      String dosiero = FilCache.findLokaltFilnavn(elsendojRssUrl);
      if (nurLokajn && !new File(dosiero).exists()) return;
      FilCache.hentFil(elsendojRssUrl, false);
      Log.d(" akiris " + elsendojRssUrl);
      EoRssParsado.ŝarĝiElsendojnDeRssUrl(Diverse.læsStreng(new FileInputStream(dosiero)), k);
    } catch (Exception ex) {
      Log.e("Eraro parsante " + k.kode, ex);
    }
  }

}
