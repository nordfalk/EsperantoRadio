/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.dr.radio.data;

import android.app.Application;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.FileInputStream;

import dk.dr.radio.data.dr_v3.NyDrRadioBackend;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.FilCache;
import dk.dr.radio.net.Diverse;
import dk.dr.radio.v3.BuildConfig;

/**
 *
 * @author j
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(packageName = "dk.dr.radio.v3", constants = BuildConfig.class, sdk = 21, application = AfproevNyBackendEo.class)
public class AfproevNyBackendEo extends Application {
  {
    App.IKKE_Android_VM = true;
    App.data = new Programdata();
    App.assets = getAssets();
    App.res = getResources();
    File filcache = new File("testcache-NyDR");
    FilCache.init(filcache);
    System.out.println( "Cache-mappe er "+ filcache );
  }

  @Test
  public void testLogik() throws Exception {
    //Date.parse("Mon, 13 Aug 2012 05:25:10 +0000");
    //Date.parse("Thu, 01 Aug 2013 12:01:01 +02:00");
    //String grunddataStr = Diverse.læsStreng(new FileInputStream("src/main/res/raw/esperantoradio_kanaloj_v8.json"));
    String grunddataStr = Diverse.læsStreng(new FileInputStream("src/main/res/raw/grunddata.json"));
    App.backend = new NyDrRadioBackend();
    System.out.println("===================================================================1");
    Grunddata ĉefdatumoj2 = App.backend.initGrunddata(grunddataStr, null);
    App.grunddata = ĉefdatumoj2;
    System.out.println("===================================================================2");

  }
}
