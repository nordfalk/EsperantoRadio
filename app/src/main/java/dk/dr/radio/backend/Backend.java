package dk.dr.radio.backend;

import android.content.Context;
import android.graphics.Bitmap;

import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import dk.dr.radio.data.Favoritter;
import dk.dr.radio.data.Grunddata;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.net.volley.Netsvar;

/**
 * Created by j on 27-02-17.
 */

public abstract class Backend {
  public Favoritter favoritter = new Favoritter();
  public HashMap<String, Bitmap> kanallogo_eo = new HashMap<>();

  public abstract String getGrunddataUrl();

  public abstract InputStream getLokaleGrunddata(Context ctx) throws IOException;

  public abstract void initGrunddata(final Grunddata grunddata, String grunddataStr) throws JSONException, IOException;

  public void hentUdsendelserPåKanal(final Kanal kanal, final String datoStr, final NetsvarBehander netsvarBehander) {
    try {
      netsvarBehander.fikSvar(Netsvar.IKKE_NØDVENDIGT);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void hentKanalStreams(final NetsvarBehander netsvarBehander) {
    try {
      netsvarBehander.fikSvar(Netsvar.IKKE_NØDVENDIGT);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void hentUdsendelseStreams(NetsvarBehander netsvarBehander) {
    try {
      netsvarBehander.fikSvar(Netsvar.IKKE_NØDVENDIGT);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public abstract void hentProgramserie(final NetsvarBehander netsvarBehander);

  public void hentPlayliste(NetsvarBehander netsvarBehander)  {
    try {
      netsvarBehander.fikSvar(Netsvar.IKKE_UNDERSTØTTET);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public String getSkaleretBilledeUrl(String logo_url) {
    return logo_url; // standardimplementationen skalerer ikke billederne
  }
}
