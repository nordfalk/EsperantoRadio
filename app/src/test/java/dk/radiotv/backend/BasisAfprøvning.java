package dk.radiotv.backend;

import android.app.Application;
import android.os.Handler;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;

import dk.dr.radio.data.Grunddata;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Programdata;
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
    App.backend = new Backend[] { backenden };
    try {
      String grunddataStr = Diverse.læsStreng(backenden.getLokaleGrunddata(ApplicationSingleton.instans));
      backenden.initGrunddata(App.grunddata = new Grunddata(), grunddataStr);
    } catch (Exception e) {
      e.printStackTrace();
    }
    App.grunddata.kanaler = backenden.kanaler;
  }
}
