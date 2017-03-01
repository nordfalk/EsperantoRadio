/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.dr.radio.data;

import android.app.Application;
import android.os.Build;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;

import dk.dr.radio.data.Programdata;
import dk.dr.radio.data.dr_v3.DRBackendTidsformater;
import dk.dr.radio.data.esperanto.EoDiverse;
import dk.dr.radio.data.esperanto.EoRssParsado;
import dk.dr.radio.data.Grunddata;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.ApplicationSingleton;
import dk.dr.radio.diverse.FilCache;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.net.Diverse;
import dk.dr.radio.v3.BuildConfig;

/**
 *
 * @author j
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21, application = EoElproviEsperantoRadioLogikon.EoTestApp.class)
public class EoElproviEsperantoRadioLogikon {
  public static class EoTestApp extends Application {
    static {
      App.IKKE_Android_VM = true;
      App.data = new Programdata();
      File filcache = new File("testcache-esperanto");
      FilCache.init(filcache);
      System.out.println( "Cache-mappe er "+ filcache );
    }
  }

  @Test
  public void testLogik() throws Exception {
    //Date.parse("Mon, 13 Aug 2012 05:25:10 +0000");
    //Date.parse("Thu, 01 Aug 2013 12:01:01 +02:00");
    String grunddata = Diverse.læsStreng(new FileInputStream("src/main/res/raw-eo/esperantoradio_kanaloj_v8.json"));
    System.out.println("===================================================================1");
    Grunddata ĉefdatumoj2 = App.grunddata = new Grunddata();
    System.out.println("===================================================================2");
    ĉefdatumoj2.eo_parseFællesGrunddata(grunddata);
    App.grunddata.parseFællesGrunddata(grunddata);

    System.out.println("===================================================================3");
    String radioTxtStr = Diverse.læsStreng(new FileInputStream(FilCache.hentFil(ĉefdatumoj2.radioTxtUrl, true)));
    ĉefdatumoj2.leguRadioTxt(radioTxtStr);
    System.out.println("===================================================================3");
    ŝarĝiElsendojnDeRss(ĉefdatumoj2, false);
    //ĉefdatumoj2.ŝarĝiElsendojnDeRssUrl("http://radioverda.squarespace.com/storage/audio/radioverda.xml",
    //ĉefdatumoj2.ŝarĝiElsendojnDeRssUrl("http://radioverda.squarespace.com/programoj/rss.xml",
    //    ĉefdatumoj2.kanalkodoAlKanalo.get("radioverda"), true);


    System.out.println("===================================================================");
    System.out.println("===================================================================");
    System.out.println("===================================================================");
    ĉefdatumoj2.rezumo();
    System.out.println("===================================================================");
    System.out.println("===================================================================");
    System.out.println("===================================================================");
    ĉefdatumoj2.forprenuMalplenajnKanalojn();
  }


  /**
   * @return true se io estis ŝarĝita
   */
  static void ŝarĝiElsendojnDeRss(Grunddata ĉefdatumoj2, boolean nurLokajn) {
    for (Kanal k : ĉefdatumoj2.kanaler) {
      ŝarĝiElsendojnDeRssUrl(k.eo_elsendojRssUrl, k, nurLokajn);
      //ŝarĝiElsendojnDeRssUrl(k.eo_json.optString("elsendojRssUrl1", null), k, nurLokajn);
      //ŝarĝiElsendojnDeRssUrl(k.json.optString("elsendojRssUrl2", null), k, nurLokajn);
    }
  }


  static void ŝarĝiElsendojnDeRssUrl(String elsendojRssUrl, Kanal k, boolean nurLokajn) {
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
