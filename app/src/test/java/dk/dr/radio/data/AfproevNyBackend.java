package dk.dr.radio.data;


import android.app.Application;
import android.os.Build;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.util.ArrayList;

import dk.dr.radio.data.dr_v3.NyDrRadioBackend;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.ApplicationSingleton;
import dk.dr.radio.diverse.FilCache;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.v3.BuildConfig;

import static dk.dr.radio.data.AfproevBackend.hentStreng;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricGradleTestRunner.class)
@Config(packageName = "dk.dr.radio.v3", constants = BuildConfig.class, sdk = 21, application = AfproevNyBackend.TestApp.class)
public class AfproevNyBackend {

  public static class TestApp extends Application {
    static {
      App.IKKE_Android_VM = true;
      App.ÆGTE_DR = true;
    }

    @Override
    public void onCreate() {
      Log.d("onCreate " + Build.PRODUCT + Build.MODEL);
      FilCache.init(new File("/tmp/drradio-cache"));
      Log.d("arbejdsmappe = " + new File(".").getAbsolutePath());
      super.onCreate();
      ApplicationSingleton.instans = this;
      App.instans = new App();
      App.instans.init(this);
      App.fejlsøgning = true;
      App.backend = new NyDrRadioBackend();
      App.instans.initData(this);
    }
  }


  @Test
  public void tjekDirekteUdsendelser() throws Exception {
    for (Kanal k : App.grunddata.kanaler) {
      if (k.kode.equals("P4F")) continue;
      String url = App.backend.getKanalUrl(k);
      String data = hentStreng(url);
      JSONObject o = new JSONObject(data);
      ArrayList<Lydstream> s = App.backend.parsStreams(o);
      k.setStreams(s);
      assertTrue(k.findBedsteStreams(false).size() > 0);
    }
    //Log.d("DRData.instans.grunddata.kanalFraSlug=" + DRData.instans.grunddata.kanalFraSlug);
    //assertTrue(Robolectric.setupActivity(Hovedaktivitet.class) != null);
  }


}
