package dk.dr.radio.data.esperanto;

import java.util.ArrayList;

import dk.dr.radio.data.Kanal;
import dk.dr.radio.data.Udsendelse;

/**
 * Created by j on 03-10-15.
 */
public class EoKanal extends Kanal {
  private static final long serialVersionUID = 1L;

  //// EO
  public String eo_hejmpaĝoEkrane;
  public String eo_hejmpaĝoButono;
  public String eo_retpoŝto;
  public Udsendelse eo_rektaElsendo;
  public String eo_emblemoUrl;
  public String eo_datumFonto;
  public ArrayList<Udsendelse> eo_udsendelserFraRadioTxt; // Provizora variablo - por kontroli ĉu ni maltrafas ion dum parsado de RSS
  public String eo_elsendojRssUrl;
  public String eo_elsendojRssUrl2;
  public boolean eo_elsendojRssIgnoruTitolon;
  public boolean eo_montruTitolojn;

  public EoKanal(EsperantoRadioBackend backend) {
    super(backend);
  }


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
}
