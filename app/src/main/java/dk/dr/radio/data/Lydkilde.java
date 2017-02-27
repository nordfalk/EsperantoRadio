package dk.dr.radio.data;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dk.dr.radio.data.dr_v3.Backend;
import dk.dr.radio.data.dr_v3.DRJson;
import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;
import dk.dr.radio.net.Netvaerksstatus;

/**
 * En lydkilde der kan spilles af afspilleren
 */
public abstract class Lydkilde implements Serializable {
  // Fix for https://www.bugsense.com/dashboard/project/cd78aa05/errors/1415558087
  // - at proguard obfuskering havde
  // Se også http://stackoverflow.com/questions/16210831/serialization-deserialization-proguard
  private static final long serialVersionUID = 6061992240626233386L;

  public String urn;   // Bemærk - kan være tom!
  public String slug;  // Bemærk - kan være tom!
  transient ArrayList<Lydstream> streams;
  public transient Lydstream hentetStream;
  public static final String INDST_lydformat = "lydformat2";

  @Override
  public boolean equals(Object o) {
    if (o == null) return false;
    if (o instanceof Lydkilde && slug != null) return slug.equals(((Lydkilde) o).slug);
    return super.equals(o);
  }

  public void nulstilForetrukkenStream() {
    if (streams == null) return;
    for (Lydstream s : streams) s.foretrukken = false;
  }


  public List<Lydstream> findBedsteStreams(boolean tilHentning) {
    ArrayList<Lydstream> kandidater = new ArrayList<Lydstream>();
    if (hentetStream != null && new File(hentetStream.url).canRead()) kandidater.add(hentetStream);
    if (streams == null) return kandidater;

    //Bedst bedst = new Bedst();
    String ønsketkvalitet = App.prefs.getString("lydkvalitet", "auto");
    String ønsketformat = App.prefs.getString(INDST_lydformat, "auto");

    Lydstream sxxx = null;
      næste_stream:
    for (Lydstream s : streams)
      try {
        sxxx = s;
        int score = 100;
        switch (s.type) {
          case HLS_fra_Akamai:
            if (tilHentning) continue næste_stream;
            if ("hls".equals(ønsketformat)) score += 40;
            if ("auto".equals(ønsketformat)) score += 20;
            break; // bryd ud af switch
          case HTTP:
            if (tilHentning) score += 20;
            if ("shoutcast".equals(ønsketformat)) score += 40;
            break; // bryd ud af switch
          case RTSP:
            if (tilHentning) continue næste_stream;
            score -= 40; // RTSP udfases og har en enorm ventetid, foretræk andre
          case Shoutcast:
            if (tilHentning) continue næste_stream;
            if ("shoutcast".equals(ønsketformat)) score += 40;
            break; // bryd ud af switch
          default:
            continue næste_stream;
        }
        switch (s.kvalitet) {
          case High:
            if ("høj".equals(ønsketkvalitet)) score += 10;
            if ("auto".equals(ønsketkvalitet) && App.netværk.status == Netvaerksstatus.Status.WIFI) score += 10;
            break;
          case Low:
          case Medium:
            if ("standard".equals(ønsketkvalitet)) score += 10;
            if ("auto".equals(ønsketkvalitet) && App.netværk.status == Netvaerksstatus.Status.MOBIL) score += 10;
            break;
          case Variable:
            if ("auto".equals(ønsketkvalitet)) score += 10;
            break;
        }
        if (s.foretrukken) score += 1000;
        if ("mp3".equals(s.format)) score += 10; // mp3 er mere pålideligt end mp4
        s.score = score;
        kandidater.add(s);
      } catch (Exception e) {
        Log.rapporterFejl(e, " ls=" + sxxx);
      }

    Collections.sort(kandidater);
    if (App.fejlsøgning) Log.d("findBedsteStreams " + kandidater);
    return kandidater;
  }

  public abstract String getStreamsUrl();

  public abstract Kanal getKanal();

  public abstract boolean erDirekte();

  public abstract Udsendelse getUdsendelse();

  public abstract String getNavn();

  public void setStreams(JSONObject o) throws JSONException {
    streams = Backend.parsStreams(o.getJSONArray(DRJson.Streams.name()));
  }

  public void setStreams(String json) throws JSONException {
    setStreams(new JSONObject(json));
  }

  public boolean harStreams() {
    return streams != null || hentetStream != null;
  }

  @Override
  public String toString() {
    return slug + " str=" + streams;
  }
}
