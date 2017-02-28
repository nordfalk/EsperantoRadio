package dk.dr.radio.data.esperanto;

import org.json.JSONException;

import java.util.ArrayList;

import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Lydstream;
import dk.dr.radio.data.Udsendelse;

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

  public String getStreamsUrl() {
    return eo_elsendojRssUrl;
  }

  @Override
  public void setStreams(ArrayList<Lydstream> str)  {
    throw new IllegalArgumentException("Ne rajtas voki, la rezulto ne estas JSON");
  }
}
