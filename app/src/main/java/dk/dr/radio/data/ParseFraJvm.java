package dk.dr.radio.data;

import java.io.File;

import dk.dr.radio.backend.Backend;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.ApplicationSingleton;
import dk.dr.radio.net.FilCache;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.net.Diverse;

public class ParseFraJvm {
  public static void main(String[] args) {
    App.IKKE_Android_VM = true;
    FilCache.init(new File("/tmp/drradio-cache"));
    Log.d("arbejdsmappe = " + new File(".").getAbsolutePath());
    //ApplicationSingleton.instans = new ApplicationSingleton();
    //net.danlew.android.joda.JodaTimeAndroid.init(ApplicationSingleton.instans);
    //App.instans = new App();
    //App.res = ApplicationSingleton.instans.getResources();
    //App.forgrundstråd = new Handler();
    App.pakkenavn = "getPackageName()";
    App.data = new Programdata();
    App.backend = new Backend();
    try {
      String grunddataStr = Diverse.læsStreng(App.backend.getLokaleGrunddata(ApplicationSingleton.instans));
      App.backend.initGrunddata(App.grunddata = new Grunddata(), grunddataStr);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
