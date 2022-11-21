package dk.dr.radio.backend;

import android.app.Application;
import android.os.Handler;

import org.robolectric.annotation.Config;

import java.io.File;

import dk.dr.radio.data.Grunddata;
import dk.dr.radio.data.Programdata;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.ApplicationSingleton;
import dk.dr.radio.net.FilCache;
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
    App.backend = backenden;
    try {
      String grunddataStr = Diverse.læsStreng(backenden.getLokaleGrunddata(ApplicationSingleton.instans));
      backenden.initGrunddata(App.grunddata = new Grunddata(), grunddataStr);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
