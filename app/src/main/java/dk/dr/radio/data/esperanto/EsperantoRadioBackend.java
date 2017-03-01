package dk.dr.radio.data.esperanto;

import org.json.JSONException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import dk.dr.radio.data.Backend;
import dk.dr.radio.data.Grunddata;
import dk.dr.radio.data.EoGrunddata;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.FilCache;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.net.Diverse;
import dk.dr.radio.v3.R;

/**
 * Created by j on 01-03-17.
 */

public class EsperantoRadioBackend extends Backend {
  public Grunddata initGrunddata(String grunddataStr, Grunddata grunddata0) throws JSONException, IOException {
    final EoGrunddata grunddata = grunddata0 != null ? (EoGrunddata) grunddata0 : new EoGrunddata();
    grunddata.eo_parseFællesGrunddata(grunddataStr);
    grunddata.ŝarĝiKanalEmblemojn(true);
    grunddata.parseFællesGrunddata(grunddataStr);

    File fil = new File(FilCache.findLokaltFilnavn(grunddata.radioTxtUrl));
    InputStream is = fil.exists() ? new FileInputStream(fil) : App.res.openRawResource(R.raw.radio);
    grunddata.leguRadioTxt(Diverse.læsStreng(is));

    new Thread() {
      @Override
      public void run() {
        try {
          grunddata.ŝarĝiKanalEmblemojn(false);

          final String radioTxtStr = Diverse.læsStreng(new FileInputStream(FilCache.hentFil(grunddata.radioTxtUrl, false)));
          App.forgrundstråd.post(new Runnable() {
            @Override
            public void run() {
              grunddata.leguRadioTxt(radioTxtStr);
              // Povas esti ke la listo de kanaloj ŝanĝiĝis, pro tio denove kontrolu ĉu reŝarĝi bildojn
              grunddata.ŝarĝiKanalEmblemojn(true);
              App.opdaterObservatører(grunddata.observatører);
            }
          });
        } catch (Exception e) {
          Log.e(e);
        }
      }
    }.start();
    return grunddata;
  }
}
