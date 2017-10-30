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

import dk.dr.radio.data.Datoformater;
import dk.dr.radio.data.Grunddata;
import dk.dr.radio.data.Indslaglisteelement;
import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Lydstream;
import dk.dr.radio.data.Playlisteelement;
import dk.dr.radio.data.Programdata;
import dk.dr.radio.data.Programserie;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.net.volley.Netsvar;

/**
 * Created by j on 27-02-17.
 */

public abstract class Backend {
  public List<Kanal> kanaler = new ArrayList<>();

  protected abstract void ikkeImplementeret();
  protected void _ikkeImplementeret() {
    Exception x = new Exception("Ikke implementeret i " + getClass().getName());
    StackTraceElement[] ss = x.getStackTrace();
    System.err.println(x);
    for (int i=1; i<5; i++)
      System.err.println("\tat " + ss[i]);
  }

  public abstract String getGrunddataUrl();

  public abstract InputStream getLokaleGrunddata(Context ctx) throws IOException;


  public abstract void initGrunddata(final Grunddata grunddata, String grunddataStr) throws JSONException, IOException;

  public Programserie parsProgramserie(JSONObject programserieJson, Programserie programserie) throws JSONException{
    ikkeImplementeret();
    return programserie;
  }

  public String getProgramserieUrl(Programserie programserie, String programserieSlug, int offset) {
    ikkeImplementeret();
    return null;
  }

  public ArrayList<Udsendelse> parseUdsendelserForProgramserie(JSONArray jsonArray, Kanal kanal, Programdata data) throws JSONException {
    ikkeImplementeret();
    return null;
  }

  String getUdsendelserPåKanalUrl(Kanal kanal, String datoStr) {
    ikkeImplementeret();
    return null;
  }

  public String getPlaylisteUrl(Udsendelse udsendelse) {
    ikkeImplementeret();
    return null;
  }

  public ArrayList<Playlisteelement> parsePlayliste(Udsendelse udsendelse, JSONArray jsonArray) throws JSONException {
    ikkeImplementeret();
    return null;
  }

  String getUdsendelseUrlFraSlug(String udsendelseSlug) {
    ikkeImplementeret();
    return null;
  }

  ArrayList<Udsendelse> parseUdsendelserForKanal(String jsonStr, Kanal kanal, Date dato, Programdata data) throws JSONException {
    ikkeImplementeret();
    return null;
  }

  ArrayList<Indslaglisteelement> parsIndslag(JSONObject jsonObj) throws JSONException {
    ikkeImplementeret();
    return null;
  }


  String getKanalStreamsUrl(Kanal kanal) {
    ikkeImplementeret();
    return null;
  }

  String getUdsendelseStreamsUrl(Udsendelse udsendelse) {
    ikkeImplementeret();
    return null;
  }

  ArrayList<Lydstream> parsStreams(JSONObject jsonobj) throws JSONException {
    ikkeImplementeret();
    return null;
  }

  public void hentUdsendelserPåKanal(Object kalder, final Kanal kanal, final Date dato, final NetsvarBehander netsvarBehander) {
    final String datoStr = Datoformater.apiDatoFormat.format(dato);
    if (kanal.harUdsendelserForDag(datoStr)) try { // brug værdier i RAMen
      netsvarBehander.fikSvar(new Netsvar(null, null, true, false));
      return;
    } catch (Exception e) { Log.rapporterFejl(e); }

    App.netkald.kald(kalder, getUdsendelserPåKanalUrl(kanal, datoStr), new NetsvarBehander() {
      @Override
      public void fikSvar(Netsvar s) throws Exception {
        Log.d(kanal + "Backend hentSendeplanForDag fikSvar " + s.toString());
        if (!s.uændret && s.json != null) {
          kanal.setUdsendelserForDag(parseUdsendelserForKanal(s.json, kanal, dato, App.data), datoStr);
        }
        netsvarBehander.fikSvar(s);
      }
    });
  }

  public void hentKanalStreams(final Kanal kanal, Request.Priority priority, final NetsvarBehander netsvarBehander) {
    App.netkald.kald(null, getKanalStreamsUrl(kanal), priority, new NetsvarBehander() {
      @Override
      public void fikSvar(Netsvar s) throws Exception {
        if (s.json != null && !s.uændret) {
          JSONObject o = new JSONObject(s.json);
          kanal.setStreams(parsStreams(o));
          Log.d("Streams parset for = " + s.url);//Data opdateret
        }
        netsvarBehander.fikSvar(s);
      }
    });
  }


  public void hentUdsendelseStreams(final Udsendelse udsendelse, final NetsvarBehander netsvarBehander) {
    App.netkald.kald(null, getUdsendelseStreamsUrl(udsendelse), new NetsvarBehander() {
      @Override
      public void fikSvar(Netsvar s) throws Exception {
        if (s.json != null && !s.uændret) {
          JSONObject o = new JSONObject(s.json);
          udsendelse.setStreams(parsStreams(o));
          Log.d("Streams parset for = " + s.url);//Data opdateret
        }
        netsvarBehander.fikSvar(s);
      }
    });
  }

  public void hentProgramserie(final Programserie programserie, final String programserieSlug, final Kanal kanal, final int offset, final NetsvarBehander netsvarBehander) {
    App.netkald.kald(this, getProgramserieUrl(programserie, programserieSlug, offset), new NetsvarBehander() {
      @Override
      public void fikSvar(Netsvar s) throws Exception {
        Log.d("fikSvar(" + s.fraCache + " " + s.url);
        if (s.json != null && !s.uændret) {
          JSONObject data = new JSONObject(s.json);
          Programserie ps = programserie;
          if (offset == 0) {
            ps = parsProgramserie(data, ps);
            App.data.programserieFraSlug.put(programserieSlug, ps);
          }
          JSONArray prg = data.getJSONArray(DRJson.Programs.name());
          ArrayList<Udsendelse> udsendelser = parseUdsendelserForProgramserie(prg, kanal, App.data);
          ps.tilføjUdsendelser(offset, udsendelser);
        }
        netsvarBehander.fikSvar(s);
      }
    });
  }
}
