package dk.dr.radio.afspilning;

import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Lydkilde;
import dk.dr.radio.data.Lydstream;
import dk.dr.radio.data.Udsendelse;

/**
 * Speciel lydkilde der bruges som erstatning når en lydkilde ikke kan spilles og der SKAL spilles en lyd
 * Created by j on 18-09-14.
 */
public class AlarmLydkilde extends Lydkilde {
  private static final long serialVersionUID = 1L;
  private final Lydkilde orgLydkilde;

  public AlarmLydkilde(String alarmUri, Lydkilde orgLydkilde) {
    hentetStream = new Lydstream();
    hentetStream.url = alarmUri;
    this.orgLydkilde = orgLydkilde;
  }

  @Override
  public String getStreamsUrl() {
    return null;
  }

  @Override
  public Kanal getKanal() {
    return orgLydkilde.getKanal();
  }

  // Returnerer true så lyden startes forfra igen og igen
  @Override
  public boolean erDirekte() {
    return true;
  }

  @Override
  public Udsendelse getUdsendelse() {
    return orgLydkilde.getUdsendelse();
  }

  @Override
  public String getNavn() {
    return "alarm";
  }
}
