package dk.dr.radio.data;

import java.util.Date;

/**
 * Repræsenterer et element i en spilleliste
 * Created by j on 28-01-14.
 */
public class Playlisteelement {
  public String titel;
  public String kunstner;
  public String billedeUrl;
  public Date startTid;
  public String startTidKl;
  public int offsetMs;

  @Override
  public String toString() {
    return startTidKl + "/ofs="+ (offsetMs/1000/60)+ "min"+ "/" + kunstner + "/" + titel;
  }
}
