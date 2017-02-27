package dk.dr.radio.data.esperanto;

import org.json.JSONException;
import org.json.JSONObject;

import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Udsendelse;
import dk.dr.radio.diverse.Log;

/**
 * Created by j on 03-10-15.
 */
public class EoKanal extends Kanal {
  private static final long serialVersionUID = 1L;
  /** Finder den aktuelle udsendelse på kanalen */
  @Override
  public Udsendelse getUdsendelse() {
    if (udsendelser==null || udsendelser.size() == 0) return null;
    return udsendelser.get(0);
  }

  @Override
  public boolean harStreams() {
    return udsendelser.size()>0;
  }

  @Override
  public boolean erDirekte() {
    return eo_rektaElsendo!=null;
  }

  @Override
  public String getStreamsUrl() {
    return eo_elsendojRssUrl;
  }

  @Override
  public void setStreams(JSONObject o) throws JSONException {
    throw new IllegalArgumentException("Ne rajtas voki, la rezulto ne estas JSON");
  }

  @Override
  public void setStreams(String json) throws JSONException {
    Log.d("eo RSS QQ por " + this + " =" + json);
    EoRssParsado.ŝarĝiElsendojnDeRssUrl(json, this);
  }
}
