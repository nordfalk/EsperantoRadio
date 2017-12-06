package dk.radiotv.backend;

import android.content.Context;

import com.android.volley.Request;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import dk.dr.radio.data.Favoritter;

import dk.dr.radio.data.Grunddata;
import dk.dr.radio.data.Indslaglisteelement;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Playlisteelement;
import dk.dr.radio.data.Programdata;
import dk.dr.radio.data.ProgramdataForBackend;
import dk.dr.radio.data.Programserie;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.data.esperanto.EoFavoritter;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.diverse.Udseende;
import dk.dr.radio.net.volley.Netsvar;

/**
 * Created by j on 27-02-17.
 */

public abstract class Backend {
  public List<Kanal> kanaler = new ArrayList<>();
  public ProgramdataForBackend data = new ProgramdataForBackend();
  public Favoritter favoritter = new Favoritter(this);

  protected void _ikkeImplementeret__UBRUGT() {
    Exception x = new Exception("Ikke implementeret i " + getClass().getName());
    StackTraceElement[] ss = x.getStackTrace();
    System.err.println(x);
    for (int i=1; i<5; i++)
      System.err.println("\tat " + ss[i]);
  }

  public abstract String getGrunddataUrl();

  public abstract InputStream getLokaleGrunddata(Context ctx) throws IOException;

  public abstract void initGrunddata(final Grunddata grunddata, String grunddataStr) throws JSONException, IOException;

  public void hentUdsendelserPåKanal(Object kalder, final Kanal kanal, final Date dato, final String datoStr, final NetsvarBehander netsvarBehander) {
    try {
      netsvarBehander.fikSvar(Netsvar.IKKE_NØDVENDIGT);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void hentKanalStreams(final Kanal kanal, Request.Priority priority, final NetsvarBehander netsvarBehander) {
    try {
      netsvarBehander.fikSvar(Netsvar.IKKE_NØDVENDIGT);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void hentUdsendelseStreams(Udsendelse udsendelse, NetsvarBehander netsvarBehander) {
    try {
      netsvarBehander.fikSvar(Netsvar.IKKE_NØDVENDIGT);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public abstract void hentProgramserie(final Programserie programserie, final String programserieSlug, final Kanal kanal, final int offset, final NetsvarBehander netsvarBehander);

  public void hentPlayliste(Udsendelse udsendelse, NetsvarBehander netsvarBehander)  {
    try {
      netsvarBehander.fikSvar(Netsvar.IKKE_UNDERSTØTTET);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public String getSkaleretBilledeUrl(String logo_url, int bredde, int højde) {
    return logo_url; // standardimplementationen skalerer ikke billederne
  }

  public void hentAlleProgramserierAtilÅ(NetsvarBehander netsvarBehander) {
    try {
      netsvarBehander.fikSvar(Netsvar.IKKE_UNDERSTØTTET);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
